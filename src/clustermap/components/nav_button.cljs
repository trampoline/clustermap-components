(ns clustermap.components.nav-button
  (:require [om.core :as om :include-macros true]
            [om-tools.core :refer-macros [defcomponentk]]
            [plumbing.core :refer [assoc-when]]
            [schema.core :as s]
            [sablono.core :as html :refer-macros [html]]
            [clustermap.app :refer [navigate]]))

(def NavButtonSchema
  {:nav-button {:text s/Str
                :target-view s/Str
                (s/optional-key :class) s/Str
                (s/optional-key :id) s/Str
                (s/optional-key :on-click-action) (s/pred fn?)}})

(defcomponentk nav-button-component
  "On click action is an optional side-effecting fn which takes 3 args
  e, app, owner."
  [[:data [:nav-button text target-view class {id nil} {on-click-action nil}]] :- NavButtonSchema
   [:shared app]
   owner]
  (render [_]
    (html
     [:button.btn (-> {:type "button"
                       :onClick (fn [e]
                                  (when on-click-action
                                    (on-click-action e app owner))
                                  (navigate app target-view))}
                      (assoc-when :class class)
                      (assoc-when :id id))
      text])))
