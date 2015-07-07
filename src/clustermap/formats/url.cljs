(ns clustermap.formats.url
  (:require [clojure.string :as str]))

(defn parse-token
  "parse a token into [path params] where
     - path : a path of the form /a/b/c
     - params : a map of params"
  [t]
  (let [[path param-str] (str/split t #"\?" 2)
        param-value-strs (str/split param-str #"&")
        param-values (map #(str/split % #"=") param-value-strs)
        keyw-values (->> param-values
                         (map #(assoc % 0 (keyword (get % 0))))
                         (map #(assoc % 1 (or (some-> % (get 1) js/decodeURIComponent)
                                              true)))) ;; map empty values to true
        params (into {} keyw-values)]
    [path params]))

(defn set-token
  "encode a path and params into a token"
  [path params]
  (let [params-str (some->> params
                            (filter (fn [[k v]] v))
                            (map (fn [[k v]] (if (= true v) [(name k)] [(name k)
                                                                        (js/encodeURIComponent v)])))
                            (map #(str/join "=" %))
                            (str/join "&")
                            not-empty)]
    (str/join "?" (filter identity [path params-str]))))

(defn remove-param-from-token
  "remove a param from the token"
  [t param]
  (let [[path params] (parse-token t)]
    (set-token path (dissoc params param))))

(defn add-param-to-token
  "add a param to the token"
  [t param value]
  (let [[path params] (parse-token t)]
    (set-token path (assoc params param value))))

(defn change-token-path
  "change the token path, preserving params"
  [t new-path]
  (let [[path params] (parse-token t)]
    (set-token new-path params)))

(defn prefix-url
  "prefix a url with http:// if no protocol is given"
  [url]
  (if (re-find #"^.*?://" url)
    url
    (str "http://" url)))

(defn strip-trailing-slash
  [s]
  (if (re-find #".*/+$" s)
    (last (re-matches #"(.*[^/]+)/+$" s))
    s))

(defn clean-http-url
  "remove trailing / from a url, and prefix with http:// if no protocol is given"
  [url]
  (if (and url
           (> (count (str/trim url)) 0))
    (-> url
        strip-trailing-slash
        prefix-url
        str/lower-case)
    nil))
