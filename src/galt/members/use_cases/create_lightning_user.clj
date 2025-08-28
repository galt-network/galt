(ns galt.members.use-cases.create-lightning-user
  (:require
   [galt.members.domain.user-repository :refer [add-user find-user-by-pub-key]]))

(defn- challenge-expired? [expires-at]
  (or (nil? expires-at)
      (> (System/currentTimeMillis) expires-at)))

(defn existing-user-with-public-key [repo pub-key]
  (first (find-user-by-pub-key repo pub-key)))

(defn new-create-lightning-user
  [{:keys [verify-signature
           user-repo
           generate-name]}
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
        (if (existing-user-with-public-key user-repo user-pub-key)
          [:ok {:user (find-user-by-pub-key user-repo user-pub-key)
                :message "Welcome back!"}]
          [:ok {:user (add-user user-repo (generate-name user-pub-key) (random-uuid) user-pub-key)
                :message "User created with public key."}])
        [:error {:message "Signature check invalid"}]))))
