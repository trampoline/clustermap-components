(ns clustermap.components.timeline-chart
  (:require-macros [hiccups.core :as hiccups])
  (:require
   [om.core :as om :include-macros true]
   [om-tools.core :refer-macros [defcomponent]]
   [jayq.core :refer [$]]
   [sablono.core :as html :refer-macros [html]]
   [clustermap.api :as api]
   [clustermap.ordered-resource :as ordered-resource]
   [clustermap.formats.number :as num]
   [clustermap.formats.money :as money]))

(defn create-chart
  [data node measure-variables {:keys [y0-title y1-title] :as opts}]
  (.log js/console (clj->js ["TIMELINE: " data]))
  (let [x-labels (->> data (map :timeline) (map #(js/Date. %)) (map #(.getYear %)) (map #(+ 1900 %)))
        stats (map :stats data)
        y-median (map (comp #(num/round-decimal % 0) #(get-in % [:stats :median])) data)
        y-mean (map (comp #(num/round-decimal % 0) #(get-in % [:stats :mean])) data)
        ;; y-total (map (comp #(num/round-decimal % 0) #(get-in % [:stats :total])) data)
        measure-variable (some-> measure-variables keyword)
        y-total (map measure-variable data)

        ;; y-total (into [] (concat (butlast yt) [(merge (last yt) {:color "#FF9900" :name "Not all data received for year"})]))
        ]

    (.log js/console (clj->js ["TIMELINE-VARS" measure-variable]))

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
                    ;; :type "logarithmic"
                    }
                   ;; {:title {:text y1-title} :opposite true}
                   ]
           :series [
                    ;; {:name (str "Median " y0-title)
                    ;;  :type "line"
                    ;;  :yAxis 0
                    ;;  :data y-median}
                    {:name (str "Total " y0-title)
                     :type "line"
                     :yAxis 0
                     :data y-total}
                    ;; {:name y1-title
                    ;;  :type "line"
                    ;;  :yAxis 1
                    ;;  :data y-count}
                    ]})))))

(defn- request-timeline-data
  [resource index index-type filter-spec _ time-variable interval metric-variables]
  (ordered-resource/api-call resource
                             api/timeline
                             index
                             index-type
                             filter-spec
                             nil
                             time-variable
                             nil
                             nil
                             interval
                             metric-variables)
  )

(defcomponent timeline-chart
  [{{{index :index
      index-type :index-type
      time-variable :time-variable
      measure-variables :measure-variables
      interval :interval
      :as controls} :controls
      timeline-data :timeline-data
     :as timeline-chart} :timeline-chart
     filter-spec :filter-spec
    :as props}
   owner
   {:keys [id] :as opts}]

  (render
   [_]
   (html [:div.timeline-chart {:id id :ref "chart"}]))

  (did-mount
   [_]
   (let [tdr (ordered-resource/make-discard-stale-resource "timeline-data-resource")]
     (om/set-state! owner :timeline-data-resource tdr)
     (ordered-resource/retrieve-responses tdr (fn [{data :data :as response}]
                                                (.log js/console (clj->js ["TIMELINE RESPONSE: " response]))
                                                (om/update! timeline-chart [:timeline-data] data))))

   (let [node (om/get-node owner)]
     (-> js/document
         $
         (.on "clustermap-change-view" (fn [e]
                                         ;; only reflow charts when they are visible
                                         ;; they disappear otherwise
                                         (let [chart (-> (om/get-node owner "chart") $)]
                                           (when (.is chart ":visible")
                                             (some-> chart
                                                     .highcharts
                                                     .reflow))))))))
  (will-update
   [_
    {{{next-index :index
       next-index-type :index-type
       next-time-variable :time-variable
       next-measure-variables :measure-variables
       next-interval :interval
       :as next-controls} :controls
       next-timeline-data :timeline-data
       :as next-timeline-chart} :timeline-chart
       next-filter-spec :filter-spec
        :as next-props}
    {next-timeline-data-resource :timeline-data-resource}]

   (.log js/console (clj->js ["FILTER_SPEC: " next-filter-spec]))
   (when (or (not next-timeline-data)
             (not= next-controls controls)
             (not= next-filter-spec filter-spec))

     (request-timeline-data next-timeline-data-resource
                            next-index
                            next-index-type
                            next-filter-spec
                            nil
                            next-time-variable
                            next-interval
                            next-measure-variables)))

  (did-update
   [_
    {{{prev-index :index
       prev-index-type :index-type
       prev-time-variable :time-variable
       prev-measure-variables :measure-variables
       prev-interval :interval
       :as prev-controls} :controls
       prev-timeline-data :timeline-data
       :as prev-timeline-chart} :timeline-chart
       {prev-filter-by-view :filter-by-view
        prev-compiled-filter :compiled
        :as prev-filter-spec} :filter-spec
        prev-bounds :bounds
        :as prev-props}
    _]
   (when (not= prev-timeline-data timeline-data)
     (create-chart timeline-data (om/get-node owner "chart") measure-variables opts))))
