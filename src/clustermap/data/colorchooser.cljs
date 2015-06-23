(ns clustermap.data.colorchooser
  (:require [clustermap.data.picker :as picker]
            [clustermap.data.colorbrewer :as colorbrewer]))

(defn auto-scale
  "chooses scale points based on the data itself... i.e. n-tile points"
  [steps data]
  (let [sorted-data (->> data (filter identity) sort vec)
        point-count (count sorted-data)]

    (->> (range 1 steps)
         (map (fn [i] (get sorted-data (int (/ (* point-count i) steps))))))))

(defn auto-no-outliers-scale
  "chooses scale points based on the data with outliers filtered by interquartile range
   k - only use points in range [Q1 - k*(Q3-Q1) , Q3 + k*(Q3-Q1)]"
  [steps k data]
  (let [sorted-data (->> data (filter identity) sort vec)
        point-count (count sorted-data)
        q1 (get sorted-data (int (/ (* point-count 1) 4)))
        q3 (get sorted-data (int (/ (* point-count 3) 4)))
        iq (- q3 q1)
        filtered-data (->> sorted-data
                           (filter (fn [d] (and (>= d (- q1 (* k iq)))
                                                (<= d (+ q3 (* k iq))))))
                           vec)
        filtered-point-count (count filtered-data)]

    (->> (range 1 steps)
         (map (fn [i] (get filtered-data (int (/ (* filtered-point-count i) steps))))))))

(defn linear-scale
  "returns a list of linear-scale thresholds"
  [min max steps]
  (let [step (/ (- max min) steps)]
    (->> (range 1 steps)
         (map (fn [i] (+ min (* i step)))))))

(defn log-scale
  "returns a list of log-scale thresholds"
  [min max steps]
  (let [translation (- 1 min) ;; translate to avoid NaNs

        log-min 0 ;; (Math/log 1)
        log-max (Math/log (+ max translation))

        log-linear-scale (linear-scale 0 log-max steps)]

    (->> log-linear-scale
         (map (fn [n] (- (Math/pow Math/E n) translation))))))

(defn choose-from-scheme
  [scheme thresholds value]
  (let [scheme-thresholds (map vector thresholds scheme)
        chosen (->> scheme-thresholds
                    (filter (fn [[t i]] (<= value t)))
                    first
                    last)]
    (or chosen
        (last scheme))))

(defn choose-color-scheme
  [color-scheme-spec]
  (let [[k & rest] color-scheme-spec]
    (if (keyword? k)
      (get-in colorbrewer/schemes (map keyword color-scheme-spec))
      color-scheme-spec)))

(defn choose
  "return a map of {key-value => colours}"
  [color-scheme-spec scale key variable data]
  (let [color-scheme (choose-color-scheme color-scheme-spec)
        col-count (count color-scheme)
        values (->> (picker/pick-variable variable data) (filter identity))
        min-value (apply min values)
        max-value (apply max values)
        thresholds (condp = scale
                     :log (log-scale min-value max-value col-count)
                     :linear (linear-scale min-value max-value col-count)
                     :auto (auto-scale col-count values)
                     :auto-no-outliers (auto-no-outliers-scale col-count 5 values))
        chooser (partial choose-from-scheme color-scheme thresholds)
        value-colors (->> data
                          (map (fn [r]
                                 [(get r key) (chooser (get r variable))]))
                          (into {}))
        threshold-colors (map vector (concat [min-value] thresholds) color-scheme)]
    [threshold-colors value-colors]))
