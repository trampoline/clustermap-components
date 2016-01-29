(ns clustermap.regions-test
  (:require  [expectations :refer :all]
             [clustermap.formats.regions :as r]))

(expect #{{:value "nuts_1__UKC" :label "North East"
           :filter {:term {"?boundarylinecolls.nuts_1" {:value "nuts_1__UKC"}}}}
          {:value "nuts_1__UKD" :label "North West"
           :filter {:term {"?boundarylinecolls.nuts_1" {:value "nuts_1__UKD"}}}}
          {:value "nuts_1__UKE" :label "Yorkshire and the Humber"
           :filter {:term {"?boundarylinecolls.nuts_1" {:value "nuts_1__UKE"}}}}
          {:value "nuts_1__UKF" :label "East Midlands"
           :filter {:term {"?boundarylinecolls.nuts_1" {:value "nuts_1__UKF"}}}}
          {:value "nuts_1__UKG" :label "West Midlands"
           :filter {:term {"?boundarylinecolls.nuts_1" {:value "nuts_1__UKG"}}}}
          {:value "nuts_1__UKH" :label "East of England"
           :filter {:term {"?boundarylinecolls.nuts_1" {:value "nuts_1__UKH"}}}}
          {:value "nuts_1__UKI" :label "London"
           :filter {:term {"?boundarylinecolls.nuts_1" {:value "nuts_1__UKI"}}}}
          {:value "nuts_1__UKJ" :label "South East"
           :filter {:term {"?boundarylinecolls.nuts_1" {:value "nuts_1__UKJ"}}}}
          {:value "nuts_1__UKK" :label "South West"
           :filter {:term {"?boundarylinecolls.nuts_1" {:value "nuts_1__UKK"}}}}
          {:value "nuts_1__UKL" :label "Wales"
           :filter {:term {"?boundarylinecolls.nuts_1" {:value "nuts_1__UKL"}}}}
          {:value "nuts_1__UKM" :label "Scotland"
           :filter {:term {"?boundarylinecolls.nuts_1" {:value "nuts_1__UKM"}}}}}

        (set (r/nuts1-uk-filter-options)))
