(ns clustermap.components.table
  (:require-macros
   [cljs.core.async.macros :refer [go]])
  (:require
   [cljs.core.async :refer [<!]]
   [om.core :as om :include-macros true]
   [sablono.core :as html :refer-macros [html]]
   [clustermap.api :as api]
   [clustermap.formats.number :as num]))

(defn order-col
  "generate a table-ordering link for table-headers"
  [controls
   {current-sort-spec :sort-spec :as table-data}
   {:keys [key sortable label render-fn] :as col}]

  (let [current-sort-spec (if (sequential? current-sort-spec) (first current-sort-spec) current-sort-spec)
        current-sort-key (some-> current-sort-spec keys first)
        current-sort-dir (some-> current-sort-spec current-sort-key :order)

        sort-dir (if (= current-sort-key key)
                   (condp = current-sort-dir
                     "asc" "sort-asc"
                     "sort-desc"))]
    (html
     [:th {:class sort-dir}
      (if sortable [:a
                    {:href "#"
                     :onClick (fn [e]
                                (.preventDefault e)
                                (if (= key current-sort-key)
                                  (condp = current-sort-dir
                                    "asc" (om/update! controls :sort-spec {key {:order :desc}})
                                    "desc" (om/update! controls :sort-spec {key {:order :asc}})
                                    (om/update! controls :sort-spec {key {:order :desc}}))

                                  (om/update! controls :sort-spec {key {:order (or current-sort-dir :desc)}})))}
                    (if (fn? label) (label) label)
                    [:i]]
          [:span label])])))


(defn paginate
  "generate a table pagination control"
  [{controls :controls
    {count :count
     from :from
     size :size
     :as table-data} :table-data} owner opts]
  (om/component
   (html
    [:div.table-nav
     [:div.record-count
      [:b (num/readable (inc from))]
      " to "
      [:b (num/readable (min (+ from size) count))]
      " of "
      [:b (num/readable count)]]

     [:nav
      [:button.btn.btn-default.btn-sm {:type "button"
                                       :onClick (fn [e]
                                                  (.preventDefault e)
                                                  (om/update! controls :from 0))}
       "First"]
      [:button.btn.btn-default.btn-sm {:type "button"
                                       :onClick (fn [e]
                                                  (.preventDefault e)
                                                  (om/update! controls :from (max 0 (- from size))))}
       "Previous"]
      [:button.btn.btn-default.btn-sm {:type "button"
                                       :onClick (fn [e]
                                                  (.preventDefault e)
                                                  (om/update! controls :from (+ from size))
                                                  )}
       "Next"]
      [:button.btn.btn-default.btn-sm {:type "button"
                                       :onClick (fn [e]
                                                  (.preventDefault e)
                                                  (om/update! controls :from (* size (quot count size))))}
       "Last"]]])))

(defn- render-table-row
  [{:keys [columns record]}]
  (om/component
   (html
    (let [row
          (into [:tr]
                (for [col columns]
                  (let [{:keys [key label right-align render-fn]} col
                        render-fn (or render-fn identity)]
                    ;; (.log js/console (clj->js [col-key col-name]))
                    ;; (.log js/console (clj->js ["KEYS" col-key (type col-key) col-name (type col-name) (get record col-key)]))
                    [:td {:class (when right-align "text-right")} (render-fn (get record key) record)])))
          ;; _ (.log js/console (clj->js ["ROW" columns record row]))
          ]
      row))))

(defn- render-table
  [{table-data :table-data
    {columns :columns
     filter-by-view :filter-by-view
     :as controls} :controls
    :as props}
   owner
   opts]
  (.log js/console (clj->js ["COLUMNS" columns]))
  (html
   [:div
    (om/build paginate {:controls controls :table-data table-data})
    [:div.table-responsive
     [:table.table.table-outlined
      [:thead
       (into [:tr]
             (for [col columns]
               (order-col controls table-data col)))]
      [:tbody
        (om/build-all render-table-row (:data table-data) {:key :key :fn (fn [r] {:columns columns
                                                                                  :record r
                                                                                  :key (:?natural_id r )})})
        ]]]
    (om/build paginate {:controls controls :table-data table-data})
     ])
  )

(defn table-component
  [{{table-data :table-data
     {index :index
      sort-spec :sort-spec
      from :from
      size :size
      columns :columns
      :as controls} :controls
     :as table-state} :table-state
     filter-spec :filter-spec
    :as props}
   owner]

  (reify
    om/IDidMount
    (did-mount [_]
      (om/set-state! owner :fetch-table-data-fn (api/simple-table-factory)))

    om/IRender
    (render [_]
      (render-table {:table-data table-data :controls controls} owner {}))

    om/IWillUpdate
    (will-update [_
                  {{next-table-data :table-data
                    {next-index :index
                     next-index-type :index-type
                     next-sort-spec :sort-spec
                     next-from :from
                     next-size :size
                     :as next-controls} :controls
                    :as next-table-state} :table-state
                   next-filter-spec :filter-spec
                   :as next-props}
                  {fetch-table-data-fn :fetch-table-data-fn}]

      (when (or (not next-table-data)
                (not= next-controls controls)
                (not= next-filter-spec filter-spec))
        (go
          (when-let [table-data (<! (fetch-table-data-fn next-index
                                                         next-index-type
                                                         next-filter-spec
                                                         nil
                                                         next-sort-spec
                                                         next-from
                                                         next-size))]
            (om/update! next-table-state [:table-data] table-data))))
      )))
