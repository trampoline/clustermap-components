(ns clustermap.components.select-chooser
  (:require [om.core :as om :include-macros true]
            [sablono.core :as html :refer-macros [html]]))

(defn select-chooser-component
  [title key value-descriptions {:as cursor} owner]
  (om/component
   (html
    [:div.select-chooser
     [:div.tbl
      [:div.tbl-row
       [:div.tbl-cell title]
       [:div.tbl-cell
        [:select {:onChange (fn [e]
                              (let [val (-> e .-target .-value not-empty)]
                                (.log js/console val)
                                (om/update! cursor key val)))}
         (for [[value description] value-descriptions]
           [:option {:value value} description])]]]]])))
