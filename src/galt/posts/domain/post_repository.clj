(ns galt.posts.domain.post-repository)

(defprotocol PostRepository
  (add-post [this post])
  (group-posts [this group-id]))
