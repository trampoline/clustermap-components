(defproject clustermap-components "0.1.0-SNAPSHOT"
  :description "om components"
  :url "https://github.com/trampoline/clustermap-components"

  :dependencies [[org.clojure/clojure "1.7.0"]
                 [org.clojure/tools.macro "0.1.5"]
                 [org.clojure/clojurescript "1.7.145" :scope "provided"]
                 [org.clojure/core.async "0.2.371" :scope "provided"]
                 [org.omcljs/om "0.9.0"]
                 [prismatic/om-tools "0.4.0" :exclusions [org.clojure/clojure prismatic/schema]]
                 [prismatic/schema "1.0.3"]
                 [domina "1.0.3"]
                 [sablono "0.3.6"]
                 [hiccups "0.3.0"]
                 [secretary "1.2.3"]
                 [binaryage/devtools "0.4.1"]
                 [com.andrewmcveigh/cljs-time "0.3.14"]
                 [jayq "2.5.4"]] ;; let's get rid of this soon

  :source-paths ["src"])
