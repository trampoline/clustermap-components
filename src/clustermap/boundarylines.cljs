(ns clustermap.boundarylines
  (:require-macros
   [cljs.core.async.macros :refer [go]])
  (:require
   [cljs.core.async :as async :refer [<! chan close! put! sliding-buffer to-chan]]
   [clustermap.api :as api]))

(def ^:private min-tolerance 0.01)

(def ^:private zoom-tolerances
  [[7 min-tolerance]
   [10 0.002]
   [12 0.0003]
   [nil 0]])

(defn- ideal-tolerance
  [zoom]
  (or (some (fn [[z t]] (when (and z (<= zoom z)) t)) zoom-tolerances)
      0))

(defn- best-available-tolerance
  [zoom available-tolerances]
  (let [i-tol (ideal-tolerance zoom)
        coarser (->> zoom-tolerances
                     (map last)
                     (filter (fn [t] (> t i-tol)))
                     reverse)
        finer (->> zoom-tolerances
                   (map last)
                   (filter (fn [t] (< t i-tol))))

        avail-set (set available-tolerances)]

    (or (get avail-set i-tol)
        (some avail-set finer)
        (some avail-set coarser))))

(defn ^:private make-sequential
  [x]
  (cond (nil? x) nil
        (sequential? x) x
        true [x]))

(defn ^:private cache-boundarylines
  "cache a seq of boundarylines in the general and collection-specific caches"
  [app-state boundarylines-path boundarylines]
  (let [boundarylines-path (make-sequential boundarylines-path)]
    (swap! app-state
           (fn [old-state]
             (->> boundarylines
                  (reduce (fn [s bl]
                            (let [boundaryline-id (aget bl "id")
                                  tolerance (aget bl "tolerance")
                                  collection-ids (aget bl "boundaryline_collection_ids")

                                  general-cache-path (concat boundarylines-path [:boundarylines boundaryline-id tolerance])

                                  collection-cache-base-path (concat boundarylines-path [:collections])
                                  collection-cache-paths (for [collection-id collection-ids]
                                                           (concat collection-cache-base-path [collection-id :boundarylines boundaryline-id tolerance]))

                                  all-cache-paths (conj collection-cache-paths general-cache-path)]

                              (->> all-cache-paths
                                   (reduce (fn [s2 cp]
                                             (assoc-in s2 cp bl))
                                           s))))
                          old-state))))))

;; should have a cache-boundarylines-index function which pulls an index apart
;; into individual boundaryline entries

(defn ^:private cache-index
  "take a geojson boundaryline collection index and index the individual boundarylines

   first put each geojson geometry into a geojson featurecollection, along
   with properties (including the original index object, for later retrieval)"
  [app-state boundarylines-path js-index]
  (let [keys (js/Object.keys js-index)]
    (swap! app-state
           (fn [old-state]
             (->> keys
                  (reduce (fn [s k]
                            (let [bl (aget js-index k)
                                  boundaryline-id (aget bl "id")
                                  tolerance (aget bl "tolerance")
                                  cache-path (concat boundarylines-path [:boundarylines boundaryline-id tolerance])]
                              (assoc-in s cache-path bl )
                              )
                            )
                          old-state))))))


(defn fetch-boundaryline-set
  [app-state boundarylines-path & {:keys [boundaryline-ids bounds tolerance] :or {tolerance min-tolerance}}]
  (let [comm (api/boundaryline-set boundaryline-ids tolerance :raw true)]
    (go
      (let [bls (<! comm)]
        (cache-boundarylines app-state boundarylines-path bls))
      true)))

(defn get-or-fetch-boundaryline
  "return a channel with a single boundaryline gotten from cache or API"
  [app-state boundarylines-path boundaryline-id & {:keys [tolerance] :or {tolerance min-tolerance}}]
  (let [boundarylines-path (make-sequential boundarylines-path)
        cache-path (concat boundarylines-path [:boundarylines boundaryline-id tolerance])
        cached-bl (get-in @app-state cache-path)]
    (go
      (if cached-bl
        cached-bl
        (do
          (<! (fetch-boundaryline-set app-state boundarylines-path :boundaryline-ids [boundaryline-id] :tolerance tolerance))
          (get-in @app-state cache-path))))))

(defn rp
  [ch]
  (go
    (let [v (<! ch)]
      (.log js/console (clj->js v)))))

(defn fetch-boundarylines
  "fetch a set of boundarylines for a given tolerance in one API call, add the results to the collection-specific and
   general caches. returns a single value true on the go-block channel when complete"
  [app-state boundarylines-path collection-id tolerance & {:keys [boundaryline-ids bounds]}]
  (let [comm (api/boundaryline-collection-view collection-id tolerance bounds :boundaryline-ids boundaryline-ids :raw true)]
    (go
      (let [bls (<! comm)]
        (cache-boundarylines app-state boundarylines-path bls))
      true)))

(defn- fetch-from-index
  [index boundaryline-id]
  (let [default-bl (when index (aget index (name boundaryline-id)))
        default-tol (when default-bl (aget default-bl "tolerance"))]
    (when (and default-bl default-tol)
      [default-tol default-bl])))

(defn get-cached-boundaryline
  [app-state boundarylines-path boundaryline-id & [{indexes :indexes}]]
  (let [cache-path (concat (make-sequential boundarylines-path) [:boundarylines boundaryline-id])
        cached (get-in @app-state cache-path)
        tolerance (->> cached keys sort last)
        cached (when tolerance (get cached tolerance))]
    (if cached
      cached

      (->> indexes
           (some (fn [collection-id]
                   (let [collection-index-path (concat (make-sequential boundarylines-path) [:collections collection-id :index])
                         collection-index (get-in @app-state collection-index-path)]
                     (when collection-index
                       (aget collection-index (name boundaryline-id))))))))))

(defn best-version
  [collection-index collection-cache general-cache zoom boundaryline-id]
  (let [bl-versions (or (get collection-cache boundaryline-id) (get general-cache boundaryline-id))
        i-tol (ideal-tolerance zoom)
        use-tol (best-available-tolerance zoom (keys bl-versions))
        use-bl (get bl-versions use-tol)]
    (if use-bl
      [use-tol use-bl]
      (fetch-from-index collection-index boundaryline-id))))

(defn get-or-fetch-best-boundarylines
  "gets the best boundaryline versions for a zoom level... where the best are not
   immediately available, fetches it asynchronously and returns the best immediately
   available
   - app-state : atom containing app-state
   - boundarylines-path : path in app-state of boundarylines state
   - collection-id : id of boundaryline collection to retrieve from
   - zoom : zoom level to retrieve for
   - boundaryline-ids : ids of boundarylines required
   - bounds : "
  [app-state boundarylines-path collection-id bounds zoom & {:keys [boundaryline-ids]}]
  (let [boundarylines-path (make-sequential boundarylines-path)

        all-collections-path (concat boundarylines-path [:collections])

        general-cache-path (concat boundarylines-path [:boundarylines])
        general-cache (get-in @app-state general-cache-path)

        collection-path (when collection-id (concat all-collections-path [collection-id]))
        collection-index-path (when collection-id (concat collection-path [:index]))
        collection-index (when collection-id (get-in @app-state collection-index-path))
        collection-cache-path (when collection-path (concat collection-path [:boundarylines]))
        collection-cache (get-in @app-state collection-cache-path)

        i-tol (ideal-tolerance zoom)

        best-versions (->> boundaryline-ids
                           (map (fn [blid]
                                  [blid (best-version collection-index collection-cache general-cache zoom blid)]))
                           (into {}))

        required (->> best-versions
                      (filter (fn [[blid [tol bl]]] (not= tol i-tol)))
                      (map first))
        ;;_     (.log js/console (clj->js ["required" required]))
        notify-chan (if (not-empty required)
                      (fetch-boundarylines app-state boundarylines-path collection-id i-tol :boundaryline-ids required :bounds bounds)
                      (let [ch (chan)] ;; already complete, so return a notify-channel pre-loaded with true
                        (put! ch true)
                        (close! ch)
                        ch))]

    [best-versions notify-chan]))

(defn boundaryline-collection-rtree
  "atomically fetch or create the rtree index object for a collection"
  [app-state boundarylines-path collection-id]
  (let [boundarylines-path (make-sequential boundarylines-path)
        all-collections-path (concat boundarylines-path [:collections])
        collection-path (concat all-collections-path [collection-id])
        rtree-path (concat collection-path [:rtree])]
    (swap! app-state update-in rtree-path (fn [old] (if old old (js/RTree. 10))))
    (get-in @app-state rtree-path)))

(defn- rtree-index
  "take a geojson boundaryline collection index and stuff it into an rtree

   first put each geojson geometry into a geojson featurecollection, along
   with properties (including the original index object, for later retrieval)"
  [rtree js-index]
  (let [keys (js/Object.keys js-index)
        features (array)
        js-feature-coll (clj->js {:type "FeatureCollection"})
        _ (aset js-feature-coll "features" features)]
    (doseq [k keys]
      (let [obj (aget js-index k)
            geom (aget obj "geojson")
            feature (clj->js {:type "Feature"
                              :properties {:id (aget obj "id")
                                           :boundaryline_id (aget obj "id")
                                           :compact_name (aget obj "compact_name")}})
            ;; use aset to avoid any recodings of js objects
            _ (aset feature "geometry" geom)
            _ (aset (aget feature "properties") "index_object" obj)]
        (.push features feature)))
    (.geoJSON rtree js-feature-coll)
    rtree))

(defn fetch-boundaryline-collection-index
  [app-state boundarylines-path collection-id]
  (let [boundarylines-path (make-sequential boundarylines-path)
        all-collections-path (concat boundarylines-path [:collections])
        collection-path (concat all-collections-path [collection-id])
        index-path (concat collection-path [:index])
        index (get-in @app-state index-path)
        rtree (boundaryline-collection-rtree app-state boundarylines-path collection-id)]
    (when-not index
      (go
        (let [bl-coll-index (<! (api/boundaryline-collection-index collection-id :raw true))]

          (swap! app-state update-in index-path (fn [old] (if old old bl-coll-index)))

          ;; add to the rtree index if this swap! was succesful
          (if (= (get-in @app-state index-path) bl-coll-index)
            (rtree-index rtree bl-coll-index))

          ;; cache the individual boundarylines
          (cache-index app-state boundarylines-path bl-coll-index))))))


(defn point-in-boundarylines
  "use the rtree index to find which paths from a collection a point is inside.
   returns a list of boundaryline objects"
  [app-state boundarylines-path collection-id x y]
  (let [boundarylines-path (make-sequential boundarylines-path)
        all-collections-path (concat boundarylines-path [:collections])
        collection-path (concat all-collections-path [collection-id])
        rtree-path (concat collection-path [:rtree])
        rtree (get-in @app-state rtree-path)
        hits (when rtree (.search rtree (clj->js {:x x :y y :w 0 :h 0})))]
    (->> (or hits [])
         (filter (fn [hit]
                   (js/gju.pointInPolygon (clj->js {:type "Point"
                                                    :coordinates [x y]})
                                          (.-geometry hit))))
         (map (fn [hit]
                {:id (-> hit .-properties .-id)
                 :compact_name (-> hit .-properties .-compact_name)})))))
