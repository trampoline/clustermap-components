(ns clustermap.components.geo-sponsors
  (:require-macros
   [cljs.core.async.macros :refer [go]])
  (:require [cljs.core.async :refer [<!]]
            [om.core :as om :include-macros true]
            [om-tools.core :refer-macros [defcomponentk]]
            [schema.core :as s :refer-macros [defschema]]
            [sablono.core :as html :refer-macros [html]]
            [clustermap.api :as api]))

(defn render*
  [geo-sponsors-data]
  (let [top3 (take 3 geo-sponsors-data)]
    (html
     [:div.sponsors
      (for [{:keys [name link_url image_url teaser]} top3]
        [:div.sponsor
         [:a {:href link_url}
          [:img {:src image_url}]]])])))


(defcomponentk geo-sponsors-component
  [[:data bounds geo-sponsors]
   owner]

  (render
   [_]
   (render* (:data geo-sponsors)))

  (did-mount
   [_]
   (om/set-state! owner :fetch-geo-sponsors-data-fn (api/geo-sponsors-factory)))

  (will-update [_
                {next-bounds :bounds
                 {next-data :data
                  :as next-geo-sponsors} :geo-sponsors}
                {fetch-geo-sponsors-data-fn :fetch-geo-sponsors-data-fn}]

               (when (or (not next-data)
                         (not= next-bounds bounds))

                 (go
                   (when-let [geo-sponsors-data (<! (fetch-geo-sponsors-data-fn next-bounds))]
                     (.log js/console (clj->js ["GEO-SPONSORS : " geo-sponsors-data]))
                     (om/update! geo-sponsors [:data] geo-sponsors-data))))))
