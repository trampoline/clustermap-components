(ns clustermap.components.filters.external-filter
  (:require-macros
   [cljs.core.async.macros :refer [go]])
  (:require [clojure.string :as str]
            [cljs.core.async :refer [<!]]
            [cljs.core.async.impl.channels :refer [ManyToManyChannel]]
            [om.core :as om :include-macros true]
            [om-tools.core :refer-macros [defcomponentk]]
            [plumbing.core :refer-macros [defnk]]
            [schema.core :as s]
            [sablono.core :as html :refer-macros [html]]
            [clustermap.filters :as filters]))


(defnk ^:private render*
  [[:filter-spec components :as filter-spec]
   [:component-spec id label {formatter identity} default-text :as component-spec]
   :as data]

  (html
   [:div.filter-body
    [:ul.filter-items
     [:li
      (formatter
       (if (not-empty (get-in filter-spec [:components id]))
         [:span [:a {:href "#"
                     :onClick (fn [e]
                                (.preventDefault e)
                                (.log js/console "clear selection")
                                (om/update! filter-spec (filters/update-filter-component filter-spec id nil nil nil)))}
                 "\u00D7"]
          "\u00a0"
          (get-in filter-spec [:component-descrs id])]
         [:span (or default-text "")]))]]]))

(def ExternalFilterComponentSchema
  {:filter-spec filters/FilterSchema
   :component-spec {:id s/Keyword
                    :type (s/eq :external)
                    :label s/Str
                    (s/optional-key :visible) s/Bool
                    (s/optional-key :skip-label) s/Bool
                    (s/optional-key :formatter) (s/pred fn?)
                    :default-text s/Str
                    :set-filter-for-url (s/pred fn?)}})

;; a <select> filter
(defcomponentk external-filter-component
  [[:data filter-spec [:component-spec id set-filter-for-url :as component-spec] :as data] :- ExternalFilterComponentSchema
   [:opts component-filter-rq-chan] :- {:component-filter-rq-chan ManyToManyChannel}
   owner]

  (did-mount
   [_]
   (go
     (while (when-let [[component-id rq] (<! component-filter-rq-chan)]
              (let [{:keys [filter-spec component-spec]} (om/get-props owner)]
                (.log js/console (clj->js ["EXTERNAL-FILTER-RQ" id rq]))

                (set-filter-for-url rq
                                    (fn [f d]
                                      (om/update! filter-spec
                                                  (filters/update-filter-component @filter-spec component-id f d rq))))
                true)))))

  (render
   [_]
   (render* data)))
