(ns clustermap.components.action-button
  (:require [om.core :as om :include-macros true]
            [om-tools.core :refer-macros [defcomponentk]]
            [plumbing.core :refer [assoc-when]]
            [schema.core :as s]
            [sablono.core :as html :refer-macros [html]]))

(def ActionButtonSchema
  {:action-button {:text s/Str
                   :action s/Any
                   (s/optional-key :class) s/Str
                   (s/optional-key :id) s/Str}})

(defcomponentk action-button-component
  [[:data [:action-button text action class {id nil}]] :- ActionButtonSchema
   [:shared app]
    owner]
  (render
   [_]
   (html
    [:button.btn (-> {:type "button"}
                     (assoc-when :onClick action)
                     (assoc-when :class class)
                     (assoc-when :id id))
     text])))
