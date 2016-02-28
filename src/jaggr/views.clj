(ns jaggr.views
  (:use [hiccup core page]
        [jaggr.jenkins]))

(defn job-details [job]
  (when-not (empty? job)
    (html5
      [:div.job
       [:div.job-name (h (:name job))]
       (when-not (empty? (:reason job))
         [:div.job-reason "'" (h (:reason job)) "'"])
       (when-not (empty? (:claimedBy job))
           [:div.job-claimed-by "was heroically claimed by" (h (:claimedBy job))])
       ])))

(defn job-list [jobs]
  (when-not (empty? jobs)
    (html5
      [:div.job-list
       (for [job jobs] (job-details job))])))


(defn index-page []
  (let [failed-jobs (get-failed-jobs)]
    (html5
      [:head
       [:meta {:http-equiv "refresh" :content "120"}]
       [:title "JAGGR"]
       (include-css "/css/style.css")]
      [:body
       (cond
         (not-empty (:unclaimed failed-jobs))
         [:div.red.fullscreen
          [:h1 "BROKEN BUILDS! CLAIM AND FIX!"]
          [:div.subtext
           "have a look at the broken builds - find someone who can fix the problem - claim the builds and fix them"]
          (job-list (:unclaimed failed-jobs))]

         (not-empty (:claimed failed-jobs))
         [:div.yellow.fullscreen
          [:h1 "BROKEN BUILDS - HELP IS ON THE WAY"]
          [:div.subtext
           "see if you can help - check in with care - be careful with merges"]
          (job-list (:claimed failed-jobs))]

         (not-empty (:unclaimable failed-jobs))
         [:div.green.fullscreen
          [:h1 "MOST BUILDS MOSTLY OK"]
          [:div.subtext "have a look at some of these jobs as well"]
          (job-list (:unclaimable failed-jobs))]

         :else
         [:div.green.fullscreen
          [:h1 "HOORAY!"]
          [:div.subtext "all builds are green!"]])])))
