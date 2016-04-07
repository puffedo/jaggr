(ns jaggr.jenkins

  (:require [org.httpkit.client :as http]
            [clojure.data.json :as json]
            [omniconf.core :as config]
            [clojure.tools.logging :as log]))


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


;; gets the claim info for a job resource and throws away everything else
(defn- get-last-build-rsrc [last-build-url]
  (->>
    (get-from-jenkins last-build-url "tree=actions[claimed,claimedBy,reason]")
    (:actions)
    (filter not-empty)
    (first)))


(defn get-failed-jobs []
  "fetches all failed jobs from jenkins and returns a map that devides them in three classes:
   :claimed, :unclaimed and :unclaimable. For each job of each class, a map is returned with
   :name, :claimed, :claimedBy and :reason"
  (try
    (->>
      (for [failed-job-rsrc (get-failed-jobs-rsrc)]
        (->
          (get-last-build-url failed-job-rsrc)
          (get-last-build-rsrc)
          (assoc :name (:name failed-job-rsrc))))
      (group-by #(cond
                  (= true (:claimed %1)) :claimed
                  (= false (:claimed %1)) :unclaimed
                  :else :unclaimable)))
    (catch Exception e
      (log/error e "Error while trying to access the Jenkins API and make sense of the data")
      (throw e))))