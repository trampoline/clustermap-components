(ns clustermap.components.filters-histogram
  "For creating a histogram from a filters aggregation"
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require
   [cljs.core.async :refer [<!]]
   [om.core :as om :include-macros true]
   [om-tools.core :refer-macros [defcomponent]]
   [schema.core :as s]
   [domina.events :as events]
   [sablono.core :as html :refer-macros [html]]
   [clustermap.api :as api]
   [clustermap.util :refer [pp make-sequential]]
   [clustermap.components.tag-histogram :refer [make-highchart]]
   [clustermap.formats.number :as num]
   [clustermap.formats.money :as money]))

;; schemas
(s/defschema TagData
  {:tag s/Str :description s/Str})

(s/defschema AggData
  {:in_nested s/Str
   :sum (s/maybe s/Num)
   s/Keyword (s/maybe s/Num)})

(def val-aggs (s/validator [AggData]))

(s/defschema Query
  {:index-name s/Str
   :index-type s/Str
   :nested-filter (s/cond-pre {s/Any s/Any} [s/Any])
   :stats-attr s/Str})
;;

(defn create-chart
  [node {:keys [query metrics
                tag-data tag-agg-data] :as params}
   {:keys [y0-title y1-title] :as opts}]
  {:pre [(val-aggs tag-agg-data)]}
  (.log js/console (pp ["FILTERS-HISTOGRAM-TAG-DATA: " tag-data]))
  (.log js/console (pp ["FILTERS-HISTOGRAM-TAG-AGG-DATA: " tag-agg-data]))

  (let [tags-by-tag (group-by :tag tag-data)
        tag-aggs-by-tag (group-by :in_nested tag-agg-data)
        records (->> (merge-with concat tags-by-tag tag-aggs-by-tag)
                     vals
                     (map (fn [rs] (apply merge-with merge rs)))
                     (sort-by (juxt :description :tag :in_nested)))

        x-labels (map #(or (:description %) (:tag %) (:in_nested %)) records)

        metrics (make-sequential metrics)

        ys (for [{:keys [metric title] :or {metric :sum} :as metric-spec} metrics]
             (assoc metric-spec :records
                    (for [record records]
                      (get-in record [(keyword metric)]))))]

    (.log js/console (pp ["TAGS-BY-TAG" tags-by-tag]))
    (.log js/console (pp ["TAG-AGGS-BY-TAG" tag-aggs-by-tag]))
    (.log js/console (pp ["RECORDS" records]))
    (.log js/console (pp ["METRICS" metrics]))
    (.log js/console (pp ["x-labels" x-labels]))
    (.log js/console (pp ["ys" ys]))

    (make-highchart (merge params {:node node :x-labels x-labels :ys ys}))))

(comment
  "[:query :nested-filter] might look like:"
  {"sectionA" {:range {"!sic07" {:gte "01110", :lt "05101"}}}
   "sectionB" {:range {"!sic07" {:gte "05101", :lt "10110"}}}})

(defcomponent filters-histogram
  [{{query :query
     metrics :metrics
     tag-data :tag-data
     tag-agg-data :tag-agg-data
     :as tag-histogram} :tag-histogram
    filter-spec :filter-spec} :- {:tag-histogram {:query Query
                                                  :tag-data [TagData]
                                                  s/Keyword s/Any}
                                  s/Keyword s/Any}
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

      (om/set-state! owner :fetch-tag-agg-data-fn (api/filters-aggregation-factory))

      (events/listen! "clustermap-change-view"
                      (fn [e]
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
         next-metrics :metrics
         next-tag-data :tag-data
         next-tag-agg-data :tag-agg-data} :tag-histogram
        next-filter-spec :filter-spec}
       {fetch-tag-agg-data-fn :fetch-tag-agg-data-fn}]
    (js/console.debug query)
    (when (or (not next-tag-agg-data)
              (not= next-query query)
              (not= next-metrics metrics)
              (not= next-filter-spec filter-spec))

      (go
        (when-let [tag-agg-data (<! (fetch-tag-agg-data-fn
                                     (merge next-query {:filter-spec next-filter-spec})))]
          (.log js/console (pp ["HISTOGRAM TAG AGGS: " tag-agg-data]))
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
      (om/set-state! owner :chart (create-chart (om/get-node owner "chart") tag-histogram opts)))))
