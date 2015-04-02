(ns clustermap.components.filters.tag-filter
  (:require [om.core :as om :include-macros true]
            [om-tools.core :refer-macros [defcomponentk]]
            [plumbing.core :refer-macros [defnk]]
            [schema.core :as s]
            [sablono.core :as html :refer-macros [html]]))

(defnk render*
  [[:component-spec id label sorted tag-type tags] components]
  (let [current-tag (get-in components [id :nested :filter :bool :must 1 :term "tag"])]
    (html
     [:select {:value (if current-tag current-tag "")
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
      [:option {:value ""} "Any"]
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
                            :label s/Str}]}
   :components {s/Keyword s/Any}})

;; a <select> filter
(defcomponentk tag-filter-component
  [data :- TagFilterComponentSchema
   owner]

  (render [_] (render* data)))
