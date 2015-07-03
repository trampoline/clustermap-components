(ns clustermap.components.filter-description
  (:require [clojure.string :as str]
            [domina :as dom]
            [domina.css :as css]
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
                                (om/update! filter-spec (filters/update-filter-component filter-spec component-id nil nil nil)))}
      "\u00D7"]]))

(defcomponentk filter-description-component
  [[:data
    components
    filter-spec] :- FilterDescriptionSchema
    state
    owner]

  (render
   [_]
   (html
    [:div.filter-settings

     [:div.filter-buttons
      [:button.btn.btn-primary#filter-toggle {:type "button"
                                              :on-click (fn [e]
                                                          (let [new-open (not (:open filter-spec))]
                                                            (om/update! filter-spec [:open] (not (:open filter-spec)))
                                                            ;; (if new-open
                                                            ;;   (css/add-class! (css/sel "#data-container") "show-filters")
                                                            ;;   (css/remove-class! (css/sel "#data-container") "show-filters"))
                                                            ))}
       (if (:open filter-spec)
         "Close filter"
         "Open filter")]
      [:button.btn.btn-default#filter-reset {:type "button"
                                             :on-click (fn [e]
                                                         (om/update! filter-spec (filters/reset-filter filter-spec)))}
       "Clear filter"]]

     (into  [:ul.filter-selected-items]
            (some->> components
                     (map #(render-filter-component filter-spec %))
                     (filter identity)))])))
