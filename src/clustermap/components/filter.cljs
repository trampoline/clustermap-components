(ns clustermap.components.filter
  (:require [om.core :as om :include-macros true]
            [om-tools.core :refer-macros [defcomponentk]]
            [plumbing.core :refer-macros [defnk]]
            [schema.core :as s]
            [sablono.core :as html :refer-macros [html]]
            [clustermap.filters :as filters]
            [clustermap.components.filters.select-filter :as select-filter]))

(defn render-filter-control
  [{:keys [components] :as filter-spec}
   {:keys [type] :as component-spec}]

  (condp = type
    (.log js/console (clj->js ["FILTER-CONTROL" {:component-spec component-spec
                                                 :components components} ]))
    :select (om/build select-filter/select-filter-component {:component-spec component-spec
                                                             :components components}))
  )

(defn render-filter-row
  [filter-spec
   {:keys [id label] :as component-spec}]

  [:div.tbl-row {:class (:id filter-spec)}
   [:div.tbl-cell label]
   [:div.tbl-cell
    (render-filter-control filter-spec component-spec)]])

(defnk render*
  [component-specs components :as filter-spec]
  (.log js/console (clj->js ["COMPONENT-SPECS" component-specs]))
  (html
   [:div.filter-component

    [:div.tbl
     (for [component-spec component-specs]
       (render-filter-row filter-spec component-spec))
     ]]))

(def FilterComponentSchema
  {:filter-spec {:component-specs [{:id s/Keyword
                                    :type s/Keyword
                                    :label s/Str
                                    s/Keyword s/Any}]
                 :components {s/Keyword s/Any}
                 (s/optional-key :base-filters) s/Any
                 (s/optional-key :composed) s/Any}})

(defcomponentk filter-component
  [[:data [:filter-spec components :as filter-spec]] :- FilterComponentSchema
   owner]

  (render [_] (render* filter-spec))

  (will-update [_
                {{next-component-specs :component-specs
                  next-components :components
                  next-base-filters :base-filters} :filter-spec}
                next-state]
               (when (or (not= next-components components))

                 (om/update! filter-spec [:composed] (filters/compose-filters next-components next-base-filters)))
               ))
