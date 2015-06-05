(ns clustermap.components.map-report
  (:require-macros
   [cljs.core.async.macros :refer [go]])
  (:require [om.core :as om :include-macros true]
            [sablono.core :as html :refer-macros [html]]
            [cljs.core.async :refer [<!]]
            [clustermap.api :as api :include-macros true]
            [clustermap.formats.number :as nf :refer [fnum]]
            [clustermap.formats.money :as mf :refer [fmoney]]
            [clustermap.formats.string :as sf :refer [pluralize]]))

(defn summary-stats-report
  [{{{variables :variables
       :as summary-stats} :summary-stats
      :as controls} :controls}
   comm
   {data :data}]
  (.log js/console (clj->js ["SUMMARY-STATS-VARIABLES" variables]))
  (.log js/console (clj->js ["SUMMARY-STATS-DATA" data]))
  (let [belongs-to-vars (->> variables (filter :belongs-to) (group-by :belongs-to))]
    (html (into [:div.row.headline-stats]
                (for [row-variables (partition-all 2 (filter (complement :belongs-to) variables))]
                  [:div.col-md-6
                   (into [:div.row]
                         (for [{:keys [key metric label render-fn] :or {render-fn identity}} row-variables]
                           [:div.col-sm-6
                            [:h4.stat-title label]
                            [:div.stat-amount (render-fn (get-in data [key metric]))]
                            (when-let [{ch-key :key ch-metric :metric ch-value-fn :value-fn ch-render-fn :render-fn
                                        :or {ch-render-fn identity}}
                                       (first (not-empty (get belongs-to-vars key)))]
                              (let [p-v (get-in data [key metric])
                                    ch-v (get-in data [ch-key ch-metric])
                                    v (if ch-value-fn (ch-value-fn p-v ch-v) ch-v)]
                                (ch-render-fn v)))
                            ]))])))))

(defn map-report-component
  [{filt :filter
    {{{:keys [index index-type variables]
       :as summary-stats} :summary-stats
       :as controls} :controls
       summary-stats-data :summary-stats-data
       :as map-report} :map-report
    :as data}
   owner]

  (reify

    om/IDidMount
    (did-mount [_]
      (om/set-state! owner :fetch-data-fn (api/summary-stats-factory)))

    om/IRenderState
    (render-state [_ state]
      (let [{:keys [comm]} (om/get-shared owner)]
        (summary-stats-report map-report comm summary-stats-data)))

    om/IWillUpdate
    (will-update [_
                  {next-filt :filter
                   {{{next-index :index
                      next-index-type :index-type
                      next-variables :variables
                      :as next-summary-stats} :summary-stats
                      :as next-controls} :controls
                      next-summary-stats-data :summary-stats-data
                      :as next-map-report} :map-report
                   :as next-data}
                  {fetch-data-fn :fetch-data-fn}]

      (js/console.log "SUMMARY-STATS-I-WILL-UPDATE")

      (when (or (not next-summary-stats-data)
                (not= next-controls controls)
                (not= next-filt filt))
        (go
          (when-let [stats (<! (fetch-data-fn next-index next-index-type (map :key next-variables) next-filt nil))]
            (om/update! map-report [:summary-stats-data] stats)))))))
