(ns clustermap.filters
  (:require [schema.core :as s :include-macros true]
            [clustermap.util :as util :refer-macros [inspect]]))

(def FilterSchema
  {:id s/Keyword
   (s/optional-key :open) s/Bool
   :component-specs [{:id s/Keyword
                      :type s/Keyword
                      :label s/Str
                      (s/optional-key :skip-label) s/Bool
                      (s/optional-key :visible) s/Bool
                      s/Keyword s/Any}]
   :components {s/Keyword s/Any}
   :component-descrs {s/Keyword s/Any}
   :url-components {s/Keyword (s/maybe s/Str)}
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
  "update the filter component k with filter f, description d, and url component u"
  [filters :- FilterSchema
   k :- s/Keyword
   f :- s/Any
   d :- s/Str
   u :- s/Str]

  (let [f (-> filters
              (assoc-in [:components k] f)
              (assoc-in [:component-descrs k] d)
              (assoc-in [:url-components k] u))]
    (assoc-in f [:composed] (compose-filters (:components f) (:base-filters f)))))

(s/defn reset-filter :- FilterSchema
  "reset all filter components"
  [filters :- FilterSchema]

  (let [f (-> filters
              (assoc-in [:components] {})
              (assoc-in [:component-descrs] {})
              (assoc-in [:url-components] {}))]
    (assoc-in f [:composed] (compose-filters (:components f) (:base-filters f)))))

(s/defn filter-url-param-value
  "JSON encode the filter for use as a URL param"
  [url-components]
  (some->> url-components
           (filter (fn [[k v]] (not-empty v)))
           (into {})
           not-empty
           clj->js
           js/JSON.stringify))

(s/defn parse-url-param-value
  [v]
  (some-> v
          js/JSON.parse
          (js->clj :keywordize-keys true)))


(defn term-to-nested-query
  "Convert a flat term query to a nested query. Only works if is first item"
  [filter-spec term path nested-term]
  (if-let [value (get-in filter-spec [:bool :must 0 :term term])]
    (assoc-in filter-spec [:bool :must 0]
              {:bool {:should {:nested {:path path
                                        :filter {:bool {:must {:term {nested-term value}}}}}}}})
    filter-spec))

(defn munge-filter-for-investments
  "Hack to convert a query to work on indexes with nested investor companies"
  [filter-spec]
  (term-to-nested-query filter-spec
                        "?investor_company_uid"
                        "?investor_companies" "investor_company_uid"))

(defn munge-filter-for-constituencies
  "Hack to convert a query to work on indexes with nested boundarylines"
  [filter-spec]
  (term-to-nested-query filter-spec
                        "?boundaryline_id"
                        "?boundarylines" "boundaryline_id"))

(defn nested-to-term-query
  [filter-spec path nested-term term]
  (if-not (= path (get-in filter-spec [:bool :must 0 :nested :path]))
    filter-spec
    (let [value (get-in filter-spec [:bool :must 0 :nested :filter :term nested-term])]
      (assoc-in filter-spec [:bool :must 0] {:term {term value}}))))

(defn add-to-must
  [filter-spec query]
  (update-in filter-spec [:bool :must] conj query))
