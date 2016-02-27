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
       [:h1 "RED"]
       [:div (str (:unclaimed failed-jobs))]
       [:h1 "YELLOW"]
       [:div (str (:claimed failed-jobs))]
       [:h1 "GREY"]
       [:div (str (:unclaimable failed-jobs))]])))
