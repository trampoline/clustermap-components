(ns clustermap.components.filters.select-filter
  (:require [om.core :as om :include-macros true]
            [om-tools.core :refer-macros [defcomponentk]]
            [plumbing.core :refer-macros [defnk]]
            [schema.core :as s]
            [sablono.core :as html :refer-macros [html]]))

(defnk render*
  [[:component-spec id label options] components]

  (let [current-filter (get-in components [id])
        current-option-id (->> options (some (fn [o] (when (= (:filter o) current-filter) o))) :value)
        options-by-value (->> options (map (fn [o] [(:value o) o])) (into {}))]
    (.log js/console (clj->js ["SELECT-OPTION" id current-option-id current-filter]))
    (html
     [:select {:value (if current-option-id current-option-id "")
               :onChange (fn [e]
                           (let [val (-> e .-target .-value)]
                             (.log js/console (clj->js ["SELECT-FILTER" label id val]))
                             (om/update! components [id]
                                         (->> val (get options-by-value) :filter))))}
      (for [{:keys [value label] :as option} options]
        [:option {:value value} label])])))

(def SelectFilterComponentSchema
  {:component-spec {:id s/Keyword
                    :type (s/eq :select)
                    :label s/Str
                    :options [{:value (s/either s/Keyword s/Str)
                               :label s/Str
                               :filter (s/maybe {s/Keyword s/Any})}]}
   :components {s/Keyword s/Any}})

;; a <select> filter
(defcomponentk select-filter-component
  [data :- SelectFilterComponentSchema
   owner]

  (render [_] (render* data)))
