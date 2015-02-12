(ns clustermap.components.tag-filter
  (:require [om.core :as om :include-macros true]
            [om-tools.core :refer-macros [defcomponentk]]
            [schema.core :as s :refer-macros [defschema]]
            [sablono.core :as html :refer-macros [html]]))

(defn render*
  [filter-spec tag-filter-spec]
  (let [tag-type (:type tag-filter-spec)
        filter-component-id (str "tag-filter-" (name tag-type))
        label (:label tag-filter-spec)
        sorted (:sorted tag-filter-spec)
        tag-filter-spec (dissoc tag-filter-spec :type :label :sorted)
        tag-filter-spec (if sorted (->> tag-filter-spec (sort-by last)) tag-filter-spec)]
    (html
     [:div.tag-filter-component
      [:div.tbl
       [:div.tbl-row
        [:div.tbl-cell label]
        [:div.tbl-cell [:select {:style {:width "100%"}
                                 :onChange (fn [e]
                                             (let [val (-> e .-target .-value)]
                                               (.log js/console val)
                                               (om/update! filter-spec [:components filter-component-id]
                                                           (when (not-empty val)
                                                             {:nested {:path "?tags"
                                                                       :filter {:bool {:must [{:term {"type" tag-type}}
                                                                                              {:term {"tag" val}}]}}}}))))
                                 }
                        [:option {:value ""} "Any"]
                        (for [[tag descr] tag-filter-spec]
                          [:option {:value tag} descr])]]]]])))

(def TagFilterComponentSchema
  {:filter-spec {:components {:tag-filter s/Any
                              s/Keyword s/Any}
                 s/Keyword s/Any}
   :tag-filter-spec {:type s/Str
                     :label s/Str
                     s/Str s/Str}})

(defcomponentk tag-filter-component
  [[:data filter-spec tag-filter-spec] :- TagFilterComponentSchema
   owner]

  (render [_]
          (render* filter-spec tag-filter-spec)

                )

  (will-update [_ {:as next-props} {:as next-state}])

  )
