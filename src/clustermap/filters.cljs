(ns clustermap.filters
  (:require [schema.core :as s :include-macros true]))

(def FilterSchema
  {:component-specs [{:id s/Keyword
                      :type s/Keyword
                      :label s/Str
                      s/Keyword s/Any}]
   :components {s/Keyword s/Any}
   :component-descrs {s/Keyword s/Any}
   :base-filters {s/Keyword s/Any}
   :composed s/Any})

(defn make-sequential
  [s]
  (cond (sequential? s) (vec s)
        (nil? s) []
        :else [s]))

(defn or-filters
  [filters]
  (let [filters (make-sequential filters)]
    {:bool {:should filters}}))

(defn de-or-filters
  [combf]
  (get-in combf [:bool :should]))

(defn and-filters
  "and a sequence of filters"
  [filters]
  (let [filters (make-sequential filters)]
    {:bool {:must filters}}))

(defn de-and-filters
  "extract a sequence of filters which have been ANDed"
  [combf]
  (get-in combf [:bool :must]))

(defn compose-base-filter
  "AND all components and a base-filter"
  [components base-filter]
  (let [filters (some-> components vals (conj base-filter))
        filters (->> filters (filter identity) vec)]

    (and-filters filters)))

(defn compose-filters
  "take the filter components, and combine with each base-filter to produce
   a restricted version of the base-filter"
  [components base-filters]
  (->> base-filters
       (map (fn [[k bf]] [k (compose-base-filter components bf)]))
       (into {})))


(s/defn update-filter-component :- FilterSchema
  "update the filter component k with filter f and description d"
  [filters :- FilterSchema
   k :- s/Str
   f :- s/Any
   d :- s/Str]

  (let [f (-> filters
              (assoc-in [:components k] f)
              (assoc-in [:component-descrs k] d))]
    (assoc-in f [:composed] (compose-filters (:components f) (:base-filters f)))))
