(ns clustermap.components.count-matching
  (:require-macros
   [cljs.core.async.macros :refer [go]])
  (:require
   [cljs.core.async :refer [<!]]
   [om.core :as om :include-macros true]
   [sablono.core :as html :refer-macros [html]]
   [clustermap.api :as api]
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
     [:span (render-fn cnt)])))


(defn count-matching-component
  [{{data :data
     {:as controls} :controls
     :as props} :props
    filter-spec :filter-spec}
   owner]

  (reify
    om/IDidMount
    (did-mount [_]
      (om/set-state! owner :fetch-count-data-fn (api/count-matching-factory)))

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
                  {fetch-count-data-fn :fetch-count-data-fn}]

      (when (or (not next-data)
                (not= next-data data)
                (not= next-controls controls)
                (not= next-filter-spec filter-spec))
        (go
          (when-let [count-data (<! (fetch-count-data-fn next-index
                                                         next-index-type
                                                         next-filter-spec))]
            (.log js/console (clj->js ["COUNT-MATCHING-DATA" data]))
            (om/update! props [:data] count-data)))))))
