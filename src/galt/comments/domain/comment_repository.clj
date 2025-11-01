(ns galt.comments.domain.comment-repository)

(defprotocol CommentRepository
  (add-comment [this entity-id entity-type comment-attrs])
  (get-comment [this comment-id])
  (delete-comment [this comment-id])
  (list-comments [this entity-id entity-type query-params]))
