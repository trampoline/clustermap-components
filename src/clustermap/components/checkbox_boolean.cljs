(ns clustermap.components.checkbox-boolean
  (:require [om.core :as om :include-macros true]
            [om-tools.core :refer-macros [defcomponentk]]
            [plumbing.core :refer [assoc-when]]
            [schema.core :as s]
            [sablono.core :as html :refer-macros [html]]))

(defn checkbox-boolean-component
  [key {:as cursor}]
  (om/component
   (html
    [:input {:type "checkbox"
             :checked (get cursor key)
             :onChange (fn [e]
                         (let [val (-> e .-target .-checked)]
                           (om/update! cursor key val)))}])))
