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

(defn get-node
  "A helper function to get at React DOM refs. Given a owning pure
  node extract the DOM ref specified by name. Modded from Om for to
  stop warnings in react 0.14"
  ([owner]
   owner)
  ([owner name]
   {:pre [(string? name)]}
   (some-> (.-refs owner) (aget name))))
