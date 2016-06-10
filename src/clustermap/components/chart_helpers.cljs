(ns clustermap.components.chart-helpers
  (:require-macros [hiccups.core :as hiccups])
  (:require [clojure.string :as str]
            goog.object
            [hiccups.runtime :as hiccupsrt]
            [clustermap.formats.number :as num]))

(defn mk-tooltip-point-formatter
  "Return a function that formats highcharts tooltip as normal but
  with the y-value formatted for money. Also remove boro. Add
  percentage if present. Use color for series name if present. Pass in
  map of functions to change behaviour. pass result
  to :point-formatter in component map

  @param {!IMap} opts
  @return {!IFn}"
  [{:keys [key-fmt num-fmt]
    :or {key-fmt identity num-fmt num/mixed}
    :as opts}]
  (fn []
    (this-as this
      (let [key (some-> this .-key key-fmt)
            pc (some-> (goog.object/get this "percentage")
                       (num/mixed {:dec 1}))
            series-name (some-> this .-series .-name)
            color (some-> this .-series .-color)
            value (some-> this .-y num-fmt)]
        (hiccups/html
         [:span
          [:span {:style "font-size: 10px"} key]
          [:br]
          [:span.hc-toolip-series (when color {:style (str "color:" color)})
           series-name ": "]
          [:b value (when pc (str " (" pc "%)"))]])))))
