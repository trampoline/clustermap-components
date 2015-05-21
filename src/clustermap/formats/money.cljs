(ns clustermap.formats.money
  (:require [clustermap.formats.number :as nform]))

(def ^:private money-suffixes
  {0 ""
   3 "k"
   6 "m"
   9 "bn"
   12 "tn"})

(defn- money-suffix
  "take an exponent and return a suffix"
  [exp]
  (or (money-suffixes exp) (str "x10^" exp)))

(defn readable
  "format human readable money amount
   :sf - number of significant figures
   :curr - currency symbol
   :plus? - use a plus prefix for +ve amounts
   :default - default result when (nil? n)"
  [n & {:keys [sf curr plus? default] :or {curr "£"}}]
  (if (and (number? n) (not (js/isNaN n)))
    (let [[sig exp] (nform/eng-notation n :sf sf)
          abs-sig (js/Math.abs sig)
          suffix (money-suffix exp)]
      (if (>= exp 0)
        (apply str (filter identity
                           [(cond (and plus? (> sig 0)) "+"
                                  (< sig 0) "-")
                            curr
                            abs-sig
                            suffix]))
        0 ;; don't display x10^-n type small numbers
        ))
    default))

(def fmoney readable)
