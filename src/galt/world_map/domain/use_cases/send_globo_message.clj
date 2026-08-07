(ns galt.world-map.domain.use-cases.send-globo-message
  "Host-originated messages through globo's validated pipeline
  (chat, object updates, ...). Passes through globo's result tuples:
  [:ok boolean] or [:error nil errors]."
  (:require
   [is.galt.globo.server :as globo]))

(defn send-globo-message-use-case
  [{:keys [globo]} message]
  (globo/send-message! globo message))
