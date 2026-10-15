(ns galt.connect.adapters.db-search-repository-test
  (:require
    [clojure.test :refer [deftest is]]
    [galt.connect.adapters.db-search-repository :as db-repo]
    [galt.connect.domain.search-repository :as repo]
    [galt.core.adapters.db-access :as db]
    [honey.sql :as sql]))

(defn- fake-db [rows]
  (reify db/DbAccess
    (query [_ _] rows)
    (query-one [_ _] (first rows))
    (in-transaction [_ cb] (cb (fn [_] rows)))))

(deftest maps-rows-to-hits
  (let [id #uuid "01900000-0000-7000-8000-000000000001"
        ts (java.sql.Timestamp/valueOf "2026-01-02 15:00:00")
        rows [{:id id
               :type "member"
               :title "Ada"
               :description "Engineer"
               :activity_at ts
               :latitude 59.4
               :longitude 24.7}]
        hits (repo/candidates (db-repo/new-db-search-repository (fake-db rows)) {})]
    (is (= 1 (count hits)))
    (is (= {:type :member
            :id id
            :title "Ada"
            :description "Engineer"
            :lat 59.4
            :lng 24.7}
           (select-keys (first hits) [:type :id :title :description :lat :lng])))
    (is (= (.toLocalDateTime ts) (:activity-at (first hits))))))

(deftest maps-qualified-event-row-to-hit
  (let [id #uuid "01900000-0000-7000-8000-000000000002"
        rows [{:events/id id
               :type "event"
               :events/title "Meetup"
               :events/description "Talk"
               :activity_at (java.sql.Timestamp/valueOf "2026-01-02 15:00:00")
               :locations/latitude 59.4
               :locations/longitude 24.7}]
        hits (repo/candidates (db-repo/new-db-search-repository (fake-db rows)) {:types #{:event}})]
    (is (= {:type :event :id id :title "Meetup" :description "Talk" :lat 59.4 :lng 24.7}
           (select-keys (first hits) [:type :id :title :description :lat :lng])))))

(deftest empty-command-sql-unions-all-types
  (let [sql (first (sql/format (db-repo/candidates-sql {})))]
    (is (re-find #"(?i)union all" sql))
    (is (re-find #"member" sql))
    (is (re-find #"group" sql))
    (is (re-find #"event" sql))
    (is (re-find #"post" sql))
    (is (not (re-find #"'comment'" sql)))))

(deftest empty-types-sql-unions-all-types
  (let [sql (first (sql/format (db-repo/candidates-sql {:types #{}})))]
    (is (re-find #"(?i)union all" sql))
    (is (re-find #"member" sql))
    (is (re-find #"FROM events" sql))
    (is (re-find #"FROM posts" sql))
    (is (not (re-find #"(?i)^LIMIT" sql)))))

(defn- branches
  [q]
  (or (:union-all q) [q]))

(deftest activity-at-is-not-a-per-row-lookup
  (is (empty? (for [branch (branches (db-repo/candidates-sql {}))
                    [expr _alias] (:select branch)
                    :when (map? expr)]
                expr))))

(deftest deleted-members-are-excluded-in-sql
  (let [sql (first (sql/format (db-repo/candidates-sql {:types #{:member}})))]
    (is (re-find #"members\.deleted_at IS NULL" sql))
    (is (re-find #"users\.deleted_at IS NULL" sql))))

(deftest text-predicate-is-full-text-search
  (let [sql (first (sql/format (db-repo/candidates-sql {:query "bitcoin"})))]
    (is (re-find #"(?i)to_tsvector" sql))
    (is (re-find #"(?i)plainto_tsquery" sql))
    (is (re-find #"@@" sql))
    (is (not (re-find #"(?i)ilike" sql)))))

(deftest no-candidate-cap-before-ranking
  (is (not (re-find #"(?i)limit" (first (sql/format (db-repo/candidates-sql {}))))))
  (is (not (re-find #"(?i)limit" (first (sql/format (db-repo/candidates-sql {:types #{:member}})))))))

(deftest a-query-asks-for-title-match-and-snippet
  (let [sql (first (sql/format (db-repo/candidates-sql {:query "bitcoin"})))]
    (is (re-find #"title_match" sql))
    (is (re-find #"(?i)ts_headline" sql)))
  (let [sql (first (sql/format (db-repo/candidates-sql {})))]
    (is (not (re-find #"title_match" sql)))
    (is (not (re-find #"(?i)ts_headline" sql)))))

(deftest maps-title-match-and-snippet-to-a-hit
  (let [row {:id #uuid "01900000-0000-7000-8000-000000000003"
             :type "member"
             :title "Bitcoin Meetup"
             :description "Monthly"
             :activity_at (java.sql.Timestamp/valueOf "2026-01-02 15:00:00")}
        run (fn [row]
              (first (repo/candidates (db-repo/new-db-search-repository (fake-db [row]))
                                      {:query "bitcoin"})))]
    (is (= :title (:match (run (assoc row :title_match true :snippet "Bitcoin Meetup")))))
    (is (= :body (:match (run (assoc row :title_match false :snippet "… bitcoin …")))))
    (is (= "… bitcoin …"
           (:snippet (run (assoc row :title_match false :snippet "… bitcoin …")))))))
