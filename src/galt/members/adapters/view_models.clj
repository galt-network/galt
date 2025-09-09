(ns galt.members.adapters.view-models
  (:require
    [galt.groups.domain.group-repository :as gr]
    [galt.members.domain.user-repository :refer [list-users]]
    ))

(defn members-model
  [{:keys [user-repo group-repo link-for-route]} _req]
  (let [users (list-users user-repo)
        add-groups (fn [user]
                     (assoc user
                            :groups
                            (map (fn [g] {:name (:groups/name g)
                                          :href (link-for-route :groups/by-id {:id (:groups/id g)})})
                                 (gr/find-groups-by-member group-repo (:users/id user)))))
        add-user (fn [user]
                   (assoc user :user {:name (:users/name user)
                                      :href (link-for-route :members/show-profile {:id (:users/id user)})}))]
    {:column-titles [["Name" :user] ["Member Since" :users/created-at] :groups]
     :users (->> users
                 (map add-user ,,,)
                 (map add-groups ,,,))}))


(defn login-result-view-model
  [status result]
  (let [name (or (-> result :member :name) (-> result :user :name))
        message-classes {:ok :is-success
                         :error :is-danger}]
    {:name name
     :message-class (get message-classes status)
     :message-header (if (= :ok status) "Login successful!" "Login not successful")
     :message-body (case status
                     :error (:message result)
                     :ok (if (:member result)
                           (str "Welcome back, " name)
                           (str "User created with public key. Your name is " name)))}))
