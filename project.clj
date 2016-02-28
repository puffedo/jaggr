(defproject jaggr "0.1.0"
  :description "An aggregated CI traffic light for jenkins, based on the Jenkins Claims plugin"
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [compojure "1.4.0"]
                 [hiccup "1.0.5"]
                 [http-kit "2.1.18"]
                 [org.clojure/data.json "0.2.6"]
                 [com.grammarly/omniconf "0.2.2"]

                 ;; test scope
                 [http-kit.fake "0.2.2"]]
  :plugins [[lein-ring "0.7.1"]]
  :ring {:handler jaggr.routes/app})
