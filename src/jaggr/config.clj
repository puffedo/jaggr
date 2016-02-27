(ns jaggr.config
  (:require [omniconf.core :as config]))

(defn init [args]
  (config/define
    {:base-url   {:description "The Jenkins URL that shows all jobs to monitor"
                  :type        :string
                  :required    true}
     :port       {:description "HTTP port"
                  :type        :number
                  :default     3000}
     :user       {:descriptions "A Jenkins user that has access to the base url"
                  :type         :string
                  :required     true}
     :user-token {:description "The users access token (see 'Configuration' page in your Jenkins user profile)"
                  :type        :string
                  :required    true
                  :secret      true}}
    )

  (config/populate-from-env)
  (config/populate-from-cmd args)
  (config/verify :quit-on-error true))

