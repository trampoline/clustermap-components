(ns clustermap.formats.regions
  "Functions and mappings for working with regions.
   Functions are used instead of plain defs of data to aid DCE in cljs")

(defn mk-nuts1-uk->description
  []
  {"nuts_1__UKF" "East Midlands"
   "nuts_1__UKE" "Yorkshire and the Humber"
   "nuts_1__UKJ" "South East"
   "nuts_1__UKD" "North West"
   "nuts_1__UKI" "London"
   "nuts_1__UKG" "West Midlands"
   "nuts_1__UKH" "East of England"
   "nuts_1__UKC" "North East"
   "nuts_1__UKL" "Wales"
   "nuts_1__UKK" "South West"
   "nuts_1__UKM" "Scotland"
   ;; "nuts_1__UKN" "Northern Ireland"
   })

(defn nuts1-uk-filter-options
  "List of nuts 1 UK filters for use in checkbox filter component"
  []
  (into []
        (for [[nuts1 desc] (mk-nuts1-uk->description)]
          {:value nuts1 :label desc
           :filter {:term {"?boundarylinecolls.nuts_1" {:value nuts1}}}})))

(defn mk-london-borough->description []
  {"osbl_district_borough_unitary_region__lewisham_london_boro"
   "Lewisham",
   "osbl_district_borough_unitary_region__harrow_london_boro" "Harrow",
   "osbl_district_borough_unitary_region__kensington_and_chelsea_london_boro"
   "Kensington and Chelsea",
   "osbl_district_borough_unitary_region__barnet_london_boro" "Barnet",
   "osbl_district_borough_unitary_region__newham_london_boro" "Newham",
   "osbl_district_borough_unitary_region__ealing_london_boro" "Ealing",
   "osbl_district_borough_unitary_region__richmond_upon_thames_london_boro"
   "Richmond upon Thames",
   "osbl_district_borough_unitary_region__barking_and_dagenham_london_boro"
   "Barking and Dagenham",
   "osbl_district_borough_unitary_region__greenwich_london_boro"
   "Greenwich",
   "osbl_district_borough_unitary_region__bromley_london_boro" "Bromley",
   "osbl_district_borough_unitary_region__wandsworth_london_boro"
   "Wandsworth",
   "osbl_district_borough_unitary_region__southwark_london_boro"
   "Southwark",
   "osbl_district_borough_unitary_region__tower_hamlets_london_boro"
   "Tower Hamlets",
   "osbl_district_borough_unitary_region__havering_london_boro"
   "Havering",
   "osbl_district_borough_unitary_region__city_and_county_of_the_city_of_london"
   "City and County of the City of London",
   "osbl_district_borough_unitary_region__enfield_london_boro" "Enfield",
   "osbl_district_borough_unitary_region__kingston_upon_thames_london_boro"
   "Kingston upon Thames",
   "osbl_district_borough_unitary_region__merton_london_boro" "Merton",
   "osbl_district_borough_unitary_region__haringey_london_boro"
   "Haringey",
   "osbl_district_borough_unitary_region__bexley_london_boro" "Bexley",
   "osbl_district_borough_unitary_region__hounslow_london_boro"
   "Hounslow",
   "osbl_district_borough_unitary_region__hackney_london_boro" "Hackney",
   "osbl_district_borough_unitary_region__islington_london_boro"
   "Islington",
   "osbl_district_borough_unitary_region__waltham_forest_london_boro"
   "Waltham Forest",
   "osbl_district_borough_unitary_region__hillingdon_london_boro"
   "Hillingdon",
   "osbl_district_borough_unitary_region__redbridge_london_boro"
   "Redbridge",
   "osbl_district_borough_unitary_region__lambeth_london_boro" "Lambeth",
   "osbl_district_borough_unitary_region__sutton_london_boro" "Sutton",
   "osbl_district_borough_unitary_region__brent_london_boro" "Brent",
   "osbl_district_borough_unitary_region__croydon_london_boro" "Croydon",
   "osbl_district_borough_unitary_region__camden_london_boro" "Camden",
   "osbl_district_borough_unitary_region__city_of_westminster_london_boro"
   "Westminster",
   "osbl_district_borough_unitary_region__hammersmith_and_fulham_london_boro"
   "Hammersmith and Fulham"})
