(ns jaggr.jenkins
  (:require [org.httpkit.client :as http]
            [clojure.data.json :as json]
            [omniconf.core :as config]
            [clojure.core.async :refer [>! >!! <! <!! alts!! timeout close! go-loop chan into to-chan]])
  (:import (java.util.concurrent TimeoutException)))


;; returns a channel with the results of the (unary) function fn applied to the values taken from the in channel
(defn- map-chan [fn in]
  (let [out (chan)]
    (go-loop [val (<! in)]
      (if val (do
                (>! out (fn val))
                (recur (<! in)))
              (close! out)))
    out))

;; calls the Jenkins JSON api for a given url (must end with /)
;; optional url parameters may be provided (e.g. "tree=key[subkey1,subkey2]" to get a filtered response)
;; returns the body of the response as a map with keys converted to clojure keywords
(defn- get-from-jenkins [base-url params]
  (let [url (str base-url "api/json?" params)
        user (config/get :user)
        user-token (config/get :user-token)
        options (when (and user user-token) {:basic-auth [user user-token] :keepalive -1})
        response @(http/get url options)]
    (json/read-str (:body response) :key-fn keyword)))


;; gets the (pre-filtered) jobs REST resource for the globally configured base-url
(defn- get-jobs-rsrc []
  (:jobs
    (get-from-jenkins
      (config/get :base-url)
      "tree=jobs[name,color,url]")))


;; returns the color of the job if it indicates a failure status
;; or nil otherwise. can be used as a predicate
(defn- failure-indicating-color [job-rsrc]
  (#{"red" "red_anime" "yellow" "yellow_anime" "aborted" "aborted_anime"} (:color job-rsrc)))


;; gets all failed jobs from the globally configured base-url
(defn- get-failed-jobs-rsrc []
  (filter failure-indicating-color (get-jobs-rsrc)))


;; gets the url of the last build from jenkins for a given job-REST-resource
(defn- get-last-completed-build-url [job-rsrc]
  (get-in
    (get-from-jenkins (:url job-rsrc) "tree=lastCompletedBuild[url]")
    [:lastCompletedBuild :url]))


;; reads from a channel with job REST resources,
;; adds the url of the last build to each job and returns it on a channel
(defn- add-last-completed-build-url-chan [job-rsrc-chan]
  (map-chan
    #(assoc %1 :lastCompletedBuildUrl (get-last-completed-build-url %1))
    job-rsrc-chan))


;; gets the claim info for a job resource and throws away everything else
(defn- get-claim-info [last-completed-build-url]
  (->>
    (get-from-jenkins last-completed-build-url "tree=actions[claimed,claimedBy,reason]")
    (:actions)
    (filter not-empty)
    (first)))


;; reads from a channel with job REST resources,
;; each job resource must have a :last-build-url
;; adds information on it's last build's claim state to a job resource
;; returns the enriched job resources on a channel
(defn- add-claim-info-chan [job-rsrc-chan]
  (map-chan
    #(merge %1 (get-claim-info (:lastCompletedBuildUrl %1)))
    job-rsrc-chan))


;; waits until a channel closes and retrieve all its values as a collection,
;; throws an exception when the refresh interval is exceeded
(defn- drain-or-timeout [c]
  (let [refresh-rate (config/get :refresh-rate)
        [vals _] (alts!!
                   [(into '() c)
                    (timeout (* 1000 refresh-rate))])]
    (when-not vals
      (throw (TimeoutException.
               (str "Did not retrieve all required job data from the Jenkins API within " refresh-rate " seconds!"))))
    vals))


(defn get-failed-jobs []
  "fetches all failed jobs from jenkins and returns a map that groups them in three classes:
   :claimed, :unclaimed and :unclaimable. For each job of each class, a map is returned with
   :name, :lastCompletedBuildUrl, :claimed, :claimedBy and :reason
   throws an exception when the screen refresh time is exceeded before all jobs have been processed."
  (->> (get-failed-jobs-rsrc)
       (to-chan)
       (add-last-completed-build-url-chan)
       (add-claim-info-chan)
       (drain-or-timeout)
       (group-by
         #(cond
           (true? (:claimed %)) :claimed
           (false? (:claimed %)) :unclaimed
           :else :unclaimable))))
