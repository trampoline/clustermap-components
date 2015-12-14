(ns clustermap.components.text
  (:require [om.core :as om :include-macros true]
            [om-tools.core :refer-macros [defcomponentk]]
            [plumbing.core :refer [assoc-when]]
            [schema.core :as s]
            [sablono.core :as html :refer-macros [html]]))

(def TextComponentSchema
  {:source s/Any
   :controls {:path (s/cond-pre s/Keyword [s/Keyword])}})

(defn make-sequential
  [x]
  (cond (nil? x) nil
        (sequential? x) x
        :else [x]))

(defcomponentk text-component
  [[:data source [:controls path]] :- TextComponentSchema
   owner]
  (render
   [_]
   (html
    [:span (get-in source (make-sequential path))])))
