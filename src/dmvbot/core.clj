(ns dmvbot.core
  (:require [cheshire.core :as json]
            [org.httpkit.client :as http]))

(def locations
  {"Queens - College Point" "fb052d6eae67926d8d5449d7317c8528e1e3d02b19441ead85f3150915e2abbe"
   "Queens - Jamaca"        "d0099bebf8e51979019b5e45b2c7dfeab9830f0213a4da0cfd569ec145eb07a9"
   "Queens college"         "887df9bcd65c813a07ac3ae5e818d4faec1aa02bb467ea5cb2e1e2e878bfa32a"
   "lower manhattan"        "8bcc5ca5cad16666ba6f5dd43d15241e172bd511f7e8d6f2e1caa2380b66776a"
   "midtown manhattan"      "0ea16b72515a86e0cc00d186b249b0ebc61ed10b5289394af9b0cab8de5dafda"
   "brooklyn - atlantic av" "c92d2048b00326a0d9452e478db504ce41ec8f67f8e008034295cbf85cf902df"})

(def service-id "10226f4de0f460aa67bb735db97f9eb434b8ac2a144e40a20ff1e1848ffbeae7")

(def site-url "https://nysdmvqw.us.qmatic.cloud/naoa/index.jsp")

;; TODO: fill this stuff in?
(def me-json
  {:appointmentReference ""
   :customer             {:dateOfBirth "" ; enter your date of birth in year-month-day ie. 2000-12-31
                          :email       "" ; enter your email
                          :externalId  "" ;leave blank
                          :firstName   "" ;first name
                          :lastName    "" ;first name
                          :phone       "" ; enter phone in ###-###-#### format
                          }
   :languageCode         "en-US"
   :notes                ""
   :notificationType     ""
   :title                ""})

(defn fetch-dates [id]
  (let [url (str "https://nysdmvqw.us.qmatic.cloud/qwebbook/rest/schedule/branches/"
                 id "/services/" service-id "/dates?_=1608422256259")]
    (-> url (http/get {:accept :json}) deref :body (json/parse-string true))))

(defn fetch-times [id date]
  (let [url (str "https://nysdmvqw.us.qmatic.cloud/qwebbook/rest/schedule/branches/"
                 id "/services/" service-id "/dates/" date "/times?_=1608422256273")]
    (-> url (http/get {:accept :json}) deref :body (json/parse-string true))))

(defn reserve! [id date time]
  (let [url (str "https://nysdmvqw.us.qmatic.cloud/qwebbook/rest/schedule/branches/"
                 id "/services/" service-id "/dates/" date "/times/" time "/reserve")
        body {:appointment {:customers []
                            :resources []}}
        req {:method :post
             :accept :json
             :body (json/generate-string body)
             :headers {"Content-Type" "application/json"}}]
    (-> req http/request deref :body (json/parse-string true))))

(defn confirm! [res-id]
  (let [url (str "https://nysdmvqw.us.qmatic.cloud/qwebbook/rest/schedule/appointments/"
                 res-id "/confirm")
        req {:method :post
             :accept :json
             :body (json/generate-string me-json)}]
    (-> req http/request deref :body (json/parse-string true))))

(defn check-location [[loc-name loc-id]]
  (let [dates (fetch-dates loc-id)]
    (when (seq dates)
      (for [date (map :date dates)
            :let [times (fetch-times loc-id date)]
            :when (seq times)
            :let [time (:time (last times))
                  res (reserve! loc-id date time)
                  res-id (:publicId res)]]
        (do (println "Found reservation at " loc-name "- Confirming...")
            (confirm! res-id))))))

(defn check-all []
  (->> locations
       (map #(future (check-location %)))
       doall
       (keep deref)))

(defn -main []
  (while true
    (println (check-all))))

(comment
  (check-all)
  )
