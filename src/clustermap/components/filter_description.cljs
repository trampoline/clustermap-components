(ns clustermap.components.filter-description
  (:require [clojure.string :as str]
            [om.core :as om :include-macros true]
            [om-tools.core :refer-macros [defcomponentk]]
            [schema.core :as s]
            [sablono.core :as html :refer-macros [html]]
            [clustermap.filters :as filters]))

(def FilterDescriptionSchema
  {:components [s/Keyword]
   :filter-spec {s/Keyword s/Any}})

(defn render-filter-component
  [filter-spec component-id]
  (when-let [component-descr (get-in filter-spec [:component-descrs component-id])]
    [:span [:a {:href "#"
                :onClick (fn [e]
                           (.preventDefault e)
                           (om/update! filter-spec (filters/update-filter-component filter-spec component-id nil nil)))}
            "\u00D7"]
     component-descr]))

(defcomponentk filter-description-component
  [[:data
    components
    filter-spec] :- FilterDescriptionSchema
    owner]

  (render
   [_]
   (html
    [:span
     (some->> components
              (map #(render-filter-component filter-spec %))
              (filter identity))])))
