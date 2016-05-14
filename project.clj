(defproject jaggr "0.3.0-SNAPSHOT"

  :description "An aggregated CI traffic light for jenkins, based on the Jenkins Claims plugin"

  :url "https://github.com/puffedo/jaggr"

  :scm {:name "git"
        :url  "https://github.com/puffedo/jaggr"}

  :license {:name "Eclipse Public License"
            :url  "http://www.eclipse.org/legal/epl-v10.html"}

  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/tools.logging "0.3.1"]
                 [org.clojure/core.async "0.2.374"]
                 [ring/ring-core "1.4.0"]
                 [ring/ring-jetty-adapter "1.4.0"]
                 [compojure "1.5.0"]
                 [hiccup "1.0.5"]
                 [http-kit "2.1.19"]
                 [org.clojure/data.json "0.2.6"]
                 [com.grammarly/omniconf "0.2.2"]
                 [url-normalizer "0.5.3-1"]]

  :dev-dependencies [[ring/ring-devel "1.1.0"]]

  :plugins [[lein-ring "0.9.7"]
            [lein-cloverage "1.0.6"]]

  :ring {:handler jaggr.core/app
         :init    jaggr.core/init}

  :main jaggr.core

  :profiles
  {:dev
   {:dependencies [[http-kit.fake "0.2.2"]
                   [kerodon "0.7.0"]]}
   }
  )