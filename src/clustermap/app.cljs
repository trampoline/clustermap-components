(ns clustermap.app
  (:require-macros
   [cljs.core.async.macros :refer [go]])
  (:require
   [clojure.string :as str]
   [goog.events :as events]
   [cljs.core.async :refer [pub chan <! put! sliding-buffer]]
   [secretary.core :as secretary :include-macros true :refer [defroute]]
   [om.core :as om :include-macros true]
   [om.dom :as dom :include-macros true]
   [schema.core :as s :refer-macros [with-fn-validation]]
   [clustermap.api :as api]
   [clustermap.nav :as nav]
   [clustermap.ganalytics :as ga]
   [clustermap.components.mount :as mount]
   [clustermap.boundarylines :as bl]
   [clustermap.data.colorchooser :as colorchooser])
  (:import [goog History]
           [goog.history EventType]))


(defonce ^:private history* (History.))

(defprotocol IAppService
  ;; initialise : returns component shared state
  (init [this app])
  ;; destroy any resources created in init
  (destroy [this app])
  ;; handle an even from the app channel
  (handle-event [this app type val])
  ;; boolean if app is in dev-mode
  (dev-mode? [this]))

(defprotocol IAppControl
  (start [this])
  (stop [this]))

(defprotocol IApp
  (get-state [this])
  (get-comm [this])
  (get-filter-rq [this])
  (get-history [this])
  (get-navigator-fn [this])
  (navigate [this view]))

(defn create-app-instance
  [initial-state-value component-defs app-service]
  (let [comm (chan)
        filter-rq (chan)
        filter-rq-pub (pub filter-rq first) ;; first element defines the filter component
        state (atom initial-state-value)
        nav-fn (nav/init history* filter-rq state [:view] "main")]

    (reify
      IAppControl
      (start [this]
        (let [shared (merge (init app-service this) {:comm comm
                                                     :filter-rq-pub filter-rq-pub
                                                     :history history*
                                                     :app this})
              initial-token (.getToken history*)]

          (events/listen history*
                         EventType.NAVIGATE
                         (fn [e]
                           (let [token (.-token e)]
                             ;; (ga/send-pageview token)
                             (secretary/dispatch! token))))
          (.setEnabled history* true)

          (doseq [{:keys [name f target path paths]} component-defs]
            (.log js/console (clj->js ["component" name f target paths]))
            (if (dev-mode? app-service)
              (s/with-fn-validation
                (mount/mount name
                             f
                             state
                             :target target
                             :shared shared
                             :path path
                             :paths paths))
              (mount/mount name
                           f
                           state
                           :target target
                           :shared shared
                           :path path
                           :paths paths)))

          (go
            (while true
              (let [{type :type :as e} (<! comm)]
                (handle-event app-service this type e))))

          ;; need to dispatch the initial token after all components have been mounted
          ;; so that any filters get applied
          (secretary/dispatch! initial-token)
          ))

      (stop [this]
        (doseq [{:keys [target]} component-defs]
          (mount/unmount target))

        (destroy app-service this)

        (nav/destroy)

        (.removeAllListeners history*))

      IApp
      (get-state [_]
        state)

      (get-comm [_]
        comm)

      (get-filter-rq [_]
        filter-rq)

      (get-history [_]
        history*)

      (get-navigator-fn [_]
        nav-fn)

      (navigate [_ view] (nav-fn view)))))

(defn start-or-restart-app
  [app-instance-atom initial-state components app-service]
  (swap! app-instance-atom
         (fn [a initial-state components app-service]
           (when a (stop a))
           (let [new-app (create-app-instance initial-state components app-service)]
             (start new-app)
             new-app))
         initial-state
         components
         app-service))
