(set-env!
 :source-paths    #{"test"}
 :resource-paths  #{"resources" "src"}
 :dependencies '[[adzerk/boot-cljs          "1.7.228-1"   :scope "test"]
                 [adzerk/boot-cljs-repl     "0.3.0"      :scope "test"]
                 [adzerk/boot-reload        "0.4.8"      :scope "test"]
                 [pandeiro/boot-http        "0.7.3"      :scope "test"]
                 [crisptrutski/boot-cljs-test "0.2.1" :scope "test"]
                 [com.cemerick/piggieback "0.2.1" :scope "test"]
                 [org.clojure/tools.nrepl "0.2.12" :scope "test"]
                 [weasel "0.7.0" :scope "test"]
                 [seancorfield/boot-expectations "1.0.9" :scope "test"]

                 [org.clojure/clojurescript "1.8.51"]
                 [org.clojure/core.async "0.2.374"]
                 [org.clojure/tools.macro "0.1.5"]
                 ;;[com.taoensso/timbre "4.3.0-RC1"]
                 [org.omcljs/om "1.0.0-alpha32"]
                 [prismatic/om-tools "0.4.0" :exclusions [org.clojure/clojure prismatic/schema]]
                 [binaryage/devtools "0.5.2"]

                 [cljsjs/react "15.1.0-0"]
                 [cljsjs/react-dom "15.1.0-0"]
                 [domina "1.0.3"]
                 [jayq "2.5.4"]
                 [prismatic/schema "1.1.1"]
                 [sablono "0.7.2"]
                 [hiccups "0.3.0"]
                 [secretary "1.2.3"]
                 [better-cond "1.0.1"]
                 [cljsjs/bootstrap "3.3.6-1"]
                 [com.andrewmcveigh/cljs-time "0.4.0"]])

(require
 '[adzerk.boot-cljs      :refer [cljs]]
 '[adzerk.boot-cljs-repl :refer [cljs-repl start-repl]]
 '[adzerk.boot-reload    :refer [reload]]
 '[pandeiro.boot-http    :refer [serve]]
 '[crisptrutski.boot-cljs-test :refer [test-cljs]]
 '[seancorfield.boot-expectations :refer :all])

(task-options!
 pom {:project 'clustermap-components
      :version "0.2.0-SNAPSHOT"
      :description "om components"}
 jar {:main 'clustermap.app})

(deftask build []
  (comp (speak)
        (cljs)))

(deftask run []
  (comp ;;(serve)
   (watch)
   (cljs-repl)
   (reload)
   (build)))

(deftask production []
  (task-options! cljs {:optimizations :advanced
                       :source-map true})
  identity)

(deftask development []
  (task-options! cljs {:optimizations :none
                       :source-map true}
                 reload {:on-jsload 'clustermap.core/init})
  identity)

(deftask dev
  "Simple alias to run application in development mode"
  []
  (comp (development)
        (run)))

(deftask auto-install
  "Watch and install any changes as a jar for dependents to checkout.
   Dependents should
   have `(checkout :dependencies ['[clustermap-components \"0.1.0-SNAPSHOT\"]])`
   in the boot pipleline"
  []
  (comp
   (watch)
   (pom)
   (jar)
   (install)))

;; below not used yet

(deftask testing []
  (set-env! :source-paths #(conj % "test/cljs"))
  identity)

;;; This prevents a name collision WARNING between the test task and
;;; clojure.core/test, a function that nobody really uses or cares
;;; about.
(ns-unmap 'boot.user 'test)

(deftask test []
  (comp (testing)
        (test-cljs :js-env :phantom
                   :exit?  true)))

(deftask auto-test []
  (comp (testing)
        (watch)
        (test-cljs :js-env :phantom)))
