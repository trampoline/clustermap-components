(ns clustermap.api
  (:require [clojure.tools.macro :refer [name-with-attributes]]))

(defmacro def-lastcall-method
  "defines an API method with last-call-wins semantics : returns a
   channel of a single-value, the response

   only responses corresponding to the the last
   call made are returned, so out-of-order responses are discarded.
   channels corresponding to discarded responses will be closed
   empty

   the body must return a core.async channel of a single-value,
   being the response of a single api call

   an additional no-args version of the function is defined which
   discards any pending responses and ensures any outstanding takes
   return nil"
  [name & params-body]
  (let [[name [params & body]] (name-with-attributes name params-body)]

    `(let [in-flight-atom# (atom nil)]
       (defn ~name
         ([]
          (let [emptych# (cljs.core.async/chan)]
            (cljs.core.async/close! emptych#)
            (clustermap.api/lastcall-method-impl in-flight-atom# emptych#)))
         (~params
          (let [valch# ~@body]
            (clustermap.api/lastcall-method-impl in-flight-atom# valch#)))))))
