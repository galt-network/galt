(ns galt.posts.domain.post-repository)

(defprotocol PostRepository
  (add-post [this post])
  (get-post [this post-id])
  (list-posts [this params]))
