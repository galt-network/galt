(ns galt.connect.domain.use-cases.search
  (:require
    [galt.connect.domain.rank :as rank]
    [galt.connect.domain.search-repository :as repo]))

(defn search
  [{:keys [search-repo]} {:keys [limit offset] :or {limit 10 offset 0} :as command}]
  [:ok (->> (rank/rank (repo/candidates search-repo command) command)
            (drop offset)
            (take limit))])
