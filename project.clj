(defproject clustermap-components "0.1.0-SNAPSHOT"
  :description "om components"
  :url "https://github.com/trampoline/clustermap-components"

  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/tools.macro "0.1.5"]
                 [org.clojure/clojurescript "1.7.228" :scope "provided"]
                 [org.clojure/core.async "0.2.374" :scope "provided"]
                 [org.omcljs/om "0.9.0"]
                 [prismatic/om-tools "0.4.0" :exclusions [org.clojure/clojure prismatic/schema]]
                 [prismatic/schema "1.0.4"]
                 [domina "1.0.3"]
                 [sablono "0.5.3"]
                 [hiccups "0.3.0"]
                 [secretary "1.2.3"]
                 [binaryage/devtools "0.4.1"]
                 [com.andrewmcveigh/cljs-time "0.4.0"]
                 [jayq "2.5.4"]] ;; let's get rid of this soon

  :profiles {:dev {:dependencies [[expectations "2.1.4"]]}}

  :plugins [[lein-expectations "0.0.7"]]

  :source-paths ["src"])
