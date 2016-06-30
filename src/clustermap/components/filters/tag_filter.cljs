(ns clustermap.components.filters.tag-filter
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

(defn ^:private get-current-value
  [components id]
  (or (get-in components [id :nested :filter :bool :must 1 :term "tag"])
      ""))

(defn ^:private tag-for-value
  [tags value]
  (->> tags
       (filter #(= value (:value %)))
       first))

(defn ^:private filter-for-value
  [{:keys [tag-type tags] :as component-spec}
   value]
  (when (not-empty value)
    (when-let [t (tag-for-value tags value)]
      {:nested {:path "?tags"
                :filter {:bool {:must [{:term {"type" tag-type}}
                                       {:term {"tag" value}}]}}}})))

(defn ^:private get-tag-description
  [{:keys [label tags] :as component-spec}
   value]
  (let [tag-spec (tag-for-value tags value)]
    (when (and tag-spec
               (not (:omit-description tag-spec)))
      (str label ": " (:label tag-spec)))))

(defn ^:private set-filters-for-value
  "return an updated filter-spec value (doesn't update the cursor)"
  [filter-spec
   {:keys [id tags] :as component-spec}
   value]
  (let [f (filter-for-value component-spec value)
        d (get-tag-description component-spec value)]
    (.log js/console ["TAG-FILTER" id val f d])
    (filters/update-filter-component filter-spec id f d value)))

(defn sort-tags
  [tags]
  tags
  (let [empty-tag (->> tags (filter #(= "" (:value %))) (into []))
        sorted (->> tags (filter #(not= "" (:value %))) (sort-by :label))]
    ;; (js/console.log (clj->js ["SORT-TAGS" empty-tag sorted]))
    (into empty-tag sorted))
  )

(defnk ^:private render*
  [[:filter-spec components :as filter-spec]
   [:component-spec id label {sorted nil} tag-type tags {chooser-type :select}
    :as component-spec]]
  (let [tags (if sorted (sort-tags tags) tags)
        select-value (get-current-value components id)
        on-event (fn [e]
                   (let [val (-> e .-target .-value)]
                     (om/update! filter-spec
                                 (set-filters-for-value filter-spec component-spec val))))]
    (html
     [:div.filter-body
      [:ul.filter-items
       (case chooser-type
         :radio [:div
                 (for [{:keys [value label]} tags]
                   [:li {:key value}
                    [:label
                     [:div
                      [:span.label
                       [:input {:type "radio"
                                :name id
                                :value value
                                :checked (= value select-value)
                                :on-click on-event}]]
                      label]]])]
         :select [:li
                  [:select {:value select-value
                            :style {:width "100%"}
                            :onChange on-event}
                   (for [{:keys [value label]} tags]
                     [:option {:value value}
                      label])]])]])))

(s/def TagFilterComponentSchema
  "Choose between different tags using a select drop down or list of
  radio buttons"
  {:filter-spec filters/FilterSchema
   :component-spec {:id s/Keyword
                    :type (s/eq :tag)
                    :label s/Str
                    (s/optional-key :visible) s/Bool
                    (s/optional-key :sorted) s/Bool
                    (s/optional-key :default) s/Str
                    (s/optional-key :chooser-type) (s/enum :radio :select)
                    :tag-type s/Str
                    :tags [{:value s/Str
                            :label s/Str
                            (s/optional-key :omit-description) (s/maybe s/Bool)}]}})

;; a <select> filter
(defcomponentk tag-filter-component
  [[:data filter-spec [:component-spec id :as component-spec] :as data] :- TagFilterComponentSchema
   [:opts component-filter-rq-chan] :- {:component-filter-rq-chan ManyToManyChannel}
   owner]

  (did-mount [_]
    (when-let [default (:default component-spec)]
      (let [{:keys [component-spec filter-spec]} (om/get-props owner)]
        (om/update! filter-spec
                    (set-filters-for-value @filter-spec @component-spec default))))
    (go
      (while (when-let [[component-id rq] (<! component-filter-rq-chan)]
               (let [{:keys [component-spec filter-spec]} (om/get-props owner)]

                 (.log js/console (clj->js ["TAG-FILTER-RQ" id rq]))
                 (om/update! filter-spec
                             (set-filters-for-value @filter-spec @component-spec rq))
                 true)))))
  (render [_]
    (render* data)))
