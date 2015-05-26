(ns clustermap.api
  (:require [clojure.tools.macro :refer [name-with-attributes]]))

(defmacro def-lastcall-method
  "defines an API method with last-call-wins semantics : no result
   will be returned until a result matching the last call made is
   received, so out-of-order responses are discarded

   the body must return a core.async channel of a single-value,
   being the result of a single api call"
  [name & params-body]
  (let [[name [params & body]] (name-with-attributes name params-body)]

    `(let [in-flight-atom# (atom nil)]
       (defn ~name ~params
         (let [valch# ~@body]
           (clustermap.api/lastcall-method-impl in-flight-atom# valch#))))))
