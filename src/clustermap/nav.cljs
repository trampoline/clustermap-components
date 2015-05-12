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

(defn change-view
  "do the DOM manip to change the view"
  [view]
  (let [view (or (not-empty view) "main")
        view-class (str ".view-" view)

        hide-sections (css/sel ".view")
        show-sections (css/sel view-class)]

    (dom/add-class! hide-sections "hide")
    (dom/remove-class! show-sections "hide")

    (events/dispatch! "clustermap-change-view" {})))

;;(def view-class-pattern #"^view-(\S+)$")
;;
;; (defn- handle-view-switches
;;   "switches views based on nav-link clicks"
;;   [nav-fn]
;;   (events/listen! (css/sel ".nav-links a")
;;                   :click
;;                   (fn [e]
;;                     (let [target (events/target e)
;;                           target-classes (dom/classes target)
;;                           view-class (some->> target-classes (filter #(re-matches view-class-pattern %)) first)
;;                           v (when (not-empty view-class) (some->> view-class (re-find view-class-pattern) last))]
;;                       (events/prevent-default e)
;;                       (when v
;;                         (nav-fn v))))))

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
    (set-view app-state path "main")
    (send-filter-rqs filter-rq query-params))

  (secretary/defroute "/" [query-params]
    (set-view app-state path "main")
    (send-filter-rqs filter-rq query-params))

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
    ;; (handle-view-switches navigator-fn)

    (init-routes filter-rq app-state path default-view)

    navigator-fn))

(defn destroy
  []
  (secretary/reset-routes!)
  (events/unlisten! (css/sel "#map-report > a"))
  (events/unlisten! (css/sel ".nav-links a")))
