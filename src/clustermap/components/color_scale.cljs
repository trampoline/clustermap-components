(ns clustermap.components.color-scale
  (:require [om.core :as om :include-macros true]
            [sablono.core :as html :refer-macros [html]]
            [clustermap.formats.money :as money]
            [clustermap.formats.color :as color]))

(defn color-scale-component
  [threshold-colors owner]
  (let [font-size (condp <= (count threshold-colors)
                    12 "40%"
                    11 "50%"
                    10 "60%"
                    9 "70%"
                    8 "80%"
                    7 "90%"
                    "100%")]
    (om/component
     (html
      [:table.table-key
       [:tbody
        [:tr
         (for [[threshold color] threshold-colors]
           [:td {:style {:background-color color
                         :color (color/stand-out-color color)
                         :font-size font-size
                         ;; :padding-left "1px"
                         ;; :padding-right "1px"
                         }}
            (when threshold
              (money/readable threshold :sf 2 :curr "" :default ""))])
         ]]]))))
