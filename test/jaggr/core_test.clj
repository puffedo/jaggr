(ns jaggr.core-test
  (:require [clojure.test :refer :all]
            [jaggr.core :refer :all]
            [jaggr.test-util :refer [with-preserved-start-params]]
            [omniconf.core :as config]))


(use-fixtures :each with-preserved-start-params)

(deftest url-normalization

  (testing "Many forms of base urls are generously accepted as a start parameter."

    (testing "It is assured that a base url always ends with a slash."

      (init '("--base-url" "https://www.test.com"))
      (is (= "https://www.test.com/" (config/get :base-url))))

    (testing "Queries or additional url fragments are ignored."

      (init '("--base-url" "https://www.test2.com?q=ignore#ignore-as-well"))
      (is (= "https://www.test2.com/" (config/get :base-url))))))