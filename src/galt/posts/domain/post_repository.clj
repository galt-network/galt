(ns galt.posts.domain.post-repository)

(defprotocol PostRepository
  (add-post [this post])
  (get-post [this post-id])
  (update-post [this post-id attrs])
  (delete-post [this post-id])
  (list-posts [this params]))
