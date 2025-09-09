(ns galt.members.use-cases.create-lightning-user
  (:require
   [galt.members.domain.user-repository :as ur]
   [galt.members.domain.member-repository :as mr]))

(defn- challenge-expired? [expires-at]
  (or (nil? expires-at)
      (> (System/currentTimeMillis) expires-at)))

(defn new-create-lightning-user
  [{:keys [verify-signature
           user-repo
           member-repo
           gen-uuid]}
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
        (let [existing-user (ur/find-user-by-pub-key user-repo user-pub-key)]
        (if existing-user
          [:ok {:user existing-user
                :member (mr/find-member-by-user-id member-repo (:id existing-user) )
                :message "Welcome back!"}]
          [:ok {:user (ur/add-user user-repo (gen-uuid) user-pub-key)
                :message "User created with public key."}]))
        [:error {:message "Signature check invalid"}]))))
