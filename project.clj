(defproject jaggr "0.4.6-SNAPSHOT"

  :description "An aggregated CI traffic light for jenkins, based on the Jenkins Claims plugin"

  :url "https://github.com/puffedo/jaggr"

  :scm {:name "git"
        :url  "https://github.com/puffedo/jaggr"}

  :license {:name "Eclipse Public License"
            :url  "http://www.eclipse.org/legal/epl-v10.html"}

  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/tools.logging "0.4.0"]
                 [org.clojure/core.async "0.3.443"]
                 [ring/ring-core "1.6.2"]
                 [ring/ring-jetty-adapter "1.6.2"]
                 [compojure "1.6.0"]
                 [hiccup "1.0.5"]
                 [formative "0.8.9"]
                 [http-kit "2.2.0"]
                 [org.clojure/data.json "0.2.6"]
                 [com.grammarly/omniconf "0.2.6"]
                 [url-normalizer "0.5.3-1"]
                 [trptcolin/versioneer "0.2.0"]]

  :dev-dependencies [[ring/ring-devel "1.1.0"]]

  :plugins [[lein-ring "0.12.1"]
            [lein-cloverage "1.0.9"]]

  :ring {:handler jaggr.core/app
         :init    jaggr.core/init}

  :main jaggr.core

  :profiles {:dev     {:dependencies [[http-kit.fake "0.2.2"]
                                      [kerodon "0.8.0"]]}
             :uberjar {:aot :all}})
