(ns clustermap.components.action-link
  (:require [om.core :as om :include-macros true]
            [om-tools.core :refer-macros [defcomponentk]]
            [plumbing.core :refer [assoc-when]]
            [schema.core :as s]
            [sablono.core :as html :refer-macros [html]]))

(def ActionLinkSchema
  {:action-link {:content s/Any
                 :action s/Any
                 (s/optional-key :class) s/Str
                 (s/optional-key :id) s/Str}})

(defcomponentk action-link-component
  [[:data [:action-link content action {class nil} {id nil}]] :- ActionLinkSchema
   [:shared app]
   owner]
  (render [_]
    (html
     [:a (-> {:href "#"}
             (assoc-when :onClick (fn [e] (.preventDefault e) (action app owner)))
             (assoc-when :class class)
             (assoc-when :id id))
      (content)])))
