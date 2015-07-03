(ns clustermap.components.filter
  (:require-macros
   [cljs.core.async.macros :refer [go]])
  (:require [clojure.string :as str]
            [clojure.set :refer [difference intersection union]]
            [om.core :as om :include-macros true]
            [om-tools.core :refer-macros [defcomponentk]]
            [plumbing.core :refer-macros [defnk]]
            [schema.core :as s]
            [sablono.core :as html :refer-macros [html]]
            [cljs.core.async :refer [chan sub unsub <! put! close!]]
            [clustermap.filters :as filters]
            [clustermap.formats.url :as url]
            [clustermap.components.filters.select-filter :as select-filter]
            [clustermap.components.filters.tag-filter :as tag-filter]
            [clustermap.components.filters.tag-checkboxes-filter :as tag-checkboxes-filter]
            [clustermap.components.filters.checkboxes-filter :as checkboxes-filter]
            [clustermap.components.filters.external-filter :as external-filter]))

(defn ^:private parse-filter-url
  "delegate to filter-component type parsers for each fragment param
   which matches a component id"
  [{:keys [component-specs components component-descrs] :as filter-spec}]

  )

(defn ^:private encode-filter-url
  "delegate to filter-component type encoders for each present
   filter component"
  [{:keys [component-specs components component-descrs] :as filter-spec}]

  )

(defn ^:private render-filter-control
  [filter-spec
   {:keys [type] :as component-spec}
   component-filter-rq-chan]

  (condp = type

    :select (om/build select-filter/select-filter-component
                      {:component-spec component-spec
                       :filter-spec filter-spec}
                      {:opts {:component-filter-rq-chan component-filter-rq-chan}})

    :tag (om/build tag-filter/tag-filter-component
                   {:component-spec component-spec
                    :filter-spec filter-spec}
                   {:opts {:component-filter-rq-chan component-filter-rq-chan}})

    :tag-checkboxes (om/build tag-checkboxes-filter/tag-checkboxes-filter-component
                              {:component-spec component-spec
                               :filter-spec filter-spec}
                              {:opts {:component-filter-rq-chan component-filter-rq-chan}})

    :checkboxes (om/build checkboxes-filter/checkboxes-filter-component
                          {:component-spec component-spec
                           :filter-spec filter-spec}
                          {:opts {:component-filter-rq-chan component-filter-rq-chan}})

    :external (om/build external-filter/external-filter-component
                        {:component-spec component-spec
                         :filter-spec filter-spec}
                        {:opts {:component-filter-rq-chan component-filter-rq-chan}})))

(defn ^:private render-filter-row
  [filter-spec
   {:keys [id label skip-label visible] :as component-spec}
   component-filter-rq-chan]

  [:div.filter-group {:class (str/join " " [(name id) (when visible "active")])}
   [:div.filter-header
    [:i.icon-toggle-filter {:on-click (fn [e] (om/update! component-spec [:visible] (not visible)))}]
    (when-not skip-label [:span label])]
   (when visible
     (render-filter-control filter-spec component-spec component-filter-rq-chan))])

(defn update-component-filter-rq-chans
  [component-filter-rq-chans component-ids]
  (let [existing-ids (-> component-filter-rq-chans keys set)
        required-ids (-> component-ids set)
        new-ids (difference required-ids existing-ids)
        dead-ids (difference existing-ids required-ids)
        changing-ids (union new-ids dead-ids)]

    (->> changing-ids
         (reduce (fn [chs id]
                   (cond

                     (new-ids id)
                     (let [ch (chan)]
                       (assoc chs id ch))

                     (dead-ids id)
                     (let [ch (get chs id)]
                       (dissoc chs id)
                       (close! ch))))
                 component-filter-rq-chans))))

(defnk ^:private render*
  [[:filter-spec component-specs components :as filter-spec]
   component-filter-rq-chans
   owner]
  (let [component-ids (map :id component-specs)
        new-component-filter-rq-chans (update-component-filter-rq-chans component-filter-rq-chans
                                                                        component-ids)]
    (.log js/console (clj->js ["COMPONENT-SPECS" component-specs]))

    (when (not= new-component-filter-rq-chans component-filter-rq-chans)
      (om/set-state! owner
                     :component-filter-rq-chans
                     new-component-filter-rq-chans))

    (html
     [:div.grdh.grdw.grda
      (for [{:keys [id] :as component-spec} component-specs]
        (let [component-filter-rq-chan ()]
          (render-filter-row filter-spec component-spec (get new-component-filter-rq-chans id))))])))

(def FilterComponentSchema
  {:filter-spec filters/FilterSchema})

(defcomponentk filter-component
  [[:data [:filter-spec id component-specs components url-components :as filter-spec]] :- FilterComponentSchema
   [:shared filter-rq-pub history]
   owner]

  (did-mount
   [_]
   (let [filter-rq-sub (chan)]
     (sub filter-rq-pub id filter-rq-sub)
     (om/set-state! owner :filter-rq-sub filter-rq-sub)

     (go
       (while (when-let [[filter-id filter-rq] (<! filter-rq-sub)]
                (.log js/console (clj->js ["FILTER-RQ" filter-id filter-rq]))
                (let [component-filter-rq-chans (om/get-state owner :component-filter-rq-chans)
                      component-ids (keys filter-rq)]

                  (doseq [component-id component-ids]
                    (.log js/console (clj->js ["FILTER" filter-id component-id (get filter-rq component-id)]))
                    (when-let [component-filter-rq-chan (get component-filter-rq-chans component-id)]
                      (put! component-filter-rq-chan [component-id (get filter-rq component-id)]))))
                true)))))

  (will-unmount
   [_]
   (let [filter-rq-sub (om/get-state owner :filter-rq-sub)
         chs (om/get-state owner :component-filter-rq-chans)]
     (unsub filter-rq-pub id filter-rq-sub)
     (close! filter-rq-sub)

     (doseq [[component-id ch] chs]
       (close! ch))
     (om/set-state! owner :component-filter-rq-chans nil)))

  (render
   [_]
   (let [component-filter-rq-chans (om/get-state owner :component-filter-rq-chans)]
     (render* {:filter-spec filter-spec
               :component-filter-rq-chans component-filter-rq-chans
               :owner owner})))

  (will-update [_
                {{{:as next-url-components} :url-components
                  :as next-filter-spec} :filter-spec
                 :as next-props}
                _]

               (when (and history
                          (not= next-url-components url-components))

                 (let [token (.getToken history)
                       encoded-filter (filters/filter-url-param-value next-url-components)
                       new-token (url/add-param-to-token token id encoded-filter)]
                   (.log js/console (clj->js ["TOKEN-CHANGE" token new-token]))
                   (.setToken history new-token)
                   ))))
