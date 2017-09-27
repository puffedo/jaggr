(ns jaggr.jenkins
  (:require [org.httpkit.client :as http]
            [clojure.data.json :as json]
            [clojure.set :refer [subset?]]
            [omniconf.core :as config]
            [clojure.core.async :refer [>! >!! <! <!! alts!! timeout close! go-loop chan into to-chan pipe]])
  (:import (java.util.concurrent TimeoutException)))


;; returns a channel with the results of the (unary) function fn applied to the values taken from the in channel
(defn- map-chan [fn in]
    (pipe in (chan 100 (map fn))))


;; calls the Jenkins JSON api for a given url (must end with /)
;; optional url parameters may be provided (e.g. "tree=key[subkey1,subkey2]" to get a filtered response)
;; returns the body of the response as a map with keys converted to clojure keywords
(defn- get-from-jenkins [base-url params]
  (let [url (str base-url "api/json?" params)
        user (config/get :user)
        user-token (config/get :user-token)
        options (when
                  (and user user-token)
                  {:basic-auth [user user-token] :keepalive -1 :insecure? true})
        response @(http/get url options)]
    (json/read-str (:body response) :key-fn keyword)))


;; http-GETs the jobs REST resource from the globally configured base-url
;; returns the jobs (name, color, url) extracted from this REST resource
(defn- get-jobs []
  (:jobs
    (get-from-jenkins
      (config/get :base-url)
      "tree=jobs[name,color,url]")))


;; returns the color of the job if it indicates a failure status
;; or nil otherwise. can be used as a predicate
(defn- failure-indicating-color [job]
  (#{"red" "red_anime" "yellow" "yellow_anime" "aborted" "aborted_anime"} (:color job)))


;; http-GETs all failed jobs from the globally configured base-url
(defn- get-failed-jobs []
  (filter failure-indicating-color (get-jobs)))


;; http-GETS the job-REST-resource for the provided job
;; returns the url of the last build from this REST resource
(defn- get-last-completed-build-url [job]
  (get-in
    (get-from-jenkins (:url job) "tree=lastCompletedBuild[url]")
    [:lastCompletedBuild :url]))


;; takes a jobs and adds the url of the last build to each job
(defn- add-last-completed-build-url [job]
    (assoc job :lastCompletedBuildUrl (get-last-completed-build-url job)))


;; http-GETs the build-REST-resource for the provided URL
;; returns the claim info from this REST resource
(defn- get-claim-info [build-url]
  (select-keys
    (->>
      (get-from-jenkins build-url "tree=actions[claimed,claimedBy,reason]")
      (:actions)
      (filter #(subset? #{:claimed :claimedBy :reason} (set (keys %1))))
      (first))
    [:claimed :claimedBy :reason]))


;; takes a job that must have a :last-build-url
;; adds information on it's last build's claim state the each job
(defn- add-claim-info [job]
    (merge job (get-claim-info (:lastCompletedBuildUrl job))))

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


(defn failed-jobs []
  "fetches all failed jobs and their last broken builds from jenkins,
   returns a map that groups them in three classes: :claimed, :unclaimed and :unclaimable.
   For each job, a map is returned with :name, :lastCompletedBuildUrl, :claimed, :claimedBy and :reason
   throws an exception when the screen refresh time is exceeded before all jobs have been processed."
  (->> (get-failed-jobs)
       (to-chan)
       (map-chan add-last-completed-build-url)
       (map-chan add-claim-info)
       (drain-or-timeout)
       (group-by
         #(cond
            (true? (:claimed %)) :claimed
            (false? (:claimed %)) :unclaimed
            :else :unclaimable))))
