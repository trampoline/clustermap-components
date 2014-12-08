(ns clustermap.components.filter
  (:require [om.core :as om :include-macros true]
            [sablono.core :as html :refer-macros [html]]
            [clustermap.filters :as filters]))

(defn render
  [{bounds :bounds
    filter-spec :filter-spec}
   owner
   state
   & [{:keys [get-cached-boundaryline-fn]}]]
  (html
   [:div.filter-component

    [:div.tbl
     ;; [:div.tbl-row
     ;;  [:div.tbl-cell "filter by view"]
     ;;  [:div.tbl-cell [:input {:type "checkbox" :name "filter-by-view" :value "true"
     ;;                          :onChange (fn [e] (let [val (-> e .-target .-checked)]
     ;;                                              (om/update! filter-spec [:filter-by-view] val)))}]]]
     [:div.tbl-row
      [:div.tbl-cell "age"]
      [:div.tbl-cell [:select {:onChange (fn [e]
                                           (let [val (-> e .-target .-value)]
                                             (.log js/console val)
                                             (om/update! filter-spec [:components :age]
                                                         (condp = val
                                                           "new" {:range {"!formation_date" {:gte "2009-01-01"}}}
                                                           "old" {:range {"!formation_date" {:lt "2009-01-01"}}}
                                                           nil))))}
                      [:option {:value ""} "any"]
                      [:option {:value "new"} "< 5 years old"]
                      [:option {:value "old"} ">= 5 years old"]]]]

     ;; [:div.tbl-row
     ;;  [:div.tbl-cell "group ?"]
     ;;  [:div.tbl-cell [:select {:onChange (fn [e]
     ;;                                       (let [val (-> e .-target .-value)]
     ;;                                         (.log js/console val)
     ;;                                         (om/update! filter-spec [:components :group]
     ;;                                                     (condp = val
     ;;                                                       "group" {:term {"!is_group" true}}
     ;;                                                       "notgroup" {:term {"!is_group" false}}
     ;;                                                       nil))))}
     ;;                  [:option {:value ""} "any"]
     ;;                  [:option {:value "group"} "group"]
     ;;                  [:option {:value "notgroup"} "not group"]]]]

     [:div.tbl-row
      [:div.tbl-cell "turnover"]
      [:div.tbl-cell [:select {:onChange (fn [e]
                                           (let [val (-> e .-target .-value)]
                                             (.log js/console val)
                                             (om/update! filter-spec [:components :turnover]
                                                         (condp = val
                                                           "low" {:range {"!latest_turnover" {:lt 1000000}}}
                                                           "mid" {:range {"!latest_turnover" {:gte 1000000 :lt 100000000}}}
                                                           "high" {:range {"!latest_turnover" {:gte 100000000 :lt 1000000000}}}
                                                           "bn" {:range {"!latest_turnover" {:gte 1000000000}}}
                                                           nil))))}
                      [:option {:value ""} "any"]
                      [:option {:value "low"} "< £1m"]
                      [:option {:value "mid"} ">= £1m, < £100m"]
                      [:option {:value "high"} ">= £100m, < £1bn"]
                      [:option {:value "bn"} ">= £1bn"]]]]

     ;; [:div.tbl-row
     ;;  [:div.tbl-cell "Sector"]
     ;;  [:div.tbl-cell
     ;;   [:select {:style {:width "100%"}
     ;;             :onChange (fn [e]
     ;;                         (let [val (-> e .-target .-value)]
     ;;                           (.log js/console (-> e .-target .-value))
     ;;                           (om/update! filter-spec [:components :sic]
     ;;                                       (condp = val
     ;;                                         "A" {:range {"!sic07" {:gte "01110" :lte "03220"}}}
     ;;                                         "B" {:range {"!sic07" {:gte "05101" :lte "09900"}}}
     ;;                                         "C" {:range {"!sic07" {:gte "10110" :lte "33200"}}}
     ;;                                         "D" {:range {"!sic07" {:gte "35110" :lte "35300"}}}
     ;;                                         "E" {:range {"!sic07" {:gte "36000" :lte "39000"}}}
     ;;                                         "F" {:range {"!sic07" {:gte "41100" :lte "43999"}}}
     ;;                                         "G" {:range {"!sic07" {:gte "45111" :lte "47990"}}}
     ;;                                         "H" {:range {"!sic07" {:gte "49100" :lte "53202"}}}
     ;;                                         "I" {:range {"!sic07" {:gte "55100" :lte "56302"}}}
     ;;                                         "J" {:range {"!sic07" {:gte "58110" :lte "63990"}}}
     ;;                                         "K" {:range {"!sic07" {:gte "64110" :lte "66300"}}}
     ;;                                         "L" {:range {"!sic07" {:gte "68100" :lte "68320"}}}
     ;;                                         "M" {:range {"!sic07" {:gte "69101" :lte "75000"}}}
     ;;                                         "N" {:range {"!sic07" {:gte "77110" :lte "82990"}}}
     ;;                                         "O" {:range {"!sic07" {:gte "84110" :lte "84300"}}}
     ;;                                         "P" {:range {"!sic07" {:gte "85100" :lte "85600"}}}
     ;;                                         "Q" {:range {"!sic07" {:gte "86101" :lte "88990"}}}
     ;;                                         "R" {:range {"!sic07" {:gte "90010" :lte "93290"}}}
     ;;                                         "S" {:range {"!sic07" {:gte "94110" :lte "96090"}}}
     ;;                                         "T" {:range {"!sic07" {:gte "97000" :lte "98200"}}}
     ;;                                         "U" {:range {"!sic07" {:gte "99000" :lte "99999"}}}
     ;;                                         nil))))}
     ;;    [:option {:value ""} "all"]
     ;;    [:option {:value "A"} "Agriculture, Forestry and Fishing"]
     ;;    [:option {:value "B"} "Mining and Quarrying"]
     ;;    [:option {:value "C"} "Manufacturing"]
     ;;    [:option {:value "D"} "Electricity, gas, steam and air conditioning supply"]
     ;;    [:option {:value "E"} "Water supply, sewerage, waste management and remediation activities"]
     ;;    [:option {:value "F"} "Construction"]
     ;;    [:option {:value "G"} "Wholesale and retail trade; repair of motor vehicles and motorcycles"]
     ;;    [:option {:value "H"} "Transportation and storage"]
     ;;    [:option {:value "I"} "Accommodation and food service activities"]
     ;;    [:option {:value "J"} "Information and communication"]
     ;;    [:option {:value "K"} "Financial and insurance activities"]
     ;;    [:option {:value "L"} "Real estate activities"]
     ;;    [:option {:value "M"} "Professional, scientific and technical activities"]
     ;;    [:option {:value "N"} "Administrative and support service activities"]
     ;;    [:option {:value "O"} "Public administration and defence; compulsory social security"]
     ;;    [:option {:value "P"} "Education"]
     ;;    [:option {:value "Q"} "Human health and social work activities"]
     ;;    [:option {:value "R"} "Arts, entertainment and recreation"]
     ;;    [:option {:value "S"} "Other service activities"]
     ;;    [:option {:value "T"} "Activities of households as employers"]
     ;;    [:option {:value "U"} "Activities of extraterritorial organisations and bodies"]
     ;;    ]]]
     ]]))



(defn filter-component
  [{{components :components
     base-filters :base-filters
     :as filter-spec} :filter-spec
    :as props}
   owner]

  (reify

    om/IRenderState
    (render-state [_ state]
      (let [{:keys [get-cached-boundaryline-fn]} (om/get-shared owner)]
        (render props owner state {:get-cached-boundaryline-fn get-cached-boundaryline-fn})))

    om/IWillUpdate
    (will-update [_
                  {{next-components :components
                    next-base-filters :base-filters} :filter-spec}
                  next-state]
      (when (or (not= next-components components))

        (om/update! filter-spec [:composed] (filters/compose-filters next-components next-base-filters))))))
