(ns jaggr.core
  (:use compojure.core
        jaggr.views
        [clojure.java.io :only (as-file)]
        [hiccup.middleware :only (wrap-base-url)]
        [ring.adapter.jetty :only (run-jetty)]
        [url-normalizer.core :only (normalize)])
  (:require [compojure.route :as route]
            [compojure.handler :as handler]
            [omniconf.core :as config])
  (:gen-class :main true))

;; removes query and fragment from a url string
;; ensures, it ends swith a slash
(defn- normalize-url-string [url]
  (let [pure-url
        (.toString
          (normalize url {:remove-query?    true
                          :remove-fragment? true}))]
    (if (.endsWith pure-url "/" )
      pure-url
      (str pure-url "/"))))


(defn init
  ([] (init []))
  ([args]
   (config/define
     {:base-url     {:description "The Jenkins URL that shows all jobs to monitor"
                     :parser      normalize-url-string
                     :required    true}
      :user         {:descriptions "A Jenkins user that has access to the base url"
                     :type         :string
                     :required     true}
      :user-token   {:description "The users access token (see 'Configuration' page in your Jenkins user profile)"
                     :type        :string
                     :required    true
                     :secret      true}
      :refresh-rate {:description "The time between two automatic page reloads in seconds"
                     :type        :number
                     :default     60}
      :port         {:description "The port"
                     :type        :number
                     :default     3000}
     :config-file   {:description "A file containing config parameters"
                     :type        :string
                     :default     "default.config"}})

   (config/populate-from-env)
   (config/populate-from-cmd args) ;; read cmd line params, so a user can specify the confog-file param
   (if (.exists (as-file (config/get :config-file))) (config/populate-from-file (config/get :config-file)))
   (config/populate-from-cmd args) ;; re-apply cmd line params to override params from config-file
   (config/verify :quit-on-error true)))


(defroutes main-routes
           (GET "/" [] (index-page))
           (GET "/backgroundimage" [] (background-image))
           (route/resources "/")
           (route/not-found "Page not found"))

(def app
  "This is the entry point for the application when
  started via lein ring server"
  (-> (handler/site main-routes)
      (wrap-base-url)))

(defn -main
  "This is the main entry point for the application when
  started directly from the command line"
  [& args]
  (init args)
  (run-jetty app {:port (config/get :port)}))
