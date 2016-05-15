(ns jaggr.test-util
  (:require [omniconf.core :as config]))

;; makes sure that tests that modify mandatory start parameters leave no side effects behind
(defn with-preserved-start-params [fn]
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
