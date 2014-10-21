(defproject clustermap-components "0.1.0-SNAPSHOT"
  :description "FIXME: write this!"
  :url "http://example.com/FIXME"

  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/clojurescript "0.0-2356" :scope "provided"]
                 [org.clojure/core.async "0.1.346.0-17112a-alpha" :scope "provided"]
                 [domina "1.0.2"]
                 [om "0.8.0-alpha1"]
                 [prismatic/om-tools "0.3.6" :exclusions [org.clojure/clojure]]
                 [sablono "0.2.22"]
                 [hiccups "0.3.0"]
                 [secretary "1.2.0"]
                 [com.andrewmcveigh/cljs-time "0.2.2"]]

  :source-paths ["src"])
