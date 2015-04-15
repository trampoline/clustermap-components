(ns clustermap.components.filters.select-filter
  (:require [om.core :as om :include-macros true]
            [om-tools.core :refer-macros [defcomponentk]]
            [plumbing.core :refer-macros [defnk]]
            [schema.core :as s]
            [sablono.core :as html :refer-macros [html]]
            [clustermap.filters :as filters]))

(defnk ^:private get-options-by-value
  [options]
  (->> (for [o options] [(:value o) o])
       (into {})))

(defnk ^:private get-option-value
  "get the selected option-value by comparing the selected filter with the
   option filters"
  [[:component-spec id label options]
   [:filter-spec components]]
  (let [current-filter (get-in components [id])]
    (or (->> options
             (some (fn [o] (when (= (:filter o) current-filter) o)))
             :value)
        "")))

(defn ^:private get-option-description
  "describe the selected option"
  [component-spec option-spec]
  (when (and option-spec
             (not (:omit-description option-spec)))
    (str (:label component-spec) ": " (:label option-spec))))

(defnk ^:private render*
  [[:component-spec id label options :as component-spec]
   [:filter-spec components :as filter-spec]
   :as data]

  (let [current-option-value (get-option-value data)
        options-by-value (get-options-by-value component-spec)]

    (.log js/console (clj->js ["SELECT-OPTION" id current-option-value]))

    (html
     [:select {:value current-option-value
               :onChange (fn [e]
                           (let [val (-> e .-target .-value)
                                 option-spec (get options-by-value val)
                                 f (get option-spec :filter)
                                 d (get-option-description component-spec option-spec)]

                             (.log js/console (clj->js ["SELECT-FILTER" label val id f d]))
                             (om/update! filter-spec (filters/update-filter-component filter-spec id f d))))}

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
   :filter-spec filters/FilterSchema})

;; a <select> filter
(defcomponentk select-filter-component
  [data :- SelectFilterComponentSchema
   owner]

  (render
   [_]
   (render* data)))
