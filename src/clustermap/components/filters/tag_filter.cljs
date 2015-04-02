(ns clustermap.components.filters.tag-filter
  (:require [om.core :as om :include-macros true]
            [om-tools.core :refer-macros [defcomponentk]]
            [plumbing.core :refer-macros [defnk]]
            [schema.core :as s]
            [sablono.core :as html :refer-macros [html]]))

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
  [[:component-spec id label sorted tag-type tags] components]
  (let [select-value (get-select-value components id)]
    (html
     [:select {:value select-value
               :style {:width "100%"}
               :onChange (fn [e]
                           (let [val (-> e .-target .-value)]
                             (.log js/console (clj->js ["TAG-FILTER" label id val]))
                             (om/update! components [id]
                                         (when (not-empty val)
                                           {:nested {:path "?tags"
                                                     :filter {:bool {:must [{:term {"type" tag-type}}
                                                                            {:term {"tag" val}}]}}}})
                                         )))}
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
   :components {s/Keyword s/Any}
   :component-descrs {s/Keyword s/Str}})

;; a <select> filter
(defcomponentk tag-filter-component
  [data :- TagFilterComponentSchema
   owner]

  (render
   [_]
   (render* data))

  (will-update
   [_
    {{next-id :id
      next-tags :tags
      :as next-component-spec} :component-spec
      next-components :components
      next-component-descrs :component-descrs
      :as next-data}
    next-state]
   (let [next-select-value (get-select-value next-components next-id)
         next-tag-spec (->> next-tags (some (fn [ts] (when (= (:value ts) next-select-value) ts))))

         next-descr (get next-component-descrs next-id)
         correct-descr (get-tag-description next-component-spec next-tag-spec)]
     (when (not= next-descr correct-descr)
       (om/update! next-component-descrs [next-id] correct-descr)))))
