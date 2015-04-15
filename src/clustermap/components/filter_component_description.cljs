(ns clustermap.components.filter-component-description
  (:require [om.core :as om :include-macros true]
            [om-tools.core :refer-macros [defcomponentk]]
            [schema.core :as s]
            [sablono.core :as html :refer-macros [html]]
            [clustermap.filters :as filters]))


(def FilterComponentDescriptionSchema
  {:filter-component-description {:component-key s/Keyword
                                  :default-text s/Str}
   :filter-spec {s/Keyword s/Any}})

(defcomponentk filter-component-description-component
  [[:data
    [:filter-component-description component-key default-text]
    filter-spec] :- FilterComponentDescriptionSchema
    owner]

  (render
   [_]
   (html
    (if (not-empty (get-in filter-spec [:components component-key]))
      [:span [:a {:href "#"
                  :onClick (fn [e]
                             (.preventDefault e)
                             (.log js/console "clear selection")
                             (om/update! filter-spec (filters/update-filter-component filter-spec component-key nil nil)))}
              "\u00D7"]
       "\u00a0"
       (get-in filter-spec [:component-descrs component-key])]
      [:span (or default-text "")]))))
