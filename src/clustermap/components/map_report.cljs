(ns clustermap.components.map-report
  (:require [om.core :as om :include-macros true]
            [sablono.core :as html :refer-macros [html]]
            [clustermap.api :as api]
            [clustermap.ordered-resource :as ordered-resource]
            [clustermap.formats.number :as nf :refer [fnum]]
            [clustermap.formats.money :as mf :refer [fmoney]]
            [clustermap.formats.string :as sf :refer [pluralize]]))

(defn full-report-button
  [comm view-path-fn]
  (html [:a.btn.btn-link {:href (view-path-fn :lists)}
         [:i.icon-lists-sm]
         "Full report"]))

(defn summary-stats-report
  [{{{variables :variables
       :as summary-stats} :summary-stats
      :as controls} :controls}
   comm
   view-path-fn
   {data :data}]
  (.log js/console (clj->js ["SUMMARY-STATS-VARIABLES" variables]))
  (.log js/console (clj->js ["SUMMARY-STATS-DATA" data]))
  (html [:div
         [:ul
          (for [{:keys [key metric label render-fn] :or {render-fn identity}} variables]
            [:li (render-fn (get-in data [key metric]))
             [:small label]])]]))

(defn request-summary-stats
  [resource index index-type variables filt bounds]
  (ordered-resource/api-call resource
                             api/summary-stats
                             index
                             index-type
                             (map :key variables)
                             filt
                             bounds))

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
      (let [ssr (ordered-resource/make-discard-stale-resource "summary-stats")]
        (om/set-state! owner :summary-stats-resource ssr)
        (ordered-resource/retrieve-responses ssr (fn [ss] (om/update! map-report [:summary-stats-data] ss)))))

    om/IRenderState
    (render-state [_ state]
      (let [{:keys [comm path-fn view-path-fn]} (om/get-shared owner)]
        (summary-stats-report map-report comm view-path-fn summary-stats-data))
      )

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
                  {:keys [summary-stats-resource]
                   :as next-state}]

      (when (or (not next-summary-stats-data)
                (not= next-controls controls)
                (not= next-filt filt))
        (.log js/console (clj->js ["MAP-REPORT-FILTER" next-filt]))
        (request-summary-stats summary-stats-resource
                               next-index
                               next-index-type
                               next-variables
                               next-filt
                               nil)))

    )
  )
