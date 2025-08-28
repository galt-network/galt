(ns galt.core.adapters.view-models
  (:require
    [galt.members.domain.user-repository :refer [find-user-by-id]]
    [clojure.string :as str]))

(defn navbar-model
  [{:keys [user-repo]} req]
  (let [route-id (get-in req [:reitit.core/match :data :id])
        user-id (get-in req [:session :user-id])
        user (find-user-by-id user-repo user-id)
        logged-in? (not (nil? user))
        login-user {:name (get user :users/name "Login")
                    :avatar "https://avatars1.githubusercontent.com/u/7221389?v=4&s=32"
                    :href (if logged-in? "/members/profile/me" "/members/login")}
        login-item {:id :login :title "Log in" :href "/members/login"}
        logout-item {:id :logout :title "Log out" :data-on-click "@post('/members/logout')"}
        session-item (if logged-in? logout-item login-item)]
    {:user login-user
     :items [{:title "Members" :href "/members" :selected? (= :members route-id) }
             {:title "Groups" :href "/groups" :selected? (= :groups route-id)}
             {:title "Posts" :href "#" :selected? (= :posts route-id)}
             {:title "Inititatives" :href "#" :selected? (= :initiatives route-id)}]
     :dropdown [{:id :profile :title "Profile" :href "/members/me" :selected? (= :profile route-id)}
                session-item]}))

(defn layout-model
  "Takes route dependencies, request object and content hiccup vector
  Returns a map suitable for rendering the page layout"
  [deps req]
  (let [page-title (str (str/capitalize (name (get-in req [:reitit.core/match :data :id]))) " | Galt" )]
    {:page-title page-title
     :path (get-in req [:reitit.core/match :path])
     :navbar (navbar-model deps req)
     :content [:div "Empty"]}))
