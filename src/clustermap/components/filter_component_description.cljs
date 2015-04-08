(ns clustermap.components.filter-component-description
  (:require [om.core :as om :include-macros true]
            [om-tools.core :refer-macros [defcomponentk]]
            [schema.core :as s]
            [sablono.core :as html :refer-macros [html]]))


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
                             (om/update! filter-spec [:components component-key] nil)
                             (om/update! filter-spec [:component-descrs component-key] nil))}
              "\u00D7"]
       "\u00a0"
       (get-in filter-spec [:component-descrs component-key])]
      [:span (or default-text "")]))))
