(ns clustermap.components.search
  (:require-macros
   [plumbing.core :refer [defnk]]
   [cljs.core.async.macros :refer [go go-loop]])
  (:require [om.core :as om :include-macros true]
            [om-tools.core :refer-macros [defcomponentk]]
            [schema.core :as s]
            [sablono.core :as html :refer-macros [html]]
            [cljs.core.async :as async :refer [<!]]))

(def ESCAPE_KEY 27)
(def ENTER_KEY 13)
(def LEFT_ARROW 37)
(def UP_ARROW 38)
(def RIGHT_ARROW 39)
(def DOWN_ARROW 40)

(defn make-sequential
  [x]
  (cond (nil? x) nil
        (sequential? x) x
        :else [x]))

(defn search-for
  [state search-cursor search-fn query]
  (om/update! search-cursor :query query)
  (when (= 0 (count query))
    (swap! state assoc :active nil))
  (go
    (let [r (<! (if (> (count query) 1)
                  (search-fn query)
                  (search-fn)))]
      (om/update! search-cursor :results r))))

(defn partition-around
  [val seq]
  (let [match-idx (first (keep-indexed (fn [idx v] (when (= val v) idx)) seq))]
    (if match-idx
      [(take match-idx seq) (rest (drop match-idx seq))]
      [nil seq])))

(defnk select-next-result
  [[:data [:search query results :as search]]
   state]
  (let [[before after] (partition-around (:active @state) results)]
    (swap! state assoc :active (or (first after) (first before)))))

(defnk select-prev-result
  [[:data [:search query results :as search]]
   state]
  (let [[before after] (partition-around (:active @state) results)]
    (swap! state assoc :active (or (last before) (last after)))))

(defnk choose-result
  [[:data [:search [:controls search-fn render-fn click-fn]
           query results :as search]]
   state]
  (let [cr (:active @state)]
    (when-let [r (some #{cr} results)]
      (js/console.log (clj->js r))
      (when click-fn
        (swap! state assoc :open false)
        (click-fn r)))))

(defnk key-down [[:data [:search [:controls search-fn render-fn click-fn]
                         query results :as search]]
                 state
                 owner
                 e
                 :as m]
  (condp == (.-keyCode e)
    ESCAPE_KEY (search-for state search search-fn nil)
    ENTER_KEY (choose-result m)
    UP_ARROW (select-prev-result m)
    DOWN_ARROW (select-next-result m)
    (swap! state assoc :open true)))

(defnk render*
  [[:data [:search [:controls search-fn col-headers render-fn click-fn]
           query
           results
           {placeholder "Company search"}
           :as search]]
   state
   owner
   :as m]

  (html
   [:div {:on-focus #(swap! state assoc :open true)
          :on-blur #(swap! state assoc :open false)}
    [:input {:type "text"
             :value query
             :placeholder placeholder
             :on-key-down (fn [e]
                            (key-down (merge m {:e e}))
                            nil)
             ;; TODO: back to on-change in react 16 which fixes IE 11 bug
             :on-input (fn [e] (let [v (.. e -target -value)]
                                 (search-for state search search-fn v)))}]

    (when (and (:open @state)
               (not-empty results))
      [:div.search-results
       (into [:ul]
             (concat (when (not-empty col-headers)
                       [[:li.header (for [h col-headers] [:div h])]])
                     (for [r results]
                       [:li
                        (into [:a {:class (when (= (:active @state) r) "active")
                                   :href "#"
                                   :on-mouse-down (fn [e]
                                                    (.preventDefault e)
                                                    (when click-fn
                                                      (swap! state assoc :open false)
                                                      (click-fn r)))}]
                              (when render-fn (make-sequential (render-fn r))))])))])]))

(def SearchComponentSchema
  {:controls {:search-fn s/Any ;; function of query string, returns list of results
              :col-headers (s/maybe [s/Str]) ;; row of column headers for results
              :render-fn s/Any ;; function of result record to render
              :click-fn s/Any} ;; function of result record clicked
   :query (s/maybe s/Str)
   (s/optional-key :placeholder) s/Str
   :results (s/maybe [{s/Keyword s/Any}])})

(defcomponentk search-component
  [[:data [:search [:controls search-fn] :as search] :- SearchComponentSchema]
   state
   owner
   :as m]

  (will-mount [_]
    (when-let [search-chan (om/get-shared owner :search-chan)]
      (go-loop []
        (when-some [op (<! search-chan)]
          (case op
            :clear (search-for state search search-fn nil)
            (js/console.debug "Unknown op " op))
          (recur)))))
  (render
      [_]
    (render* m)))
