(ns clustermap.util
  "Keep this small and promote members to meaningful modules"
  (:require #_[devtools.core :as devtools]
            ;; [devtools.custom-formatters]
            [om.core :as om]))

(defn chrome-canary? []
  (if-let [v (last (re-find #"Chrom(e|ium)/([0-9]+)\." js/navigator.userAgent))]
    (<= 47 (int v))
    false))

(def pp
  (if ^boolean js/goog.DEBUG
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
  node extract the DOM ref specified by name.
 DEPRECATED: use original om.core/get-node"
  [& args]
  (js/console.warn "DEPRECATED: use om.core/get-node again")
  (apply om/get-node args))

(defn make-sequential
  [x]
  (cond (nil? x) nil
        (sequential? x) x
        :else [x]))
