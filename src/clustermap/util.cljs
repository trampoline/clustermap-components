(ns clustermap.util)

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
