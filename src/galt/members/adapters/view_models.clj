(ns galt.members.adapters.view-models
  (:require
   [galt.core.adapters.time-helpers :as th]))

(defn profile-view-model
  [{:keys [member groups location]}]
  {:member? true
   :name (:name member)
   :description (:description member)
   :avatar (:avatar member)
   :slug (:slug member)
   :groups (map :name groups)
   :location-name (str (:name location) (when (:country-code location) (str ", " (:country-code location))))
   :latitude (:latitude location)
   :longitude (:longitude location)})

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


(defn members-search-view-model
  [result link-for-route]
  (map
    (fn [m]
      {:name (:name m)
       :member-since (str (th/relative-time (:created-at m))
                          " ("
                          (th/short-format (:created-at m))
                          ")")
       :groups (map (fn [g]
                      {:name (:name g)
                       :href (link-for-route :groups/by-id {:id (:id g)})})
                    (get-in result [:groups (:id m)]))
       :groups-count (count (get-in result [:groups (:id m)]))
       :link-to-profile (link-for-route :members/by-id {:id (:id m)})})
    (:members result)))
