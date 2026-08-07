(ns galt.world-map.world-map-test
  (:require
   [clojure.java.io :as io]
   [clojure.string :as str]
   [clojure.test :refer [deftest is testing]]
   [galt.core.infrastructure.web.helpers :as wh]
   [galt.members.domain.member-repository :as mr]
   [galt.members.domain.user-repository :as ur]
   [galt.world-map.adapters.handlers :as handlers]
   [galt.world-map.external.routes :as routes]
   [is.galt.globo.server :as globo-server]
   [is.galt.globo.server.connections :as globo-connections]
   [is.galt.globo.server.storage :as globo-storage]
   [org.httpkit.server :as hk]
   [reitit.ring :as rr]))

(defn user-repo-stub []
  (reify ur/UserRepository
    (add-user [_ _ _] nil)
    (delete-user [_ _] nil)
    (list-users [_] nil)
    (find-user-by-id [_ _] nil)
    (find-user-by-pub-key [_ _] nil)))

(defn user-repo-with-name [name]
  (reify ur/UserRepository
    (add-user [_ _ _] nil)
    (delete-user [_ _] nil)
    (list-users [_] nil)
    (find-user-by-id [_ _] (when name {:id "11111111-1111-1111-1111-111111111111" :name name}))
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

(defn member-repo-with-name [name]
  (reify mr/MemberRepository
    (add-member [_ _] nil)
    (update-member [_ _ _] nil)
    (find-members-by-name [_ s] nil)
    (find-members-by-name [_ s group-id] nil)
    (find-member-by-id [_ _] (when name {:id "11111111-1111-1111-1111-111111111111" :name name}))
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
  (let [globo (globo-server/create-globo
               {:mount-path "/world-map"
                :storage (globo-storage/in-memory-globo-storage storage)
                :connections (globo-connections/in-memory-connection-store sse-clients)})]
    {:globo globo
     :globo-mount-path "/world-map"
     :user-repo (user-repo-stub)
     :member-repo (member-repo-stub)
     :render wh/render-html
     :with-layout wh/with-layout}))

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
        body "{\"type\":\"broadcast\",\"content\":{}}"
        resp (handler (->req :post "/world-map/send-message"
                             {:body (io/input-stream (.getBytes ^String body))
                              :user-id "u1"}))]
    (is (= 200 (:status resp)))
    (is (= 1 (count @sent)))))

(deftest send-message-error-paths-test
  (let [handler (test-handler (test-deps {}))]
    ;; valid message, no clients -> 404
    (is (= 404 (:status (handler (->req :post "/world-map/send-message"
                                        {:body (io/input-stream (.getBytes "{\"type\":\"broadcast\",\"content\":{}}"))
                                         :user-id "u1"})))))
    ;; invalid message (missing content) -> 400
    (is (= 400 (:status (handler (->req :post "/world-map/send-message"
                                        {:body (io/input-stream (.getBytes "{\"type\":\"broadcast\"}"))
                                         :user-id "u1"})))))
    ;; malformed json -> 400
    (is (= 400 (:status (handler (->req :post "/world-map/send-message"
                                        {:body (io/input-stream (.getBytes "{not json"))
                                         :user-id "u1"})))))))

(defn- globo-storage-atom []
  (atom {:users {} :map-objects #{} :user-connections {} :messages []}))

(defn- name-seeding-deps
  [{:keys [member-name user-name]}]
  (let [storage (globo-storage-atom)]
    {:storage storage
     :globo (globo-server/create-globo
             {:storage (globo-storage/in-memory-globo-storage storage)})
     :member-repo (member-repo-with-name member-name)
     :user-repo (user-repo-with-name user-name)}))

(defn- seed-name
  [deps user-id]
  ((handlers/wrap-globo-user-name deps (fn [_] {:status 200})) {:user-id user-id}))

(def test-user-id "11111111-1111-1111-1111-111111111111")

(deftest galt-user-name-precedence-test
  (testing "member profile name wins over generated user name"
    (is (= "Alice" (handlers/galt-user-name
                    (name-seeding-deps {:member-name "Alice" :user-name "Generated"})
                    test-user-id))))
  (testing "falls back to generated user name without a member profile"
    (is (= "Generated" (handlers/galt-user-name
                        (name-seeding-deps {:member-name nil :user-name "Generated"})
                        test-user-id))))
  (testing "nil when no name data exists"
    (is (nil? (handlers/galt-user-name
               (name-seeding-deps {:member-name nil :user-name nil})
               test-user-id))))
  (testing "nil for unparseable user-id"
    (is (nil? (handlers/galt-user-name
               (name-seeding-deps {:member-name "Alice" :user-name "Generated"})
               "u1")))))

(deftest wrap-globo-user-name-seeds-name-test
  (testing "member name is seeded into globo storage on connection"
    (let [deps (name-seeding-deps {:member-name "Alice" :user-name "Generated"})]
      (seed-name deps test-user-id)
      (is (= "Alice" (get-in @(:storage deps) [:users test-user-id :name])))))
  (testing "generated user name is seeded without a member profile"
    (let [deps (name-seeding-deps {:member-name nil :user-name "Generated"})]
      (seed-name deps test-user-id)
      (is (= "Generated" (get-in @(:storage deps) [:users test-user-id :name])))))
  (testing "nothing is seeded when no name data exists"
    (let [deps (name-seeding-deps {:member-name nil :user-name nil})]
      (seed-name deps test-user-id)
      (is (nil? (get-in @(:storage deps) [:users test-user-id]))))))

(deftest wrap-globo-user-name-skips-anon-test
  (let [deps (name-seeding-deps {:member-name "Alice" :user-name "Generated"})]
    (seed-name deps "anon-123")
    (is (nil? (get-in @(:storage deps) [:users "anon-123"])))))
