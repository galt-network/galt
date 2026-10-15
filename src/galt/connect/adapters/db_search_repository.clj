(ns galt.connect.adapters.db-search-repository
  (:require
   [clojure.string :as str]
   [galt.connect.domain.search-repository :as repo]
   [galt.core.adapters.db-access :as db]
   [galt.core.adapters.db-result-transformations :refer [->local-date-time without-namespace]]
   [honey.sql :as sql]))

(def ^:private ts-match (keyword "@@"))
(def ^:private word-similar (keyword "<%"))

;; honey.sql has no built-in operator for Postgres text search @@ or trigram <% .
(sql/register-op! ts-match)
(sql/register-op! word-similar)

(def ^:private typo-min-length 5)

;; shortcut: a fixed minimum word length stands in for pg_trgm's word_similarity
;; threshold; align both if a real corpus shows them disagreeing.
(defn- typo-word?
  [word]
  (>= (count word) typo-min-length))

(def all-types #{:member :group :event :post})

(defn- ->ldt [v]
  (when v (->local-date-time v)))

(defn- row->hit
  [row]
  (let [row (update-keys row without-namespace)]
    (cond-> {:type (keyword (:type row))
             :id (:id row)
             :title (:title row)
             :description (:description row)
             :activity-at (or (->ldt (:activity_at row)) (:activity_at row))
             :lat (:latitude row)
             :lng (:longitude row)
             :start-time (->ldt (:start_time row))
             :publish-at (->ldt (:publish_at row))}
      (some? (:title_match row)) (assoc :match (cond (:title_match row) :title
                                                      (:typo_match row) :title-fuzzy
                                                      :else :body))
      (some? (:snippet row)) (assoc :snippet (str/replace (:snippet row) #"</?b>" "")))))

(defn- doc-text
  [& cols]
  (into [:||] (interpose [:inline " "] (map #(vector :coalesce % [:inline ""]) cols))))

(defn- ts-doc
  [& cols]
  [:to_tsvector [:inline "english"] (apply doc-text cols)])

(defn- ts-query
  [query]
  [:plainto_tsquery [:inline "english"] [:inline query]])

(defn- query-words
  [query]
  (->> (str/split (str query) #"\s+")
       (remove str/blank?)))

(defn- word-where
  [title-col desc-col word]
  (cond-> [[ts-match (ts-doc title-col desc-col) (ts-query word)]
           [:= [:cast (ts-query word) :text] [:inline ""]]]
    (typo-word? word) (conj [word-similar [:inline word] title-col])))

(defn- text-where
  [title-col desc-col query]
  (when-let [words (seq (query-words query))]
    (into [:and [:<> [:cast (ts-query (str/join " " words)) :text] [:inline ""]]]
          (map #(into [:or] (word-where title-col desc-col %)) words))))

(defn- query-selects
  [title-col desc-col query]
  (when-let [words (seq (query-words query))]
    (into [[[ts-match (ts-doc title-col) (ts-query (str/join " " words))] :title_match]
           [[:ts_headline [:inline "english"] (doc-text title-col desc-col)
             (ts-query (str/join " " words))]
            :snippet]]
          (when (some typo-word? words)
            [[(into [:or] (map #(vector word-similar [:inline %] title-col)
                               (filter typo-word? words)))
              :typo_match]]))))

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

(defn- flat-joins
  [pairs]
  (vec (mapcat identity pairs)))

;; shortcut: activity_at is aggregated per search instead of stored, move to a
;; stored column if members/comments/events/posts outgrow these grouped scans.
(defn- latest-created
  [{:keys [key created from joins where]}]
  (cond-> {:select [[key :key] [[:max created] :latest]]
           :from [from]
           :group-by [key]}
    (seq joins) (assoc :join (flat-joins joins))
    where (assoc :where where)))

(defn- with-joins
  [q & pairs]
  (assoc q :left-join (flat-joins pairs)))

(defn- member-q
  [{:keys [query location]}]
  (-> {:select (into [[:members.id :id]
                      [[:inline "member"] :type]
                      [:members.name :title]
                      [:members.description :description]
                      [(greatest-of
                        :members.created_at
                        :member_events.latest
                        :member_comments.latest)
                       :activity_at]
                      [:locations.latitude :latitude]
                      [:locations.longitude :longitude]
                      [nil-ts :start_time]
                      [nil-ts :publish_at]]
                     (query-selects :members.name :members.description query))
       :from [:members]}
      (with-joins
        [:locations [:= :locations.id :members.location_id]]
        [[(latest-created {:key :author_id :created :created_at :from :events})
          :member_events]
         [:= :member_events.key :members.id]]
        [[(latest-created {:key :author_id :created :created_at :from :comments})
          :member_comments]
         [:= :member_comments.key :members.id]]
        [:users [:= :users.id :members.id]])
      (with-where
        [:is :members.deleted_at nil]
        [:is :users.deleted_at nil]
        (text-where :members.name :members.description query)
        (geo-where location))))

(defn- group-q
  [{:keys [query location]}]
  (-> {:select (into [[:groups.id :id]
                      [[:inline "group"] :type]
                      [:groups.name :title]
                      [:groups.description :description]
                      [(greatest-of
                        :groups.created_at
                        :group_events.latest
                        :group_posts.latest
                        :group_event_comments.latest
                        :group_post_comments.latest)
                       :activity_at]
                      [:locations.latitude :latitude]
                      [:locations.longitude :longitude]
                      [nil-ts :start_time]
                      [nil-ts :publish_at]]
                     (query-selects :groups.name :groups.description query))
       :from [:groups]}
      (with-joins
        [:locations [:= :locations.id :groups.location_id]]
        [[(latest-created {:key :group_id :created :created_at :from :events})
          :group_events]
         [:= :group_events.key :groups.id]]
        [[(latest-created {:key :target_id :created :created_at :from :posts
                           :where [:= :target_type "group"]})
          :group_posts]
         [:= :group_posts.key :groups.id]]
        [[(latest-created {:key :events.group_id :created :comments.created_at
                           :from :event_comments
                           :joins [[:comments [:= :comments.id :event_comments.comment_id]]
                                   [:events [:= :events.id :event_comments.event_id]]]})
          :group_event_comments]
         [:= :group_event_comments.key :groups.id]]
        [[(latest-created {:key :posts.target_id :created :comments.created_at
                           :from :post_comments
                           :joins [[:comments [:= :comments.id :post_comments.comment_id]]
                                   [:posts [:= :posts.id :post_comments.post_id]]]
                           :where [:= :posts.target_type "group"]})
          :group_post_comments]
         [:= :group_post_comments.key :groups.id]])
      (with-where
        (text-where :groups.name :groups.description query)
        (geo-where location))))

(defn- event-q
  [{:keys [query location from to]}]
  (-> {:select (into [[:events.id :id]
                      [[:inline "event"] :type]
                      [:events.name :title]
                      [:events.description :description]
                      [(greatest-of
                        :events.created_at
                        :event_comment_activity.latest)
                       :activity_at]
                      [:locations.latitude :latitude]
                      [:locations.longitude :longitude]
                      [:events.start_time :start_time]
                      [nil-ts :publish_at]]
                     (query-selects :events.name :events.description query))
       :from [:events]}
      (with-joins
        [:locations [:= :locations.id :events.location_id]]
        [[(latest-created {:key :event_comments.event_id :created :comments.created_at
                           :from :event_comments
                           :joins [[:comments [:= :comments.id :event_comments.comment_id]]]})
          :event_comment_activity]
         [:= :event_comment_activity.key :events.id]])
      (with-where
        [:= :events.hidden false]
        (text-where :events.name :events.description query)
        (geo-where location)
        (period-where :events.start_time from to))))

(defn- post-q
  [{:keys [query location from to]}]
  (-> {:select (into [[:posts.id :id]
                      [[:inline "post"] :type]
                      [:posts.title :title]
                      [:posts.content :description]
                      [(greatest-of
                        :posts.created_at
                        :posts.publish_at
                        :post_comment_activity.latest)
                       :activity_at]
                      [:locations.latitude :latitude]
                      [:locations.longitude :longitude]
                      [nil-ts :start_time]
                      [:posts.publish_at :publish_at]]
                     (query-selects :posts.title :posts.content query))
       :from [:posts]}
      (with-joins
        [[:groups :post_groups] [:and
                                 [:= :posts.target_type "group"]
                                 [:= :posts.target_id :post_groups.id]]]
        [:locations [:= :locations.id :post_groups.location_id]]
        [[(latest-created {:key :post_comments.post_id :created :comments.created_at
                           :from :post_comments
                           :joins [[:comments [:= :comments.id :post_comments.comment_id]]]})
          :post_comment_activity]
         [:= :post_comment_activity.key :posts.id]])
      (with-where
        [:= :posts.hidden false]
        (text-where :posts.title :posts.content query)
        (geo-where location)
        (period-where :posts.publish_at from to))))

;; shortcut: every candidate comes back and is ranked in memory, so a broad
;; query scans the whole table; add a cap or a ranked index if these outgrow it.
(defn candidates-sql
  [{:keys [types] :as command}]
  (let [wanted (if (seq types) types all-types)
        branches (cond-> []
                   (wanted :member) (conj (member-q command))
                   (wanted :group) (conj (group-q command))
                   (wanted :event) (conj (event-q command))
                   (wanted :post) (conj (post-q command)))]
    (if (= 1 (count branches))
      (first branches)
      {:union-all branches})))

(defrecord DbSearchRepository [db-access]
  repo/SearchRepository
  (candidates [_ command]
    (->> (candidates-sql command)
         (db/query db-access)
         (map row->hit))))

(defn new-db-search-repository
  [db-access]
  (DbSearchRepository. db-access))
