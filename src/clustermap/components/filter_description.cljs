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
    [:li
     [:span component-descr]
     [:button.close {:type "button"
                     :onClick (fn [e]
                                (.preventDefault e)
                                (om/update! filter-spec (filters/update-filter-component filter-spec component-id nil nil)))}
      "\u00D7"]]))

(defcomponentk filter-description-component
  [[:data
    components
    filter-spec] :- FilterDescriptionSchema
    owner]

  (render
   [_]
   (html
    [:div.filter-settings
     
     [:div.filter-buttons
      [:button.btn.btn-primary#filter-toggle {:type "button"}
       "Open Filter"]
      [:button.btn.btn-default#filter-reset {:type "button"
                                             :onClick (fn [e]
                                                        (om/update! filter-spec (filters/reset-filter filter-spec)))}
       "Reset"]]
     
     (into  [:ul.filter-selected-items]
            (some->> components
                     (map #(render-filter-component filter-spec %))
                     (filter identity)))])))
