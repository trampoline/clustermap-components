(ns clustermap.components.filter-description
  (:require [clojure.string :as str]
            [om.core :as om :include-macros true]
            [om-tools.core :refer-macros [defcomponentk]]
            [schema.core :as s]
            [sablono.core :as html :refer-macros [html]]))

(def FilterDescriptionSchema
  {:components [s/Keyword]
   :filter-spec {s/Keyword s/Any}})

(defcomponentk filter-description-component
  [[:data
    components
    filter-spec] :- FilterDescriptionSchema
    owner]

  (render
   [_]
   (html
    [:span (some->> components
                    (map #(get-in filter-spec [:component-descrs %]))
                    (filter identity)
                    (str/join ", "))])))
