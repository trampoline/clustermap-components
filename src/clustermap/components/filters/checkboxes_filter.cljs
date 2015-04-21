(ns clustermap.components.filters.checkboxes-filter
  (:require-macros
   [cljs.core.async.macros :refer [go]])
  (:require [clojure.string :as str]
            [cljs.core.async :refer [<!]]
            [cljs.core.async.impl.channels :refer [ManyToManyChannel]]
            [om.core :as om :include-macros true]
            [om-tools.core :refer-macros [defcomponentk]]
            [plumbing.core :refer-macros [defnk]]
            [schema.core :as s]
            [sablono.core :as html :refer-macros [html]]
            [clustermap.filters :as filters]))

(defn make-sequential
  [s]
  (cond (nil? s) nil
        (sequential? s) s
        :else [s]))

(defn ^:private get-options-by-value
  "return a map of options keyed by value"
  [options]
  (->> (for [o options] [(:value o) o])
       (into {})))

(defn ^:private extract-option-values-from-filter
  "given a combined filter, extract the set of option ids this represents"
  [options f]
  (let [fs (-> f filters/de-or-filters set)]
    (->> options (filter #(fs (:filter %))) (map :value) set)))

(defn ^:private combine-filter-for-option-values
  "given a seq of option values, combine a filter representing them"
  [options values]
  (.log js/console (clj->js ["COMBINE-FILTER" options values]))
  (let [values (set values)
        fs (some->> options (filter #(values (:value %))) (map :filter))]
    (filters/or-filters fs)))

(defn ^:private get-options-description
  "describe the selected options given a seq of their values"
  [{:keys [label options] :as component-spec} values]
  (let [values (set values)
        sel (->> options (filter #(values (:value %))))]
    (when (not-empty sel)
      (str label ": " (str/join ", " (map :label sel))))))

(defn ^:private set-filters-for-values
  "given a seq of option values set the filters"
  [filter-spec
   {:keys [id options] :as component-spec}
   values]
  (let [f (combine-filter-for-option-values options values)
        d (get-options-description component-spec values)
        u (when (not-empty values) (js/JSON.stringify (clj->js values)))]
    (.log js/console (clj->js ["CHECBOXES-FILTER" id val f]))
    (om/update! filter-spec (filters/update-filter-component filter-spec id f d u))))

(defn ^:private set-filters-for-url-component
  "given a map of url components set the filters"
  [filter-spec
   {:keys [id] :as component-spec}
   values-str]
  (let [values (some-> values-str js/JSON.parse js->clj make-sequential)]
    (set-filters-for-values filter-spec component-spec values)))

(defnk ^:private render*
  [[:filter-spec components :as filter-spec]
   [:component-spec id label options :as component-spec]
   :as data]

  (let [options-by-value (get-options-by-value options)
        current-option-values (extract-option-values-from-filter options (get components id))]

    (.log js/console (clj->js ["SELECT-CHECKBOXES" id current-option-values]))

    (html
     [:div
      (for [{:keys [value label filter]} options]
        [:label
         [:input {:type "checkbox"
                  :name id
                  :value value
                  :checked (current-option-values value)
                  :onChange (fn [e]
                              (let [val (-> e .-target .-value)
                                    checked (-> e .-target .-checked)

                                    values (if checked
                                             (conj current-option-values value)
                                             (disj current-option-values value))]

                                (set-filters-for-values filter-spec component-spec values)))}]
         label])])))

(def CheckboxesFilterComponentSchema
  {:filter-spec filters/FilterSchema
   :component-spec {:id s/Keyword
                    :type (s/eq :checkboxes)
                    :label s/Str
                    :options [{:value (s/either s/Keyword s/Str)
                               :label s/Str
                               :filter (s/maybe {s/Keyword s/Any})
                               (s/optional-key :omit-description) (s/maybe s/Bool)}]}})

;; a <select> filter
(defcomponentk checkboxes-filter-component
  [[:data filter-spec [:component-spec id :as component-spec] :as data] :- CheckboxesFilterComponentSchema
   [:opts component-filter-rq-chan] :- {:component-filter-rq-chan ManyToManyChannel}
   owner]

  (did-mount
   [_]
   (go
     (while (when-let [[component-id rq] (<! component-filter-rq-chan)]

              (.log js/console (clj->js ["CHECKBOXES-FILTER-RQ" id rq]))
              (set-filters-for-url-component filter-spec component-spec rq)

              true))))

  (render
   [_]
   (render* data)))
