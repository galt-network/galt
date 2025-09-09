(ns galt.members.domain.use-cases.show-profile
  (:require
    [galt.members.domain.user-repository :as ur]
    [galt.members.domain.member-repository :as mr]))

(defn show-profile-use-case
  [{:keys [user-repo member-repo]} {:keys [logged-in-user-id profile-user-id]}]
  (let [logged-in-user (ur/find-user-by-id user-repo logged-in-user-id)
        user (ur/find-user-by-id user-repo profile-user-id)
        member (mr/find-member-by-user-id member-repo profile-user-id)]

    (if (= logged-in-user user)
      (if member
        [:ok {:member member :user user}]
        [:ok {:user user}])
      [:error "You're not allowed to view this profile"])
    ))
