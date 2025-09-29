(ns galt.posts.domain.use-cases.create-post
  (:require
    [clojure.spec.alpha :as s]
    [clojure.spec.test.alpha :as st]))

(defn between [min max v]
  (<= min v max))

(s/def ::author-id uuid?)
(s/def ::title (s/and string? (partial between 3 30)))
(s/def ::content (s/and string? (partial between 3 3000)))

(s/def ::command
  (s/keys :req-un [::author-id
                   ::title
                   ::content
                   ::target-type
                   ::target-id
                   ::comment-policy]))

(defn create-post-use-case
  [{:keys [add-post]} command]
  (s/assert ::command command)
  [:ok (add-post (:post command))])
