(ns clustermap.components.color-scale
  (:require [om.core :as om :include-macros true]
            [sablono.core :as html :refer-macros [html]]
            [clustermap.formats.money :as money]
            [clustermap.formats.color :as color]))

(defn color-scale-component
  [threshold-colors owner]
  (om/component
   (html
    [:div.color-scale
     [:div.tbl
      [:div.tbl-row
       (for [[threshold color] threshold-colors]
         [:div.tbl-cell {:style {:background-color color
                                 :color (color/stand-out-color color)}}
          (money/readable threshold :sf 2 :curr "" :default "")])
]]])))
