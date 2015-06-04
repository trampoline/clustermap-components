(ns clustermap.components.filters.select-filter
  (:require-macros
   [cljs.core.async.macros :refer [go]])
  (:require [cljs.core.async :refer [<!]]
            [cljs.core.async.impl.channels :refer [ManyToManyChannel]]
            [om.core :as om :include-macros true]
            [om-tools.core :refer-macros [defcomponentk]]
            [plumbing.core :refer-macros [defnk]]
            [schema.core :as s]
            [sablono.core :as html :refer-macros [html]]
            [clustermap.filters :as filters]))

(defn ^:private get-options-by-value
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

(defn ^:private filter-for-option-value
  [options value]
  (let [options-by-value (get-options-by-value options)
        option-spec (get options-by-value value)]
    (:filter option-spec)))

(defn ^:private get-option-description
  "describe the selected option"
  [{:keys [label options] :as component-spec}
   value]
  (let [option-spec (->> options (filter #(= value (:value %))) first)]
    (when (and option-spec
               (not (:omit-description option-spec)))
      (str label ": " (:label option-spec)))))

(defn ^:private set-filters-for-value
  [filter-spec
   {:keys [id options] :as component-spec}
   value]
  (let [f (filter-for-option-value options value)
        d (get-option-description component-spec value)]
    (.log js/console (clj->js ["SELECT-FILTER" id value  f d]))
    (filters/update-filter-component filter-spec id f d value)))

(defn sort-options
  [options]
  options
  (let [empty-option (->> options (filter #(= "" (:value %))) (into []))
        sorted (->> options (filter #(not= "" (:value %))) (sort-by :label))]
    ;; (js/console.log (clj->js ["SORT-OPTIONS" empty-option sorted]))
    (into empty-option sorted))
  )

(defnk ^:private render*
  [[:filter-spec components :as filter-spec]
   [:component-spec id label {sorted nil} options :as component-spec]
   :as data]

  (let [options (if sorted (sort-options options) options)
        current-option-value (get-option-value data)
        options-by-value (get-options-by-value options)]

    (.log js/console (clj->js ["SELECT-OPTION" id current-option-value]))

    (html
     [:ul.filter-items
      [:li
       [:select {:value current-option-value
                 :onChange (fn [e]
                             (let [val (-> e .-target .-value)]

                               (om/update! filter-spec (set-filters-for-value filter-spec component-spec val))))}

        (for [{:keys [value label] :as option} options]
          [:option {:value value} label])]]])))

(def SelectFilterComponentSchema
  {:filter-spec filters/FilterSchema
   :component-spec {:id s/Keyword
                    :type (s/eq :select)
                    :label s/Str
                    (s/optional-key :visible) s/Bool
                    (s/optional-key :sorted) s/Bool
                    :options [{:value (s/either s/Keyword s/Str)
                               :label s/Str
                               :filter (s/maybe {s/Keyword s/Any})
                               (s/optional-key :omit-description) (s/maybe s/Bool)}]}})

;; a <select> filter
(defcomponentk select-filter-component
  [[:data filter-spec [:component-spec id :as component-spec] :as data] :- SelectFilterComponentSchema
   [:opts component-filter-rq-chan] :- {:component-filter-rq-chan ManyToManyChannel}
   owner]

  (did-mount
   [_]
   (go
     (while (when-let [[component-id rq] (<! component-filter-rq-chan)]
              (let [{:keys [component-spec filter-spec]} (om/get-props owner)]

                (.log js/console (clj->js ["SELECT-FILTER-RQ" id rq]))
                (om/update! filter-spec
                            (set-filters-for-value @filter-spec @component-spec rq))
                true)))))
  (render
   [_]
   (render* data)))
