(ns galt.core.seed
  (:require
   [donut.system :as ds]
   [galt.core.adapters.db-access :as db-access]
   [galt.core.infrastructure.name-generator :as name-generator]
   [galt.locations.domain.location-repository :as lr]
   [galt.members.domain.member-repository :as mr]
   [galt.members.domain.user-repository :as ur]
   [galt.groups.domain.group-repository :as gr]))

(defn random-location-in-country
  [system country-code]
  (let [db-access (get-in system [::ds/instances :storage :db-access])
        random-city (->> {:select [:*]
                          :from [:cities]
                          :order-by [:%random]
                          :where [:= :country_code country-code]
                          :limit 100}
                          (db-access/query db-access ,,,)
                          (first ,,,)
                         )]
    {:name (:cities/name random-city)
     :latitude (:cities/latitude random-city)
     :longitude (:cities/longitude random-city)
     :country-code (:cities/country_code random-city)
     :city-id (:cities/id random-city)}))

(def example-avatars
  ["/assets/images/example-avatars/avatar-1.png"
   "/assets/images/example-avatars/avatar-2.png"
   "/assets/images/example-avatars/avatar-3.png"
   "/assets/images/example-avatars/avatar-4.png"
   "/assets/images/example-avatars/avatar-5.png"
   "/assets/images/example-avatars/avatar-6.png"
   "/assets/images/example-avatars/avatar-7.png"
   "/assets/images/example-avatars/avatar-8.png"
   "/assets/images/example-avatars/avatar-9.png"
   "/assets/images/example-avatars/avatar-10.png"])

(def lorem-ipsum
  "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do
  eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut
  enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi
  ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit
  in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur
  sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt
  mollit anim id est laborum.")

(defn seed-members [system count]
  (let [user-repo (get-in system [::ds/instances :storage :user])
        location-repo (get-in system [::ds/instances :storage :location])
        member-repo (get-in system [::ds/instances :storage :member])
        db-access (get-in system [::ds/instances :storage :db-access])
        previous-user-count (->> {:select [[:%count.*]] :from [:users]}
                                 (db-access/query db-access ,,,)
                                 (first ,,,)
                                 :count)
        range-to-create-in (range previous-user-count (+ previous-user-count count))
        pub-keys (map #(str "fake-pub-key-" %) range-to-create-in)
        users (map (fn [pub-key] (ur/add-user user-repo (random-uuid) pub-key)) pub-keys)
        locations (map (fn [_] (lr/add-location location-repo (random-location-in-country system "SV")))
                       range-to-create-in)
        members-data (map (fn [user location]
                            {:name (str (name-generator/generate (:pub-key user)) " von Test")
                             :user-id (:id user)
                             :avatar (rand-nth example-avatars)
                             :location-id (:id location)}) users locations)
        members (map (fn [member] (mr/add-member member-repo member)) members-data)]
    (doall members)))


(defn seed-groups
  [system count]
  (let [group-repo (get-in system [::ds/instances :storage :group])
        db-access (get-in system [::ds/instances :storage :db-access])
        previous-count (->> {:select [[:%count.*]] :from [:groups]}
                            (db-access/query db-access ,,,)
                            (first ,,,)
                            :count)
        range-to-create-in (range previous-count (+ previous-count count))
        founders (seed-members system count)
        groups (map (fn [founder nr]
                      (gr/add-group
                        group-repo
                        (:id founder)
                        {:id (random-uuid)
                         :name (str "Example Group " nr)
                         :description lorem-ipsum
                         :avatar (rand-nth example-avatars)
                         :location-id (:location-id founder)}))
                    founders range-to-create-in)]
    (doall groups)))

(comment
  (require '[galt.main :refer [running-system]])
  (random-location-in-country @running-system "SV")
  (seed-members @running-system 30)
  (seed-groups @running-system 10))
