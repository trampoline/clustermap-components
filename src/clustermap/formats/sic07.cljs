(ns clustermap.formats.sic07)

(defn describe-sic-section
  [code]
  (let [code (js/parseInt code)]
    (cond
     (<= 1110 code 3220) "A : Agriculture, Forestry and Fishing"
     (<= 5101 code 9900) "B : Mining and Quarrying"
     (<= 10110 code 33200) "C : Manufacturing"
     (<= 35110 code 35300) "D : Electricity, gas, steam and air conditioning supply"
     (<= 36000 code 39000) "E : Water supply, sewerage, waste management and remediation activities"
     (<= 41100 code 43999) "F : Construction"
     (<= 45111 code 47990) "G : Wholesale and retail trade; repair of motor vehicles and motorcycles"
     (<= 49100 code 53202) "H : Transportation and storage"
     (<= 55100 code 56302) "I : Accommodation and food service activities"
     (<= 58110 code 63990) "J : Information and communication"
     (<= 64110 code 66300) "K : Financial and insurance activities"
     (<= 68100 code 68320) "L : Real estate activities"
     (<= 69101 code 75000) "M : Professional, scientific and technical activities"
     (<= 77110 code 82990) "N : Administrative and support service activities"
     (<= 84110 code 84300) "O : Public administration and defence; compulsory social security"
     (<= 85100 code 85600) "P : Education"
     (<= 86101 code 88990) "Q : Human health and social work activities"
     (<= 90010 code 93290) "R : Arts, entertainment and recreation"
     (<= 94110 code 96090) "S : Other service activities"
     (<= 97000 code 98200) "T : Activities of households as employers"
     (<= 99000 code 99999) "U : Activities of extraterritorial organisations and bodies")
    ))
