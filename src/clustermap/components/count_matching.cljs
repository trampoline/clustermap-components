(ns clustermap.components.count-matching
  (:require
   [om.core :as om :include-macros true]
   [sablono.core :as html :refer-macros [html]]
   [clustermap.api :as api]
   [clustermap.ordered-resource :as ordered-resource]
   [clustermap.formats.html :as htmlf]))


(defn- render-count
  [{{query :query
     {cnt :count
      :as results} :results
     :as data} :data
     {render-fn :render-fn
      :as controls} :controls}
   owner
   opts]
  (let [render-fn (or render-fn identity)]
    (html
     [:span cnt])))


(defn count-matching-component
  [{{data :data
     {:as controls} :controls
     :as props} :props
    filter-spec :filter-spec}
   owner]

  (reify
    om/IDidMount
    (did-mount [_]
      (let [dr (ordered-resource/make-discard-stale-resource "data-resource")]
        (om/set-state! owner :data-resource dr)
        (ordered-resource/retrieve-responses dr (fn [data]
                                                  (.log js/console (clj->js ["COUNT-MATCHING-DATA" data]))
                                                  (om/update! props [:data] data)))))

    om/IRender
    (render [_]
      (render-count props owner {}))

    om/IWillUpdate
    (will-update [_
                  {{next-data :data
                    {next-index :index
                     next-index-type :index-type
                     :as next-controls} :controls} :props
                   next-filter-spec :filter-spec}
                  {data-resource :data-resource}]

      (when (or (not next-data)
                (not= next-data data)
                (not= next-controls controls)
                (not= next-filter-spec filter-spec))

        (ordered-resource/api-call data-resource
                                   api/count-matching
                                   next-index
                                   next-index-type
                                   next-filter-spec)))))
