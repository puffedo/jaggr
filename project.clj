(defproject jaggr "0.1.0"

  :description "An aggregated CI traffic light for jenkins, based on the Jenkins Claims plugin"

  :url "https://github.com/puffedo/jaggr"

  :scm {:name "git"
        :url "https://github.com/puffedo/jaggr"}

  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  :dependencies [[org.clojure/clojure "1.8.0"]
                 [ring/ring-core "1.4.0"]
                 [ring/ring-jetty-adapter "1.4.0"]
                 [compojure "1.4.0"]
                 [hiccup "1.0.5"]
                 [http-kit "2.1.19"]
                 [org.clojure/data.json "0.2.6"]
                 [com.grammarly/omniconf "0.2.2"]]

  :dev-dependencies [[ring/ring-devel "1.1.0"]
                     [http-kit.fake "0.2.2"]]

  :plugins [[lein-ring "0.7.1"]]

  :ring {:handler jaggr.routes/app
         :init    jaggr.routes/init}

  :main jaggr.routes

  )