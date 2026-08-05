(ns galt.world-map.world-map-test
  (:require
   [clojure.java.io :as io]
   [clojure.string :as str]
   [clojure.test :refer [deftest is]]
   [galt.core.infrastructure.web.helpers :as wh]
   [galt.members.domain.member-repository :as mr]
   [galt.members.domain.user-repository :as ur]
   [galt.world-map.external.routes :as routes]
   [org.httpkit.server :as hk]
   [reitit.ring :as rr]))

(defn user-repo-stub []
  (reify ur/UserRepository
    (add-user [_ _ _] nil)
    (delete-user [_ _] nil)
    (list-users [_] nil)
    (find-user-by-id [_ _] nil)
    (find-user-by-pub-key [_ _] nil)))

(defn member-repo-stub []
  (reify mr/MemberRepository
    (add-member [_ _] nil)
    (update-member [_ _ _] nil)
    (find-members-by-name [_ s] nil)
    (find-members-by-name [_ s group-id] nil)
    (find-member-by-id [_ _] nil)
    (find-member-by-user-id [_ _] nil)
    (list-members [_] nil)
    (list-members [_ _] nil)
    (fuzzy-find-member [_ s] nil)
    (fuzzy-find-member [_ s group-id] nil)))

(defrecord MockChannel [sent]
  hk/Channel
  (open? [_] true)
  (websocket? [_] false)
  (close [_] false)
  (send! [_ data close-after-send?]
    (swap! sent conj data)
    true)
  (on-receive [_ cb] nil)
  (on-ping [_ cb] nil)
  (on-close [_ cb] nil))

(defn test-deps
  [{:keys [storage sse-clients] :or {storage (atom {:users {}
                                                    :map-objects #{}
                                                    :user-connections {}
                                                    :messages []})
                                     sse-clients (atom {})}}]
  {:globo-mount-path "/world-map"
   :globo-storage storage
   :globo-sse-clients sse-clients
   :user-repo (user-repo-stub)
   :member-repo (member-repo-stub)
   :render wh/render-html
   :with-layout wh/with-layout})

(defn test-handler
  [deps]
  (-> (routes/router deps)
      (rr/ring-handler nil)))

(defn ->req
  ([method uri] (->req method uri {}))
  ([method uri req]
   (merge {:request-method method :uri uri} req)))

(deftest world-map-page-test
  (let [handler (test-handler (test-deps {}))
        resp (handler (->req :get "/world-map"))]
    (is (= 200 (:status resp)))
    (let [body (:body resp)]
      (is (str/includes? body "id=\"app\""))
      (is (str/includes? body "id=\"world-map-handle\""))
      (is (str/includes? body "/world-map/assets/js/globo.js"))
      (is (str/includes? body "is.galt.globo.ui.init"))
      (is (str/includes? body "/assets/js/world-map.js"))
      (is (str/includes? body "globo-api-base-url"))
      (is (not (str/includes? body "footer"))))))

(deftest world-map-assets-test
  (let [handler (test-handler (test-deps {}))]
    (is (= 200 (:status (handler (->req :get "/world-map/assets/css/main.css")))))
    (is (= 200 (:status (handler (->req :get "/world-map/assets/3d/carrot.glb")))))
    (is (= 404 (:status (handler (->req :get "/world-map/assets/nonexistent.js")))))))

(deftest send-message-broadcast-test
  (let [sent (atom [])
        ch (->MockChannel sent)
        storage (atom {:users {"u1" {:id "u1" :name "Bob" :favorites []}}
                       :map-objects #{}
                       :user-connections {"u1" #{:conn1}}
                       :messages []})
        sse-clients (atom {:conn1 ch})
        handler (test-handler (test-deps {:storage storage :sse-clients sse-clients}))
        body "{\"type\":\"broadcast\",\"content\":\"hello\"}"
        resp (handler (->req :post "/world-map/send-message"
                             {:body (io/input-stream (.getBytes ^String body))
                              :user-id "u1"}))]
    (is (= 200 (:status resp)))
    (is (= 1 (count @sent)))))

(deftest send-message-error-paths-test
  (let [handler (test-handler (test-deps {}))]
    ;; no clients -> 404
    (is (= 404 (:status (handler (->req :post "/world-map/send-message"
                                        {:body (io/input-stream (.getBytes "{\"type\":\"broadcast\"}"))
                                         :user-id "u1"})))))
    ;; malformed json -> 400
    (is (= 400 (:status (handler (->req :post "/world-map/send-message"
                                        {:body (io/input-stream (.getBytes "{not json"))
                                         :user-id "u1"})))))))
