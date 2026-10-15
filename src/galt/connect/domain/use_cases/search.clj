(ns galt.connect.domain.use-cases.search
  (:require
    [clojure.string :as str]
    [galt.connect.domain.rank :as rank]
    [galt.connect.domain.search-repository :as repo]))

(defn- text-query
  [query]
  (when (seq (str/replace (str query) #"[^\p{L}\p{N}]" ""))
    query))

(defn- ranked
  [search-repo command]
  (rank/rank (repo/candidates search-repo command) command))

(defn search
  [{:keys [search-repo]} {:keys [limit offset] :or {limit 10 offset 0} :as command}]
  (let [command (update command :query text-query)
        local (ranked search-repo command)
        dropped? (boolean (and (:query command) (:location command) (empty? local)))]
    [:ok {:hits (->> (if dropped? (ranked search-repo (dissoc command :location)) local)
                     (drop offset)
                     (take limit))
          :near-me-dropped? dropped?}]))
