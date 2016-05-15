(ns jaggr.views-test
  (:use jaggr.views)
  (:use clojure.test)
  (:require
    [jaggr.core :refer [app]]
    [kerodon.core :refer :all]
    [kerodon.test :refer :all]
    [jaggr.jenkins :as jenkins]
    [net.cgrand.enlive-html :as enlive]))


;; convenience macro that is, for some reason, not provided by kerodon
(defmacro element? [selector]
  `(validate >
             #(count (enlive/select (:enlive %) ~selector))
             0
             (~'element? ~selector)))


(deftest display-error-page-when-exception-is-thrown
  (with-redefs-fn
    {#'jenkins/get-failed-jobs (fn [] (throw (.Exception "An exception")))}
    #(-> (session app)
         (visit "/")
         (has (element? [:div.error])
              "The error page should have an element of class 'error'")
         (has (missing? [:div.red])
              "The error page should not show the status 'red' -
              an exception was thrown, so the status cannot be determined reliably")
         (has (missing? [:div.yellow])
              "The error page should not show the status 'yellow' -
              an exception was thrown, so the status cannot be determined reliably")
         (has (missing? [:div.green])
              "The error page should not show the status 'green' -
              an exception was thrown, so the status cannot be determined reliably"))))


(deftest display-red-page-when-unclaimed-failed-builds-exist
  (with-redefs-fn
    {#'jenkins/get-failed-jobs
     (fn []
       {:unclaimed   [{:name           "failed-unclaimed-build"
                       :claimedBy      nil :claimed false :reason nil
                       :last-build-url "/failed-unclaimed-build/"}]
        :claimed     [{:name           "failed-claimed-build-should-be-ignored"
                       :claimedBy      "somebody" :claimed true :reason "a reason"
                       :last-build-url "/failed-claimed-build/"}]
        :unclaimable [{:name           "failed-unclaimable-build-should-be-ignored"
                       :last-build-url "/failed-unclaimable-build/"}]})}

    #(-> (session app)
         (visit "/")
         (has (element? [:div.red])
              "The red page should be shown when unclaimed failed builds exist")
         (has (some-text? "failed-unclaimed-build")
              "The name of the unclaimed failed job should be displayed")
         (has (link? :href "/failed-unclaimed-build/")
              "A link to the jobs last broken build should exist")
         (has (missing? [:div.yellow])
              "The red page should not show have elements of class 'yellow'")
         (has (missing? [:div.green])
              "The red page should not show have elements of class 'green'"))))


(deftest display-yellow-page-when-all-failed-builds-are-claimed
  (with-redefs-fn
    {#'jenkins/get-failed-jobs
     (fn []
       {:claimed     [{:name           "failed-claimed-build"
                       :claimedBy      "somebody" :claimed true :reason "a reason"
                       :last-build-url "/failed-claimed-build/"}]
        :unclaimable [{:name           "failed-unclaimable-build-should-be-ignored"
                       :last-build-url "/failed-unclaimable-build/"}]})}

    #(-> (session app)
         (visit "/")
         (has (element? [:div.yellow])
              "The yellow Page should be shown when all failed builds are claimed")
         (has (some-text? "failed-claimed-build")
              "The name of the claimed failed job should by displayed")
         (has (link? :href "/failed-claimed-build/")
              "A link to the jobs last broken build should exist")
         (has (some-text? "somebody")
              "The name of the claimer of the failed build should by displayed")
         (has (some-text? "a reason")
              "The reason for the job failure should by displayed")
         (has (missing? [:div.red])
              "The yellow page should not show have elements of class 'red'")
         (has (missing? [:div.green])
              "The yellow page should not show have elements of class 'green'"))))


(deftest display-green-page-when-no-failed-builds-exist
  (with-redefs-fn
    {#'jenkins/get-failed-jobs
     (fn []
       {:unclaimable [{:name           "failed-unclaimable-build"
                       :last-build-url "/failed-unclaimable-build/"}]})}

    #(-> (session app)
         (visit "/")
         (has (element? [:div.green])
              "The green page should be shown when no claimable failed jobs exist")
         (has (some-text? "failed-unclaimable-build")
              "since there are no more claimable (= team-owned) failed builds,
              the other failed jobs should be shown for information)")
         (has (link? :href "/failed-unclaimable-build/")
              "A link to the jobs last broken build should exist")
         (has (missing? [:div.red])
              "The green page should not show have elements of class 'red'")
         (has (missing? [:div.yellow])
              "The green page should not show have elements of class 'yellow'"))))


(deftest red-custom-image-is-used-when-provided
  (let [image-placeholder "PLACEHOLDER FOR IMAGE"]
    (with-redefs-fn
      {#'jaggr.views/random-image-from
       (fn [dir]
         (if (= "images/red/" dir)
           image-placeholder
           nil))}

      #(-> (session app)
           (visit "/background-image-red")
           (has (status? 200)
                "The red image page is found")
           ; we use text as a placeholder for the image, since there is no "image?" matcher
           (has (some-text? image-placeholder)
                "The body contains the provided image")))))


(deftest yellow-custom-image-is-used-when-provided
  (let [image-placeholder "PLACEHOLDER FOR IMAGE"]
    (with-redefs-fn
      {#'jaggr.views/random-image-from
       (fn [dir]
         (if (= "images/yellow/" dir)
           image-placeholder
           nil))}

      #(-> (session app)
           (visit "/background-image-yellow")
           (has (status? 200)
                "The yellow image page is found")
           (has (some-text? image-placeholder)
                "The body contains the provided image")))))


(deftest green-custom-image-is-used-when-provided
  (let [image-placeholder "PLACEHOLDER FOR IMAGE"]
    (with-redefs-fn
      {#'jaggr.views/random-image-from
       (fn [dir]
         (if (= "images/green/" dir)
           image-placeholder
           nil))}

      #(-> (session app)
           (visit "/background-image-green")
           (has (status? 200)
                "The green image page is found")
           (has (some-text? image-placeholder)
                "The body contains the provided image")))))


(deftest error-custom-image-is-used-when-provided
  (let [image-placeholder "PLACEHOLDER FOR IMAGE"]
    (with-redefs-fn
      {#'jaggr.views/random-image-from
       (fn [dir]
         (if (= "images/error/" dir)
           image-placeholder
           nil))}

      #(-> (session app)
           (visit "/background-image-error")
           (has (status? 200)
                "The error image page is found")
           (has (some-text? image-placeholder)
                "The body contains the provided image")))))


(deftest redirect-to-image-service-when-no-image-is-provided
  (with-redefs-fn
    {#'jaggr.views/random-image-from
     (fn [_] nil)}

    #(-> (session app)
         (visit "/background-image-red")
         (has (status? 302)
              "redirect to image service when no red image is provided")
         (visit "/background-image-yellow")
         (has (status? 302)
              "redirect to image service when no yellow image is provided")
         (visit "/background-image-green")
         (has (status? 302)
              "redirect to image service when no green image is provided")
         (visit "/background-image-error")
         (has (status? 302)
              "redirect to image service when no error image is provided"))))

