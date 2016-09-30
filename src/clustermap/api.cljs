(ns clustermap.api
  (:require-macros
   [clustermap.lastcall-method :refer [def-lastcall-method def-lastcall-method-factory defnk-lastcall-method-factory]]
   [cljs.core.async.macros :refer [go]]
   plumbing.core)
  (:require
   [clojure.string :as str]
   [cljs.core.async :as async :refer [<! chan close! put! sliding-buffer to-chan]]
   [goog.net.XhrIo :as xhr]
   [clustermap.lastcall-method]))

;;TODO: use cljs-http or similar
(defn AJAX [url & {:keys [raw method content send-error] :as opts}]
  "send a request, returning a channel with a single result value.
  If send-error is truthy will put an exception on the chan if there
  is an error."
  (let [comm (async/promise-chan
              (map (fn [event]
                     (js/console.log event)
                     (let [target (.-target event)
                           error-code (.getLastErrorCode target)
                           response-text (.getResponseText target)
                           response (if (= error-code 0)
                                      (-> response-text
                                          js/JSON.parse
                                          (aget "data")
                                          ((fn [d]
                                             (if raw
                                               d
                                               (js->clj d :keywordize-keys true)))))
                                      ;; else
                                      (do (if send-error
                                            (ex-info "Ajax error" {:error-code error-code
                                                                   :response-text response-text
                                                                   :status (.getStatus target)})
                                            (js/console.warn "Unhandled XHR error, use :send-error to handle:" url))
                                          (when (and (exists? js/Raven) (js/Raven.isSetup))
                                            (js/Raven.captureMessage
                                             (str "Ajax error " url)
                                             #js {:extra #js {:error-code error-code
                                                              :response-text response-text
                                                              :status (.getStatus target)}}))
                                          response-text))]
                       response)
                                        ;(close! comm)
                     ))
              (fn [e] (js/console.warn " error " e) e))]
    (xhr/send url
              (partial put! comm)
              (or (some-> method name str/upper-case) "GET")
              (when content (js/JSON.stringify (clj->js content)))
              (when content (clj->js {"Content-Type" "application/json"})))
    comm))

(defn GET [url & {:as opts}]
  (apply AJAX url (apply concat (merge opts {:method "GET"}))))

(defn POST [url content & {:as opts}]
  (apply AJAX url (apply concat (merge opts {:method "POST" :content content}))))

(defn PUT [url content & {:as opts}]
  (apply AJAX url (apply concat (merge opts {:method "PUT" :content content}))))

(defn log-api
  [f & args]
  (let [ch (apply f args)]
    (go
     (.log js/console (<! ch)))))

(defn map-json-params
  [m]
  (->> m
       (map (fn [[k v]] (str (name k) "=" (js/JSON.stringify (clj->js v)))))
       (str/join "&")))

(defn fetch
  "put the results of an api call into an atom... makes ad-hoc api calls easy"
  [comm]
  (let [r (atom nil)]
    (go
      (reset! r (<! comm)))
    r))

(def api-prefix
  (let [config (goog.object.get js/window "config")]
    (-> (goog.object.get config "api")
        (goog.object.get "prefix"))))
;; boundarylines

(defn boundarylines
  [id tolerance & opts]
  (apply GET (str "/api/boundarylines/" id "/" tolerance) opts))

(defn boundaryline-collection-index
  [id & opts]
  (apply GET (str "/api/boundaryline-collection-index/" (name id)) opts))

(defn boundaryline-set
  "fetch a whole set of boundarylines in one api hit"
  [ids tolerance & opts]
  (apply POST "/api/boundaryline-set"
         {:ids ids
          :tolerance tolerance}
         opts))

(defn boundaryline-collection-view
  [collection-id tolerance bounds & {:keys [boundaryline-ids] :as opts}]
  (let [opts (-> opts (dissoc :boundaryline-ids) seq flatten)]
    (apply POST (str "/api/boundaryline-collection-view/" (name collection-id) "/" tolerance)
           {:bounds bounds
            :boundaryline-ids boundaryline-ids}
           opts)))

;; aggregation over boundarylines

(def-lastcall-method-factory boundaryline-aggregation-factory
  [index type blcoll attr filter bounds scale-attr post-scale-factor & [type-ids]]
  (POST (str "/api/" api-prefix "/boundaryline-agg/" index "/" type "/" blcoll "/" attr "?" (map-json-params type-ids))
      {:filter filter
       :bounds bounds
       :scale-attr scale-attr
       :post-scale-factor post-scale-factor}))

(def-lastcall-method-factory nested-aggregation-factory
  [{:keys [index-name index-type filter-spec sort-spec nested-path nested-filter nested-attr stats-attr] :as q}]
  (POST (str "/api/" api-prefix "/nested-agg")
      q))

(def-lastcall-method-factory filters-aggregation-factory
  [{:keys [index-name index-type filter-spec nested-filter stats-attr] :as q}]
  (POST (str "/api/" api-prefix "/filters-agg")
      q))

;; summary stats
(def-lastcall-method-factory summary-stats-factory
  [index type statsattrs filter bounds & [type-ids]]
  (POST (str "/api/" api-prefix "/summary-stats/" index "/" type "?" (map-json-params type-ids))
      {:statsattrs statsattrs
       :filter filter
       :bounds bounds}))

;; points in view
(def-lastcall-method-factory location-lists-factory
  [index type location-attr attrs max-count filter bounds & [type-ids]]
  (POST (str "/api/" api-prefix "/location-lists/" index "/" type "/" location-attr "?" (map-json-params type-ids))
      {:max-count max-count
       :attrs attrs
       :filter filter
       :bounds bounds}))

(defnk-lastcall-method-factory location-lists-2-factory
  "saner version"
  [index-name index-type filter-spec bounds location-attr attrs sort-spec max-count :as args]
  (POST (str "/api/" api-prefix "/location-lists/" index-name "/" index-type "/" location-attr) args))


;; tabular data
(def-lastcall-method-factory simple-table-factory
  [index type filter-spec bounds sort-spec from size & [type-ids]]
  (POST (str "/api/" api-prefix "/simple-table/" index "/" type "?" (map-json-params type-ids))
      {:filter filter-spec
       :bounds bounds
       :sort sort-spec
       :from from
       :size size}))

(def-lastcall-method-factory timeline-factory
  [query filter-spec]
  (POST (str "/api/" api-prefix "/timeline")
      {:query query
       :filter-spec filter-spec}))

(def-lastcall-method-factory geo-sponsors-factory
  [bounds]
  (GET (str "/api/" api-prefix "/geo-sponsors?" (map-json-params {:bounds bounds}))))

(def-lastcall-method-factory rankings-factory
  [index index-type filter-spec sort-spec periods metric-variables merge-key fields size]
  (POST (str "/api/" api-prefix "/rankings")
      {:index-name index
       :index-type index-type
       :filter filter-spec
       :sort sort-spec
       :periods periods
       :metric-variables metric-variables
       :merge-key merge-key
       :fields fields
       :size size}))

(defnk-lastcall-method-factory ranges-factory
  [index-name index-type filter-spec row-path row-aggs col-path col-aggs metric-path metric-aggs :as args]
  (POST (str "/api/" api-prefix "/ranges")
        args))

(defnk-lastcall-method-factory geohash-grid-factory
  "LOOK AT ME : this is the way to do it, with defnk... check out the server-side too
   clustermap.datasets.companies.web-geohash"
  [index-name index-type filter-spec bounds geo-point-field precision :as args]
  (POST (str "/api/" api-prefix "/geohash-grid")
        args))

(def-lastcall-method-factory count-matching-factory
  [index index-type filter-spec]
  (POST (str "/api/" api-prefix "/count-matching")
      {:index-name index
       :index-type index-type
       :filter filter-spec}))

(def-lastcall-method-factory records-factory
  [index index-type filter-spec sort-spec size]
  (POST (str "/api/" api-prefix "/records")
      {:index-name index
       :index-type index-type
       :filter filter-spec
       :sort sort-spec
       :size size}))

(def-lastcall-method-factory tags-of-type-factory
  [tag-type]
  (GET (str "/api/" api-prefix "/tags/" tag-type)))

(def-lastcall-method-factory geotags-of-type-factory
  [tag-type]
  (GET (str "/api/" api-prefix "/geotags/" tag-type)))

(def-lastcall-method company-search
  [query & [{:keys [search-fields]}]] ;;TODO: use an ajax todo params properly
  (GET (str "/api/" api-prefix "/companies/v2/name-id-search?q=" (some-> query str/trim)
            "&search_fields=" (str/join "," search-fields))))

(defnk-lastcall-method-factory company-links-factory
  [filter-spec]
  (POST (str "/api/" api-prefix "/companylinks")
      {:filter filter-spec} :send-error true))
