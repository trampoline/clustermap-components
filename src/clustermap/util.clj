(ns clustermap.util)

(defn- inspect-1 [expr]
  `(let [result# ~expr]
     (js/console.info (str (pr-str '~expr) " => " (pr-str result#)))
     result#))

(defmacro inspect [& exprs]
  `(do ~@(map inspect-1 exprs)))

(defmacro breakpoint []
  '(do (js* "debugger;")
       nil))

(defmacro <? [ch]
  `(clustermap.util/throw-err (cljs.core.async/<! ~ch)))
