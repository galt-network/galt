(ns galt.members.domain.use-cases.complete-lnurl-login
  (:require
    [ring.middleware.session.store :as ss]))

(defn- challenge-expired? [expires-at]
  (or (nil? expires-at)
      (> (System/currentTimeMillis) expires-at)))

(defn find-or-create-user-and-add-to-session
  [{:keys [session-store gen-uuid find-user-by-pub-key find-member-by-user-id add-user]}
   {:keys [pub-key session-id]}]
  (let [user (or
               (find-user-by-pub-key pub-key)
               (add-user (gen-uuid) pub-key))
        member (find-member-by-user-id (:id user))]
    (ss/write-session session-store session-id {:user-id (:id user)
                                                :member-id (:id member)})
    {:user user :member member}))

(defn complete-lnurl-login-use-case
  [{:keys [session-store
           verify-signature
           gen-uuid
           find-user-by-pub-key
           find-member-by-user-id
           add-user] :as deps}
   {:keys [challenge signed-challenge user-pub-key session-id]}]
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
