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
