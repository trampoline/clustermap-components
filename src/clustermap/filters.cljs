(ns clustermap.filters)

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
