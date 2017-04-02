(ns jaggr.jenkins-test
  (:require [clojure.test :refer :all]
            [jaggr.core :refer [init]]
            [jaggr.jenkins :refer :all]
            [jaggr.test-util :refer [with-preserved-start-params]]
            [omniconf.core :as config]
            [org.httpkit.fake :refer [with-fake-http]])
  (:import (java.util.concurrent TimeoutException)))


;; test fixtures
;; each one exemplifies the body of a response to a http GET on a certain type of REST resource

;; a jenkins jobs list, referencing several jobs in different failure states
(def
  jobs-rsrc-body
  "{\"jobs\" : [
   {\"name\" : \"red-job\",             \"url\" : \"http://example.com/view/failed-1/\", \"color\" : \"red\"},
   {\"name\" : \"red-job-running\",     \"url\" : \"http://example.com/view/failed-2/\", \"color\" : \"red_anime\"},
   {\"name\" : \"yellow-job\",          \"url\" : \"http://example.com/view/failed-3/\", \"color\" : \"yellow\"},
   {\"name\" : \"yellow-job-running\",  \"url\" : \"http://example.com/view/failed-4/\", \"color\" : \"yellow_anime\"},
   {\"name\" : \"aborted-job\",         \"url\" : \"http://example.com/view/failed-5/\", \"color\" : \"aborted\"},
   {\"name\" : \"aborted-job-running\", \"url\" : \"http://example.com/view/failed-6/\", \"color\" : \"aborted_anime\"},
   {\"name\" : \"disabled-job\",        \"url\" : \"http://example.com/view/disabled/\", \"color\" : \"disabled\"},\n
   {\"name\" : \"ok-job\",              \"url\" : \"http://example.com/view/ok/\",       \"color\" : \"blue\"}]}")

;; a specific job, with a reference to its last completed build
(def
  job-rsrc-body
  "{\"lastCompletedBuild\" : {\"url\" : \"http://example.com/job/failed-job/42/\"}}")

;; a claimed build
(def
  build-rsrc-body-claimed
  "{\"actions\" : [{}, {\"claimed\" : true, \"claimedBy\" : \"somebody\", \"reason\" : \"some reason\"}]}")

;; a claimed build, Jenkins 2 format
(def
  build-rsrc-body-claimed-jenkins-2
  "{\"actions\" : [{}, {\"_class\" : \"ClaimPlugin\" ,\"claimed\" : true, \"claimedBy\" : \"somebody\", \"reason\" : \"some reason\"}]}")

;; an unclaimed build
(def
  build-rsrc-body-unclaimed
  "{\"actions\" : [{}, {\"claimed\" : false, \"claimedBy\" : null, \"reason\" : null}]}")

;; a build of a job not configured to be claimable
(def
  build-rsrc-body-unclaimable
  "{\"actions\" : [{}]}")



(use-fixtures :once with-preserved-start-params)

(deftest jenkins-api-access

  (init '("--base-url" "http://example.com/"))

  (testing "The Jenkins JSON api is used to find failed jobs and group them by the claimed-state of their last completed build."

    (testing "This requires some intermediate steps:"

      (testing "The jobs-REST-resource is retrieved, it contains all jobs."

        (with-fake-http
          [#"http://example.com/api/json?.*" jobs-rsrc-body]

          (let [jobs (@#'jaggr.jenkins/get-jobs)
                all-job-names #{"red-job" "red-job-running"
                                "yellow-job" "yellow-job-running"
                                "aborted-job" "aborted-job-running"
                                "disabled-job" "ok-job"}]
            (is (= all-job-names (set (map :name jobs))))

            (testing
              "The failed jobs are identified. Red, yellow and aborted jobs are considered a failure, as well as running jobs that were in a failure state before they started. "

              (let [failed-jobs (@#'jaggr.jenkins/get-failed-jobs)
                    failed-job-names #{"red-job" "red-job-running"
                                       "yellow-job" "yellow-job-running"
                                       "aborted-job" "aborted-job-running"}]
                (is (= failed-job-names (set (map :name failed-jobs))))

                (testing "A failed job's last-completed-build's REST-resource is located."

                  (with-fake-http
                    [#"http://example.com/view/failed-.*/api/json.*" job-rsrc-body]

                    (let [last-completed-build-url (@#'jaggr.jenkins/get-last-completed-build-url (first failed-jobs))]
                      (is (= "http://example.com/job/failed-job/42/" last-completed-build-url))

                      (testing "For a claimed build, it contains claimed-state, claimer and reason."

                        (with-fake-http
                          [#"http://example.com/job/failed-job/42/api/json?.*" build-rsrc-body-claimed]

                          (let [claim-info (@#'jaggr.jenkins/get-claim-info last-completed-build-url)]

                            (is (not-empty claim-info))
                            (is (true? (:claimed claim-info)))
                            (is (= "somebody" (:claimedBy claim-info)))
                            (is (= "some reason" (:reason claim-info))))))

                      (testing "For an unclaimed build, it contains :claimed = false"

                        (with-fake-http
                          [#"http://example.com/job/failed-job/42/api/json?.*" build-rsrc-body-unclaimed]

                          (let [claim-info (@#'jaggr.jenkins/get-claim-info last-completed-build-url)]

                            (is (not-empty claim-info))
                            (is (false? (:claimed claim-info))))))

                      (testing "For a build of an unclaimable job, it contains no claimed-state, claimer or reason."

                        (with-fake-http
                          [#"http://example.com/job/failed-job/42/api/json?.*" build-rsrc-body-unclaimable]

                          (let [claim-info (@#'jaggr.jenkins/get-claim-info last-completed-build-url)]

                            (is (nil? (:claimed claim-info)))
                            (is (nil? (:claimedBy claim-info)))
                            (is (nil? (:reason claim-info))))))

                      (testing "Both Jankins 1.x and 2.x API versions are accepted."

                        (let [jenkins-1-url #"http://example.com/jenkins-1/api/json?.*"
                              jenkins-2-url #"http://example.com/jenkins-2/api/json?.*"]

                        (with-fake-http
                          [jenkins-1-url build-rsrc-body-claimed
                           jenkins-2-url build-rsrc-body-claimed-jenkins-2]

                          (let [jenkins-1-claim-info (@#'jaggr.jenkins/get-claim-info jenkins-1-url)
                                jenkins-2-claim-info (@#'jaggr.jenkins/get-claim-info jenkins-2-url)]

                            (is (= jenkins-1-claim-info jenkins-2-claim-info)))))))))))))))


    (testing "Failed jobs with claimed builds are returned under the key :claimed ."

      (with-fake-http
        [#"http://example.com/api/json?.*" jobs-rsrc-body
         #"http://example.com/view/failed.*/api/json?.*" job-rsrc-body
         #"http://example.com/job/failed-job/42/api/json?.*" build-rsrc-body-claimed]

        (is (not-empty (:claimed (failed-jobs))))
        (is (empty? (:unclaimed (failed-jobs))))
        (is (empty? (:unclaimable (failed-jobs))))))

    (testing "Failed jobs with unclaimed builds are returned under the key :unclaimed ."

      (with-fake-http
        [#"http://example.com/api/json?.*" jobs-rsrc-body
         #"http://example.com/view/failed.*/api/json?.*" job-rsrc-body
         #"http://example.com/job/failed-job/42/api/json?.*" build-rsrc-body-unclaimed]

        (is (not-empty (:unclaimed (failed-jobs))))
        (is (empty? (:claimed (failed-jobs))))
        (is (empty? (:unclaimable (failed-jobs))))))

    (testing "Failed unclaimable jobs are returned under the key :unclaimable ."

      (with-fake-http
        [#"http://example.com/api/json?.*" jobs-rsrc-body
         #"http://example.com/view/failed.*/api/json?.*" job-rsrc-body
         #"http://example.com/job/failed-job/42/api/json?.*" build-rsrc-body-unclaimable]

        (is (not-empty (:unclaimable (failed-jobs))))
        (is (empty? (:claimed (failed-jobs))))
        (is (empty? (:unclaimed (failed-jobs))))))

    (testing "If the Jenkins API calls don't finish within one screen refresh interval, an Exception is thrown."

      (with-fake-http
        [#"http://example.com/api/json?.*"
         (fn [_ _ _] (do (Thread/sleep 1100) jobs-rsrc-body))
         #"http://example.com/view/failed.*/api/json?.*"
         (fn [_ _ _] (do (Thread/sleep 1100) job-rsrc-body))
         #"http://example.com/job/failed-job/42/api/json?.*"
         (fn [_ _ _] (do (Thread/sleep 1100) build-rsrc-body-unclaimable))]

        (config/set :refresh-rate 1)
        (is (thrown? TimeoutException (failed-jobs)))
        ;; allow enough time for all faked http calls to time out
        ;; the with-fake-http-macro doesn't play well with async http call chains
        (Thread/sleep 7000)))))