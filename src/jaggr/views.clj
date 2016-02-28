(ns jaggr.views
  (:use [hiccup core page]
        [jaggr.jenkins]))

(defn index-page []
  (let [failed-jobs (get-failed-jobs)]
    (html5
      [:head
       [:meta {:http-equiv "refresh" :content "120"}]
       [:title "JAGGR"]
       (include-css "/css/style.css")]
      [:body
       [:div {:class "red failed"}
        [:h1 "Unclaimed failed jobs!"]
        [:div (str (:unclaimed failed-jobs))]]
       [:div {:class "yellow failed"}
        [:h1 "All failed jobs are claimed"]
        [:div (str (:claimed failed-jobs))]]
       [:div {:class "grey failed"}
        [:h1 "Some minor jobs failed"]
        [:div (str (:unclaimable failed-jobs))]]])))
