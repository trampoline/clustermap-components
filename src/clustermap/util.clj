(ns clustermap.util)

(defn- inspect-1 [expr]
  `(let [result# ~expr]
     (js/console.info (str (pr-str '~expr) " => " (pr-str result#)))
     result#))

(defmacro inspect2 [& exprs]
  `(do ~@(map inspect-1 exprs)))

(defn- inspect-2 [expr]
  `(let [result# ~expr]
     (js/console.debug (pp (symbol (pr-str '~expr)))
                       (pp (symbol "=>"))
                       (pp result#))
     result#))

(defmacro inspect [& exprs]
  `(do ~@(map inspect-2 exprs)))


(defmacro breakpoint []
  '(do (js* "debugger;")
       nil))

(defmacro <? [ch]
  `(clustermap.util/throw-err (cljs.core.async/<! ~ch)))
