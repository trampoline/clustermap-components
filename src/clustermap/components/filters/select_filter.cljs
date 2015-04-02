(ns clustermap.components.filters.select-filter
  (:require [om.core :as om :include-macros true]
            [om-tools.core :refer-macros [defcomponentk]]
            [plumbing.core :refer-macros [defnk]]
            [schema.core :as s]
            [sablono.core :as html :refer-macros [html]]))

(defnk get-options-by-value
  [options]
  (->> (for [o options] [(:value o) o])
       (into {})))

(defnk get-option-value
  "get the selected option-value by comparing the selected filter with the
   option filters"
  [[:component-spec id label options] components]
  (let [current-filter (get-in components [id])]
    (or (->> options
             (some (fn [o] (when (= (:filter o) current-filter) o)))
             :value)
        "")))

(defn get-option-description
  "describe the selected option"
  [component-spec option-spec]
  (when (and option-spec
             (not (:omit-description option-spec)))
    (str (:label component-spec) ": " (:label option-spec))))

(defnk render*
  [[:component-spec id label options :as component-spec] components :as data]

  (let [current-option-value (get-option-value data)
        options-by-value (get-options-by-value component-spec)]

    (.log js/console (clj->js ["SELECT-OPTION" id current-option-value]))

    (html
     [:select {:value current-option-value
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
                               :filter (s/maybe {s/Keyword s/Any})
                               (s/optional-key :omit-description) (s/maybe s/Bool)}]}
   :components {s/Keyword s/Any}
   :component-descrs {s/Keyword s/Str}})

;; a <select> filter
(defcomponentk select-filter-component
  [[:data component-spec components component-descrs :as data] :- SelectFilterComponentSchema
   owner]

  (render
   [_]
   (render* data))

  (will-update
   [_
    {{next-id :id
      :as next-component-spec} :component-spec
      next-components :components
      next-component-descrs :component-descrs
      :as next-data}
    next-state]

   (let [options-by-value (get-options-by-value component-spec)
         next-option-value (get-option-value next-data)

         next-descr (get next-component-descrs next-id)
         correct-descr (get-option-description next-component-spec (get options-by-value next-option-value))]

     (when (not= next-descr correct-descr)
       (om/update! next-component-descrs [next-id] correct-descr)))))
