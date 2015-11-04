(defproject clustermap-components "0.1.0-SNAPSHOT"
  :description "om components"
  :url "https://github.com/trampoline/clustermap-components"

  :dependencies [[org.clojure/clojure "1.7.0"]
                 [org.clojure/clojurescript "1.7.166" :scope "provided"]
                 [org.clojure/core.async "0.2.371" :scope "provided"]
                 [om "0.8.0-alpha2"]
                 [prismatic/om-tools "0.4.0" :exclusions [org.clojure/clojure]]
                 [domina "1.0.3"]
                 [sablono "0.3.6"]
                 [hiccups "0.3.0"]
                 [secretary "1.2.3"]
                 [com.andrewmcveigh/cljs-time "0.3.14"]
                 [jayq "2.5.4"] ;; let's get rid of this soon
                 ]

  :source-paths ["src"])
