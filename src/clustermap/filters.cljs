(ns clustermap.filters
  (:require [schema.core :as s :include-macros true]))

(def FilterSchema
  {:components {s/Keyword s/Any}
   :component-descrs {s/Keyword (s/maybe s/Str)}
   :base-filters {s/Keyword s/Any}
   :component-specs [s/Any]
   :composed s/Any})

(defn compose-base-filter
  "AND all components and a base-filter"
  [components base-filter]
  (let [filters (some-> components vals (conj base-filter))
        filters (->> filters (filter identity) vec)]
    (cond

     (> (count filters) 1)
     {:bool {:must (vec filters)}}

     (= (count filters) 1)
     (first filters)

     :else
     {:exists {:field "_uid"}})))

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
