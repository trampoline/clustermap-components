(ns clustermap.util
  "Keep this small and promote members to meaningful modules"
  (:require [devtools.core :as devtools]))

(defn chrome-canary? []
  (if-let [v (last (re-find #"Chrom(e|ium)/([0-9]+)\." js/navigator.userAgent))]
    (<= 47 (int v))
    false))

(devtools/install!)
(def pp
  (if (and (chrome-canary?) #_(#'devtools/installed?))
    identity
    cljs.core/clj->js))

(defn error? [x]
  (instance? js/Error x))

(defn throw-err [x]
  (if (error? x)
    (throw x)
    x))

(defn display [show]
  (if show
    #js {}
    #js {:display "none"}))

(defn get-node
  "A helper function to get at React DOM refs. Given a owning pure
  node extract the DOM ref specified by name. Modded from Om for to
  stop warnings in react 0.14"
  ([owner]
   (ReactDOM.findDOMNode owner))
  ([owner name]
   {:pre [(string? name)]}
   (some-> (.-refs owner) (aget name) (ReactDOM.findDOMNode owner))))

(defn make-sequential
  [x]
  (cond (nil? x) nil
        (sequential? x) x
        :else [x]))
