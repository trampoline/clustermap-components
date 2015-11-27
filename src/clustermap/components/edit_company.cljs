(ns clustermap.components.edit-company
  (:require-macros
   [cljs.core.async.macros :refer [go]])
  (:require [cljs.core.async :refer [<!]]
            [clojure.string :as str]
            [om.core :as om :include-macros true]
            [om-tools.core :refer-macros [defcomponentk]]
            [schema.core :as s :refer-macros [defschema]]
            [schema.coerce :as coerce]
            [sablono.core :as html :refer-macros [html]]
            [clustermap.app :as app]
            [clustermap.util :refer-macros [inspect <?] :refer [display]]
            [clustermap.date-util :as dt]
            [clustermap.api :as api :refer [api-prefix POST]]
            [clustermap.formats.number :as num :refer [div! *! -! +!]]
            [clustermap.formats.money :as money]
            [clustermap.formats.time :as time]))

(def hub->boundaryline-id
  {"London" "nuts_1__UKI"
   "Paris" "nuts_1__FR1"
   "Manchester" "nuts_2__UKD3"
   "Helsinki" "nuts_2__FI1B"
   "Stockholm" "nuts_2__SE11"
   "Brussels" "nuts_1__BE1"
   "Bucuresti" "nuts_2__RO32"
   "Madrid" "nuts_1__ES3"
   "Berlin" "nuts_1__DE3"
   "Munich" "nuts_3__DE212"})

(defn submit-company
  [record]
  (POST (str "/api/" api-prefix "/eustartuphubs/submit-company")
        record :send-error true))


(defschema NEmptyStr
  (s/pred #(not (str/blank? %)) "Non-empty string"))


(defschema Company
  {:name NEmptyStr
   :registry_id NEmptyStr
   :direct_contact_email NEmptyStr
   :boundaryline_id (apply s/enum (vals hub->boundaryline-id))
   (s/optional-key :formation_date) s/Inst
   (s/optional-key :accounts_date) s/Inst
   (s/optional-key :turnover) (s/maybe s/Num)
   (s/optional-key :employment) (s/maybe s/Int)
   (s/optional-key :address) s/Str
   (s/optional-key :postcode) (s/maybe s/Str)
   :country_code NEmptyStr
   (s/optional-key :angellist_url)  (s/maybe s/Str)
   (s/optional-key :crunchbase_url) (s/maybe s/Str)
   (s/optional-key :dealroom_url) (s/maybe s/Str)
   (s/optional-key :description) (s/maybe s/Str)})

(defn validate-fields [record]
  (s/check Company record))

(def parse-company-fields
  (coerce/coercer Company coerce/string-coercion-matcher))

(def fields (for [k (keys Company)] (or (:k k) k)))

(def metadata-transforms
  "Functions to build the state record from the metadata record. A
  simple keyword selects the key, a function applies a transform "
  (merge (zipmap fields fields)
         {:boundaryline_id (fn [record]
                             (->> (:tags record)
                                  (filter #(= "startup_region" (:type %)))
                                  first
                                  :tag))
          :registry_id :natural_id
          :accounts_date #(some-> (:latest_accounts_date %)
                                  dt/goog-date->js-date)
          :formation_date #(some-> (:formation_date %)
                                   dt/goog-date->js-date)
          :turnover :latest_turnover
          :employment :latest_employee_count}))

(defn extract-values [r]
  (into {} (for [[k f] metadata-transforms]
             [k (f r)])))

(defn get-field-value
  [owner ref]
  (let [input (om/get-node owner (name ref))]
    (.-value input)))

(defn handle-change
  "Get the value of the event and set the state to the key.
  Applies f to the value if supplied"
  [e owner key & [f]]
  (let [v (.. e -target -value)]
    (om/set-state! owner [:record key] (if f (f v) v))
    (inspect (om/get-state owner [:record key]) )))


(defn submit-form [record owner e]
  (.preventDefault e)
  (js/console.log (clj->js ["SUBMISSION" record]))
  (let [coerced-data (parse-company-fields record)
        submit-fn (om/get-state owner :submit-fn)]
    (if (instance? schema.utils/ErrorContainer coerced-data)
      (om/set-state! owner :validation (:error (inspect coerced-data)))
      (do
        (om/set-state! owner :validation nil)
        (go
          (try
            (let [res (<? (submit-fn coerced-data))]
              (inspect res)
              (js/console.log (clj->js ["RESPONSE" res]))
              (app/navigate @clustermap.core/app-instance "main"))
            (catch js/Error e
              (om/set-state! owner :error (-> e .-data))
              (inspect e))))))))


(defn render*
  [{:keys [record validation error]} {:keys [] :as controls} owner]
  (html
   [:div.panel-grid-container
    [:div.panel-grid
     [:div.panel-row

      [:div.panel
       [:div.panel-body
        [:form
         {:on-submit (partial submit-form record owner)}

         [:div.company-details
          [:ul

           [:li
            [:h4 "Hub"]
            [:select {:type "text" :ref "boundaryline_id"
                      :required true
                      :value (:boundaryline_id record "")
                      :on-change #(handle-change % owner :boundaryline_id)}
             (for [[hub id] (conj hub->boundaryline-id ["" ""])]
               [:option {:value id} hub])]]
           [:li
            [:h4 "Registry identifier"]
            [:input {:type "text" :ref "registry_id" :value (:registry_id record)
                     :required true
                     :on-change #(handle-change % owner :registry_id)}]]
           [:li
            [:h4 "Name"]
            [:input {:type "text" :ref "name" :value (:name record) :required true
                     :on-change #(handle-change % owner :name) }]]
           [:li
            [:h4 "Formation date"]
            [:input {:type "text" :ref "formation_date" :value (dt/display-date (:formation_date record))
                     :required true}]]
           [:li
            [:h4 "Accounts date"]
            [:input {:type "text" :ref "accounts_date"
                     :value (dt/display-date (:accounts_date record))}]]
           [:li
            [:h4 "Revenue"]
            [:input {:type "number" :step "any" :ref "turnover"
                     :value (:turnover record)
                     :on-change #(handle-change % owner :turnover (fn [v] (if (str/blank? v) nil v))) }]]

           [:li
            [:h4 "Employee Count"]
            [:input {:type "number" :ref "employment"
                     :value (:employment record)
                     :on-change #(handle-change % owner :employment (fn [v] (if (str/blank? v) nil v))) }]]


           [:li
            [:h4 "Address"]
            [:input {:type "text" :ref "address" :value (:address record)
                     :on-change #(handle-change % owner :address) }]]
           [:li
            [:h4 "Postcode"]
            [:input {:type "text" :ref "postcode" :value (:postcode record)
                     :on-change #(handle-change % owner :postcode)}]]
           [:li
            [:h4 "Country"]
            [:input {:type "text" :ref "country_code" :value (:country_code record)
                     :required true
                     :on-change #(handle-change % owner :country_code)}]]
           [:li
            [:h4 "AngelList URL"]
            [:input {:type "url" :ref "angellist_url" :value (:angellist_url record)
                     :on-change #(handle-change % owner :angellist_url)}]]
           [:li
            [:h4 "Crunchbase URL"]
            [:input {:type "url" :ref "crunchbase_url" :value (:crunchbase_url record)
                     :on-change #(handle-change % owner :crunchbase_url)}]]
           [:li
            [:h4 "Dealroom URL"]
            [:input {:type "url" :ref "dealroom_url" :value (:dealroom_url record)
                     :on-change #(handle-change % owner :dealroom_url)}]]
           [:li
            [:h4 "Contact email"]
            [:input {:type "email" :ref "direct_contact_email" :value (:direct_contact_email record)
                     :required true
                     :on-change #(handle-change % owner :direct_contact_email)}]]
           [:li
            [:h4 "Company description"]
            [:textarea {:ref "description" :value (:description record "")
                        :on-change #(handle-change % owner :description)}]]
           [:li
            [:button.btn
             {:on-click (fn [e] (app/navigate @clustermap.core/app-instance "main"))
              :type "button"}
             "Cancel"]
            [:button.btn.btn-primary {:type "submit"} "Save"]
            [:span {:style (display validation)} (str "Invalid fields: " (prn-str validation))]
            [:span {:style (display error)} (str "Submission error: " (prn-str (:status error)))]
            ]]]]]]]]]))

(defn edit-company-component
  [{{record :record
     {index :index
      index-type :index-type
      sort-spec :sort-spec
      size :size
      new-company :new-company
      :as controls} :controls
     :as metadata} :metadata
    filter-spec :filter-spec
    :as props}
   owner
   opts]

  (reify
    om/IDidMount
    (did-mount [_]
      (dt/add-date-picker! nil (om/get-node owner "formation_date") {} dt/default-fmt
                           #(om/set-state! owner [:record :formation_date] %))
      (dt/add-date-picker! nil (om/get-node owner "accounts_date") {} dt/default-fmt
                           #(om/set-state! owner [:record :accounts_date] %))
      (let [{:keys [fetch-metadata-factory submit-company-fn]} (om/get-shared owner)]
        (assert (fn? fetch-metadata-factory))
        (assert (fn? submit-company-fn))
        (om/set-state! owner :fetch-metadata-fn (fetch-metadata-factory))
        (om/set-state! owner :submit-fn submit-company-fn)))

    om/IRenderState
    (render-state [_ state]
      (render* state controls owner))

    om/IWillUpdate
    (will-update [_
                  {{next-record :record
                    {next-index :index
                     next-index-type :index-type
                     next-sort-spec :sort-spec
                     next-size :size
                     next-new-company :new-company
                     :as next-controls} :controls
                    :as next-metadata} :metadata
                   next-filter-spec :filter-spec}
                  {fetch-metadata-fn :fetch-metadata-fn}]
      (when-not (and (om/get-state owner :record)
                     (= record next-record))
        (when-let [r (om/value next-record)]
          (om/set-state! owner :record r)
          (om/set-state! owner :error nil)
          (om/set-state! owner :validation nil)))
      (when (and (not next-new-company)
                 (or (not next-record)
                     (not= next-controls controls)
                     (not= next-filter-spec filter-spec)))

        (go
          (if fetch-metadata-fn
            (when-let [metadata-data (<! (fetch-metadata-fn
                                          next-index
                                          next-index-type
                                          next-filter-spec
                                          next-sort-spec
                                          next-size))]
              (.log js/console (clj->js ["EDIT-COMPANY-DATA" metadata-data]))
              (om/update! metadata [:record] (when-let [r (some-> metadata-data :records first)]
                                               (extract-values r))))
            ))))))


(defn edit-company-render-fn
  [app-instance make-company-selection name record]
  [:a {:href "#"
       :target "_blank"
       :onClick (fn [e]
                  (.preventDefault e)
                  (make-company-selection (:?natural_id record))
                  (app/navigate @app-instance "edit-company"))}
   name])

(defn edit-company-fn
  [app-instance make-company-selection e]
  (.preventDefault e)
  (let [state-atom (app/get-state @app-instance)
        record (get-in @state-atom [:company-info :record])]
    (make-company-selection (:natural_id record))
    (swap! state-atom assoc-in [:edit-company :controls :new-company] false)
    (swap! state-atom assoc-in [:edit-company :record] nil)
    (app/navigate @app-instance "edit-company")))

(defn edit-new-company-fn
  [app-instance]
  (let [state-atom (app/get-state @app-instance)
        record (get-in @state-atom [:company-info :record])]
    (swap! state-atom assoc-in [:edit-company :controls :new-company] true)
    (swap! state-atom assoc-in [:edit-company :record] {})
    (app/navigate @app-instance "edit-company")))
