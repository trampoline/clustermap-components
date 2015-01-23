(ns clustermap.components.metadata
  (:require [om.core :as om :include-macros true]
            [om-tools.core :refer-macros [defcomponentk]]
            [schema.core :as s :refer-macros [defschema]]
            [sablono.core :as html :refer-macros [html]]
            [clustermap.ordered-resource :as ordered-resource]
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
      (let [mdr (ordered-resource/make-discard-stale-resource "metadata-resource")]
        (om/set-state! owner :metadata-resource mdr)
        (ordered-resource/retrieve-responses mdr (fn [data]
                                                   (.log js/console (clj->js ["METADATA-DATA" data]))
                                                   (om/update! metadata [:record] (some-> data :records first))))))

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
                  {metadata-resource :metadata-resource}]

      (when (or (not next-record)
                (not= next-controls controls)
                (not= next-filter-spec filter-spec))

        (ordered-resource/api-call metadata-resource
                                   api/records
                                   next-index
                                   next-index-type
                                   next-filter-spec
                                   next-sort-spec
                                   next-size)))))
