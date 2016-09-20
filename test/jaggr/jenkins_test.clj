(ns jaggr.jenkins-test
  (:require [clojure.test :refer :all]
            [jaggr.core :refer [init]]
            [jaggr.jenkins :refer :all]
            [jaggr.test-util :refer [with-preserved-start-params]]
            [omniconf.core :as config]
            [org.httpkit.fake :refer [with-fake-http]])
  (:import (java.util.concurrent TimeoutException)))


;; test fixtures
(def
  get-jobs-response-body
  "{\"jobs\":[
  {\"name\":\"failed-job\",\"url\":\"http://some-base-url/view/failed-job/\",\"color\":\"red\"},
  {\"name\":\"ok-job\",\"url\":\"http://some-base-url/view/ok-job/\",\"color\":\"blue\"}]}")
(def
  get-job-response-body
  "{\"lastBuild\":{\"url\":\"http://some-base-url/job/failed-job/42/\"}}")
(def
  get-build-response-body-claimed
  "{\"actions\":[{},{\"claimed\":true,\"claimedBy\":\"somebody\",\"reason\":\"some reason\"}]}")
(def
  get-build-response-body-unclaimed
  "{\"actions\":[{},{\"claimed\":false,\"claimedBy\":null,\"reason\":null}]}")
(def
  get-build-response-body-unclaimable
  "{\"actions\":[{}]}")


(use-fixtures :once with-preserved-start-params)

(deftest jenkins-api-access

  (init '("--base-url" "http://some-base-url/"))

  (testing "The Jenkins JSON api is used to find failed jobs and group them by the claimed-state of their last broken build."

    (testing "This requires some intermediate steps:"

      (testing "The jobs-REST-resource is retrieved, it contains all jobs."

        (with-fake-http
          [#"http://some-base-url/api/json?.*" get-jobs-response-body]

          (let [jobs-rsrc (@#'jaggr.jenkins/get-jobs-rsrc)]
            (is (= 2 (count jobs-rsrc)))
            (is (= "failed-job" (:name (first jobs-rsrc))))
            (is (= "ok-job" (:name (nth jobs-rsrc 1))))

            (testing "The failed jobs are identified."

              (let [failed-jobs-rsrc (@#'jaggr.jenkins/get-failed-jobs-rsrc)]
                (is (= 1 (count failed-jobs-rsrc)))
                (is (= "failed-job" (:name (first failed-jobs-rsrc))))

                (testing "A failed job's last-build-REST-resource is located."

                  (with-fake-http
                    [#"http://some-base-url/view/failed-job/api/json?.*" get-job-response-body]

                    (let [last-build-url (@#'jaggr.jenkins/get-last-build-url (first failed-jobs-rsrc))]
                      (is (= "http://some-base-url/job/failed-job/42/" last-build-url))

                      (testing "When fetching a claimed last-build-REST-resource, it contains claimed-state, claimer and reason."

                        (with-fake-http
                          [#"http://some-base-url/job/failed-job/42/api/json?.*" get-build-response-body-claimed]

                          (let [last-build-rsrc (@#'jaggr.jenkins/get-claim-info last-build-url)]

                            (is (not-empty last-build-rsrc))
                            (is (true? (:claimed last-build-rsrc)))
                            (is (= "somebody" (:claimedBy last-build-rsrc)))
                            (is (= "some reason" (:reason last-build-rsrc))))))

                      (testing "When fetching an unclaimed last-build-REST-resource, it contains :claimed = false"

                        (with-fake-http
                          [#"http://some-base-url/view/failed-job/api/json?.*" get-job-response-body
                           #"http://some-base-url/job/failed-job/42/api/json?.*" get-build-response-body-unclaimed]

                          (let [last-build-rsrc (@#'jaggr.jenkins/get-claim-info last-build-url)]

                            (is (not-empty last-build-rsrc))
                            (is (false? (:claimed last-build-rsrc))))))

                      (testing "When fetching a last-build-REST-resource of an unclaimable job, it contains no claimed-state, claimer or reason."

                        (with-fake-http
                          [#"http://some-base-url/view/failed-job/api/json?.*" get-job-response-body
                           #"http://some-base-url/job/failed-job/42/api/json?.*" get-build-response-body-unclaimable]

                          (let [last-build-rsrc (@#'jaggr.jenkins/get-claim-info last-build-url)]

                            (is (nil? (:claimed last-build-rsrc)))
                            (is (nil? (:claimedBy last-build-rsrc)))
                            (is (nil? (:reason last-build-rsrc)))))))))))))))

    (testing "Failed jobs with claimed builds are returned under the key :claimed ."

      (with-fake-http
        [#"http://some-base-url/api/json?.*" get-jobs-response-body
         #"http://some-base-url/view/failed-job/api/json?.*" get-job-response-body
         #"http://some-base-url/job/failed-job/42/api/json?.*" get-build-response-body-claimed]

        (is (not-empty (:claimed (get-failed-jobs))))
        (is (empty? (:unclaimed (get-failed-jobs))))
        (is (empty? (:unclaimable (get-failed-jobs))))))

    (testing "Failed jobs with unclaimed builds are returned under the key :unclaimed ."

      (with-fake-http
        [#"http://some-base-url/api/json?.*" get-jobs-response-body
         #"http://some-base-url/view/failed-job/api/json?.*" get-job-response-body
         #"http://some-base-url/job/failed-job/42/api/json?.*" get-build-response-body-unclaimed]

        (is (not-empty (:unclaimed (get-failed-jobs))))
        (is (empty? (:claimed (get-failed-jobs))))
        (is (empty? (:unclaimable (get-failed-jobs))))))

    (testing "Failed unclaimable jobs are returned under the key :unclaimable ."

      (with-fake-http
        [#"http://some-base-url/api/json?.*" get-jobs-response-body
         #"http://some-base-url/view/failed-job/api/json?.*" get-job-response-body
         #"http://some-base-url/job/failed-job/42/api/json?.*" get-build-response-body-unclaimable]

        (is (not-empty (:unclaimable (get-failed-jobs))))
        (is (empty? (:claimed (get-failed-jobs))))
        (is (empty? (:unclaimed (get-failed-jobs))))))

    (testing "If the Jenkins API calls don't finish within one screen refresh interval, an Exception is thrown."

      (with-fake-http
        [#"http://some-base-url/api/json?.*"
         (fn [_ _ _] (do (Thread/sleep 1100) get-jobs-response-body))
         #"http://some-base-url/view/failed-job/api/json?.*"
         (fn [_ _ _] (do (Thread/sleep 1100) get-job-response-body))
         #"http://some-base-url/job/failed-job/42/api/json?.*"
         (fn [_ _ _] (do (Thread/sleep 1100) get-build-response-body-unclaimable))]

        (config/set :refresh-rate 1)
        (is (thrown? TimeoutException (get-failed-jobs)))))))