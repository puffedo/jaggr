(ns jaggr.views
  (:use [hiccup core page]
        [jaggr.jenkins])
  (:require [omniconf.core :as config]
            [clojure.java.io :as io]
            [clojure.tools.logging :as log]))



(defn header []
  [:head
   [:meta {:http-equiv "refresh" :content (str (config/get :refresh-rate))}]
   [:title "JAGGR"]
   (include-css "/css/style.css")
   (include-css "https://fonts.googleapis.com/css?family=Open+Sans:400,700,400italic")
   [:link {:rel "shortcut icon" :href "img/favicon.ico"}]])

(defn job-details [job]
  (when job
    [:div.job
     [:div.job-name (h (:name job))]
     (when (:claimedBy job)
       [:div.job-claimed-by "was heroically claimed by " (h (:claimedBy job))])
     (when (:reason job)
       [:div.job-reason "'" (h (:reason job)) "'"])]))


(defn job-list [jobs]
  (when jobs
    [:div.job-list
     (for [job jobs] (job-details job))]))


(defn index-page []
  (try
    (let [failed-jobs (get-failed-jobs)]
      (html5
        (header)
        [:body
         [:img#logo {:src "/img/jaggr-logo.png"}]
         (cond
           (not-empty (:unclaimed failed-jobs))
           [:div.fullscreen
            [:img {:src "/background-image-red"}]
            [:div.fullscreen.red
             [:h1 "BROKEN BUILDS - CLAIM AND FIX THEM!"]
             [:div.subtext
              "have a look at the broken builds - find someone who can fix the problem - claim the builds and fix them"]
             (job-list (:unclaimed failed-jobs))]]

           (not-empty (:claimed failed-jobs))
           [:div.fullscreen
            [:img {:src "/background-image-yellow"}]
            [:div.fullscreen.yellow
             [:h1 "BROKEN BUILDS - HELP IS ON THE WAY!"]
             [:div.subtext
              "see if you can help - check in with care - be careful with merges"]
             (job-list (:claimed failed-jobs))]]

           (not-empty (:unclaimable failed-jobs))
           [:div.fullscreen
            [:img {:src "/background-image-green"}]
            [:div.fullscreen.green
             [:h1 "MOST BUILDS ARE MOSTLY OK"]
             [:div.subtext "maybe have a look at some of these jobs as well"]
             (job-list (:unclaimable failed-jobs))]]

           :else
           [:div.fullscreen.green
            [:h1 "HOORAY!"]
            [:div.subtext "all builds are green! - better go home now..."]])]))

    (catch Exception e
      (log/error e "Something is wrong here!")
      (html5
        (header)
        [:body
         [:img#logo {:src "/img/jaggr-logo.png"}]
         [:div.fullscreen
          [:img {:src "/background-image-error"}]
          [:div.fullscreen.error
           [:h1 "SOMETHING IS WRONG HERE"]
           [:div.subtext "is Jenkins accessible? - wrong parameters? - network problems?"]
           [:div "I tried to access " (config/get :base-url) " with user " (config/get :user)]
           [:div "trying again every " (config/get :refresh-rate) " seconds"]]]]))))


;; show specific background images if provided, random pictures otherwise

(defn- selectRandomImageFrom [files]
  {:status 200
   :body   (io/file (rand-nth files))})

(defn- redirectToImageService []
  {:status  302
   :headers {"Location" "http://lorempixel.com/g/400/200"}
   :body    ""})

(defn- imageFrom [directory]
  (let [files (.listFiles (io/file directory))]
    (cond
      (empty? files)
      (redirectToImageService)
      :else
      (selectRandomImageFrom files))))

(defn background-image-red []
  (imageFrom "images/red/"))

(defn background-image-yellow []
  (imageFrom "images/yellow/"))

(defn background-image-green []
  (imageFrom "images/green/"))

(defn background-image-error []
  (imageFrom "images/error/"))

