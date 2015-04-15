(ns clustermap.components.filters.tag-filter
  (:require [om.core :as om :include-macros true]
            [om-tools.core :refer-macros [defcomponentk]]
            [plumbing.core :refer-macros [defnk]]
            [schema.core :as s]
            [sablono.core :as html :refer-macros [html]]
            [clustermap.filters :as filters]))

(defn ^:private get-select-value
  [components id]
  (or (get-in components [id :nested :filter :bool :must 1 :term "tag"])
      ""))

(defn ^:private get-tag-description
  [component-spec tag-spec]
  (when (and tag-spec
             (not (:omit-description tag-spec)))
    (str (:label component-spec) ": " (:label tag-spec))))

(defnk ^:private render*
  [[:component-spec id label sorted tag-type tags :as component-spec]
   [:filter-spec components :as filter-spec]]
  (let [select-value (get-select-value components id)]
    (html
     [:select {:value select-value
               :style {:width "100%"}
               :onChange (fn [e]
                           (let [val (-> e .-target .-value)

                                 f (when (not-empty val)
                                     {:nested {:path "?tags"
                                               :filter {:bool {:must [{:term {"type" tag-type}}
                                                                      {:term {"tag" val}}]}}}})
                                 tag-spec (->> tags (some (fn [ts] (when (= (:value ts) val) ts))))
                                 d (get-tag-description component-spec tag-spec)]
                             (.log js/console (clj->js ["TAG-FILTER" label val id f d]))
                             (om/update! filter-spec
                                         (filters/update-filter-component filter-spec id f d))))}
      (for [{:keys [value label]} tags]
        [:option {:value value}
         label])])))

(def TagFilterComponentSchema
  {:component-spec {:id s/Keyword
                    :type (s/eq :tag)
                    :label s/Str
                    (s/optional-key :sorted) s/Bool
                    :tag-type s/Str
                    :tags [{:value s/Str
                            :label s/Str
                            (s/optional-key :omit-description) (s/maybe s/Bool)}]}
   :filter-spec filters/FilterSchema})

;; a <select> filter
(defcomponentk tag-filter-component
  [data :- TagFilterComponentSchema
   owner]

  (render
   [_]
   (render* data)))
