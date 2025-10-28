(ns galt.core.adapters.view-models
  (:require
    [galt.members.domain.user-repository :refer [find-user-by-id]]
    [galt.members.domain.member-repository :as mr]
    [clojure.string :as str]))

(defn navbar-model
  [{:keys [user-repo member-repo]} req]
  (let [route-id (get-in req [:reitit.core/match :data :id])
        user-id (get-in req [:session :user-id])
        user (find-user-by-id user-repo user-id)
        member (mr/find-member-by-user-id member-repo user-id)
        avatar (get member :avatar "/assets/images/profile-user-account.svg")
        name (or (get member :name) (get user :name) "Login")
        logged-in? (not (nil? user))
        login-user {:name name
                    :avatar avatar
                    :href (if logged-in? "/members/profile/me" "/members/login")}
        login-item {:id :login :title "Log in" :href "/members/login"}
        logout-item {:id :logout :title "Log out" :data-on:click "@post('/members/logout')"}
        session-item (if logged-in? logout-item login-item)]
    {:user login-user
     :items [{:title "Members" :href "/members" :selected? (= :members route-id) }
             {:title "Groups" :href "/groups" :selected? (= :groups route-id)}
             {:title "Posts" :href "/posts" :selected? (= :posts route-id)}
             {:title "Events" :href "/events" :selected? (= :events route-id)}]
     :dropdown [{:id :profile :title "Profile" :href "/members/me" :selected? (= :profile route-id)}
                {:id :invitations :title "Invitations" :href "/invitations" :selected? (= :invitations route-id)}
                session-item]}))

(defn layout-model
  "Takes route dependencies, request object and content hiccup vector
  Returns a map suitable for rendering the page layout"
  [deps req]
  (let [title-source-from-route (or
                                  (get-in req [:reitit.core/match :data :id])
                                  (get-in req [:reitit.core/match :data :name]))
        page-title (str/capitalize (name title-source-from-route))]
    {:page-title page-title
     :path (get-in req [:reitit.core/match :path])
     :navbar (navbar-model deps req)
     :head-tags []
     :content [:div "Empty"]}))
