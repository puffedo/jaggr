(ns jaggr.views-test
  (:require [clojure.test :refer :all]
            [jaggr.core :refer [app]]
            [jaggr.jenkins :as jenkins]
            [jaggr.test-util :refer [with-preserved-start-params]]
            [kerodon.core :refer :all]
            [kerodon.test :refer :all]
            [net.cgrand.enlive-html :as enlive]
            [omniconf.core :as config]))


;; convenience macro that is, for some reason, not provided by kerodon
(defmacro element? [selector]
  `(validate >
             #(count (enlive/select (:enlive %) ~selector))
             0
             (~'element? ~selector)))


(deftest display-error-page-when-exception-is-thrown
  (with-redefs-fn
    {#'jenkins/failed-jobs
     (fn [] (throw (Exception. "This Exception was thrown deliberately to test error handling")))}

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
    {#'jenkins/failed-jobs
     (fn []
       {:unclaimed   [{:name                  "failed-unclaimed-build"
                       :claimedBy             nil :claimed false :reason nil
                       :lastCompletedBuildUrl "/failed-unclaimed-build/"}]
        :claimed     [{:name                  "failed-claimed-build-should-be-ignored"
                       :claimedBy             "somebody" :claimed true :reason "a reason"
                       :lastCompletedBuildUrl "/failed-claimed-build/"}]
        :unclaimable [{:name                  "failed-unclaimable-build-should-be-ignored"
                       :lastCompletedBuildUrl "/failed-unclaimable-build/"}]})}

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
    {#'jenkins/failed-jobs
     (fn []
       {:claimed     [{:name                  "failed-claimed-build"
                       :claimedBy             "somebody" :claimed true :reason "a reason"
                       :lastCompletedBuildUrl "/failed-claimed-build/"}]
        :unclaimable [{:name                  "failed-unclaimable-build-should-be-ignored"
                       :lastCompletedBuildUrl "/failed-unclaimable-build/"}]})}

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
    {#'jenkins/failed-jobs
     (fn []
       {:unclaimable [{:name                  "failed-unclaimable-build"
                       :lastCompletedBuildUrl "/failed-unclaimable-build/"}]})}

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


; in Jenkins 2 there is a bug, so that not all claim info is set to null when dropping a claim
; therefore, Jaggr must make sure that no defective claim info is displayed for unclaimed builds
(deftest display-claim-info-only-when-claimed-is-true
  (with-redefs-fn
    {#'jenkins/failed-jobs
     (fn []
       {:unclaimed [{:name                  "failed-build-with-dropped-claim"
                     :claimed               false
                     :claimedBy             "last-claimer-should-be-empty-but-is-not"
                     :reason                "reason-should-be-empty-but-is-not"
                     :lastCompletedBuildUrl "/failed-unclaimed-build/"}]})}

    #(-> (session app)
         (visit "/")
         (has (some-text? "failed-build-with-dropped-claim")
              "The name of the unclaimed failed job should be displayed")
         (has (link? :href "/failed-unclaimed-build/")
              "A link to the jobs last broken build should exist")
         (has (missing? [:div.job-claimed-by])
              "The claimer should be ommitted since the build is not claimed anymore")
         (has (missing? [:div.job-reason])
              "The reason should be ommitted since the build is not claimed anymore"))))


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



(defn- test-invalid-input [input-selector input-value expected-result]

  ;; make sure the global config is not accidentally changed on submit
  (with-preserved-start-params

    #(-> (session app)
         (visit "/config")
         (fill-in input-selector input-value)
         (press "Submit")
         (has (element? [:div.form-problems])
              expected-result))))


(deftest config-form-validates-input

  (test-invalid-input [:input#field-base-url] ""
                      "Configuring an empty base-url should lead to an error message")

  (test-invalid-input [:input#field-base-url] "not-a-valid-url"
                      "Configuring a base-url that is not a valid URL should lead to an error message")

  (test-invalid-input [:input#field-base-url] "http://my-jenkins.com"
                      "Configuring a base-url without a trailing slash should lead to an error message")

  (test-invalid-input [:input#field-image-url] "not-a-valid-url"
                      "Configuring a default-image-url that is not a valid URL should lead to an error message")

  (test-invalid-input [:input#field-image-url-red] "not-a-valid-url"
                      "Configuring a red-image-url that is not a valid URL should lead to an error message")

  (test-invalid-input [:input#field-image-url-yellow] "not-a-valid-url"
                      "Configuring a yellow-image-url that is not a valid URL should lead to an error message")

  (test-invalid-input [:input#field-image-url-green] "not-a-valid-url"
                      "Configuring a green-image-url that is not a valid URL should lead to an error message")

  (test-invalid-input [:input#field-image-url-error] "not-a-valid-url"
                      "Configuring an error-image-url that is not a valid URL should lead to an error message")

  (test-invalid-input [:input#field-refresh-rate] 9
                      "Configuring a refresh rate of less than 10 seconds should lead to an error message"))


(deftest config-form-submit-changes-config

  ;; make sure the global config is changed only temporarily on submit
  (with-preserved-start-params

    #(do
      (-> (session app)
          (visit "/config")
          (fill-in [:input#field-base-url] "http://my-jenkins.com/test/")
          (fill-in [:input#field-user] "test-user")
          (fill-in [:input#field-user-token] "test-token")
          (fill-in [:input#field-refresh-rate] 42)
          (fill-in [:input#field-image-url] "http://my-images.com/")
          (fill-in [:input#field-image-url-red] "http://my-images.com/red/")
          (fill-in [:input#field-image-url-yellow] "http://my-images.com/yellow/")
          (fill-in [:input#field-image-url-green] "http://my-images.com/green/")
          (fill-in [:input#field-image-url-error] "http://my-images.com/error/")
          (press "Submit"))

      (is (= (config/get :base-url) "http://my-jenkins.com/test/"))
      (is (= (config/get :user) "test-user"))
      (is (= (config/get :user-token) "test-token"))
      (is (= (config/get :refresh-rate) 42))
      (is (= (config/get :image-url) "http://my-images.com/"))
      (is (= (config/get :image-url-red) "http://my-images.com/red/"))
      (is (= (config/get :image-url-yellow) "http://my-images.com/yellow/"))
      (is (= (config/get :image-url-green) "http://my-images.com/green/"))
      (is (= (config/get :image-url-error) "http://my-images.com/error/")))))