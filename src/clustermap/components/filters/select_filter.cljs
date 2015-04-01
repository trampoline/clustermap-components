(ns clustermap.components.filters.select-filter
  (:require [om.core :as om :include-macros true]
            [om-tools.core :refer-macros [defcomponentk]]
            [plumbing.core :refer-macros [defnk]]
            [schema.core :as s]
            [sablono.core :as html :refer-macros [html]]))

(defnk render*
  [[:component-spec id label options] components]

  (let [options-by-id (->> options (map (fn [o] [(:id o) o])) (into {}))]
    (html
     [:select {:onChange (fn [e]
                           (let [val (-> e .-target .-value)]
                             (.log js/console (clj->js ["SELECT-FILTER" label id val]))
                             (om/update! components [id]
                                         (->> val (get options-by-id) :filter))))}
      (for [{:keys [id label] :as option} options]
        [:option {:value id} label])])))

(def SelectFilterComponentSchema
  {:component-spec {:id s/Keyword
                    :type (s/eq :select)
                    :label s/Str
                    :options [{:id (s/either s/Keyword s/Str)
                               :label s/Str
                               :filter (s/either (s/eq nil) {s/Keyword s/Any})}]}
   :components {s/Keyword s/Any}})

;; a <select> filter
(defcomponentk select-filter-component
  [data :- SelectFilterComponentSchema
   owner]

  (render [_] (render* data))

  (will-update [_ {:as next-props} {:as next-state}])
  )
