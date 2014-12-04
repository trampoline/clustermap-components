(ns clustermap.components.timeline-chart
  (:require-macros [hiccups.core :as hiccups])
  (:require
   [om.core :as om :include-macros true]
   [om-tools.core :refer-macros [defcomponent]]
   [domina.events :as events]
   [jayq.core :refer [$]]
   [sablono.core :as html :refer-macros [html]]
   [clustermap.api :as api]
   [clustermap.ordered-resource :as ordered-resource]
   [clustermap.formats.number :as num]
   [clustermap.formats.money :as money]))

(defn make-sequential
  [x]
  (cond (nil? x) nil
        (sequential? x) x
        :else [x]))

(defn create-chart
  [node {:keys [metrics]} records {:keys [y0-title y1-title] :as opts}]
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

    (-> node
        $
        (.highcharts
         (clj->js
          {:chart {:width nil
                   :height 300}
           :title {:text nil}

           :xAxis {:categories x-labels
                   :labels {:rotation 270}}

           :yAxis [{:title {:text y0-title}
                    :min 0
                    :labels {:formatter (fn [] (this-as this (money/readable (.-value this) :sf 3 :curr "")))}
                    }]

           :series (for [y ys]
                     {:name (:title y)
                      :type (or (:type y) "line")
                      :yAxis 0
                      :data (:records y)})})))))

(defn- request-timeline-data
  [resource query filter-spec]
  (ordered-resource/api-call resource
                             api/timeline
                             query
                             filter-spec))

(defcomponent timeline-chart
  [{{query :query
     timeline-data :timeline-data
     :as timeline-chart} :timeline-chart
     filter-spec :filter-spec}
   owner
   {:keys [id] :as opts}]

  (render
   [_]
   (html [:div.timeline-chart {:id id :ref "chart"}]))

  (did-mount
   [_]
   (let [node (om/get-node owner)
         tdr (ordered-resource/make-discard-stale-resource "timeline-data-resource")
         last-dims (atom nil)
         w (.-offsetWidth node)
         h (.-offsetHeight node)]

     ;; only set last-dims if we are initialised on-screen... later
     ;; when chart shows, if last-dims is nil, we reflow again
     (when (and (> w 0) (> h 0))
       (reset! last-dims [w h]))

     (om/set-state! owner :timeline-data-resource tdr)
     (ordered-resource/retrieve-responses tdr (fn [{records :records :as response}]
                                                (.log js/console (clj->js ["TIMELINE RESPONSE: " response]))
                                                (om/update! timeline-chart [:timeline-data] records)))

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
      next-timeline-data :timeline-data} :timeline-chart
      next-filter-spec :filter-spec}
    {next-timeline-data-resource :timeline-data-resource}]

   (.log js/console (clj->js ["FILTER_SPEC: " next-filter-spec]))
   (when (or (not next-timeline-data)
             (not= next-query query)
             (not= next-filter-spec filter-spec))

     (request-timeline-data next-timeline-data-resource
                            next-query
                            next-filter-spec)))

  (did-update
   [_
    {{prev-query :query
      prev-timeline-data :timeline-data} :timeline-chart
      prev-filter-spec :filter-spec}
    _]
   (when (not= prev-timeline-data timeline-data)
     (create-chart  (om/get-node owner "chart") query timeline-data opts))))
