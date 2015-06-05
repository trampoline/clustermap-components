(ns clustermap.components.rankings-table
  (:require-macros
   [cljs.core.async.macros :refer [go]])
  (:require
   [cljs.core.async :refer [<!]]
   [om.core :as om :include-macros true]
   [sablono.core :as html :refer-macros [html]]
   [clustermap.api :as api]
   [clustermap.formats.html :as htmlf]
   [clustermap.components.table-common :as tc]))


(defn- render-table-row
  [{:keys [columns record row-i]}]
  (om/component
   (html
    (let [row
          (into [:tr {:class (str "row-" row-i)}]
                (for [[col-i col] (map vector (iterate inc 1) columns)]
                  (let [{:keys [key name render-fn]} col
                        render-fn (or render-fn identity)]
                    ;; (.log js/console (clj->js [col-key col-name]))
                    ;; (.log js/console (clj->js ["KEYS" col-key (type col-key) col-name (type col-name) (get record col-key)]))
                    [:td {:class (htmlf/combine-classes (str "col-" col-i) (:class col))} (when key (render-fn (get record key) record))])))
          ;; _ (.log js/console (clj->js ["ROW" columns record row]))
          ]
      row))))

(defn- render-table
  [{{query :query
     results :results
     :as table-data} :table-data
    {merge-key :merge-key
     columns :columns
     :as controls} :controls
    :as props}
   owner
   opts]

  (let [col-value-descriptors (tc/column-value-descriptors columns)]

    (html
     [:div.full-report-list
      [:div.table-responsive
       [:table.table

        [:thead
         (tc/column-header-rows columns {:controls controls :current-sort-spec (:sort query)})]

        [:tbody
         (om/build-all render-table-row
                       (map vector (iterate inc 1) results)
                       {:key :key
                        :fn (fn [[row-i r]] {:row-i row-i
                                             :columns col-value-descriptors
                                             :merge-key merge-key
                                             :record r
                                             :key (:?natural_id r )})})
         ]]]
      ]))
  )

(defn rankings-table-component
  [{{table-data :table-data
     {index :index
      index-type :index-type
      sort-spec :sort-spec
      periods :periods
      metric-variables :metric-variables
      merge-key :merge-key
      fields :fields
      size :size
      columns :columns
      :as controls} :controls
     :as table-state} :table-state
     filter-spec :filter-spec
    :as props}
   owner]

  (reify
    om/IDidMount
    (did-mount [_]
      (om/set-state! owner :fetch-table-data-fn (api/rankings-factory)))

    om/IRender
    (render [_]
      (render-table {:table-data table-data :controls controls} owner {}))

    om/IWillUpdate
    (will-update [_
                  {{next-table-data :table-data
                    {next-index :index
                     next-index-type :index-type
                     next-sort-spec :sort-spec
                     next-periods :periods
                     next-metric-variables :metric-variables
                     next-merge-key :merge-key
                     next-fields :fields
                     next-size :size
                     next-columns :columns
                     :as next-controls} :controls
                    :as next-table-state} :table-state
                    next-filter-spec :filter-spec
                   :as next-props}
                  {fetch-table-data-fn :fetch-table-data-fn}]

      (when (or (not next-table-data)
                (not= next-controls controls)
                (not= next-filter-spec filter-spec))

        (go
          (when-let [table-data (<! (fetch-table-data-fn next-index
                                                         next-index-type
                                                         next-filter-spec
                                                         next-sort-spec
                                                         next-periods
                                                         next-metric-variables
                                                         next-merge-key
                                                         next-fields
                                                         next-size))]
            (.log js/console (clj->js ["RANKINGS-TABLE-DATA" table-data]))
            (om/update! table-state [:table-data] table-data)))))))
