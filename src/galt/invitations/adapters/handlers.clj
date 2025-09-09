(ns galt.invitations.adapters.handlers
  (:require
   [galt.core.adapters.link-generator :refer [link-for-route]]
   [galt.invitations.adapters.presentation :as presentation]))

(defn new-invitation
  [{:keys [render layout]} req]
  (let [
        model {}]
    ; TODO: implement fuzzy group and member search (members need repository)
    ;       This handler function needs to distinguish between SSE and normal
    ;       In the SSE case it'll send the search dropdown hiccup vectors
    {:status 200 :body (render (layout {:content (presentation/present model)
                                        :page-title "New Invitation"}))}))

(defn create-invitation
  [{:keys [create-invitation-use-case]} req]
  (let [from-user-id (get-in req [:session :user-id])
        to-member-id (get-in req [:params :member-id])
        to-group-id (get-in req [:params :group-id])
        content (get-in req [:params :content])
        [status result] (create-invitation-use-case {:from-user-id from-user-id
                                                     :to-member-id to-member-id
                                                     :to-group-id to-group-id
                                                     :content content})]
    (case status
      :error {:status 500 :body (str "No can do:" result)}
      :ok {:status 303 :headers {"Location" (link-for-route req :invitations)}})))

(defn list-invitations
  [{:keys [render layout]} req]
  {:status 200 :body (render (layout [:h1.title.is-2 "Your Invitation Requests:"])) })
