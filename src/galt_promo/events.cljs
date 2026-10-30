(ns galt-promo.events
  (:require [re-frame.core :as rf]))

(rf/reg-event-db
 ::initialize
 (fn [_ _]
   {:users []
    :connection :connecting}))

(rf/reg-event-db
 ::users-received
 (fn [db [_ users]]
   (assoc db :users users)))

(rf/reg-event-db
 ::connection-changed
 (fn [db [_ status]]
   (assoc db :connection status)))

(rf/reg-sub ::users (fn [db] (:users db)))
(rf/reg-sub ::connection (fn [db] (:connection db)))
