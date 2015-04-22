(ns clustermap.nav
  (:require [domina :as dom]
            [domina.css :as css]
            [domina.xpath :as xpath]
            [domina.events :as events]
            [secretary.core :as secretary :include-macros true]
            [jayq.core :as jayq :refer [$]]
            [cljs.core.async :refer [put! chan <!]]
            [clustermap.formats.url :as url]
            [clustermap.filters :as filters]))

(defn- init-bootstrap-tooltips
  []
  (-> "[data-toggle='tooltip']"
      $
      .tooltip))

(defn- handle-hide-show-map-report
  []
  (events/listen! (css/sel "#map-report > a")
                  :click
                  (fn [e]
                    (let [target (events/target e)
                          mr (css/sel "#map-report")]

                      (events/prevent-default e)
                      (cond

                       (dom/has-class? mr "open")
                       (do
                         (dom/remove-class! mr "open")
                         (-> mr dom/nodes first $ (jayq/anim {"right" "-300px"} 400)))

                       true
                       (do
                         (dom/add-class! mr "open")
                         (-> mr dom/nodes first $ (jayq/anim {"right" "0px"} 400))))))))

(def view-class-pattern #"^view-(\S+)$")

(defn change-view
  "do the DOM manip to change the view"
  [view]
  (let [target-page (css/sel (str "#page-" view))
        _ (when (= 0 (count (dom/nodes target-page)))
            (throw (ex-info (str "can't find view element: #page-" view) {:view view})))

        body (css/sel "body")
        view-body-class (str "view-" view)
        body-classes (conj (dom/classes body) view-body-class)
        new-body-classes (->> body-classes
                              (filter (fn [s]
                                        (or (not (re-matches view-class-pattern s))
                                            (= s view-body-class)))))

        target (css/sel (str ".nav-links .view-" view))
        links (css/sel ".nav-links")
        active (css/sel links ".active")]

    (dom/remove-class! active "active")
    (dom/add-class! target "active")
    (dom/set-classes! body new-body-classes)

    (events/dispatch! "clustermap-change-view" {})))

(defn- handle-view-switches
  "switches views based on nav-link clicks"
  [nav-fn]
  (events/listen! (css/sel ".nav-links a")
                  :click
                  (fn [e]
                    (let [target (events/target e)
                          target-classes (dom/classes target)
                          view-class (some->> target-classes (filter #(re-matches view-class-pattern %)) first)
                          v (when (not-empty view-class) (some->> view-class (re-find view-class-pattern) last))]
                      (events/prevent-default e)
                      (when v
                        (nav-fn v))))))

(defn set-route
  [history view]
  (let [token (.getToken history)
        new-token (cond
                    view (url/change-token-path token (str "/" (name view)))
                    :else (url/change-token-path token (str "")))]
    (.setToken history new-token)))

(defn set-view
  [app-state path view]
  (.log js/console (clj->js ["change-view" view]))
  (swap! app-state assoc-in path view)
  (change-view view))

(defn send-filter-rqs
  [filter-rq query-params]
  (.log js/console (str ["ROUTE-PARAMS" query-params]))
  (doseq [[filter-id filter-str] query-params]
    (let [f (filters/parse-url-param-value filter-str)]
      (put! filter-rq [filter-id f]))))

(defn init-routes
  [filter-rq app-state path default-view]

  (secretary/defroute "" [query-params]
    ;; (set-view app-state path default-view)
    )

  (secretary/defroute "/" [query-params]
    ;; (set-view app-state path default-view)
    )

  (secretary/defroute "/:view" [view query-params]
    (set-view app-state path view)
    (send-filter-rqs filter-rq query-params)))

(defn init
  "initialise navigation and routing

   history : the History object
   filter-rq : core.async channel for filter request messages
   app-state : the app state atom
   path : the path to update with the current view
   default-view : default-view to be applied

   returns a function of a single param, thew view, which
   can be used to navigate to that view"
  [history filter-rq app-state path default-view]
  (let [navigator-fn (partial set-route history)]

    (init-bootstrap-tooltips)
    (handle-hide-show-map-report)
    (handle-view-switches navigator-fn)

    (init-routes filter-rq app-state path default-view)

    navigator-fn))

(defn destroy
  []
  (secretary/reset-routes!)
  (events/unlisten! (css/sel "#map-report > a"))
  (events/unlisten! (css/sel ".nav-links a")))
