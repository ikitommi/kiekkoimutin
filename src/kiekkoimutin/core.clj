(ns kiekkoimutin.core
  (:require [clj-http.client :as http]
            [net.cgrand.enlive-html :as enlive]
            [clojure.core.reducers :as r]
            [clojure.data.csv :as csv]
            [clojure.string :as s]
            [clojure.java.io :as io]))

(defn fetch-url [url] (enlive/html-resource (java.net.URL. url)))
(defn text [xml] (-> xml :content first))
(defn href [xml] (-> xml :attrs :href))
(defn select1 [xml selector] (first (enlive/select xml selector)))

(defn find-products [xml] (enlive/select xml [:#main :table :> :tr]))

(defn ->tags [url] (zipmap [:ryhma :kategoria] (rest (re-matches #".*kategoria-(.*)=(.*)" url))))

(defn ->product [xml]
  {:kuva     (-> (select1 xml [:.tuote-kuva :a]) href (s/replace #"(.*/uploads/)" ""))
   :koko     (-> (select1 xml [:.tuote-koko]) text)
   :nimi     (-> (select1 xml [:.tuote-nimi]) text)
   :kunto    (-> (select1 xml [:.tuote-kunto]) text)
   :hinta    (-> (select1 xml [:.tuote-hinta]) text)
   :tuotenro (-> (select1 xml [:.tuote-tuotenro]) text)})

(defn ->products [acc url]
  (let [tags (->tags url)
        xml  (fetch-url url)
        pxml (find-products xml)
        products (->> pxml
                   (map ->product)
                   (map (partial merge tags)))]
    (println "consumed " url)
    (into acc products)))

(def urls ["http://kiekkobussi.com/?kategoria-luistimet=kaikki-luistimet"
           "http://kiekkobussi.com/?kategoria-mailat=leftin-mailat"
           "http://kiekkobussi.com/?kategoria-mailat=rightin-mailat"
           "http://kiekkobussi.com/?kategoria-mailat=varret"
           "http://kiekkobussi.com/?kategoria-mailat=kaantovarret"
           "http://kiekkobussi.com/?kategoria-mailat=leftin-lavat"
           "http://kiekkobussi.com/?kategoria-mailat=rightin-lavat"
           "http://kiekkobussi.com/?kategoria-mailat=mv-mailat"
           "http://kiekkobussi.com/?kategoria-pelaaja=hartiasuojat"
           "http://kiekkobussi.com/?kategoria-pelaaja=housut"
           "http://kiekkobussi.com/?kategoria-pelaaja=kyynarsuojat"
           "http://kiekkobussi.com/?kategoria-pelaaja=hanskat"
           "http://kiekkobussi.com/?kategoria-pelaaja=polvisuojat"
           "http://kiekkobussi.com/?kategoria-pelaaja=kyparat"
           "http://kiekkobussi.com/?kategoria-pelaaja=alasuojat"
           "http://kiekkobussi.com/?kategoria-maalivahti=rintapanssarit"
           "http://kiekkobussi.com/?kategoria-maalivahti=mv-housut"
           "http://kiekkobussi.com/?kategoria-maalivahti=rapylat"
           "http://kiekkobussi.com/?kategoria-maalivahti=kilvet"
           "http://kiekkobussi.com/?kategoria-maalivahti=patjat"
           "http://kiekkobussi.com/?kategoria-maalivahti=maskit"
           "http://kiekkobussi.com/?kategoria-maalivahti=luistimet"
           "http://kiekkobussi.com/?kategoria-maalivahti=muut"
           "http://kiekkobussi.com/?kategoria-muut=pelipaidat"
           "http://kiekkobussi.com/?kategoria-muut=harjoituspaidat"
           "http://kiekkobussi.com/?kategoria-muut=muu-urheilu"])

(defonce all (reduce ->products [] urls))

(let [columns (->> all first keys (map name))
      values  (map vals all)]
  (with-open [out-file (io/writer "kiekkobussi.csv")]
    (csv/write-csv out-file
      (into [columns] values)
      :separator \;)))

;; all files exist
(assert (->> all
          (map :kuva)
          (map io/file)
          (map #(.exists %))
          (every? true?)))