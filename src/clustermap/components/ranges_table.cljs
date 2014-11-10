(ns clustermap.components.ranges-table
  (:require
   [om.core :as om :include-macros true]
   [sablono.core :as html :refer-macros [html]]
   [clustermap.api :as api]
   [clustermap.ordered-resource :as ordered-resource]
   [clustermap.components.table-common :as tc]))

(defn- render-table
  [{{query :query
     results :results
     :as table-data} :table-data
     {title :title
      row-variable :row-variable
      row-ranges :row-ranges
      col-variable :col-variable
      col-ranges :col-ranges
      metric-variable :metric-variable
      metric :metric
      :as controls} :controls}
   owner
   opts]
  (html

    [:div "BOO"]))

(defn ranges-table-component
  [{{table-data :table-data
     {index :index
      index-type :index-type
      title :title
      row-variable :row-variable
      row-ranges :row-ranges
      col-variable :col-variable
      col-ranges :col-ranges
      metric-variable :metric-variable
      metric :metric
      :as controls} :controls
     :as table-state} :table-state
     filter-spec :filter-spec
    :as props}
   owner]

  (reify
    om/IDidMount
    (did-mount [_]
      (let [tdr (ordered-resource/make-discard-stale-resource "table-data-resource")]
        (om/set-state! owner :table-data-resource tdr)
        (ordered-resource/retrieve-responses tdr (fn [data]
                                                   (.log js/console (clj->js ["RANGES-TABLE-DATA" data]))
                                                   (om/update! table-state [:table-data] data))))
      )

    om/IRender
    (render [_]
      (render-table table-state owner {}))

    om/IWillUpdate
    (will-update [_
                  {{next-table-data :table-data
                    {next-index :index
                     next-index-type :index-type
                     next-title :title
                     next-row-variable :row-variable
                     next-row-ranges :row-ranges
                     next-col-variable :col-variable
                     next-col-ranges :col-ranges
                     next-metric-variable :metric-variable
                     next-metric :metric
                     :as next-controls} :controls
                    :as next-table-state} :table-state
                    next-filter-spec :filter-spec
                   :as next-props}
                  {table-data-resource :table-data-resource
                   :as next-state}]

      (when (or (not next-table-data)
                (not= next-controls controls)
                (not= next-filter-spec filter-spec))

        (ordered-resource/api-call table-data-resource
                                   api/ranges
                                   next-index
                                   next-index-type
                                   next-filter-spec
                                   next-row-variable
                                   next-row-ranges
                                   next-col-variable
                                   next-col-ranges
                                   next-metric-variable
                                   next-metric))
      )))
