(ns clustermap.components.color-scale
  (:require [om.core :as om :include-macros true]
            [sablono.core :as html :refer-macros [html]]
            [clustermap.formats.number :as num]
            [clustermap.formats.color :as color]))

(defn color-scale-component
  [{:keys [threshold-colors boundaryline-fill-opacity] :as controls} owner]
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
           [:td {:style {:padding 0}}
            [:div {:style {:position "relative"}}
             [:div {:style {:position "absolute"
                            :top 0
                            :left 0
                            :width "100%"
                            :height "100%"
                            :background-color color
                            :opacity boundaryline-fill-opacity
                            ;; :padding-left "1px"
                            ;; :padding-right "1px"
                            }}]
             [:div {:style {:position "static"
                            :color "black" ;; (color/stand-out-color color)
                            :font-size font-size
                            :margin "4px"
                            }}
              (when threshold
                (num/compact threshold))]]])
         ]]]))))
