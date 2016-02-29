(ns jaggr.views
  (:use [hiccup core page]
        [jaggr.jenkins])
  (:require [omniconf.core :as config]))


(defn job-details [job]
  (when-not (empty? job)
    [:div.job
     [:div.job-name (h (:name job))]
     (when-not (empty? (:claimedBy job))
       [:div.job-claimed-by "was heroically claimed by " (h (:claimedBy job))])
     (when-not (empty? (:reason job))
       [:div.job-reason "'" (h (:reason job)) "'"])]))


(defn job-list [jobs]
  (when-not (empty? jobs)
    [:div.job-list
     (for [job jobs] (job-details job))]))


(defn header []
  [:head
   [:meta {:http-equiv "refresh" :content (str (config/get :refresh-rate))}]
   [:title "JAGGR"]
   (include-css "/css/style.css")
   (include-css "https://fonts.googleapis.com/css?family=Open+Sans:400,700,400italic")])


(defn index-page []
  (try
    (let [failed-jobs (get-failed-jobs)]
      (html5
        (header)
        [:body
         (cond
           (not-empty (:unclaimed failed-jobs))
           [:div.red.fullscreen
            [:h1 "BROKEN BUILDS - CLAIM AND FIX THEM!"]
            [:div.subtext
             "have a look at the broken builds - find someone who can fix the problem - claim the builds and fix them"]
            (job-list (:unclaimed failed-jobs))]

           (not-empty (:claimed failed-jobs))
           [:div.yellow.fullscreen
            [:h1 "BROKEN BUILDS - HELP IS ON THE WAY!"]
            [:div.subtext
             "see if you can help - check in with care - be careful with merges"]
            (job-list (:claimed failed-jobs))]

           (not-empty (:unclaimable failed-jobs))
           [:div.green.fullscreen
            [:h1 "MOST BUILDS ARE MOSTLY OK"]
            [:div.subtext "maybe have a look at some of these jobs as well"]
            (job-list (:unclaimable failed-jobs))]

           :else
           [:div.green.fullscreen
            [:h1 "HOORAY!"]
            [:div.subtext "all builds are green! - better go home now..."]])]))

    (catch Exception e
      (html5
        (header)
        [:body
         [:div.fullscreen.error
          [:h1 "SOMETHING IS WRONG HERE"]
          [:div.subtext "is Jenkins accessible? - wrong parameters? - network problems?"]
          [:div "I tried to access " (config/get :base-url) " with user " (config/get :user)]
          [:div "trying again every " (config/get :refresh-rate) " seconds"]]]))))
