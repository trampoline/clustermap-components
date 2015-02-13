(ns clustermap.formats.time
  (:require [cljs-time.format :as format]
            [cljs-time.coerce :as coerce]
            [cljs-time.core :as tc]))

(defn get-year
  [t]
  (some-> t
          js/Date.
          .getYear
          (+ 1900)))

(def date-formatter (format/formatters :date))

(defn coerce-from-string
  [s]
  (if (string? s)
    (coerce/from-string s)
    s))

(defn format-date
  [t]
  (some->> t
           coerce-from-string
           (format/unparse date-formatter)))

(defn now
  []
  (tc/now))

(defn today-str
  []
  (-> (now)
      format-date))

(defn year-quarter-ago
  "returns [start end] for a quarter at least n months before t"
  [t n]
  (let [max-end (-> t (tc/minus (tc/years (quot n 12))) (tc/minus (tc/months (mod n 12))))
        max-end-month (tc/month max-end)
        quarter-offset (-> (- max-end-month 1) ;; zero-based for mod
                           (mod 3))
        quarter-end-month (- max-end-month quarter-offset)
        quarter-end (tc/date-midnight (tc/year max-end) quarter-end-month)
        quarter-start (tc/minus quarter-end (tc/months 12))]
    [quarter-start quarter-end]))

(defn months-ago
  [n]
  (-> (now)
      (tc/minus (tc/years (quot n 12)))
      (tc/minus (tc/months (mod n 12)))))
