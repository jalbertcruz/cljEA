(defproject pools-based-ea "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [
                  [org.clojure/clojure "1.6.0"]
                  [org.clojure/data.json "0.2.5"]
                  ]
  :main ^:skip-aot pools-based-ea.core
  :target-path "target/%s"
  :resource-paths ["resources/EACommonLib.jar"]
  :profiles {:uberjar {:aot :all}})
