(ns clustermap.components.filter-selection
  (:require [om.core :as om :include-macros true]
            [sablono.core :as html :refer-macros [html]]))


(defn filter-selection-component
  [{{{render-fn :render-fn
      clear-fn :clear-fn
      component-key :component-key
      default-text :default-text} :controls
      selection :selection} :props
     filter-spec :filter-spec}]
  (let [render-fn (or render-fn identity)]
    (om/component
     (.log js/console (clj->js ["FILTER-SELECTION-COMPONENT" component-key default-text selection]))
     (html
      (if (not-empty selection)
        [:span [:a {:href "#"
                    :onClick (fn [e]
                               (.preventDefault e)
                               (.log js/console "clear selection")
                               (when clear-fn (clear-fn)))}
                "\u00D7"]
         "\u00a0"
         (render-fn selection)]
        [:span (or default-text "")])))))
