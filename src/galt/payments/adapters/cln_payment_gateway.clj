(ns galt.payments.adapters.cln-payment-gateway
  (:require
    [galt.payments.domain.payment-gateway :refer [PaymentGateway] :as pg]
    [galt.core.adapters.db-result-transformations :refer [transform-row defaults ->local-date-time]]
    [java-time.api :as jt]
    [org.httpkit.client :as http]
    [galt.core.infrastructure.web.helpers :refer [->json read-json]]
    [camel-snake-kebab.core :as csk]
    [camel-snake-kebab.extras :as cske]
    [clojure.spec.alpha :as s]
    [failjure.core :as f]
    ))

(s/def ::create-invoice (s/keys :req-un [:invoice/amount-msat :invoice/label :invoice/description]
                                :opt-un [:invoice/expiry]))

(def invoice-fields
  {:label defaults
   :payment-hash defaults
   :status defaults
   :expires-at [(first defaults) #(jt/instant (* 1000 %))]
   :created-index defaults
   :description defaults
   :amount-msat defaults
   :bolt-11 defaults
   :amount-received-msat defaults
   :paid-at [(first defaults) #(jt/instant (* 1000 %))]
   :payment-preimage defaults})

(defn convert-to-instant [m key]
  (assoc m key (jt/instant (* 1000 (get m key)))))

(defrecord ClnPaymentGateway [post]
  PaymentGateway
  (create-invoice [_ params]
    {:pre (s/valid? ::create-invoice params)}
    (f/ok-> (post :invoice params)
             (select-keys [:bolt-11 :expires-at :payment-hash :payment-secret :created-index])
             (convert-to-instant ,,, :expires-at)))

  (list-invoices [_]
    (:invoices (post :listinvoices)))

  (invoice-by-hash [_ payment-hash]
    (-> (post :listinvoices {:payment-hash payment-hash})
        :invoices
        first))

  (invoice-by-label [_ label]
    (f/ok->> (post :listinvoices {:label label})
             :invoices
             first
             (transform-row invoice-fields ,,,)))

  (call-method [_ method]
    (pg/call-method _ method {}))

  (call-method [_ method params]
    (post method params)))

(def last-res (atom nil))
(defn post-to-cln-node
  ([node-url rune rpc-method]
   (post-to-cln-node node-url rune rpc-method {}))
  ([node-url rune rpc-method params]
   (let [headers {"Rune" rune
                  "Content-Type" "application/json"}
         endpoint-url (str node-url "/v1/" (name rpc-method))
         json-params (->> params
                          (cske/transform-keys csk/->snake_case_string ,,,)
                          (->json ,,,))
         fail-on-error (fn [response]
                         (reset! last-res response)
                         (if (:error response)
                           (f/fail "Error in communicating with Lightning Node: %s"
                                   (get-in response [:error :cause]))
                           response))]
     (f/ok->>
       (f/try* @(http/post endpoint-url {:headers headers :body json-params}))
       fail-on-error
       :body
       read-json
       (cske/transform-keys csk/->kebab-case-keyword ,,,)
       ))))

(defn new-cln-payment-gateway
  ([post-fn] (ClnPaymentGateway. post-fn))
  ([node-url rune] (ClnPaymentGateway. (partial post-to-cln-node node-url rune))))

(comment
  (require '[galt.main :refer [running-system]])
  (def gw-instance (get-in @running-system [:donut.system/instances :gateways :payment]))
  (pg/list-invoices gw-instance)
  (pg/invoice-by-hash gw-instance  "e6360191dc6762f383cc7805c3756dc5c88003e3a2d2d2253cd4a1fd0d8da0a7")
  (pg/invoice-by-label gw-instance "membership-payment-019954c1-bc96-706d-85b2-4b9b01e197c7-2025092017313603")
  (pg/invoice-by-label gw-instance "membership-payment-019954c1-bc96-706d-85b2-4b9b01e197c7-2025092107294138")
  (pg/create-invoice gw-instance {:amount-msat 100000 :label "pretty-unique3" :description "Hello!"})
  (pg/call-method gw-instance :getinfo {})
  )
