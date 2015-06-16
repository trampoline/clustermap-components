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
  "given a seq of option values set the filters.
   returns an updated filter-spec value, but doesn't update the cursor"
  [filter-spec
   {:keys [id options] :as component-spec}
   values]
  (let [f (combine-filter-for-option-values options values)
        d (get-options-description component-spec values)
        u (when (not-empty values) values)]
    (.log js/console (clj->js ["CHECBOXES-FILTER" id val f]))
    (filters/update-filter-component filter-spec id f d u)))

(defn ^:private set-all
  "returns an updated filter-spec value with all checkboxes selected (doesn't update cursor)"
  [filter-spec
   {:keys [id options] :as component-spec}]
  (set-filters-for-values filter-spec component-spec (map :value options)))

(defn ^:private clear-all
  "returns an updated filter-spec value with all checkboxes cleared (doesn't update cursor)"
  [filter-spec
   {:keys [id options] :as component-spec}]
  (set-filters-for-values filter-spec component-spec nil))

(defnk ^:private render*
  [[:filter-spec components :as filter-spec]
   [:component-spec id label {sorted nil} {controls nil} options :as component-spec]
   :as data]

  (let [options (if sorted (sort-by :label options) options)
        options-by-value (get-options-by-value options)
        current-option-values (extract-option-values-from-filter options (get components id))]

    (.log js/console (clj->js ["SELECT-CHECKBOXES" id current-option-values]))

    (html
     [:div.filter-body
      (when controls
        [:div.filter-controls
         [:button.btn.btn-default.btn-sm
          {:onClick (fn [e] (om/update! filter-spec
                                        (set-all filter-spec component-spec)))}
          "Select all"]
         [:button.btn.btn-default.btn-sm
          {:onClick (fn [e] (om/update! filter-spec
                                        (clear-all filter-spec component-spec)))}
          "Clear"]])
      [:ul.filter-items
       (for [{:keys [value label filter]} options]
         [:li
          [:label
           [:div
            [:span.label
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

                                    (om/update! filter-spec
                                                (set-filters-for-values filter-spec component-spec values))))}]
             label]]
           ;; [:div
           ;;  [:span.badge 123]]
           ]])]])))

(def CheckboxesFilterComponentSchema
  {:filter-spec filters/FilterSchema
   :component-spec {:id s/Keyword
                    :type (s/eq :checkboxes)
                    :label s/Str
                    (s/optional-key :visible) s/Bool
                    (s/optional-key :sorted) s/Bool
                    (s/optional-key :controls) s/Bool
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
              (let [{:keys [filter-spec component-spec]} (om/get-props owner)]
                (.log js/console (clj->js ["CHECKBOXES-FILTER-RQ" id rq]))
                (om/update! filter-spec
                            (set-filters-for-values @filter-spec @component-spec rq))
                true)))))

  (render
   [_]
   (render* data)))
