(ns jaggr.core
  (:use compojure.core
        jaggr.views
        [hiccup.middleware :only (wrap-base-url)]
        [ring.adapter.jetty :only (run-jetty)])
  (:require [compojure.route :as route]
            [compojure.handler :as handler]
            [omniconf.core :as config])
  (:gen-class :main true))

(defn init
  ([] (init []))
  ([args]
   (config/define
     {:base-url     {:description "The Jenkins URL that shows all jobs to monitor"
                     :type        :string
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
                     :default     3000}})

   (config/populate-from-env)
   (config/populate-from-cmd args)
   (config/verify :quit-on-error true)))


(defroutes main-routes
           (GET "/" [] (index-page))
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
