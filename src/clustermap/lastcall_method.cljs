(ns clustermap.lastcall-method
  (:require-macros
   [cljs.core.async.macros :refer [go]])
  (:require
   [cljs.core.async :as async :refer [<! chan close! put! sliding-buffer]]))

(defn lastcall-method-impl
  "implements last-call-wins aync api-call semantics, discarding results from
  any earlier api calls
  - in-flight-atom : an atom used to match received results to calls
  - valch : a single-value channel eventually containing one api-call result"
  [in-flight-atom valch]
  (let [rx (chan)]
    (reset! in-flight-atom valch)

    (go
      (let [val (<! valch)]
        (when (and val
                   (= @in-flight-atom valch))
          (put! rx val))
        (close! valch)
        (close! rx)))

    rx))
