(ns clustermap.components.map
  (:require-macros [hiccups.core :as hiccups]
                   [cljs.core.async.macros :refer [go]])
  (:require
   [clojure.set :as set]
   [cljs.core.async :refer [put! <!]]
   [domina :as domina]
   [domina.events :as events]
   [om.core :as om :include-macros true]
   [jayq.core :refer [$]]
   [sablono.core :as html :refer-macros [html]]
   [hiccups.runtime :as hiccupsrt]
   [clustermap.api :as api]
   [clustermap.ordered-resource :as ordered-resource]
   [clustermap.boundarylines :as bl]
   [clustermap.data.colorchooser :as colorchooser]))

(def ^:private ticket (atom 0))

(defn next-ticket
  []
  (swap! ticket inc))

(defn bounds-array
  "convert a Leaflet LatLngBounds object into nested-array form"
  [bounds]
  (if (instance? js/L.LatLngBounds bounds)
    [[(.getSouth bounds) (.getWest bounds)] [(.getNorth bounds) (.getEast bounds)]]
    bounds))

(defn geojson-point-bounds
  "return a single LatLngBounds object containing all
   given latlongs"
  [longlats]
  (let [s (apply min (map last longlats))
        w (apply min (map first longlats))
        n (apply max (map last longlats))
        e (apply max (map first longlats))]
    (when (and s w n e)
      (js/L.latLngBounds (clj->js [[s w] [n e]])))))

(defn locate-map
  [m initial-bounds]
  (.fitBounds m
              (clj->js initial-bounds)
              (clj->js {"paddingTopLeft" [0 0]
                        "paddingBottomRight" [0 0]})))

(def default-api-key (or (some-> js/config .-components .-map .-api_key)
                 "mccraigmccraig.h4f921b9"))

(defn create-map
  [id-or-el {:keys [initial-bounds map-options api-key] :or {api-key default-api-key}}]
  (let [zoom-control (if (false? (:zoomControl map-options)) false true)
        m ((-> js/L .-map) id-or-el (clj->js (merge map-options {:zoomControl false :maxZoom 19})))
        tiles ((-> js/L .-mapbox .-tileLayer) api-key #js {:detectRetina (not js/config.repl)})
        zoom ((-> js/L .-control .-zoom) #js {:position "bottomleft"})]
    (.addLayer m tiles)
    (when zoom-control
      (.addControl m zoom))

    (locate-map m initial-bounds)

    {:leaflet-map m
     :markers (atom {})
     :geotag-markers (atom {})
     :paths (atom {})
     :path-selections (atom #{})}))

(defn pan-to-show
  [m & all-bounds]
  (if (not-empty all-bounds)
    (let [fb (first all-bounds)
          fb-copy (new js/L.LatLngBounds (.getSouthWest fb) (.getNorthEast fb))
          super-bounds (reduce (fn [sb bounds] (.extend sb bounds))
                               fb-copy
                               (rest all-bounds))]
      (.fitBounds m super-bounds))))

(defn marker-popup-content
  [path-fn location-sites]
  (hiccups/html
   [:ul.map-marker-popup-location-list
    (for [site location-sites]
      [:li (when path-fn
             (path-fn site))])]))

(defn create-marker
  [path-fn leaflet-map location-sites]
  ;; extract the location-sites from the first record... they are all the same
  (if-let [latlong (some-> location-sites first :location reverse clj->js)]
    (let [icon (js/L.divIcon (clj->js {:className "map-marker" :iconSize [24,28] :iconAnchor [12 14] :popupAnchor [0, -8] })) ;;
          marker (js/L.marker latlong (clj->js {:icon icon}) ) ;;
          popup-content (marker-popup-content path-fn location-sites)]
      ;; (.log js/console popup-content)
      (.bindPopup marker popup-content)
      (.addTo marker leaflet-map)
      marker)
    (.log js/console (str "missing location: " (with-out-str (pr location-sites))))))

(defn update-marker
  [path-fn leaflet-map marker location]
  (.setPopupContent marker (marker-popup-content path-fn location))
  marker)

(defn remove-marker
  [leaflet-map marker]
  (.removeLayer leaflet-map marker))

(defn update-markers
  [path-fn leaflet-map markers-atom show-points new-locations]
  (let [markers @markers-atom
        marker-keys (-> markers keys set)
        location-keys (when show-points (-> new-locations keys set))

        _ (.log js/console (clj->js [(count location-keys) location-keys]))

        update-marker-keys (set/intersection marker-keys location-keys)
        new-marker-keys (set/difference location-keys marker-keys)
        remove-marker-keys (set/difference marker-keys location-keys)

        new-markers (->> new-marker-keys
                         (map (fn [k] [k (create-marker path-fn leaflet-map (get-in new-locations [k :records]))]))
                         (into {}))

        updated-markers (->> update-marker-keys
                             (map (fn [k] [k (update-marker path-fn leaflet-map (get markers k) (get-in new-locations [k :records]))]))
                             (into {}))

        _ (doseq [k remove-marker-keys] (remove-marker leaflet-map (get markers k)))]

    (reset! markers-atom (merge updated-markers new-markers))))

;;;;;;;;;;;;;;;;;;;;;;;;;

(defn create-geotag-marker
  [leaflet-map {:keys [icon-render-fn popup-render-fn] :as geotag-agg-spec} geotag geotag-agg]
  (let [latlong (clj->js [(:latitude geotag) (:longitude geotag)])
        icon (js/L.divIcon (clj->js {:className "map-marker-3"
                                     :iconSize [24,28]
                                     :iconAnchor [12 14]
                                     :popupAnchor [0, -8]
                                     :html (icon-render-fn geotag geotag-agg )}))
        marker (js/L.marker latlong (clj->js {:icon icon}) )
        popup (js/L.popup (clj->js {:autoPan false}))]
    (.setContent popup (popup-render-fn geotag geotag-agg))
    (.bindPopup marker popup)
    (.addTo marker leaflet-map)
    marker))

(defn update-geotag-marker
  [leaflet-map {:keys [icon-render-fn popup-render-fn] :as geotag-aggs} marker geotag geotag-agg]
  (.setPopupContent marker (popup-render-fn geotag geotag-agg ))
  marker)

(defn update-geotag-markers
  [leaflet-map geotag-markers-atom {:keys [icon-render-fn popup-render-fn geotag-data geotag-agg-data] :as geotag-agg-spec}]
  (let [geotags-by-tag (reduce (fn [m t] (assoc m (:tag t) t)) {} geotag-data)
        geotag-aggs-by-tag (reduce (fn [m a] (assoc m (:nested_attr a) a)) {} geotag-agg-data)

        markers @geotag-markers-atom
        marker-keys (-> markers keys set)

        latest-marker-keys (-> geotag-aggs-by-tag keys set)
        update-marker-keys (set/intersection marker-keys latest-marker-keys)
        new-marker-keys (set/difference latest-marker-keys marker-keys)
        remove-marker-keys (set/difference marker-keys latest-marker-keys)

        _ (.log js/console (clj->js {:geotag-aggs geotag-agg-spec
                                     :latest-marker-keys latest-marker-keys
                                     :update-marker-keys update-marker-keys
                                     :new-marker-keys new-marker-keys
                                     :remove-marker-keys remove-marker-keys}))

        new-markers (->> new-marker-keys
                         (map (fn [k] [k (create-geotag-marker leaflet-map geotag-agg-spec (get geotags-by-tag k) (get geotag-aggs-by-tag k))]))
                         (into {}))

        updated-markers (->> update-marker-keys
                             (map (fn [k] [k (update-geotag-marker leaflet-map geotag-agg-spec (get markers k) (get geotags-by-tag k) (get geotag-aggs-by-tag k))]))
                             (into {}))

        _ (doseq [k remove-marker-keys] (remove-marker leaflet-map (get markers k)))]


    (reset! geotag-markers-atom (merge updated-markers new-markers))
    ))

;; path-utilities

(defn postgis-envelope->latlngbounds
  "turns a PostGIS envelope into a L.LatLngBounds"
  [envelope]
  (let [{[[[miny0 minx0] [maxy1 minx1] [maxy2 maxx2] [miny3 maxx3] [miny4 minx4] :as inner] :as coords] "coordinates" :as clj-envelope} (js->clj envelope)]
    (js/L.latLngBounds (clj->js [[minx0 miny0] [maxx2 maxy2]]))))

;; manage paths

(defn boundary-marker-popup-content
  [path-fn js-boundaryline]
  ;; (.log js/console js-boundaryline)
  (let [bl-id (aget js-boundaryline "id")
        bl-name (aget js-boundaryline "compact_name") ]
    (hiccups/html
     [:a.boundaryline-popup-link {:href (if path-fn (path-fn :map :js-boundaryline js-boundaryline) "#")
                                  :data-boundaryline-id bl-id}
      [:span.map-marker-js-boundaryline-name {:data-boundaryline-id bl-id} bl-name]])))

(defn style-leaflet-path
  [leaflet-path {:keys [selected highlighted fill-color]}]
  (cond (and selected highlighted) (.setStyle leaflet-path (clj->js {:color "#000000" :fillColor fill-color :weight 2 :opacity 1 :fill true :fillOpacity 0.6}))
        selected                   (.setStyle leaflet-path (clj->js {:color "#8c2d04" :fillColor fill-color :weight 1 :opacity 1 :fill true :fillOpacity 0.6}))
        highlighted                (.setStyle leaflet-path (clj->js {:color "#000000" :fillColor fill-color :weight 2 :opacity 1 :fill false}))
        true                       (.setStyle leaflet-path (clj->js {:color "#8c2d04" :fillColor fill-color :weight 1 :opacity 0 :fill false :fillOpacity 0}))))

(defn create-path
  [comm leaflet-map boundaryline-id js-boundaryline {:keys [selected] :as path-attrs} & [{:keys [filter-spec] :as opts}]]
  (let [tolerance (aget js-boundaryline "tolerance")
        bounds (postgis-envelope->latlngbounds (aget js-boundaryline "envelope"))
        leaflet-path (js/L.geoJson (aget js-boundaryline "geojson"))
        popup-content (boundary-marker-popup-content nil js-boundaryline)]
    (style-leaflet-path leaflet-path path-attrs)
    (.addTo leaflet-path leaflet-map)
    (.bindPopup leaflet-path popup-content)
    (.on leaflet-path "click" (fn [e]
                                (when filter-spec
                                  (om/update! filter-spec [:components :boundaryline]
                                              {:nested {:path "?boundarylines"
                                                        :filter {:term {"boundaryline_id" boundaryline-id}}}}))

                                (put! comm {:type :clustermap.components.map/path-click
                                            :id boundaryline-id})))

    {:id boundaryline-id
     :tolerance tolerance
     :selected selected
     :leaflet-path leaflet-path
     :bounds bounds}))

(defn fetch-create-path
  "create leaflet paths for every boundaryline in boundaryline-index"
  [comm leaflet-map boundaryline-id tolerance js-boundaryline path-attrs & [{:keys [filter-spec] :as opts}]]

  ;;  (.log js/console (clj->js ["fetch-create" boundaryline-id]))
  (create-path comm leaflet-map boundaryline-id js-boundaryline path-attrs opts))

(defn replace-path
  [comm leaflet-map boundaryline-id old-path js-boundaryline path-attrs & [{:keys [filter-spec] :as opts}]]
  ;; (.log js/console (clj->js ["replace-path" boundaryline-id old-path js-boundaryline path-attrs]))
  (.removeLayer leaflet-map (:leaflet-path old-path))
  (create-path comm leaflet-map (:id old-path) js-boundaryline path-attrs opts))

(defn update-path
  "update a Leaflet path for a boundaryline"
  [comm leaflet-map {boundaryline-id :id :as path} tolerance js-boundaryline path-attrs & [{:keys [filter-spec] :as opts}]]
  ;; (.log js/console (clj->js ["update-path" boundaryline-id path tolerance js-boundaryline path-attrs]))
  (if (not= tolerance (:tolerance path))
    (replace-path comm leaflet-map boundaryline-id path js-boundaryline path-attrs opts)
    (do (style-leaflet-path (:leaflet-path path) path-attrs)
        path)))

(defn delete-path
  [leaflet-map path]
  ;;   (.log js/console (clj->js ["delete-path" (:id path)]))
  (.removeLayer leaflet-map (:leaflet-path path)))

(defn update-paths
  [comm fetch-boundarylines-fn leaflet-map paths-atom path-selections-atom new-path-highlights new-selection-paths & [{:keys [filter-spec] :as opts}]]
  (let [paths @paths-atom
        path-keys (-> paths keys set)

        old-selection-path-keys @path-selections-atom
        new-selection-path-keys (-> new-selection-paths keys set)
        ;; _ (.log js/console (clj->js ["new-selection-path-keys" new-selection-path-keys]))

        live-path-keys (set/union new-selection-path-keys new-path-highlights)

        create-path-keys (set/difference live-path-keys path-keys)
        delete-path-keys (set/difference path-keys live-path-keys)
        update-path-keys (set/intersection path-keys live-path-keys)

        _ (.log js/console (clj->js {:create create-path-keys :delete delete-path-keys :update update-path-keys}))

        [tolerance-paths notifychan] (fetch-boundarylines-fn (bounds-array (.getBounds leaflet-map)) (.getZoom leaflet-map) :boundaryline-ids live-path-keys)

        ;; _ (.log js/console (clj->js tolerance-paths))

        created-paths (->> create-path-keys
                           (map (fn [k] (let [[tolerance js-boundaryline] (get tolerance-paths k)]
                                          (when (and k tolerance js-boundaryline)
                                            [k tolerance js-boundaryline]))))
                           (filter identity)
                           (map (fn [[k tolerance js-boundaryline]] (fetch-create-path comm
                                                                                       leaflet-map
                                                                                       k
                                                                                       tolerance
                                                                                       js-boundaryline
                                                                                       {:selected (contains? new-selection-path-keys k)
                                                                                        :fill-color (new-selection-paths k)
                                                                                        :highlighted (contains? new-path-highlights k)}
                                                                                       opts))))

        updated-paths (->> update-path-keys
                           (map (fn [k] (let [[tolerance js-boundaryline] (get tolerance-paths k)]
                                          (when (and k tolerance js-boundaryline)
                                            [k tolerance js-boundaryline]))))
                           (filter identity)
                           (map (fn [[k tolerance js-boundaryline]] (update-path comm
                                                                                 leaflet-map
                                                                                 (get paths k)
                                                                                 tolerance
                                                                                 js-boundaryline
                                                                                 {:selected (contains? new-selection-path-keys k)
                                                                                  :fill-color (new-selection-paths k)
                                                                                  :highlighted (contains? new-path-highlights k)}
                                                                                 opts)))
                           )

        _ (doseq [k delete-path-keys] (if-let [path (get paths k)] (delete-path leaflet-map path)))

        ;; _ (.log js/console (clj->js updated-paths))

        new-paths (->> (concat created-paths updated-paths)
                       (filter identity)
                       (reduce (fn [m {:keys [id] :as path}] (assoc m id path))
                               {}))]

    ;; (.log js/console (clj->js live-path-keys))

    (reset! path-selections-atom new-selection-path-keys)
    (reset! paths-atom new-paths)
    notifychan
    ))

;; (defn pan-to-selection
;;   [owner leaflet-map paths-atom path-selections-atom]
;;   (let [paths @paths-atom
;;         path-selections @path-selections-atom]
;;     ;; (.log js/console (clj->js ["pan-to-selection"]))
;;     (if (empty? path-selections)
;;       (do
;;         ;; (.log js/console (clj->js ["empty selection" path-selections]))
;;         (locate-map leaflet-map)
;;           ;; (om/set-state! owner :pan-pending true)
;;           )
;;       (if (empty? paths)
;;         (do
;;           ;; (.log js/console (clj->js ["non-empty selection : empty paths" path-selections]))
;;           (om/set-state owner :pan-pending true))
;;         (do
;;           ;; (.log js/console (clj->js ["non-empty selection" path-selections]))
;;           (if (om/get-state owner :pan-pending) (om/set-state! owner :pan-pending false))
;;           (if-let [bounds (some->> (select-keys paths path-selections) vals (map :bounds) not-empty)]
;;             (apply pan-to-show leaflet-map bounds)
;;             (pan-to-show initial-bounds)))
;;         ))))

(defn choose-boundaryline-collection
  [threshold-collections zoom]
  (->> threshold-collections
       (filter (fn [[tz collection]] (>= zoom tz)))
       reverse
       first
       last))

(defn request-aggregation-data
  [resource index index-type blcoll variable filter bounds scale-attr post-scale-factor]
  (ordered-resource/api-call resource
                             api/boundaryline-aggregation
                             index
                             index-type
                             blcoll
                             variable
                             filter
                             bounds
                             scale-attr
                             post-scale-factor))

(defn request-point-data
  [resource index index-type filter bounds]
  (ordered-resource/api-call resource
                             api/location-lists
                             index
                             index-type
                             "!postcode"
                             ["?natural_id" "!name" "!location" "!latest_employee_count" "!latest_turnover"]
                             1000
                             filter
                             bounds))

(defn request-geotag-data
  [resource tag-type]
  (ordered-resource/api-call resource
                             api/geotags-of-type
                             tag-type))

(defn request-geotag-agg-data
  [resource query]
  (ordered-resource/api-call resource
                             api/nested-aggregation
                             query))

(defn map-component
  "put the leaflet map as state in the om component"
  [{{data :data
     point-data :point-data
     boundaryline-collections :boundaryline-collections
     {:keys [initial-bounds
             map-options
             link-render-fn
             bounds zoom show-points
             boundaryline-collection
             colorchooser
             boundaryline-agg
             threshold-colors
             geotag-aggs] :as controls} :controls :as cursor} :map-state
     filter-spec :filter-spec
     filter :filter
     :as cursor-data}
   owner]

  (reify
    om/IRender
    (render [this]
      (html [:div.map {:ref "map"}]))

    om/IDidMount
    (did-mount [this]
      (let [node (om/get-node owner)
            {:keys [leaflet-map markers path] :as map} (create-map node controls)
            {:keys [comm fetch-boundarylines-fn point-in-boundarylines-fn link-fn path-fn
                    path-marker-click-fn]} (om/get-shared owner)
            last-dims (atom nil)
            w (.-offsetWidth node)
            h (.-offsetHeight node)]

        ;; only set last-dims if we are initialised on-screen... later
        ;; when map shows, if last-dims is nil, we locate-map again
        (when (and (> w 0) (> h 0))
          (reset! last-dims [w h]))

        ;; reflect bounds and zoom in controls immediately
        (om/update! cursor [:controls :zoom] (.getZoom leaflet-map))
        (om/update! cursor [:controls :bounds] (bounds-array (.getBounds leaflet-map)))

        (om/set-state! owner :map map)
        (om/set-state! owner :path-highlights #{})

        (.on leaflet-map "moveend" (fn [e]
                                     (.log js/console "moveend")
                                     (om/update! cursor [:controls :zoom] (.getZoom leaflet-map))
                                     (om/update! cursor [:controls :bounds] (bounds-array (.getBounds leaflet-map)))))

        ;; discard mousemoves on open popups...
        (.on leaflet-map "popupopen" (fn [e]
                                       (let [popup-el (-> e .-popup .-_container)
                                             marker-popup-location-list-cnt (-> popup-el $ (.find ".map-marker-popup-location-list") .-length)]
                                         (if (> marker-popup-location-list-cnt 0)
                                           (om/set-state! owner :popup-selected true))
                                         (-> popup-el
                                             $
                                             (.on "mousemove" (fn [e] (.preventDefault e) false))))))

        (.on leaflet-map "popupclose" (fn [e] (om/set-state! owner :popup-selected nil)))


        (when path-marker-click-fn
          ;; click off of a path resets boundary selection
          (.on leaflet-map "click" (fn [e] (path-marker-click-fn nil)))
          (-> js/document $ (.on "click" "a.boundaryline-popup-link"
                                 (fn [e]
                                   (.preventDefault e)
                                   (some-> e
                                           .-target
                                           (domina/attr "data-boundaryline-id")
                                           path-marker-click-fn)))))

        ;; if there is a window size change when the map isn't visible, invalidate the map size
        (events/listen! "clustermap-change-view" (fn [e]
                                                   (let [w (.-offsetWidth node)
                                                         h (.-offsetHeight node)
                                                         current-dims [w h]]
                                                     (when (and (> w 0)
                                                                (> h 0)
                                                                (not= @last-dims current-dims))
                                                       (.log js/console "window size changed !")
                                                       (.invalidateSize leaflet-map)
                                                       (when-not @last-dims
                                                         (.log js/console "first map show !")
                                                         (locate-map leaflet-map initial-bounds))
                                                       (reset! last-dims current-dims)))))

        (let [adr (ordered-resource/make-discard-stale-resource "aggregation-data-resource")]
          (om/set-state! owner :aggregation-data-resource adr)
          (ordered-resource/retrieve-responses adr (fn [data] (om/update! cursor [:data] data))))

        (let [pdr (ordered-resource/make-discard-stale-resource "point-data-resource")]
          (om/set-state! owner :point-data-resource pdr)
          (ordered-resource/retrieve-responses pdr (fn [point-data] (om/update! cursor [:point-data] point-data))))

        (let [gtdr (ordered-resource/make-discard-stale-resource "geotag-data-resource")]
          (om/set-state! owner :geotag-data-resource gtdr)
          (ordered-resource/retrieve-responses gtdr (fn [geotag-data] (om/update! cursor [:controls :geotag-aggs :geotag-data] geotag-data)))
          )

        (let [gtadr (ordered-resource/make-discard-stale-resource "geotag-agg-data-resource")]
          (om/set-state! owner :geotag-agg-data-resource gtadr)
          (ordered-resource/retrieve-responses gtadr (fn [geotag-agg-data] (om/update! cursor [:controls :geotag-aggs :geotag-agg-data] (:records geotag-agg-data))))
          )
        ))



    om/IWillUpdate
    (will-update [this
                  {{next-data :data
                    next-point-data :point-data
                    next-boundaryline-collections :boundaryline-collections
                    {next-zoom :zoom
                     next-bounds :bounds
                     next-show-points :show-points
                     next-link-render-fn :link-render-fn
                     next-boundaryline-collection :boundaryline-collection
                     next-colorchooser :colorchooser
                     next-boundaryline-agg :boundaryline-agg
                     next-threshold-colors :threshold-colors
                     next-geotag-aggs :geotag-aggs} :controls
                    :as next-cursor
                    } :map-state
                      next-filter :filter
                      next-filter-spec :filter-spec
                      :as next-cursor-data}
                  {{next-markers :markers
                    next-geotag-markers :geotag-markers
                    next-paths :paths
                    next-path-selections :path-selections} :map
                    next-path-highlights :path-highlights
                    next-aggregation-data-resource :aggregation-data-resource
                    next-point-data-resource :point-data-resource
                    next-geotag-data-resource :geotag-data-resource
                    next-geotag-agg-data-resource :geotag-agg-data-resource
                    }]

      (let [{:keys [comm path-fn link-fn fetch-boundarylines-fn point-in-boundarylines-fn]} (om/get-shared owner)
            {{:keys [leaflet-map markers paths path-selections]} :map
             pan-pending :pan-pending
             path-highlights :path-highlights} (om/get-state owner)]

        ;; apply any requested but not-yet-applied zoom
        (when (and leaflet-map next-zoom (not= next-zoom zoom) (not= next-zoom (.getZoom leaflet-map)))
          (.setZoom leaflet-map next-zoom))

        ;; apply requested but not-yet-applied bounds changes
        (when (and leaflet-map next-bounds (not= next-bounds bounds) (not= next-bounds (bounds-array (.getBounds leaflet-map))))
          (.fitBounds leaflet-map (clj->js next-bounds))
          (om/update! cursor [:controls :bounds] (bounds-array (.getBounds leaflet-map))))

        ;; change the boundaryline-collection if necessary
        (when (and leaflet-map boundaryline-collections
                   (not= next-boundaryline-collection
                         (choose-boundaryline-collection next-boundaryline-collections (.getZoom leaflet-map))))
          (.log js/console (clj->js ["change-collection" (choose-boundaryline-collection next-boundaryline-collections (.getZoom leaflet-map))]))
          (om/update! cursor [:controls :boundaryline-collection] (choose-boundaryline-collection next-boundaryline-collections (.getZoom leaflet-map))))

        (when (and next-boundaryline-collection
                   next-boundaryline-agg
                   (or (not= next-boundaryline-agg boundaryline-agg)
                       (not= next-filter filter)
                       (not= next-bounds bounds)))
          (let [ticket (next-ticket)]
            (om/update! cursor [:controls :ticket] ticket)
            ;; time for some new data !
            (request-aggregation-data next-aggregation-data-resource
                                      (:index next-boundaryline-agg)
                                      (:index-type next-boundaryline-agg)
                                      (choose-boundaryline-collection next-boundaryline-collections (.getZoom leaflet-map))
                                      (:variable next-boundaryline-agg)
                                      (om/-value next-filter)
                                      (bounds-array (.getBounds leaflet-map))
                                      (:scale-attr next-boundaryline-agg)
                                      (:post-scale-factor next-boundaryline-agg))

            (request-point-data next-point-data-resource
                                (:index next-boundaryline-agg)
                                (:index-type next-boundaryline-agg)
                                (om/-value next-filter)
                                (bounds-array (.getBounds leaflet-map)))
            ))

        (when (and next-geotag-aggs
                   (or (not (:geotag-data next-geotag-aggs))))
          (request-geotag-data next-geotag-data-resource
                                (:tag-type next-geotag-aggs)))

        (when (and next-geotag-aggs
                   (or (not (:geotag-agg-data next-geotag-aggs))
                       (not= next-filter filter)
                       (not= next-bounds bounds)))
          (request-geotag-agg-data next-geotag-agg-data-resource
                                   (merge (:query next-geotag-aggs) {:filter-spec next-filter})))

        (when (and next-colorchooser
                   next-data
                   (or (not= next-data data)
                       (not= next-colorchooser colorchooser)))

          ;; (.log js/console (clj->js ["next-data" next-data]))
          ;; (.log js/console (clj->js ["threshold-colors" new-threshold-colors]))
          ;; (.log js/console (clj->js ["selection-path-colors" selection-path-colours]))

          (let [[new-threshold-colors selection-path-colours] (colorchooser/choose
                                                               (:scheme next-colorchooser)
                                                               (keyword (:scale next-colorchooser))
                                                               :boundaryline_id
                                                               (keyword (:variable next-colorchooser))
                                                               (:records next-data))

                update-paths-invocation (fn [] (update-paths comm
                                                             (partial fetch-boundarylines-fn next-boundaryline-collection)
                                                             leaflet-map
                                                             next-paths
                                                             next-path-selections
                                                             next-path-highlights
                                                             selection-path-colours
                                                             {:filter-spec next-filter-spec}))]

            (when (not= new-threshold-colors next-threshold-colors)
              (om/update! cursor [:controls :threshold-colors] new-threshold-colors))

            (when-let [notify-chan (update-paths-invocation)]

              (go
                (let [_ (<! notify-chan)]
                  (update-paths-invocation)))
              )))

        (when (or (not= next-show-points show-points)
                  (not= next-point-data point-data))

          (update-markers link-render-fn leaflet-map next-markers next-show-points (:records next-point-data)))

        (when (or (not= (:geotag-data next-geotag-aggs) (:geotag-data geotag-aggs))
                  (not= (:geotag-agg-data next-geotag-aggs) (:geotag-agg-data geotag-aggs)))
          (update-geotag-markers leaflet-map
                                 next-geotag-markers
                                 next-geotag-aggs))

        ))

    om/IWillUnmount
    (will-unmount [this]
      (-> js/document $ (.off "click" "a.boundaryline-popup-link"))
      (events/unlisten! "clustermap-change-view")

      (let [{{:keys [leaflet-map markers paths path-selections]} :map
             :keys [aggregation-data-resource point-data-resource]} (om/get-state owner)]
        (ordered-resource/close aggregation-data-resource)
        (ordered-resource/close point-data-resource)

        (.remove leaflet-map)))

    ))
