(ns clustermap.components.table-common
  (:require
   [clojure.string :as str]
   [om.core :as om :include-macros true]
   [clustermap.formats.html :as html]))

(defn make-sequential
  "return v if it's already sequential, otherwise wrap it in a vector"
  [v]
  (cond
   (nil? v) []
   (sequential? v) v
   true [v]))

(defn- es-sort-dir
  [dir]
  (if (= \d (some-> dir name first))
    :desc
    :asc))

(defn parse-sort-spec
  "turn an ES sort-spec into a seq of [key order] pairs"
  [sort-spec]
  (let [sort-spec (make-sequential sort-spec)]

    (for [order-spec sort-spec]

      (if (map? order-spec)

        (let [[key order-dir] (some-> order-spec seq first)]
          (if (map? order-dir)

            (let [[_ order-dir] (-> order-dir seq first)]
              [key (es-sort-dir order-dir)])

            [key (es-sort-dir order-dir)]))

        [(keyword order-spec) :asc]))))

(defn order-col
  "generate a table-ordering link for table-headers
   - controls : cursor with :sort-spec member containing sort-spec
   - current-sort-spec : the current sort-spec
   - col-key : the column key
   - col-label : the column name"
  [controls
   current-sort-spec
   col-key
   col-label]

  (let [[current-sort-key current-sort-dir] (some-> current-sort-spec parse-sort-spec first)
        content [col-label (when (and col-key (= current-sort-key col-key))
                            (condp = current-sort-dir
                              :asc [:i.icon-asc]
                              [:i.icon-desc]))]]
    (if controls
      (into  [:a
              {:href "#"
               :onClick (fn [e]
                            (.preventDefault e)
                            (cond

                             (and (= col-key current-sort-key)
                                  (= :asc current-sort-dir))
                             (om/update! controls :sort-spec {col-key {:order :desc}})

                             (and (= col-key current-sort-key)
                                  (= :desc current-sort-dir))
                             (om/update! controls :sort-spec {col-key {:order :asc}})

                             true
                             (om/update! controls :sort-spec {col-key {:order :asc}})))}]
             content)
      (into [:span] content))))

(defn column-value-descriptors
  "given a possibly nested seq of column descriptions,
   extract the column value descriptors in the correct order.
   each value descriptor is a map {:key col-key :name col-name :render-fn render-fn}"
  [columns]
  (->> columns
       (reduce (fn [cks col]
                 (if (sequential? col)
                   (into cks (column-value-descriptors (rest col))) ;; first value is group title
                   (into cks [col])))
               [])))

(defn- extract-column-headers*
  "extract one row of column headers"
  [columns & [{:keys [controls current-sort-spec insert-blank-col]}]]
  (let [group-row (some sequential? columns)]
    (->> columns
         (map vector (iterate inc (if insert-blank-col 2 1)))
         (map (fn [[ci col]]
                   (if (sequential? col)
                     [:th {:class (html/combine-classes (str "col-" ci) (:class col))
                           :colSpan (count (column-value-descriptors (rest col)))} (first col) ]
                     (if group-row
                       [:th {:class (html/combine-classes (str "col-" ci) (:class col))}]
                       [:th {:class (html/combine-classes (str "col-" ci) (:class col))} (order-col controls current-sort-spec (:key col) (:label col))])))))))

(defn- extract-sub-columns*
  "extract the next row of column descriptions, if there are any"
  [columns]
  (if (some sequential? columns)
    (->> columns
         (mapcat (fn [col]
                   (if (sequential? col)
                     (rest col)
                     [col])))))) ;; have to wrap col, or concat undoes the hash

(defn- column-header-row-seq*
  "lazy seq of column header rows"
  [columns & [opts]]
  (when (not-empty columns)
    (lazy-seq
     (cons
      (extract-column-headers* columns opts)
      (column-header-row-seq*
       (extract-sub-columns* columns)
       opts)))))

(defn column-header-rows
  "given a possibly nested seq of column descriptions extract table header rows suitable
   for direct inclusion in a <thead> element
   - columns : the column descriptions
   - controls : the table controls cursor
   - current-sort-spec : the current sort spec"
  [columns & [{:keys [controls current-sort-spec insert-blank-col] :as opts}]]
  (->> (column-header-row-seq* columns opts)
       (map vector (iterate inc 1))
       (map (fn [[ri r]] (into [:tr {:class (str "row-" ri)} (when insert-blank-col [:th {:class "col-1"}])] r)))))
