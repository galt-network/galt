(ns galt.members.adapters.view-models
  (:require
   [galt.core.adapters.time-helpers :as th]
   [galt.core.adapters.url-helpers :refer [add-query-params]]))

(defn profile-view-model
  [{:keys [member groups location edit-href]}]
  {:member? true
   :edit-href edit-href
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


; :tabs [{:name "All" :href (add-query-params (link-for-route :members/search))}]
(defn members-search-view-model
  [{:keys [members groups locations link-for-route active-tab]}]
  (let [search-link (link-for-route :members)]
    {:active-tab (keyword active-tab)
     :tabs
     [{:name "All" :href (add-query-params search-link {:tab "all"}) :active? (= "all" active-tab)}
      {:name "Near you" :href (add-query-params search-link {:tab "nearby"}) :active? (= "nearby" active-tab)}]
     :locations locations
     :members
     (map
       (fn [m]
         {:name (:name m)
          :avatar (:avatar m)
          :member-since (str (th/relative-time (:created-at m))
                             " ("
                             (th/short-format (:created-at m))
                             ")")
          :location (get locations (:location-id m))
          :groups (map (fn [g]
                         {:name (:name g)
                          :href (link-for-route :groups/by-id {:id (:id g)})})
                       (get groups (:id m)))
          :groups-count (count (get groups (:id m)))
          :link-to-profile (link-for-route :members/by-id {:id (:id m)})})
       members)}))
