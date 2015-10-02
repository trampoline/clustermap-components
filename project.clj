(defproject clustermap-components "0.1.0-SNAPSHOT"
  :description "om components"
  :url "https://github.com/trampoline/clustermap-components"

  :dependencies [[org.clojure/clojure "1.7.0"]
                 [org.clojure/tools.macro "0.1.5"]
                 [org.clojure/clojurescript "1.7.122" :scope "provided"]
                 [org.clojure/core.async "0.1.346.0-17112a-alpha" :scope "provided"]
                 [org.omcljs/om "0.9.0"]
                 [prismatic/om-tools "0.3.11" :exclusions [org.clojure/clojure]]
                 [domina "1.0.3"]
                 [sablono "0.3.4"]
                 [hiccups "0.3.0"]
                 [secretary "1.2.3"]
                 [com.andrewmcveigh/cljs-time "0.3.13"]
                 [jayq "2.5.4"]] ;; let's get rid of this soon

  :source-paths ["src"])
