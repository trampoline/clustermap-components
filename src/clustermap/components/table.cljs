(ns clustermap.components.table
  (:require-macros
   [cljs.core.async.macros :refer [go go-loop]])
  (:require
   [cljs.core.async :refer [<!]]
   [clojure.spec :as s]
   [clustermap.api :as api]
   [clustermap.filters :as filters]
   [clustermap.formats.number :as num]
   [clustermap.util :as util :refer [pp inspect] :include-macros true]
   [om-tools.core :refer-macros [defcomponentk]]
   [om.core :as om :include-macros true]
   [sablono.core :as html :refer-macros [html]]))

(defn order-col
  "generate a table-ordering link for table-headers.
   Uses key or sort-key if provided"
  [controls
   {current-sort-spec :sort-spec :as table-data}
   {:keys [key sortable sort-key label render-fn] :as col}]

  (let [key (or sort-key key)
        current-sort-spec (if (sequential? current-sort-spec) (first current-sort-spec) current-sort-spec)
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
  (.log js/console (pp ["COLUMNS" columns]))
  (html
   [:div
    (om/build paginate {:controls controls :table-data table-data})
    [:div.table-responsive
     [:table.table.table-outlined
      [:thead
       (into [:tr]
             (for [col columns]
               (order-col controls table-data col)))]
      [:tbody ;; TODO: take unique keys as vec
       (om/build-all render-table-row (:data table-data) {:key :key :fn (fn [r] {:columns columns
                                                                                 :record r
                                                                                 :key (str (:?natural_id r ) (:?postcode r) (:!company_name r) (:?investment_uid r))})})
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
      fields :fields
      title :title
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
      (html [:div (when title
                    [:header [:h3 title]])
             (render-table {:table-data table-data :controls controls} owner {})]))

    om/IWillUpdate
    (will-update [_
                  {{next-table-data :table-data
                    {next-index :index
                     next-index-type :index-type
                     next-sort-spec :sort-spec
                     next-from :from
                     next-size :size
                     next-fields :fields
                     next-title :title
                     :as next-controls} :controls
                    :as next-table-state} :table-state
                   next-filter-spec :filter-spec
                   :as next-props}
                  {fetch-table-data-fn :fetch-table-data-fn}]

      (when (or (not next-table-data)
                (not= next-controls controls)
                (not= next-filter-spec filter-spec))
        (go
          (when-let [table-data
                     (<! (fetch-table-data-fn next-index
                                              next-index-type
                                              ((:filter-munge-fn next-controls identity) next-filter-spec)
                                              nil
                                              next-sort-spec
                                              next-from
                                              next-size
                                              next-fields))]
            (let [table-data ((:data-munge-fn next-controls identity) table-data)]
              (om/update! next-table-state [:table-data] table-data)))))
      )))

(s/def ::table (s/keys :req-un [::controls ::table-data] :opt-un [::type]))
(s/def ::tables (s/and (s/map-of keyword? ::table)))
(s/def ::table-state (s/and (s/keys :req-un [::tables ::current-table ::default-table])
                            #(contains? (:tables %) (:default-table %))
                            #(contains? (:tables %) (:current-table %))))

(s/def ::tables-props (s/keys :req-un [::table-state ::filter-spec]))

(defn multi-table-component
  "One component which can render one table of a number of tables at
  once"
  [{{:keys [table-data tables current-table default-table title?]
     :as   table-state} :table-state
    filter-spec         :filter-spec
    :as                 props}
   owner]
  (reify
    om/IWillMount
    (will-mount [_]
      (util/assert-spec ::tables-props props)
      ;;TODO: use subscription instead?
      (when-let [table-chan (om/get-shared owner :table-chan)]
        (go-loop []
          (when-some [op (<! table-chan)]
            (case op
              :clear (om/update! table-state :current-table default-table)
              (js/console.debug "Unknown op " op))
            (recur)))))

    om/IRender
    (render [_]
      (om/build table-component {:table-state (tables current-table) :filter-spec filter-spec}))))
