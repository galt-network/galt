(ns galt.design.handlers
  (:require
   [clojure.string :as str]
   [galt.core.views.landing-page :as l-views]
   [galt.groups.adapters.handlers :refer [head-tags-for-maps]]
   [galt.groups.adapters.views :as g-views]
   [galt.groups.adapters.presentation.show-group :as presentation.show-group]
   [galt.locations.domain.location-repository :as lr]))

(def landing-model
  {})

(defn landing
  [{:keys [layout render]} req]
  {:status 200 :body (-> landing-model l-views/page layout render)})

(def edit-model
  {:group {:id "abc123"
           :name "Caballeros Ancap de Córdoba"
           :description (-> "El liberalismo es el respeto irrestricto del proyecto de vida del prójimo,
                            basado en el principio de no agresión y en defensa del derecho a la vida, a
                            la libertad y a la propiedad."
                            (str/replace ,,, #"\s+" " "))}
   :location {:id 1
              :name "Famous place nobody knows"
              :latitude -31.4135
              :longitude -64.18105
              :country-code "AR"
              :city-id 815}
   :form {:action-name "Save"
          :action-method "PUT"
          :action-target "#"
          :delete-action ""}})

(defn edit-group
  [{:keys [layout render location-repo]} req]
  (let [model (merge edit-model {:countries (lr/all-countries location-repo)})]
    {:status 200 :body (render (layout {:content (g-views/edit-group model)
                                        :head-tags head-tags-for-maps}))}))

(def show-model
  {:id "abc123"
   :name "Caballeros Ancap de Córdoba"
   :avatar "https://avatars.githubusercontent.com/u/52789443?s=280&v=4"
   :languages ["English" "Spanish"]
   :location-name "Famous place nobody knows"
   :latitude -31.4135
   :longitude -64.18105
   :founded-at "9 months ago (2024-01-01)"
   :members [{:name "Siimar Sapikas" :href "#"} {:name "Milton Friedman" :href "#"}]
   :description (-> "El liberalismo es el respeto irrestricto del proyecto de vida del prójimo,
                    basado en el principio de no agresión y en defensa del derecho a la vida, a
                    la libertad y a la propiedad."
                    (str/replace ,,, #"\s+" " "))
   :activity
   [{:author "Milton Friedman" :slug "@mfriedman" :content "Just published a book: Capitalism and Freedom"}
    {:author "Thomas Sowell" :slug "@sowell" :content "If the socialists knew economics, they wouldn't be socialists"}]})

(defn show-group
  [{:keys [layout render]} req]
  {:status 200 :body (render (layout {:content (presentation.show-group/present show-model)
                                      :head-tags head-tags-for-maps}))})
