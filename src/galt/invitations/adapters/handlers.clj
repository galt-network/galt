(ns galt.invitations.adapters.handlers
  (:require
    [galt.invitations.adapters.presentation :as presentation]))

(defn new-invitation
  [{:keys [render layout]} req]
  (let [model {}]
    ; TODO: implement fuzzy group and member search (members need repository)
    ;       This handler function needs to distinguish between SSE and normal
    ;       In the SSE case it'll send the search dropdown hiccup vectors
    {:status 200 :body (render (layout {:content (presentation/present model)
                                        :page-title "New Invitation"}))}))
