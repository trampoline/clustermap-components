(ns clustermap.components.filter
  (:require [om.core :as om :include-macros true]
            [om-tools.core :refer-macros [defcomponentk]]
            [plumbing.core :refer-macros [defnk]]
            [schema.core :as s]
            [sablono.core :as html :refer-macros [html]]
            [clustermap.filters :as filters]
            [clustermap.components.filters.select-filter :as select-filter]
            [clustermap.components.filters.tag-filter :as tag-filter]))

(defn ^:private parse-filter-url
  "delegate to filter-component type parsers for each fragment param
   which matches a component id"
  [{:keys [component-specs components component-descrs] :as filter-spec}]

  )

(defn ^:private encode-filter-url
  "delegate to filter-component type encoders for each present
   filter component"
  [{:keys [component-specs components component-descrs] :as filter-spec}]

  )

(defn ^:private render-filter-control
  [filter-spec
   {:keys [type] :as component-spec}]

  (condp = type

    :select (om/build select-filter/select-filter-component {:component-spec component-spec
                                                             :filter-spec filter-spec})
    :tag (om/build tag-filter/tag-filter-component {:component-spec component-spec
                                                    :filter-spec filter-spec})))

(defn ^:private render-filter-row
  [filter-spec
   {:keys [id label] :as component-spec}]

  [:div.tbl-row {:class (:id filter-spec)}
   [:div.tbl-cell label]
   [:div.tbl-cell
    (render-filter-control filter-spec component-spec)]])

(defnk ^:private render*
  [component-specs components :as filter-spec]
  (.log js/console (clj->js ["COMPONENT-SPECS" component-specs]))
  (html
   [:div.filter-component

    [:div.tbl
     (for [component-spec component-specs]
       (render-filter-row filter-spec component-spec))]]))

(def FilterComponentSchema
  {:filter-spec filters/FilterSchema})

(defcomponentk filter-component
  [[:data [:filter-spec components :as filter-spec]] :- FilterComponentSchema
   owner]

  (render [_] (render* filter-spec)))
