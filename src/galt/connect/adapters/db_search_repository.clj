(ns galt.connect.adapters.db-search-repository
  (:require
   [galt.connect.domain.search-repository :as repo]
   [galt.core.adapters.db-access :as db]
   [galt.core.adapters.db-result-transformations :refer [->local-date-time without-namespace]]))

(def candidate-limit 200)

(def all-types #{:member :group :event :post})

(defn- ->ldt [v]
  (when v (->local-date-time v)))

(defn- row->hit
  [row]
  (let [row (update-keys row without-namespace)]
    {:type (keyword (:type row))
     :id (:id row)
     :title (:title row)
     :description (:description row)
     :activity-at (or (->ldt (:activity_at row)) (:activity_at row))
     :lat (:latitude row)
     :lng (:longitude row)
     :start-time (->ldt (:start_time row))
     :publish-at (->ldt (:publish_at row))}))

(defn- text-where
  [title-col desc-col query]
  (when (seq query)
    [:or
     [:ilike title-col (str "%" query "%")]
     [:ilike desc-col (str "%" query "%")]]))

(defn- geo-where
  [location]
  (when location
    (let [{:keys [lat lng radius-km]} location
          meters (* 1000.0 (or radius-km 100))]
      [:and
       [:is-not :locations.latitude nil]
       [:<= [:earth_distance
             [:ll_to_earth lat lng]
             [:ll_to_earth :locations.latitude :locations.longitude]]
        meters]])))

(defn- period-where
  [time-col from to]
  (when (and from to)
    [:and [:>= time-col from] [:< time-col to]]))

(def nil-ts [:cast [:inline nil] :timestamp])

(defn- with-where
  [q & clauses]
  (let [xs (remove nil? clauses)]
    (if (seq xs)
      (assoc q :where (into [:and] xs))
      q)))

(defn- greatest-of
  [created & extras]
  (into [:greatest created]
        (map (fn [expr] [:coalesce expr created]) extras)))

(defn- member-q
  [{:keys [query location]}]
  (-> {:select [[:members.id :id]
                [[:inline "member"] :type]
                [:members.name :title]
                [:members.description :description]
                [(greatest-of
                  :members.created_at
                  {:select [[[:max :e.created_at]]]
                   :from [[:events :e]]
                   :where [:= :e.author_id :members.id]}
                  {:select [[[:max :c.created_at]]]
                   :from [[:comments :c]]
                   :where [:= :c.author_id :members.id]})
                 :activity_at]
                [:locations.latitude :latitude]
                [:locations.longitude :longitude]
                [nil-ts :start_time]
                [nil-ts :publish_at]]
       :from [:members]
       :left-join [:locations [:= :locations.id :members.location_id]]}
      (with-where
        (text-where :members.name :members.description query)
        (geo-where location))))

(defn- group-q
  [{:keys [query location]}]
  (-> {:select [[:groups.id :id]
                [[:inline "group"] :type]
                [:groups.name :title]
                [:groups.description :description]
                [(greatest-of
                  :groups.created_at
                  {:select [[[:max :e.created_at]]]
                   :from [[:events :e]]
                   :where [:= :e.group_id :groups.id]}
                  {:select [[[:max :p.created_at]]]
                   :from [[:posts :p]]
                   :where [:and [:= :p.target_type "group"] [:= :p.target_id :groups.id]]}
                  {:select [[[:max :c.created_at]]]
                   :from [[:event_comments :ec]]
                   :join [[:comments :c] [:= :c.id :ec.comment_id]
                          [:events :e] [:= :e.id :ec.event_id]]
                   :where [:= :e.group_id :groups.id]}
                  {:select [[[:max :c.created_at]]]
                   :from [[:post_comments :pc]]
                   :join [[:comments :c] [:= :c.id :pc.comment_id]
                          [:posts :p] [:= :p.id :pc.post_id]]
                   :where [:and [:= :p.target_type "group"] [:= :p.target_id :groups.id]]})
                 :activity_at]
                [:locations.latitude :latitude]
                [:locations.longitude :longitude]
                [nil-ts :start_time]
                [nil-ts :publish_at]]
       :from [:groups]
       :left-join [:locations [:= :locations.id :groups.location_id]]}
      (with-where
        (text-where :groups.name :groups.description query)
        (geo-where location))))

(defn- event-q
  [{:keys [query location from to]}]
  (-> {:select [[:events.id :id]
                [[:inline "event"] :type]
                [:events.name :title]
                [:events.description :description]
                [(greatest-of
                  :events.created_at
                  {:select [[[:max :c.created_at]]]
                   :from [[:event_comments :ec]]
                   :join [[:comments :c] [:= :c.id :ec.comment_id]]
                   :where [:= :ec.event_id :events.id]})
                 :activity_at]
                [:locations.latitude :latitude]
                [:locations.longitude :longitude]
                [:events.start_time :start_time]
                [nil-ts :publish_at]]
       :from [:events]
       :left-join [:locations [:= :locations.id :events.location_id]]}
      (with-where
        [:= :events.hidden false]
        (text-where :events.name :events.description query)
        (geo-where location)
        (period-where :events.start_time from to))))

(defn- post-q
  [{:keys [query location from to]}]
  (-> {:select [[:posts.id :id]
                [[:inline "post"] :type]
                [:posts.title :title]
                [:posts.content :description]
                [(greatest-of
                  :posts.created_at
                  :posts.publish_at
                  {:select [[[:max :c.created_at]]]
                   :from [[:post_comments :pc]]
                   :join [[:comments :c] [:= :c.id :pc.comment_id]]
                   :where [:= :pc.post_id :posts.id]})
                 :activity_at]
                [:locations.latitude :latitude]
                [:locations.longitude :longitude]
                [nil-ts :start_time]
                [:posts.publish_at :publish_at]]
       :from [:posts]
       :left-join [[:groups :post_groups] [:and
                                           [:= :posts.target_type "group"]
                                           [:= :posts.target_id :post_groups.id]]
                   :locations [:= :locations.id :post_groups.location_id]]}
      (with-where
        [:= :posts.hidden false]
        (text-where :posts.title :posts.content query)
        (geo-where location)
        (period-where :posts.publish_at from to))))

(defn candidates-sql
  [{:keys [types] :as command}]
  (let [wanted (if (seq types) types all-types)
        branches (cond-> []
                   (wanted :member) (conj (member-q command))
                   (wanted :group) (conj (group-q command))
                   (wanted :event) (conj (event-q command))
                   (wanted :post) (conj (post-q command)))]
    (if (= 1 (count branches))
      (assoc (first branches) :limit candidate-limit)
      {:union-all branches :limit candidate-limit})))

(defrecord DbSearchRepository [db-access]
  repo/SearchRepository
  (candidates [_ command]
    (->> (candidates-sql command)
         (db/query db-access)
         (map row->hit))))

(defn new-db-search-repository
  [db-access]
  (DbSearchRepository. db-access))
