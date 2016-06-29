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
        (str (normalize url {:remove-query?    true
                             :remove-fragment? true}))]
    (if (.endsWith pure-url "/")
      pure-url
      (str pure-url "/"))))


(defn init
  "Initializes the start parameters, that can be either configured via
  command line, environment parameters or a config file. Command line
  parameters override file parameters, file parameters override environment
  parameters."
  ([] (init []))
  ([args]
   (config/define
     {:base-url         {:description "The Jenkins URL that shows all jobs to monitor"
                         :parser      normalize-url-string
                         :required    true}
      :user             {:description "A Jenkins user that has access to the base url"
                         :type        :string
                         :required    false}
      :user-token       {:description "The users access token (see 'Configuration' page in your Jenkins user profile)"
                         :type        :string
                         :required    false
                         :secret      true}
      :refresh-rate     {:description "The time between two automatic page reloads in seconds"
                         :type        :number
                         :default     60}
      :port             {:description "The port"
                         :type        :number
                         :default     3000}
      :config-file      {:description "A file containing config parameters"
                         :type        :string
                         :default     "default.config"}
      :image-url        {:description "A URL that serves a background image (unless a more specific one can be found in the file system)"
                         :type        :string
                         :default     "http://lorempixel.com/g/400/200"}
      :image-url-red    {:description "A URL that serves a background image for red screens. Overrides the image-url parameter.)"
                         :type        :string
                         :required    false}
      :image-url-yellow {:description "A URL that serves a background image for yellow screens. Overrides the image-url parameter.)"
                         :type        :string
                         :required    false}
      :image-url-green  {:description "A URL that serves a background image for green screens. Overrides the image-url parameter.)"
                         :type        :string
                         :required    false}
      :image-url-error  {:description "A URL that serves a background image for error screens. Overrides the image-url parameter.)"
                         :type        :string
                         :required    false}})


   (config/populate-from-env)
    ; read cmd line paramaterss, so a user can specify the config-file parameter
   (config/populate-from-cmd args)
   (if (.exists (as-file (config/get :config-file)))
     (config/populate-from-file (config/get :config-file)))
    ; re-apply cmd line parameters to override parameters from config-file
   (config/populate-from-cmd args)
   ; keep the app running on config errors so the config page is accessible to fix the problems
   (try (config/verify :quit-on-error false) (catch Exception _) )))


(defroutes main-routes
           (GET "/" [] (index-page))
           (GET "/config" [] (config-page))
           (POST "/config" [& params] (submit-config-form params))
           (GET "/background-image-red" [] (background-image-red))
           (GET "/background-image-yellow" [] (background-image-yellow))
           (GET "/background-image-green" [] (background-image-green))
           (GET "/background-image-error" [] (background-image-error))
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
