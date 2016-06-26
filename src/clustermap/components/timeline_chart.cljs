(ns clustermap.components.timeline-chart
  (:require-macros [hiccups.core :as hiccups]
                   [cljs.core.async.macros :refer [go]])
  (:require
   [om.core :as om :include-macros true]
   [om-tools.core :refer-macros [defcomponent]]
   [domina.events :as events]
   [cljs.core.async :refer [<!]]
   [sablono.core :as html :refer-macros [html]]
   [clustermap.api :as api]
   [clustermap.util :as util]
   [clustermap.formats.number :as num]
   [clustermap.formats.money :as money]))

(defn make-sequential
  [x]
  (cond (nil? x) nil
        (sequential? x) x
        :else [x]))

(defn create-chart
  [node {{:keys [metrics]} :query color :color records :timeline-data :as params}
   {:keys [y0-title y1-title] :as opts}]
  (.log js/console (clj->js ["TIMELINE: " records]))
  (let [x-labels (->> records (map :timeline) (map #(js/Date. %)) (map #(.getYear %)) (map #(+ 1900 %)))

        metrics (make-sequential metrics)

        ys (for [{:keys [variable metric title] :or {metric :sum} :as metric-spec} metrics]
             (assoc metric-spec :records
                    (for [record records]
                      (get-in record [(keyword variable) (keyword metric) ]))))]

    (.log js/console (clj->js ["CHART" {:metrics metrics
                                        :x-labels x-labels
                                        :ys ys}]))

    (js/Highcharts.Chart.
     (clj->js
      {:chart {:width nil
               :height 300
               :renderTo node}
       :title {:text nil}

       :xAxis {:categories x-labels
               :labels {:rotation 270}}

       :yAxis [{:title {:text y0-title}
                ;; :min 0
                :labels {:formatter (fn [] (this-as this (money/readable (.-value this) :sf 3 :curr "")))}
                }]

       :tooltip {:valueDecimals 0
                 :formatter (:point-formatter params)}

       :series (for [y ys]
                 {:name (:title y)
                  :type (or (:type y) "line")
                  :color color
                  :yAxis 0
                  :data (for [[name v] (zipmap x-labels (:records y))]
                          {:name name :y v})})}))))

(defcomponent timeline-chart
  [{{query :query
     timeline-data :timeline-data
     :as timeline-chart} :timeline-chart
     filter-spec :filter-spec}
   owner
   {:keys [id] :as opts}]

  (render [_]
    (if-let [data-available-fn (om/get-shared owner :data-available-fn)]
      (let [show (data-available-fn filter-spec)]  ;; cambridge only
        (html [:span
               [:div.timeline-chart {:id id :ref "chart" :style (util/display show)}]
               [:div {:style (util/display (not show))} "Data not available"]]))
      (html [:div.timeline-chart {:id id :ref "chart"}])))

  (did-mount
   [_]
   (let [node (om/get-node owner)
         last-dims (atom nil)
         w (.-offsetWidth node)
         h (.-offsetHeight node)]

     ;; only set last-dims if we are initialised on-screen... later
     ;; when chart shows, if last-dims is nil, we reflow again
     (when (and (> w 0) (> h 0))
       (reset! last-dims [w h]))

     ;; this fetch function discards data corresponding to superceded calls
     (om/set-state! owner :fetch-data-fn (api/timeline-factory))

     (events/listen! "clustermap-change-view" (fn [e]
                                                ;; only reflow charts when they are visible
                                                ;; they disappear otherwise
                                                (let [w (.-offsetWidth node)
                                                      h (.-offsetHeight node)]

                                                  (when (and (> w 0)
                                                             (> h 0)
                                                             (not= @last-dims [w h]))

                                                    (some-> (om/get-state owner :chart)
                                                            .reflow)))))))
  (will-update
   [_
    {{next-query :query
      next-timeline-data :timeline-data} :timeline-chart
      next-filter-spec :filter-spec}
    {fetch-data-fn :fetch-data-fn}]

   (.log js/console (clj->js ["FILTER_SPEC: " next-filter-spec]))
   (when (or (not next-timeline-data)
             (not= next-query query)
             (not= next-filter-spec filter-spec))

     (go
       (when-let [response (<! (fetch-data-fn next-query next-filter-spec))]
         (js/console.log (clj->js ["TIMELINE DATA" response]))
         (om/update! timeline-chart [:timeline-data] (:records response))))))

  (did-update
   [_
    {{prev-query :query
      prev-timeline-data :timeline-data} :timeline-chart
      prev-filter-spec :filter-spec}
    _]
   (when (or (not= prev-timeline-data timeline-data)
             (not= prev-query query))
     (om/set-state! owner :chart (create-chart (om/get-node owner "chart") timeline-chart opts)))))
