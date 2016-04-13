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

