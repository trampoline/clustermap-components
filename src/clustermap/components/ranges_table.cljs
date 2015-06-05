(ns clustermap.components.ranges-table
  (:require-macros
   [cljs.core.async.macros :refer [go]])
  (:require
   [cljs.core.async :refer [<!]]
   [om.core :as om :include-macros true]
   [sablono.core :as html :refer-macros [html]]
   [clustermap.api :as api]
   [clustermap.formats.html :as htmlf]
   [clustermap.components.table-common :as tc]))

(defn- render-table
  [{{query :query
     results :results
     :as table-data} :table-data
     {title :title
      rows :rows
      cols :cols
      render-fn :render-fn
      :as controls} :controls}
   owner
   opts]
  (let [rowcol (->> results
                    (map (fn [r] [[(:row r) (:col r)] r]))
                    (into {}))
        render-fn (or render-fn identity)]
    ;; (.log js/console (clj->js ["ROWCOL" rowcol]))
    (html

     [:div
      (when title [:h2 title])
      [:div.table-responsive
       [:table.table.table-outlined
        [:thead
         (->> (tc/column-header-rows cols {:insert-blank-col true}))]
        [:tbody
         (for [[row-i row] (map vector (iterate inc 1) rows)]
           [:tr {:class (str "row-" row-i)}
            [:td {:class "col-1"} (:label row)]
            (for [[col-i col] (map vector (iterate inc 2) cols)]
              (do
                ;; (.log js/console (clj->js (get rowcol [(:key row-range) (:key col-range)])))
                [:td {:class (htmlf/combine-classes (str "col-" col-i) (:class col))}
                 (when (and (:key row) (:key col))
                   (some->> [(:key row) (:key col)]
                            (get rowcol)
                            :metric
                            render-fn))])
              )])]
        ]]])

    ))

(defn ranges-table-component
  [{{table-data :table-data
     {index :index
      index-type :index-type
      title :title
      rows :rows
      row-path :row-path
      row-aggs :row-aggs
      cols :cols
      col-path :col-path
      col-aggs :col-aggs
      metric-path :metric-path
      metric-aggs :metric-aggs
      :as controls} :controls
     :as table-state} :table-state
     filter-spec :filter-spec
    :as props}
   owner]

  (reify
    om/IDidMount
    (did-mount [_]
      (om/set-state! owner :fetch-data-fn (api/ranges-factory)))

    om/IRender
    (render [_]
      (render-table table-state owner {}))

    om/IWillUpdate
    (will-update [_
                  {{next-table-data :table-data
                    {next-index :index
                     next-index-type :index-type
                     next-title :title
                     next-rows :rows
                     next-row-path :row-path
                     next-row-aggs :row-aggs
                     next-cols :cols
                     next-col-path :col-path
                     next-col-aggs :col-aggs
                     next-metric-path :metric-path
                     next-metric-aggs :metric-aggs
                     :as next-controls} :controls
                    :as next-table-state} :table-state
                    next-filter-spec :filter-spec
                   :as next-props}
                  {fetch-data-fn :fetch-data-fn}]

      (when (or (not next-table-data)
                (not= next-controls controls)
                (not= next-filter-spec filter-spec))

        (go
          (when-let [ranges (<! (fetch-data-fn next-index
                                               next-index-type
                                               next-filter-spec
                                               next-row-path
                                               next-row-aggs
                                               next-col-path
                                               next-col-aggs
                                               next-metric-path
                                               next-metric-aggs))]
            (.log js/console (clj->js ["RANGES-TABLE-DATA" ranges]))
            (om/update! table-state [:table-data] ranges)))
        ))))
