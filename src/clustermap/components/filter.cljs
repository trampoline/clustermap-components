(ns clustermap.components.filter
  (:require [om.core :as om :include-macros true]
            [sablono.core :as html :refer-macros [html]]
            [clustermap.filters :as filters]))

(defn render-filter-row
  [{:keys [components] :as filter-spec} {:keys [label options] :as component-spec}]
  (let [options-by-key (->> options (map (fn [o] [(:key o) o])) (into {}))]
    [:div.tbl-row
     [:div.tbl-cell label]
     [:div.tbl-cell [:select {:onChange (fn [e]
                                          (let [val (-> e .-target .-value)]
                                            (.log js/console (clj->js ["SELECT-FILTER" label ]) val)
                                            (om/update! filter-spec [:components key]
                                                        (->> val (get options-by-key) :filter))))}
                     (for [{:keys [key label] :as option} options]
                       [:option {:value key} label])]]]))

(defn render
  [{:keys [components component-specs] :as filter-spec}]
  (.log js/console (clj->js ["COMPONENT-SPECS" component-specs]))
  (html
   [:div.filter-component

    [:div.tbl
     (for [component-spec component-specs]
       (render-filter-row filter-spec component-spec))
     ]]))

(defn filter-component
  [{{components :components
     base-filters :base-filters
     component-specs :component-specs
     :as filter-spec} :filter-spec
    :as props}
   owner]

  (reify

    om/IRenderState
    (render-state [_ state]
      (render filter-spec))

    om/IWillUpdate
    (will-update [_
                  {{next-components :components
                    next-base-filters :base-filters
                    next-component-specs :component-specs} :filter-spec}
                  next-state]
      (when (or (not= next-components components))

        (om/update! filter-spec [:composed] (filters/compose-filters next-components next-base-filters))))))
