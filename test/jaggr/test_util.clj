(ns jaggr.test-util
  (:require [omniconf.core :as config]))

(defn with-preserved-start-params
  "A test wrapper that allows tests to modify start parameters
   by restoring the old parameters after the test is finished.
   Please note that it is often better to set the parameters with
   the jaggr.core/init function instead of omniconf.core/set, because
   init sets default values for some mandatory parameters."
  [fn]
  (let [old-base-url (config/get :base-url)
        old-user (config/get :user)
        old-user-token (config/get :user-token)
        old-refresh-rate (config/get :refresh-rate)
        old-port (config/get :port)
        old-config-file (config/get :config-file)]
    (fn)
    (config/set :base-url old-base-url)
    (config/set :user old-user)
    (config/set :user-token old-user-token)
    (config/set :refresh-rate old-refresh-rate)
    (config/set :port old-port)
    (config/set :config-file old-config-file)))
