(ns clustermap.components.ranges-chart
  (:require-macros
   [cljs.core.async.macros :refer [go]])
  (:require
   [cljs.core.async :refer [<!]]
   [om.core :as om :include-macros true]
   [om-tools.core :refer-macros [defcomponent]]
   [domina.events :as events]
   [jayq.core :refer [$]]
   [sablono.core :as html :refer-macros [html]]
   [clustermap.api :as api]
   [clustermap.formats.html :as htmlf]
   [clustermap.components.table-common :as tc]))

(defn create-chart
  [node
   {{query :query
     results :results
     :as table-data} :table-data
     {title :title
      color :color
      x-axis :cols
      y-axis :rows
      render-fn :render-fn
      :as controls} :controls}]

  (let [y-keys (map :key y-axis)
        x-keys (map :key x-axis)
        xy-points (into {} (map (fn [{x :col y :row v :metric}] [[x y] v]) results))
        x-series-by-y (into {} (for [yk y-keys]
                                 [yk (into [] (for [xk x-keys] (get xy-points [xk yk])))]))

        chart {:chart {:type "column"
                       :width nil
                       :height 300}
               :title {:text nil}

               :xAxis {:categories (for [xa x-axis] (:label xa));; x-labels
                       ;; :labels {:rotation 270}
                       }

               :yAxis (for [[ya i] (map vector y-axis (iterate inc 0))]
                        {:title (:label ya)})

               :tooltip {:valueDecimals 0}

               :series (for [[ya i] (map vector  y-axis (iterate inc 0))]
                         {:name (:label ya)
                          :color color
                          :yAxis i
                          :data (get x-series-by-y (:key ya))})
               }]

    (js/console.log (clj->js ["RANGES-CHART" chart]))

    (-> node
        $
        (.highcharts
         (clj->js chart)))))

(defcomponent ranges-chart-component
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

  (did-mount
   [_]
   (om/set-state! owner :fetch-data-fn (api/ranges-factory))
   (let [node (om/get-node owner)
         last-dims (atom nil)
         w (.-offsetWidth node)
         h (.-offsetHeight node)]

     ;; only set last-dims if we are initialised on-screen... later
     ;; when chart shows, if last-dims is nil, we reflow again
     (when (and (> w 0) (> h 0))
       (reset! last-dims [w h]))

     (events/listen! "clustermap-change-view" (fn [e]
                                                ;; only reflow charts when they are visible
                                                ;; they disappear otherwise
                                                (let [w (.-offsetWidth node)
                                                      h (.-offsetHeight node)]

                                                  (when (and (> w 0)
                                                             (> h 0)
                                                             (not= @last-dims [w h]))

                                                    (some-> (om/get-node owner "ranges-chart")
                                                            $
                                                            .highcharts
                                                            .reflow))))))

   )

  (render
   [_]
   (html [:div {:ref "ranges-chart"}]))

  (will-update
   [_
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
     ))

  (did-update
   [_
    {{prev-table-data :table-data} :table-state :as prev-props}
    _]

   (when (not= prev-table-data table-data)
     (create-chart (om/get-node owner "ranges-chart") table-state))))
