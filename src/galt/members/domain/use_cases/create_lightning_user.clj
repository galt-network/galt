(ns galt.members.domain.use-cases.create-lightning-user)

(defn- challenge-expired? [expires-at]
  (or (nil? expires-at)
      (> (System/currentTimeMillis) expires-at)))

(defn create-lightning-user-use-case
  [{:keys [verify-signature
           gen-uuid
           find-user-by-pub-key
           find-member-by-user-id
           add-user]}
   {:keys [our-k1
           user-k1
           k1-expires-at
           user-pub-key
           signed-challenge]}]
  (if (challenge-expired? k1-expires-at)
    [:error "k1 challenge missing or expired"]
    (if-not (= user-k1 our-k1)
      [:error "User provided challenge doesn't match with our's"]
      (if (verify-signature our-k1 user-pub-key signed-challenge)
        (let [existing-user (find-user-by-pub-key user-pub-key)]
          (if existing-user
            [:ok {:user existing-user
                  :member (find-member-by-user-id (:id existing-user))
                  :message "Welcome back!"}]
            [:ok {:user (add-user (gen-uuid) user-pub-key)
                  :message "User created with public key."}]))
        [:error {:message "Signature check invalid"}]))))
