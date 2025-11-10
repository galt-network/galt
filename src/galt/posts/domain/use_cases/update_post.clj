(ns galt.posts.domain.use-cases.update-post)

(defn update-post-use-case
  [{:keys [update-post get-post]}
   {:keys [updater-id post-id post]}]
  (let [existing-post (get-post post-id)]
    (cond
      (not (= (:author-id existing-post) updater-id))
      [:error "This user is not allowed to update this post"]

      (> 10 (count (:title post)))
      [:error "Title needs to be at least 10 characters"]

      (not (<= 10 (count (:content post)) 10000))
      [:error "Content needs to be between 10 and 10000 characters"]

      :else [:ok (update-post post-id post)])))
