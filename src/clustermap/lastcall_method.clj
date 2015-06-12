(ns clustermap.lastcall-method
  (:require [clojure.tools.macro :refer [name-with-attributes]]))

(defn ^:private lastcall-method*
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
  [def-sym name params-body]
  (let [[name [params & body]] (name-with-attributes name params-body)]

    `(let [in-flight-atom# (atom nil)]
       (~def-sym ~name
         ([]
          (let [emptych# (cljs.core.async/chan)]
            (cljs.core.async/close! emptych#)
            (clustermap.lastcall-method/lastcall-method-impl in-flight-atom# emptych#)))
         (~params
          (let [valch# ~@body]
            (clustermap.lastcall-method/lastcall-method-impl in-flight-atom# valch#)))))))

(defn ^:private fnk-lastcall-method*
  "like lastcall-method* but generates a hidden plumbing fnk function with the
   params and passes all args to it.
  there is stil a no-args version which discards pending responses"
  [def-sym name params-body]
  (let [[name [params & body]] (name-with-attributes name params-body)]

    `(let [in-flight-atom# (atom nil)
           f# (plumbing.core/fnk ~params
                                (let [valch# ~@body]
                                  (clustermap.lastcall-method/lastcall-method-impl in-flight-atom# valch#)))]
       (~def-sym ~name
         ([]
          (let [emptych# (cljs.core.async/chan)]
            (cljs.core.async/close! emptych#)
            (clustermap.lastcall-method/lastcall-method-impl in-flight-atom# emptych#)))
         ([& p#]
          (apply f# p#))))))

(defmacro def-lastcall-method
  "defn a lastcall method"
  [name & params-body]
  (lastcall-method* 'defn name params-body))

(defmacro def-lastcall-method-factory
  "defn a zero-args factory for a lastcall method, which returns an instance
  of the method when called"
  [name & params-body]
  (let [[name [params & body]] (name-with-attributes name params-body)
        method-name (gensym name)
        method-fn (lastcall-method* 'fn method-name params-body)]
    `(defn ~name
       []
       ~method-fn)))

(defmacro lastcall-method
  "return an anonymous instance of a lastcall method"
  [name & params-body]
  (lastcall-method* 'fn name params-body))

;; fnk versions of all the above

(defmacro defnk-lastcall-method
  "defnk a fnk-lastcall method"
  [name & params-body]
  (fnk-lastcall-method* 'defn name params-body))

(defmacro defnk-lastcall-method-factory
  "defn a zero-args factory for an fnk-lastcall method, which returns an instance
  of the method when called"
  [name & params-body]
  (let [[name [params & body]] (name-with-attributes name params-body)
        method-name (gensym name)
        method-fn (fnk-lastcall-method* 'fn method-name params-body)]
    `(defn ~name
       []
       ~method-fn)))

(defmacro fnk-lastcall-method
  "return an anonymous instance of an fnk-lastcall method"
  [name & params-body]
  (fnk-lastcall-method* 'fn name params-body))
