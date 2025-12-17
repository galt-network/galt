(ns galt.members.domain.use-cases.complete-lnurl-login
  (:require
   [clojure.spec.alpha :as s]
   [java-time.api :as jt]
   [ring.middleware.session.store :as ss]))

(defn- challenge-expired? [expires-at]
  (or (nil? expires-at)
      (> (System/currentTimeMillis) expires-at)))

(defn- find-or-create-user-and-add-to-session
  [{:keys [session-store
           gen-uuid
           find-user-by-pub-key
           find-member-by-user-id
           current-membership-payment
           add-user]}
   {:keys [pub-key session-id]}]
  (let [user (or
               (find-user-by-pub-key pub-key)
               (add-user (gen-uuid) pub-key))
        member (find-member-by-user-id (:id user))
        payment (current-membership-payment (:id member))
        membership-active? (jt/before? (jt/local-date-time) (jt/plus (:paid-at payment) (jt/years 1)))
        updated-session {:user-id (:id user)
                         :membership-active? membership-active?
                         :member-id (:id member)}]
    (ss/write-session session-store session-id updated-session)
    {:user user :member member :session updated-session}))

(s/def ::session-store fn?)
(s/def ::verify-signature fn?)
(s/def ::gen-uuid fn?)
(s/def ::find-user-by-pub-key fn?)
(s/def ::find-member-by-user-id fn?)
(s/def ::current-membership-payment fn?)
(s/def ::add-user fn?)
(s/def ::deps (s/keys :req-un [::session-store
                               ::verify-signature
                               ::gen-uuid
                               ::find-user-by-pub-key
                               ::find-member-by-user-id
                               ::current-membership-payment
                               ::add-user]))

(s/def ::challenge string?)
(s/def ::signed-challenge string?)
(s/def ::user-pub-key string?)
(s/def ::session-id string?)
(s/def ::command (s/keys :req-un [::challenge ::signed-challenge ::user-pub-key ::session-id]))

(defn complete-lnurl-login-use-case
  [{:keys [session-store verify-signature] :as deps}
   {:keys [challenge signed-challenge user-pub-key session-id] :as command}]
  (s/assert ::deps deps)
  (s/assert ::command command)
  (let [lnurl-session-data (some->> session-id
                                    (ss/read-session session-store ,,,)
                                    :lnurl-auth)
        challenge-expires-at (:expires-at lnurl-session-data)
        our-challenge (:k1 lnurl-session-data)]
    (if (challenge-expired? challenge-expires-at)
      [:error "k1 challenge missing or expired"]
      (if-not (= challenge our-challenge)
        [:error "User provided challenge doesn't match with our's"]
        (if (verify-signature our-challenge user-pub-key signed-challenge)
          [:ok (find-or-create-user-and-add-to-session deps {:pub-key user-pub-key
                                                             :session-id session-id})]
          [:error {:message "Signature check invalid"}])))))
