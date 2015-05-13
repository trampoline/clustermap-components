(ns clustermap.components.select-chooser
  (:require [om.core :as om :include-macros true]
            [sablono.core :as html :refer-macros [html]]))

(defn select-chooser-component
  [title key value-descriptions {:as cursor} owner]
  (om/component

   (let [current-val (some-> cursor (get-in [key]) name)]
     (html
      [:select {:onChange (fn [e]
                            (let [val (-> e .-target .-value not-empty)]
                              (.log js/console val)
                              (om/update! cursor key val)))
                :value current-val}
       (for [[value description] value-descriptions]
         [:option {:value (name value)} description])]))))
