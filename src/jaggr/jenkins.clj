(ns jaggr.jenkins
  (:require [org.httpkit.client :as http]
            [clojure.data.json :as json]
            [omniconf.core :as config]
            [clojure.core.async :refer [>! >!! <! <!! alts!! timeout close! go-loop chan into to-chan]])
  (:import (java.util.concurrent TimeoutException)))


;; calls the Jenkins JSON api for a given url (must end with /),
;; returns the body of the response as JSON with keys converted to clojure keywords
(defn- get-from-jenkins [base-url params]
  (json/read-str
    (:body
      @(http/get
         (str base-url "api/json?" params)
         {:basic-auth [(config/get :user) (config/get :user-token)]
          :keepalive  -1}))
    :key-fn keyword))


;; gets the jobs REST resource for the globally configured base-url
(defn- get-jobs-rsrc []
  (:jobs
    (get-from-jenkins
      (config/get :base-url)
      "tree=jobs[name,color,url]")))


;; returns red or yellow if this is the actual color of the job
;; or nil otherwise. can be used as a predicate
(defn- red-or-yellow [job-rsrc]
  (#{"red" "yellow"} (:color job-rsrc)))


;; gets all failed (red or yellow) jobs from the globally configured base-url
(defn- get-failed-jobs-rsrc []
  (filter red-or-yellow (get-jobs-rsrc)))


;; gets the url of the last build from jenkins for a given job-REST-resource
(defn- get-last-build-url [job-rsrc]
  (get-in
    (get-from-jenkins (:url job-rsrc) "tree=lastBuild[url]")
    [:lastBuild :url]))


;; gets the url of the last build from jenkins for a given job-REST-resource
(defn- add-last-build-url-chan [job-rsrc-chan]
  (let [out (chan)]
    (go-loop [job-rsrc (<! job-rsrc-chan)]
      (if job-rsrc
        (do
          (>! out (assoc job-rsrc :last-build-url (get-last-build-url job-rsrc)))
          (recur (<! job-rsrc-chan)))
        (close! out)))
    out))


;; gets the claim info for a job resource and throws away everything else
(defn- get-claim-info [last-build-url]
  (->>
    (get-from-jenkins last-build-url "tree=actions[claimed,claimedBy,reason]")
    (:actions)
    (filter not-empty)
    (first)))


;; adds information on it's last build's claim state to a job resource
;; the job resource must have a :last-build-url
(defn- add-claim-info-chan [job-rsrc-chan]
  (let [out (chan)]
    (go-loop [job-rsrc (<! job-rsrc-chan)]
      (if job-rsrc
        (do
          (->>
            (get-claim-info (:last-build-url job-rsrc))
            (merge job-rsrc)
            (>! out))
          (recur (<! job-rsrc-chan)))
        (close! out)))
    out))


;; wait until a channel closes and retrieve all its values as a collection,
;; timeout when refresh interval is exceeded
(defn- drain-or-timeout [c]
  (let [refresh-rate (config/get :refresh-rate)
        [vals _] (alts!!
                   [(into '() c)
                    (timeout (* 1000 refresh-rate))])]
    (when-not vals
      (throw (TimeoutException.
               (str "Did not retrieve anything from the Jenkins API within " refresh-rate " seconds!"))))
    vals))


(defn get-failed-jobs []
  "fetches all failed jobs from jenkins and returns a map that devides them in three classes:
   :claimed, :unclaimed and :unclaimable. For each job of each class, a map is returned with
   :name, :claimed, :claimedBy and :reason"
  (->> (get-failed-jobs-rsrc)
       (to-chan)
       (add-last-build-url-chan)
       (add-claim-info-chan)
       (drain-or-timeout)
       (group-by
         #(cond
           (true? (:claimed %)) :claimed
           (false? (:claimed %)) :unclaimed
           :else :unclaimable))))
