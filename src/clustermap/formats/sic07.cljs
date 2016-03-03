(ns clustermap.formats.sic07)

(defn describe-sic-section
  [code]
  (let [code (js/parseInt code)]
    (cond
     (<= 1110 code 3220) "Agriculture, Forestry and Fishing"
     (<= 5101 code 9900) "Mining and Quarrying"
     (<= 10110 code 33200) "Manufacturing"
     (<= 35110 code 35300) "Electricity, gas, steam and air conditioning supply"
     (<= 36000 code 39000) "Water supply, sewerage, waste management and remediation activities"
     (<= 41100 code 43999) "Construction"
     (<= 45111 code 47990) "Wholesale and retail trade; repair of motor vehicles and motorcycles"
     (<= 49100 code 53202) "Transportation and storage"
     (<= 55100 code 56302) "Accommodation and food service activities"
     (<= 58110 code 63990) "Information and communication"
     (<= 64110 code 66300) "Financial and insurance activities"
     (<= 68100 code 68320) "Real estate activities"
     (<= 69101 code 75000) "Professional, scientific and technical activities"
     (<= 77110 code 82990) "Administrative and support service activities"
     (<= 84110 code 84300) "Public administration and defence; compulsory social security"
     (<= 85100 code 85600) "Education"
     (<= 86101 code 88990) "Human health and social work activities"
     (<= 90010 code 93290) "Arts, entertainment and recreation"
     (<= 94110 code 96090) "Other service activities"
     (<= 97000 code 98200) "Activities of households as employers"
     (<= 99000 code 99999) "Activities of extraterritorial organisations and bodies")
    ))

;; Functions are used instead of plain defs of data to aid DCE in cljs

(defn custom-sic-section-filters
  "Use in EUStartup and others, pass to checkbox filters or use in
  filters histogram after transformation"
  []
  [;; {:value "any" :label "Any" :filter nil :omit-description true}
   #_{:value "any" :label "Has SIC"
      :filter {:range {"!sic07" {:gte ""}}}}
   {:value "sectionA" :label "Agriculture, forestry & fishing"
    :filter {:range {"!sic07" {:gte "01110" :lt "05101"}}}}
   {:value "sectionB" :label "Mining & quarrying"
    :filter {:range {"!sic07" {:gte "05101" :lt "10110"}}}}
   {:value "sectionC" :label "Manufacturing"
    :filter {:range {"!sic07" {:gte "10110" :lt "35110"}}}}
   {:value "sectionD" :label "Electricity, gas & air conditioning"
    :filter {:range {"!sic07" {:gte "35110" :lt "36000"}}}}
   {:value "sectionE" :label "Water, sewage & waste"
    :filter {:range {"!sic07" {:gte "36000" :lt "41100"}}}}
   {:value "sectionF" :label "Construction"
    :filter {:range {"!sic07" {:gte "41100" :lt "45111"}}}}
   {:value "sectionG" :label "Wholesale, retail & automative repair	"
    :filter {:range {"!sic07" {:gte "45111" :lt "49100"}}}}
   {:value "sectionH" :label "Transportation and storage"
    :filter {:range {"!sic07" {:gte "49100" :lt "55100"}}}}
   {:value "sectionI" :label "Accommodation, food & drink"
    :filter {:range {"!sic07" {:gte "55100" :lt "58110"}}}}
   #_{:value "sectionJ" :label "Information and communication"
      :filter {:range {"!sic07" {:gte "58110" :lt "64110"}}}}
   {:value "sectionJ1" :label "Publishing & Broadcasting"
    :filter {:range {"!sic07" {:gte "58110" :lt "61100"}}}}
   {:value "sectionJ2" :label "Telecommunications"
    :filter {:range {"!sic07" {:gte "61100" :lt "62000"}}}}
   {:value "sectionJ3" :label "IT: Software development"
    :filter {:range {"!sic07" {:gte "62000" :lt "63120"}}}}
   {:value "sectionJ4" :label "IT: Web & information services"
    :filter {:range {"!sic07" {:gte "63120" :lt "64000"}}}}
   {:value "sectionK" :label "Financial & insurance"
    :filter {:range {"!sic07" {:gte "64110" :lt "68100" }}}}
   {:value "sectionL" :label "Real estate"
    :filter {:range {"!sic07" {:gte "68100" :lt "69101"}}}}
   {:value "sectionM" :label "Scientific & technical services"
    :filter {:range {"!sic07" {:gte "69101" :lt "77110"}}}}
   {:value "sectionN" :label "Administrative & support services"
    :filter {:range {"!sic07" {:gte "77110" :lt "84110"}}}}
   {:value "sectionO" :label "Government & defence"
    :filter {:range {"!sic07" {:gte "84110" :lt "85100"}}}}
   {:value "sectionP" :label "Education"
    :filter {:range {"!sic07" {:gte "85100" :lt "86101"}}}}
   {:value "sectionQ" :label "Health & social work"
    :filter {:range {"!sic07" {:gte "86101" :lt "90010"}}}}
   {:value "sectionR" :label "Arts & entertainment"
    :filter {:range {"!sic07" {:gte "90010" :lt "94110"}}}}
   {:value "sectionS" :label "Other services"
    :filter {:range {"!sic07" {:gte "94110" :lt "97000"}}}}
   {:value "sectionT" :label "Household activities"
    :filter {:range {"!sic07" {:gte "97000" :lt "99000" }}}}
   {:value "sectionU" :label "International organisations"
    :filter {:range {"!sic07" {:gte "99000"}}}}
   ])

(def options->tags-xform
  "Transducer to turn options formats to tag-data format, for charts"
  (comp
   (map #(select-keys % [:value :label]))
   (map #(clojure.set/rename-keys % {:value :tag :label :description}))))


(defn custom-sic-section-filters-tags []
  (into [] options->tags-xform (custom-sic-section-filters)))

(defn mk-l3-nontoxic->description []
  {"digi_tech" "Digital Technologies"
   "lifesci_health" "Life Sciences & Healthcare"
   "pub_broad" "Publishing & Broadcasting"
   "other_scitechmanf" "Other scientific/technological manufacture"
   "other_scitech_serv" "Other scientific/technological services"})

(defn l3-nontoxic-tags []
  (into [] (map (fn [[v l]] {:value v :label l}))
        (mk-l3-nontoxic->description)))

(defn mk-l4-nontoxic->description []
  {"aud_vis_broad" "Audio-visual broadcasting"
   "arch_eng_surv" "Architecture engineering & quantity surveying"
   "manf_rep_air_space" "Manufacture and repair of air and spacecraft"
   "pub_mktg_graph_des" "Publishing Marketing & Graphic Design"
   "dig_comp_serv" "Digital & Computer services"
   "comp_elec_manf" "Computer & Electronic manufacture"
   "aero_transp" "Aerospace transport"
   "telecomm_serv" "Telecommunication services"
   "humanitities_randd" "Scientific research & development"
   "biotech_randd" "Biotechnology research and development"
   "healthcare_serv" "Healthcare services including veterinary"
   "chem_manf" "Chemical Product manufacturing"
   "non_elec_mach_manf" "Non-electrical machinery manufacture"
   "pharm_manf" "Pharmaceutical manufacture"
   "def_tech" "Defence technologies"
   "precision_eng" "Precision engineering"
   "med_opt_equip_manf" "Medical & optical equipment manufacture"
   "higher_ed" "Higher education"
   "elec_mach_manf" "Electrical Machinery manufacture"
   "auto_manf" "Automotive manufacture"
   "comm_equip_manf" "Communication Equipment manufacture"})


(defn l4-nontoxic-tags []
  (into [] (map (fn [[v l]] {:value v :label l}))
        (mk-l4-nontoxic->description)))
