(ns jaggr.test-util
  (:require [omniconf.core :as config]))

(defn with-preserved-start-params
  "A test wrapper that allows tests to modify start parameters
   by restoring the old parameters after the test is finished.
   Please note that it is often better to set the parameters with
   the jaggr.core/init function instead of omniconf.core/set, because
   init sets default values for some mandatory parameters."
  [fn]
  (let [old-config (config/get)]
    (fn)
    (doseq [[k v] old-config] (config/set k v))))
