(ns jaggr.views-test
  (:use jaggr.views)
  (:use clojure.test)
  (:require
    [jaggr.core :refer [app]]
    [kerodon.core :refer :all]
    [kerodon.test :refer :all]
    [kerodon.impl :refer [get-attr]]
    [jaggr.jenkins :as jenkins]))


;; a custom kerodon matcher - verifies that an attribute value contains a given substring
;; see https://semaphoreci.com/community/tutorials/how-to-write-a-custom-kerodon-matcher
(defmacro attr-contains? [selector attr expected]
  `(validate .contains
             #(get-attr % ~selector ~attr)
             ~expected
             (~'attr-contains? ~selector ~attr ~expected)))


(deftest display-error-page-when-exception-is-thrown
  (with-redefs-fn
    {#'jenkins/get-failed-jobs (fn [] (throw (.Exception "An exception")))}
    #(-> (session app)
         (visit "/")
         (has (attr-contains? [:div] :class "error")
              "The error Page should have an element of class 'error'")
         (has (missing? [:div.red])
              "The error Page should not show the status 'red' -
              an exception ws thrown, so the status cannot be determined reliably")
         (has (missing? [:div.yellow])
              "The error Page should not show the status 'yellow' -
              an exception ws thrown, so the status cannot be determined reliably")
         (has (missing? [:div.green])
              "The error Page should not show the status 'green' -
              an exception ws thrown, so the status cannot be determined reliably"))))


(deftest display-red-page-when-unclaimed-failed-builds-exist
  (with-redefs-fn
    {#'jenkins/get-failed-jobs
     (fn []
       {:unclaimed   [{:name      "failed-unclaimed-build"
                       :claimedBy nil :claimed false :reason nil}]
        :claimed     [{:name      "failed-claimed-build-should-be-ignored"
                       :claimedBy "somebody" :claimed true :reason "a reason"}]
        :unclaimable [{:name "failed-unclaimable-build-should-be-ignored"}]})}

    #(-> (session app)
         (visit "/")
         (has (attr-contains? [:div] :class "red")
              "The red Page should be shown when unclaimed failed builds exist")
         (has (some-text? "failed-unclaimed-build")
              "The name of the unclaimed failed job should by displayed")
         (has (missing? [:div.yellow])
              "The Page should not show the status 'yellow'")
         (has (missing? [:div.green])
              "The Page should not show the status 'green'"))))


(deftest display-yellow-page-when-all-failed-builds-are-claimed
  (with-redefs-fn
    {#'jenkins/get-failed-jobs
     (fn []
       {:claimed     [{:name      "failed-claimed-build"
                       :claimedBy "somebody" :claimed true :reason "a reason"}]
        :unclaimable [{:name "failed-unclaimable-build-should-be-ignored"}]})}

    #(-> (session app)
         (visit "/")
         (has (attr-contains? [:div] :class "yellow")
              "The yellow Page should be shown when all failed builds are claimed")
         (has (some-text? "failed-claimed-build")
              "The name of the claimed failed job should by displayed")
         (has (some-text? "somebody")
              "The name of the claimer of the failed job should by displayed")
         (has (some-text? "a reason")
              "The reason for the job failure should by displayed")
         (has (missing? [:div.red])
              "The Page should not show the status 'red'")
         (has (missing? [:div.green])
              "The Page should not show the status 'green'"))))


(deftest display-green-page-when-no-failed-builds-exist
  (with-redefs-fn
    {#'jenkins/get-failed-jobs
     (fn []
       {:unclaimable [{:name "failed-unclaimable-build"}]})}

    #(-> (session app)
         (visit "/")
         (has (attr-contains? [:div] :class "green")
              "The green Page should be shown when no claimable failed jobs exist")
         (has (some-text? "failed-unclaimable-build")
              "since there are no more team-owned broken builds,
              the other failed builds should be shown for information)")
         (has (missing? [:div.red])
              "The Page should not show the status 'red'")
         (has (missing? [:div.yellow])
              "The Page should not show the status 'yellow'"))))
