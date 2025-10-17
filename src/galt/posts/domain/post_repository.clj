(ns galt.posts.domain.post-repository)

(defprotocol PostRepository
  (add-post [this post])
  (list-posts [this params]))
