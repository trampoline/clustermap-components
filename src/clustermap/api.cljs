(ns clustermap.api
  (:require-macros
   [clustermap.api :refer [def-lastcall-method def-lastcall-method-factory]]
   [cljs.core.async.macros :refer [go]])
  (:require
   [clojure.string :as str]
   [cljs.core.async :as async :refer [<! chan close! put! sliding-buffer to-chan]]
   [goog.net.XhrIo :as xhr]))

(defn AJAX [url & {:keys [raw method content] :as opts}]
  "send a GET request, returning a channel with a single result value"
  (let [comm (chan 1)]
    (xhr/send url
              (fn [event]
                (put! comm (-> event
                               .-target
                               .getResponseText
                               js/JSON.parse
                               (aget "data")
                               ((fn [d]
                                  (if raw
                                    d
                                    (js->clj d :keywordize-keys true))))))
                (close! comm))
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

(defn- ordered-api-results
  "- ocomm : a channel containing [result-chans result-handler-args]
             gather results from the one-or-more result-chans, and
             pass them to the handler along with any result-handler-args
   - handler : invoked with (apply handler result-or-results result-handler-args)"
  [ocomm handler]
  (go
   (while true
     (let [[rcomms result-handler-args] (<! ocomm)
           result (if (sequential? rcomms)
                    (loop [rem rcomms
                           results []]
                      (if (empty? rem)
                        results
                        (recur (rest rem) (conj results (when (first rem) (<! (first rem)))))))
                    (when rcomms (<! rcomms)))]
       ;; (.log js/console (clj->js result))
       (apply handler result result-handler-args)))))

(defn ordered-api
  "order responses from an async API according to the order of requests
   - request-handler: fn to send an async API request, returning a channel of a single result value,
                      and optional additional args for the result-handler. return nil to
                      abandon request
   - result-handler: function of API result and optional additional args from request-handler result"
  [request-handler result-handler]

  (let [ocomm (chan (sliding-buffer 1))
        _ (ordered-api-results ocomm result-handler)]
    (fn [& req-args]
      (if-let [r (apply request-handler req-args)]
        (let [rseq (if (sequential? r) r [r])
              [rcomm & result-handler-args] rseq]
          (put! ocomm [rcomm result-handler-args]))))))

(defn lastcall-method-impl
  "implements last-call-wins aync api-call semantics, discarding results from
   any earlier api calls
   - in-flight-atom : an atom used to match received results to calls
   - valch : a single-value channel eventually containing one api-call result"
  [in-flight-atom valch]
  (let [rx (chan)]
    (reset! in-flight-atom valch)

    (go
      (let [val (<! valch)]
        (when (and val
                   (= @in-flight-atom valch))
          (put! rx val))
        (close! valch)
        (close! rx)))

    rx))

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

(def api-prefix js/config.api.prefix)

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

(defn rankings
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

(def-lastcall-method-factory ranges-factory
  [index index-type filter-spec row-path row-aggs col-path col-aggs metric-path metric-aggs]
  (POST (str "/api/" api-prefix "/ranges")
      {:index-name index
       :index-type index-type
       :filter filter-spec
       :row-path row-path
       :row-aggs row-aggs
       :col-path col-path
       :col-aggs col-aggs
       :metric-path metric-path
       :metric-aggs metric-aggs}))

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
  [query]
  (GET (str "/api/" api-prefix "/companies/v2/name-id-search?q=" query)))
