(ns clustermap.formats.number
  (:require
   [clojure.string :as str]
   [goog.string :as gstring]
   [goog.string.format]))

(defn div!
  "a nil-safe /"
  [num & args]
  (when-not (some nil? args)
    (apply / num args)))

(defn *!
  [& args]
  (when-not (some nil? args)
    (apply * args)))

(defn +!
  [& args]
  (when-not (some nil? args)
    (apply + args)))

(defn -!
  [num & args]
  (when-not (some nil? args)
    (apply - num args)))

(defn actually-really-number?
  [n]
  (and (number? n)
       (not (js/isNaN n))
       (< n js/Number.POSITIVE_INFINITY)
       (> n js/Number.NEGATIVE_INFINITY)))

(defn round-decimal
  [n dec-places]
  (if dec-places
    (/ (js/Math.round (* n (js/Math.pow 10 dec-places)))
       (js/Math.pow 10 dec-places))
    n))

(defn log-percent-scale
  "return a % representing the size of a +ve number compared to the scale max
   - logmax : a power of 10
   - n : any number
   returns an integer in the range 0-100"
  [logmax n]
  (if (and n (> n 0))
    (let [n (max n 0)
          l (/ (js/Math.log n) (js/Math.log 10))
          l (min l logmax)]
      (js/Math.round (* 100 (/ l logmax))))
    0))

(defn table-percent-scale
  "return a % representing the size of a number
   - table a seq of values representing ticks on the scale
   - n : any number"
  [table n]
  (let [cnt (count table)
        i-table (keep-indexed vector table)
        i (or (some
               (fn [[i v]] (when (<= n v) i))
               i-table)
              cnt)]
    (js/Math.round (* 100 (/ i cnt)))))

(defn table-chooser-fn
  "returns a function which chooses a value for it's argument based on a table and sample values
   - table of proportions 0 < p < 1 which will be used to choose output thresholds
   - vals : sample values which will be sorted and representative threshold values chosen according to table
  returns a number 0 <= (count table)"
  ([table vals] (table-chooser-fn table vals {:default 0}))
  ([table vals {:keys [default]}]
   (let [vals (into [] (sort vals))
         c (count vals)
         idxs (->> table sort (map (fn [tv] (int (* tv c)))))
         ts (map (fn [idx] (get vals idx)) idxs)
         i-ts (keep-indexed vector ts)]

     (fn [val]
       (if val
         (or (some (fn [[i t]] (when (< val t) i)) i-ts)
             (count table))
         default)))))

(defn- prefix-sign
  [n-str n plus?]
  (cond
   (< n 0) (str "-" n-str)
   (and plus? (> n 0)) (str "+" n-str)
   true n-str))

(defn- split-thousands
  [n-str]
  (->> n-str
       reverse
       (partition 3 3 [])
       (map reverse)
       reverse
       (map #(apply str %))
       (str/join ",")))

(defn eng-notation
  "splits a number into a [coefficient, exponent] pair, where
   exponent is a multiple of 3
   :sf : # of significant figures"
  [n & {:keys [sf]}]
  (when n
    (let [sign (cond (> n 0) 1 (= n 0) 0 (< n 0) -1)
          abs-n (js/Math.abs n)
          ;; [i d] (.split (goog.string.format "%.20f" abs-n) ".") ;; format is better, but slow
          [i d] (.split (str abs-n) ".")
          exp (cond (>= abs-n 1) (* 3 (int (/ (-> i count dec) 3)))
                    (> abs-n 0) (* -3 (int (/ (->> d (take-while (fn [c] (= c \0))) count (+ 3)) 3)))
                    true 0)

          usig (* abs-n (js/Math.pow 10 (* -1 exp)))

          ;; now truncate to a given number of significant figures... first need to round,
          ;; then textually truncate to deal with fp junk
          ;; [usig-i usig-d] (when sf (.split (goog.string.format "%.20f" usig) ".")) ;; format is better, but slow
          [usig-i usig-d] (when sf (.split (str usig) "."))
          sigmult (when (and sf (> usig 0)) (js/Math.pow 10 (- (count usig-i) sf)))
          round-usig-str (when sigmult (str (* sigmult (js/Math.round (/ usig sigmult)))))
          [round-usig-i round-usig-d] (when round-usig-str (.split round-usig-str "."))
          trunc-usig-str (when round-usig-str (->> [(take sf round-usig-i)
                                                    (repeat (- (count round-usig-i) sf) "0")
                                                    (if (> (count round-usig-d) 0)
                                                      ["." (take (- sf (count round-usig-i)) round-usig-d)])]
                                                   flatten
                                                   (filter identity)
                                                   concat
                                                   (apply str)))
          trunc-usig (when trunc-usig-str (js/parseFloat trunc-usig-str))
          sig (* sign (or trunc-usig usig))]
      [sig exp])))

(defn readable
  "format a human readable number, with commas between groups of thousands
   :dec - number of decimal places
   :plus? - include a leading + for positive numbers
   :curr - optional currency symbol
  :default - default result when (nil? n)"
  ([n] (readable n nil))
  ([n {:keys [dec plus? curr default] :or {dec 0 plus? false curr "" default ""}}]
   (if (actually-really-number? n)
     (let [abs-n (js/Math.abs n)
           round-n (round-decimal abs-n dec)
           round-n-str (str round-n)
           [i-str d-str] (str/split round-n-str #"\.")
           t-str (split-thousands i-str)
           t-dec-str (str/join "." (filter identity [t-str d-str]))]
       (apply str (filter identity
                          [(cond (and plus? (> n 0)) "+"
                                 (< n 0) "-")
                           curr
                           t-dec-str])))
     default)))

(def fnum readable)

(def ^:private eng-suffixes
  {0 ""
   3 "k"
   6 "m"
   9 "bn"
   12 "tn"})

(defn- eng-suffix
  "take an exponent and return a suffix"
  [exp]
  (or (eng-suffixes exp) (str "x10^" exp)))

(defn eng-readable
  ([n] (eng-readable n nil))
  ([n {:keys [default plus? curr sf] :or {default "" plus? false curr "" sf 3}}]
   (if (actually-really-number? n)

     (let [[sig exp] (eng-notation n :sf sf)
           abs-sig (js/Math.abs sig)
           suffix (eng-suffix exp)]
       (apply str (filter identity
                          [(cond (and plus? (> sig 0)) "+"
                                 (< sig 0) "-")
                           curr
                           abs-sig
                           suffix])))

     default)))


(defn mixed
  "format a number with mixed format, showing decimal places below threshold and eng notation after
   :default - default result when (nil? n) or NaN

   :plus? - use a plus prefix for +ve amounts
   :curr - optional currency symbol

   :dec - number of decimal places (below threshold)
   :threshold - default 1000000

   :sf - number of significant figures (above threshold)"
  ([n] (mixed n nil))
  ([n {:keys [default plus? curr dec threshold sf] :or {default "" plus? false curr "" dec 0 threshold 1000000 sf 3} :as opts}]
   (if (actually-really-number? n)

     (if (< (js/Math.abs n) threshold)
       (readable n opts)
       (eng-readable n opts))

     default)))

(defn compact
  ([n] (compact n nil))
  ([n opts]
   (mixed n (merge {:sf 3 :threshold 100} opts ))))
