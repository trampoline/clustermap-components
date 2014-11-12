(ns clustermap.formats.html
  (:require [clojure.string :as str]))

(defn- combine-classes
  [& classes]
  (->> classes
       (filter identity)
       (filter (complement str/blank?))
       (str/join " ")))
