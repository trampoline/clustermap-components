(ns clustermap.components.geo-sponsors
  (:require [om.core :as om :include-macros true]
            [om-tools.core :refer-macros [defcomponentk]]
            [schema.core :as s :refer-macros [defschema]]
            [sablono.core :as html :refer-macros [html]]
            [clustermap.ordered-resource :as ordered-resource]
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


(defn request-geo-sponsors-data
  [resource bounds]
  (ordered-resource/api-call resource api/geo-sponsors bounds))

(defcomponentk geo-sponsors-component
  [[:data bounds geo-sponsors]
   owner]

  (render
   [_]
   (render* (:data geo-sponsors)))

  (did-mount
   [_]
   (let [gsr (ordered-resource/make-discard-stale-resource "geo-sponsors-data-resource")]
     (om/set-state! owner :geo-sponsors-data-resource gsr)
     (ordered-resource/retrieve-responses gsr (fn [response]
                                                (.log js/console (clj->js ["GEO-SPONSORS : " response]))
                                                (om/update! geo-sponsors [:data] response)))))

  (will-update [_
                {next-bounds :bounds
                 {next-data :data
                  :as next-geo-sponsors} :geo-sponsors}
                {next-geo-sponsors-data-resource :geo-sponsors-data-resource}]

               (when (or (not next-data)
                         (not= next-bounds bounds))

                 (request-geo-sponsors-data next-geo-sponsors-data-resource
                                            next-bounds))))
