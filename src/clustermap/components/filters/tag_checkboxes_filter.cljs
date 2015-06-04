(ns clustermap.components.filters.tag-checkboxes-filter
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

(defn ^:private get-tags-by-value
  [tags]
  (->> (for [t tags] [(:value t) t])
       (into {})))

(defn ^:private extract-tag-value-from-filter
  "given a single option filter, extract the option id this represents"
  [f]
  (get-in f [:nested :filter :bool :must 1 :term "tag"]))

(defn ^:private extract-tag-values-from-filters
  "given a combined option filter, extract the option ids this represents"
  [components id]
  (let [fs (get-in components [id :bool :should])]
    (->> fs
         (map extract-tag-value-from-filter)
         (filter identity)
         set)))

(defn ^:private tag-for-value
  [tags value]
  (->> tags
       (filter #(= value (:value %)))
       first))

(defn ^:private filter-for-tag-value
  "a filter for a single tag value"
  [{:keys [tag-type tags] :as component-spec}
   value]
  (when (not-empty value)
    (when-let [t (tag-for-value tags value)]
      {:nested {:path "?tags"
                :filter {:bool {:must [{:term {"type" tag-type}}
                                       {:term {"tag" value}}]}}}})))
(defn ^:private combine-filter-for-tag-values
  "given a seq of option values, combine a filter representing them"
  [component-spec values]
  (let [fs (map #(filter-for-tag-value component-spec %) values)]
    (filters/or-filters fs)))

(defn ^:private get-tags-description
  [{:keys [label tags sorted] :as component-spec}
   values]
  (let [values (set values)
        sel (->> tags (filter #(values (:value %))))]
    (when (not-empty sel)
      (str label ": " (str/join ", " (map :label sel))))))

(defn ^:private set-filters-for-values
  "return an updated filter-spec value (doesn't update the cursor)"
  [filter-spec
   {:keys [id tags] :as component-spec}
   values]
  (let [f (combine-filter-for-tag-values component-spec values)
        d (get-tags-description component-spec values)
        u (when (not-empty values) values)]
    (.log js/console (clj->js ["TAG-CHECKBOXES-FILTER" id val f d u]))
    (filters/update-filter-component filter-spec id f d u)))

(defnk ^:private render*
  [[:filter-spec components :as filter-spec]
   [:component-spec id label {sorted nil} tag-type tags :as component-spec]]
  (let [tags (if sorted (sort-by :label tags) tags)
        tags-by-value (get-tags-by-value tags)
        current-tag-values (extract-tag-values-from-filters components id)]
    (html
     [:ul.filter-items
      (for [{:keys [value label]} tags]
        [:li
         [:label
          [:div
           [:span.label
            [:input {:type "checkbox"
                     :name id
                     :value value
                     :checked (current-tag-values value)
                     :onChange (fn [e]
                                 (let [val (-> e .-target .-value)
                                       checked (-> e .-target .-checked)

                                       values (if checked
                                                (conj current-tag-values value)
                                                (disj current-tag-values value))]
                                   (om/update! filter-spec
                                               (set-filters-for-values filter-spec component-spec values))))}]
            label]]
          ;; [:div
          ;;  [:span.badge 123]]
          ]])])))

(def TagCheckboxesFilterComponentSchema
  {:filter-spec filters/FilterSchema
   :component-spec {:id s/Keyword
                    :type (s/eq :tag-checkboxes)
                    :label s/Str
                    (s/optional-key :visible) s/Bool
                    (s/optional-key :sorted) s/Bool
                    :tag-type s/Str
                    :tags [{:value s/Str
                            :label s/Str
                            (s/optional-key :omit-description) (s/maybe s/Bool)}]}})

;; a <select> filter
(defcomponentk tag-checkboxes-filter-component
  [[:data filter-spec [:component-spec id :as component-spec] :as data] :- TagCheckboxesFilterComponentSchema
   [:opts component-filter-rq-chan] :- {:component-filter-rq-chan ManyToManyChannel}
   owner]

  (did-mount
   [_]
   (go
     (while (when-let [[component-id rq] (<! component-filter-rq-chan)]
              (let [{:keys [component-spec filter-spec]} (om/get-props owner)]

                (.log js/console (clj->js ["TAG-CHECKBOXES-FILTER-RQ" id rq]))
                (om/update! filter-spec
                            (set-filters-for-values @filter-spec @component-spec rq))
                true)))))
  (render
   [_]
   (render* data)))
