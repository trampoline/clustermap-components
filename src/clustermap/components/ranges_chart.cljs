(ns clustermap.components.ranges-chart
  (:require-macros
   [cljs.core.async.macros :refer [go]])
  (:require
   [cljs.core.async :refer [<!]]
   [om.core :as om :include-macros true]
   [om-tools.core :refer-macros [defcomponent]]
   [domina.events :as events]
   [sablono.core :as html :refer-macros [html]]
   [clustermap.api :as api]
   [clustermap.formats.html :as htmlf]
   [clustermap.util :as util :refer [pp]]
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
     merge-yAxis? :merge-yAxis?
     :as view} :view}]

  (let [y-keys (map :key y-axis)
        x-keys (map :key x-axis)
        xy-points (into {} (map (fn [{x :col y :row v :metric}] [[x y] v]) results))
        x-series-by-y (into {} (for [yk y-keys]
                                 [yk (into [] (for [xk x-keys] (get xy-points [xk yk])))]))

        chart {:chart {:type "column"
                       :renderTo node
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
                          :yAxis (if merge-yAxis? 0 i)
                          :data (get x-series-by-y (:key ya))})
               }]

    (js/console.log (pp ["RANGES-CHART" chart]))


    (js/Highcharts.Chart.
     (clj->js chart))))

(defcomponent ranges-chart-component
  [{{table-data :table-data
     query :query
     view :view
     :as table-state} :table-state
    filter-spec :filter-spec
    :as props}
   owner]

  (did-mount [_]
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

                                                     (some-> (om/get-state owner :chart)
                                                             .reflow))))))

    )

  (render [_]
    (if-let [data-available-fn (om/get-shared owner :data-available-fn)]
      (let [agg-field (get-in table-state [:query :col-aggs :col :range :field])
            show (data-available-fn filter-spec agg-field)]  ;; cambridge only
        (html [:span
               [:div {:ref "ranges-chart" :style (util/display show)}]
               [:div {:style (util/display (not show))} "Data not available"]]))
      (html [:div {:ref "ranges-chart"}])))

  (will-update
      [_
       {{next-table-data :table-data
         next-query :query
         next-view :view
         :as next-table-state} :table-state
        next-filter-spec :filter-spec
        :as next-props}
       {fetch-data-fn :fetch-data-fn}]

    (when (or (not next-table-data)
              (not= next-query query)
              (not= next-filter-spec filter-spec))

      (go
        (js/console.log fetch-data-fn)
        (when-let [ranges (<! (fetch-data-fn (merge next-query {:filter-spec next-filter-spec})))]
          (.log js/console (pp ["RANGES-TABLE-DATA" ranges]))
          (om/update! table-state [:table-data] ranges)))))

  (did-update
      [_
       {{prev-view :view
         prev-table-data :table-data} :table-state :as prev-props}
       _]

    (when (or (not= prev-table-data table-data)
              (not= prev-view view))
      (om/set-state! owner :chart (create-chart (om/get-node owner "ranges-chart") table-state)))))
