(ns galt.members.domain.user-repository)

(defprotocol UserRepository
  (add-user [this name id pub-key])
  (delete-user [this id])
  (list-users [this])
  (find-user-by-id [this id])
  (find-user-by-pub-key [this pub-key]))
