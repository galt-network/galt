(ns galt.members.domain.use-cases.watch-lnurl-login)

(defn watch-lnurl-login-use-case
  [{:keys [read-session]} {:keys [session-id]}]
  (let [session (read-session session-id)]
    (if (nil? session)
      [:ok {:status :logged-out :message "User is logged out"}]
      (if (contains? session :lnurl-auth)
        (if (< (System/currentTimeMillis) (get-in session [:lnurl-auth :expires-at]))
          [:ok {:status :in-progress :message "LNURL-auth session is in progress"}]
          [:ok {:status :expired :message "LNURL-auth session expired. Try again"}])
        (if (not (nil? (get-in session [:user-id])))
          [:ok {:status :logged-in :message "User is logged in"}]
          [:error {:status :unknown :message "Shouldn't have gotten here"}])))))
