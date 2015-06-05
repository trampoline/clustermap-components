(ns clustermap.components.metadata
  (:require-macros
   [cljs.core.async.macros :refer [go]])
  (:require [cljs.core.async :refer [<!]]
            [om.core :as om :include-macros true]
            [om-tools.core :refer-macros [defcomponentk]]
            [schema.core :as s :refer-macros [defschema]]
            [sablono.core :as html :refer-macros [html]]
            [clustermap.api :as api]))

(defn render-metadata-row
  [record {:keys [key label render-fn] :or {render-fn identity} :as field}]
  [[:div.tbl-cell label]
   [:div.tbl-cell (some-> (get record key) render-fn)]])

(defn render-metadata
  [record {:keys [title-field fields] :as controls}]
  (.log js/console (clj->js ["RECORD" record]))
  (html
   [:div.box.box-first
    [:header (get record title-field)]
    [:div.tbl
     (for [field fields]
       (into [:div.tbl-row]
             (render-metadata-row record field)))]]))

(defn metadata-component
  [{{record :record
     {index :index
      index-type :index-type
      sort-spec :sort-spec
      size :size
      :as controls} :controls
      :as metadata} :metadata
      filter-spec :filter-spec
      :as props}
   owner]

  (reify
    om/IDidMount
    (did-mount [_]
      (om/set-state! owner :fetch-metadata-fn (api/records-factory)))

    om/IRender
    (render [_]
      (render-metadata record controls))

    om/IWillUpdate
    (will-update [_
                  {{next-record :record
                    {next-index :index
                     next-index-type :index-type
                     next-sort-spec :sort-spec
                     next-size :size
                     :as next-controls} :controls
                    :as next-metadata} :metadata
                    next-filter-spec :filter-spec}
                  {fetch-metadata-fn :metadata-fn}]

      (when (or (not next-record)
                (not= next-controls controls)
                (not= next-filter-spec filter-spec))

        (go
          (when-let [metadata-data (<! (fetch-metadata-fn next-index
                                                      next-index-type
                                                      next-filter-spec
                                                      next-sort-spec
                                                      next-size))]
            (.log js/console (clj->js ["METADATA-DATA" metadata-data]))
            (om/update! metadata [:record] (some-> metadata-data :records first))))))))
