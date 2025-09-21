(ns galt.members.domain.use-cases.start-lnurl-login
  (:require
    [ring.middleware.session.store :as ss]))

(defn start-lnurl-login-use-case
  [{:keys [generate-lnurl session-store]} {:keys [session-id callback-path]}]
  (let [lnurl-result (generate-lnurl callback-path {:galt-session-id session-id})
        {:keys [lnurl k1-hex]} lnurl-result
        ten-minutes (* 1000 60 10)
        in-ten-minutes (+ (System/currentTimeMillis) ten-minutes)
        original-session (ss/read-session session-store session-id)
        updated-session (merge original-session {:lnurl-auth {:k1 k1-hex :expires-at in-ten-minutes}})]
    (ss/write-session session-store session-id updated-session)
    [:ok lnurl]))
