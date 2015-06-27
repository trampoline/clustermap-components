(ns clustermap.components.select-chooser
  (:require [om.core :as om :include-macros true]
            [sablono.core :as html :refer-macros [html]]))

(defn select-chooser-component
  "update a cursor with values from a [[val label]+] seq
   - title : title
   - value-labels a seq of {:value v :label l} records
   - read-update-fn : dual-arity fn, called with [cursor] to read the value, or [cursor value-label-record] to update"
  [title value-labels read-update-fn cursor owner]
  (om/component

   (let [current-value (some-> (read-update-fn cursor) name)
         value-labels-by-val (reduce
                              (fn [m {:keys [value label] :as value-label}] (assoc m (name value) value-label))
                              {}
                              value-labels)]
     (js/console.log (clj->js ["SELECT-CHOOSER" value-labels-by-val]))
     (html
      [:select {:onChange (fn [e]
                            (let [val (-> e .-target .-value not-empty)]
                              (.log js/console val)
                              (read-update-fn cursor (get value-labels-by-val val))))
                :value current-value}
       (for [{:keys [value label]} value-labels]
         [:option {:value (name value)} label])]))))
