(ns clustermap.components.tag-histogram
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require
   [cljs.core.async :refer [<!]]
   [om.core :as om :include-macros true]
   [om-tools.core :refer-macros [defcomponent]]
   [domina.events :as events]
   [jayq.core :refer [$]]
   [sablono.core :as html :refer-macros [html]]
   [clustermap.api :as api]
   [clustermap.formats.number :as num]
   [clustermap.formats.money :as money]))

(defn make-sequential
  [x]
  (cond (nil? x) nil
        (sequential? x) x
        :else [x]))

(defn create-chart
  [node {:keys [] :as query} metrics tag-data tag-agg-data {:keys [y0-title y1-title] :as opts}]
  (.log js/console (clj->js ["TAG-HISTOGRAM-TAG-DATA: " tag-data]))
  (.log js/console (clj->js ["TAG-HISTOGRAM-TAG-AGG-DATA: " tag-agg-data]))
  (let [tags-by-tag (group-by :tag tag-data)
        tag-aggs-by-tag (group-by :nested_attr tag-agg-data)
        records (->> (merge-with concat tags-by-tag tag-aggs-by-tag)
                     vals
                     (map (fn [rs] (apply merge-with merge rs)))
                     (sort-by :description))


        x-labels (->> records (map :description))

        metrics (make-sequential metrics)

        ys (for [{:keys [metric title] :or {metric :sum} :as metric-spec} metrics]
             (assoc metric-spec :records
                    (for [record records]
                      (get-in record [(keyword metric) ]))))
        ]

    (.log js/console (clj->js ["TAGS-BY-TAG" tags-by-tag]))
    (.log js/console (clj->js ["TAG-AGGS-BY-TAG" tag-aggs-by-tag]))
    (.log js/console (clj->js ["RECORDS" records]))
    (.log js/console (clj->js ["METRICS" metrics]))
    (.log js/console (clj->js ["x-labels" x-labels]))
    (.log js/console (clj->js ["ys" ys]))

    ;; (.log js/console (clj->js ["CHART" {:metrics metrics
    ;;                                     :x-labels x-labels
    ;;                                     :ys ys}]))

    (-> node
        $
        (.highcharts
         (clj->js
          {:chart {:type "bar"
                   :width nil
                   :height 300}
           :title {:text nil}

           :xAxis {:categories x-labels
                   ;;:labels {:rotation 270}
                   }

           :yAxis (for [{:keys [title label-formatter]} ys]
                    {:title title
                     :labels {:formatter label-formatter}})

           :series (for [[y i] (map vector ys (iterate inc 0))]
                     {:name (:title y)
                      :yAxis i
                      :data (:records y)})})))
    ))

(defcomponent tag-histogram
  [{{query :query
     metrics :metrics
     tag-type :tag-type
     tag-data :tag-data
     tag-agg-data :tag-agg-data
     :as tag-histogram} :tag-histogram
     filter-spec :filter-spec}
   owner
   {:keys [id] :as opts}]

  (render
   [_]
   (html [:div.tag-histogram {:id id :ref "chart"}]))

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

     (om/set-state! owner :fetch-tag-data-fn (api/tags-of-type-factory))
     (om/set-state! owner :fetch-tag-agg-data-fn (api/nested-aggregation-factory))

     (events/listen! "clustermap-change-view" (fn [e]
                                                ;; only reflow charts when they are visible
                                                ;; they disappear otherwise
                                                (let [w (.-offsetWidth node)
                                                      h (.-offsetHeight node)]

                                                  (when (and (> w 0)
                                                             (> h 0)
                                                             (not= @last-dims [w h]))

                                                    (some-> (om/get-node owner "chart")
                                                            $
                                                            .highcharts
                                                            .reflow)))))))
  (will-update
   [_
    {{next-query :query
      next-metrics :metrics
      next-tag-type :tag-type
      next-tag-data :tag-data
      next-tag-agg-data :tag-agg-data} :tag-histogram
      next-filter-spec :filter-spec}
    {fetch-tag-data-fn :fetch-tag-data-fn
     fetch-tag-agg-data-fn :fetch-tag-agg-data-fn}]

   (when (or (not next-tag-data)
             (not= next-tag-type tag-type))

     (go
       (when-let [tag-data (<! (fetch-tag-data-fn next-tag-type))]
         (.log js/console (clj->js ["HISTOGRAM TAGS: " tag-data]))
         (om/update! tag-histogram [:tag-data] tag-data))))

   (when (or (not next-tag-agg-data)
             (not= next-query query)
             (not= next-metrics metrics)
             (not= next-filter-spec filter-spec))

     (go
       (when-let [tag-agg-data (<! (fetch-tag-agg-data-fn
                                   (merge next-query {:filter-spec next-filter-spec})))]
         (.log js/console (clj->js ["HISTOGRAM TAG AGGS: " tag-agg-data]))
         (om/update! tag-histogram [:tag-agg-data] (:records tag-agg-data))))))

  (did-update
   [_
    {{prev-query :query
      prev-metrics :metrics
      prev-tag-data :tag-data
      prev-tag-agg-data :tag-agg-data} :tag-histogram
      prev-filter-spec :filter-spec}
    _]
   (when (or (not= prev-metrics metrics)
             (not= prev-tag-data tag-data)
             (not= prev-tag-agg-data tag-agg-data))
     (create-chart  (om/get-node owner "chart") query metrics tag-data tag-agg-data opts))))
