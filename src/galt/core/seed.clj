(ns galt.core.seed
  (:require
   [donut.system :as ds]
   [galt.core.adapters.db-access :as db-access]
   [galt.core.infrastructure.name-generator :as name-generator]
   [galt.locations.domain.location-repository :as lr]
   [galt.core.adapters.time-helpers :as th]
   [java-time.api :as jt]
   [galt.members.domain.member-repository :as mr]
   [galt.members.domain.user-repository :as ur]
   [galt.events.domain.event-repository :as er]
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

(defn grouped-name [n]
  (-> {0 "Zeroeth"
       1 "First"
       2 "Second"
       3 "Third"
       4 "Fourth"
       5 "Fifth"
       6 "Sixth"
       7 "Seventh"
       8 "Eigth"
       9 "Ninth"}
      (get ,,, (mod n 10))))

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
  "## Lorem ipsum dolor sit amet,
  > _consectetur adipiscing elit, sed do_
  > _eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut_

  Anarcho-capitalism (colloquially: ancap or an-cap) is a political philosophy and
  economic theory that advocates for the abolition of centralized states in favor
  of stateless societies, where systems of private property are enforced by private agencies.

  ### enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi
  Ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit
  1. in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur
  2. sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt

  Mollit anim id est laborum.")

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
        members-data (map (fn [user location idx]
                            {:name (str (grouped-name idx) " " (name-generator/generate (:pub-key user)) " von Test")
                             :user-id (:id user)
                             :avatar (rand-nth example-avatars)
                             :description lorem-ipsum
                             :location-id (:id location)}) users locations range-to-create-in)
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
                         :name (str (grouped-name nr) " Example Group " nr)
                         :description lorem-ipsum
                         :avatar (rand-nth example-avatars)
                         :location-id (:location-id founder)}))
                    founders range-to-create-in)]
    (doall groups)))

(defn add-members-to-groups
  [system members groups]
  (doall (for [group groups
               member members]
           (gr/add-to-group (get-in system [::ds/instances :storage :group]) (:id group) (:id member) "member"))))

(defn seed-events
  [system group author & [opts]]
  (let [id (random-uuid)
        start-time (or (:start-time opts) (jt/local-date-time))
        end-time (jt/plus start-time (jt/hours 1.5))
        publish-time start-time
        event {:id id
               :name (str (:name group) "'s Event \"" (name-generator/generate (str id)) "\"")
               :description lorem-ipsum
               :author-id (:id author)
               :start-time start-time
               :end-time end-time
               :publish-at publish-time
               :location-id (:location-id group)
               :type (rand-nth ["live" "online"])}]
    (er/add-event (get-in system [::ds/instances :storage :event]) event)))

(comment
  (require '[galt.main :refer [running-system]])
  (random-location-in-country @running-system "SV")
  (def members (seed-members @running-system 30))
  (def groups (seed-groups @running-system 10))
  (let [[m-first m-second m-third] (partition 3 members)
        [g-first g-second g-third] (partition 3 groups)]
    (add-members-to-groups @running-system m-first  g-first)
    (add-members-to-groups @running-system (concat m-first m-second) g-second)
    (add-members-to-groups @running-system (concat m-first m-second m-third) g-third))

  (doall (for [group groups member members] (seed-events @running-system group member)))
  )
