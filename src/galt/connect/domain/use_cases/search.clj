(ns galt.connect.domain.use-cases.search
  (:require
    [clojure.string :as str]
    [galt.connect.domain.rank :as rank]
    [galt.connect.domain.search-repository :as repo]))

(defn- text-query
  [query]
  (when (seq (str/replace (str query) #"[^\p{L}\p{N}]" ""))
    query))

(defn search
  [{:keys [search-repo]} {:keys [limit offset] :or {limit 10 offset 0} :as command}]
  (let [command (update command :query text-query)
        hits (rank/rank (repo/candidates search-repo command) command)]
    [:ok {:hits (->> hits (drop offset) (take limit))
          :near-me-dropped? false}]))
