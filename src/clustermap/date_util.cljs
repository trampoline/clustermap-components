(ns clustermap.date-util
  "date stuff from https://github.com/hiram-madelaine/om-inputs.
  Ideally would use om-inputs completely but it doesn't support
  initial data yet. Will investigate later."
  (:require goog.events
            [clojure.string :as str]
            [clustermap.util :refer-macros [inspect]])
  (:import [goog.i18n DateTimeFormat DateTimeParse]
           [goog.ui InputDatePicker]
           [goog.date Date Interval]))

(def default-fmt "dd/MM/yyyy")



(def format-map
  (let [f DateTimeFormat.Format]
    {:FULL_DATE (.-FULL_DATE f)
     :FULL_DATETIME (.-FULL_DATETIME f)
     :FULL_TIME (.-FULL_TIME f)
     :LONG_DATE (.-LONG_DATE f)
     :LONG_DATETIME (.-LONG_DATETIME f)
     :LONG_TIME (.-LONG_TIME f)
     :MEDIUM_DATE (.-MEDIUM_DATE f)
     :MEDIUM_DATETIME (.-MEDIUM_DATETIME f)
     :MEDIUM_TIME (.-MEDIUM_TIME f)
     :SHORT_DATE (.-SHORT_DATE f)
     :SHORT_DATETIME (.-SHORT_DATETIME f)
     :SHORT_TIME (.-SHORT_TIME f)}))

(defn fmt
  "Format a date using either the built-in goog.i18n.DateTimeFormat.Format enum
   or a formatting string like \"dd MMMM yyyy\""
  ([date]
   (fmt default-fmt date))
  ([date-format date]
   (.format (DateTimeFormat. (or (format-map date-format) date-format))
            (js/Date. date))))

(defn parse
  "Parse a Date according to the format specified
   Default format is dd/MM/yyyy"
  ([f s]
   (let [p (DateTimeParse. f)
         d (js/Date.)]
     (.strictParse p s d)
     d))
  ([s]
   (parse default-fmt s)))

(defn display-date
  "Takes care of date rendering in the input."
  ([f v]
   (when-not (str/blank? v) (fmt f v)))
  ([v]
   (display-date default-fmt v)))


(defn goog-date->js-date
  [d]
  (when d
    (parse (fmt default-fmt d))))

(defn date-picker
  "Build a google.ui.InputDatePicker with a specific format"
  [f]
  (InputDatePicker. (DateTimeFormat. f) (DateTimeParse. f) nil nil))

(defn add-date-picker!
  "Decorate an HTML node with google.ui.inputdatepicker"
  [k node chan f cb]
  (let [dp (date-picker f)]
    (.decorate dp node )
    (goog.events/listen dp goog.ui.DatePicker.Events.CHANGE #(cb (goog-date->js-date (.-date %))))))

;; #(do (put! chan [k (goog-date->js-date (.-date %))])
;;      (put! chan [:validate k]))
