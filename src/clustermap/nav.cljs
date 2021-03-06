(ns clustermap.nav
  (:require [domina :as dom]
            [domina.css :as css]
            [domina.xpath :as xpath]
            [domina.events :as events]
            [secretary.core :as secretary :include-macros true]
            [jayq.core :as jayq :refer [$]]
            [cljs.core.async :refer [put! chan <!]]
            [clustermap.util :refer-macros [inspect] :refer [pp]]
            [clustermap.formats.url :as url]
            [clustermap.filters :as filters]
            [clustermap.api :as api]
            [clojure.string :as str]))

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

(defn zero-company-info
  "Workaround to stop flash-of-old-data in company info page.
  Set the company-info record to nil unless selection-filter-spec is
  for the same natural_id that is already loaded."
  [app-state]
  (let [state @app-state]
    (when (not= (str (get-in state
                             [:selection-filter-spec :components
                              :natural-id :term "?natural_id"]))
                (str (get-in state [:company-info :record :natural_id])))
      (swap! app-state assoc-in [:company-info :record] nil))))

(defn set-route
  [history view]
  (let [token (.getToken history)
        new-token (cond
                    view (url/change-token-path token (str "/" (name view)))
                    :else (url/change-token-path token (str "")))]
    (.setToken history new-token)))

(defn set-view
  [app-state path view]
  (.log js/console (pp ["change-view" view path]))
  (when (= view "company")
    (zero-company-info app-state))
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
    (cond ;; TODO: config via init params?
      (and (str/starts-with? api/api-prefix "bvca")
           (contains? query-params :coll)
           (str/includes? (:coll query-params) "boundaryline"))
      (let [coll (filters/parse-url-param-value (:coll query-params))]
        (send-filter-rqs filter-rq query-params)
        (events/dispatch! :clustermap-bvca-constituency
                          {:boundaryline_id (:boundaryline coll)}))

      :else  (send-filter-rqs filter-rq query-params)))

  (secretary/defroute "/company/:id" [id]
    (set-view app-state path "company")
    (events/dispatch! :clustermap-company-selection {:company-id id}))

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
