(ns galt.core.adapters.sse-helpers
  (:require
    [hiccup2.core :as h]
    [starfederation.datastar.clojure.api :as d*]
    [starfederation.datastar.clojure.api.common :as d*-common]
    [starfederation.datastar.clojure.adapter.http-kit :refer [->sse-response on-open]]
    [galt.core.infrastructure.web.helpers :refer [->json]]))

(defn send-html [sse hiccup]
  (d*/patch-elements! sse (-> hiccup (h/html ,,,) (str ,,,)))
  sse)

(defn send-signals [sse signals]
  (d*/patch-signals! sse (->json signals))
  sse)

(defn notification
  [text class]
  [:div#notification-container
   [:div {:class [:notification class] :data-class-is-visible "$notification-visible"}
    [:button.delete {:data-on-click "$notification-visible = false"}]
    [:p text]]])

(defn send-notification [sse message & [class]]
  ; TODO Figure out how to do this more cleanly with datastar
  (if (= class :is-danger)
    (send-signals sse {:notification-is-danger true})
    (send-signals sse {:notification-is-success true}))
  (send-signals sse {:notification-text message :notification-visible true})
  (Thread/sleep 5000)
  (send-signals sse {:notification-visible false}))

(defn send-cljs [sse forms]
  (let [script-id (str (random-uuid))
        run-scittle (str "scittle.core.eval_script_tags([document.getElementById('" script-id "')])")]
    (d*/execute-script! sse forms {d*-common/attributes {:type "application/x-scittle" :id script-id}
                                   d*-common/auto-remove false})
    (d*/execute-script! sse run-scittle)
    sse))

(defn push-state [url]
  (str "console.log('calling history.pushState', '" url "');"
       "history.pushState({url: '" url "'}, null, '" url "');"))

(defn send! [sse type data & [extra]]
  (case type
    :html (do
            (send-html sse data)
            (when extra (d*/execute-script! sse (push-state extra)))
            sse)
    :signals (send-signals sse data)
    :js (d*/execute-script! sse data)
    :cljs (send-cljs sse data)
    :notification (send-notification sse data extra)))

(defn with-sse
  "Passes the callback send-html function after SSE connection has been opened.
  send-html takes a vector of hiccup forms and uses datastar to patch respective
  elements in the browser (identified by element id).

  Allows sending HTML to front-end multiple times, not just one return value.

  Another benefit of using this is that the send-html can be passed to other layers,
  which avoids the tight coupling and source-code dependency on framework

  Example:

  (defn my-route-handler [req]
    (with-sse
      req
      (fn [send!]
        (send! :html [:div#my-id \"Starting some work\"])
        (send! :signals {:info-class \"is-hidden\"})
        ;; Do some more things
        (send! :html [:div#my-id \"Finished processing\"]))))"
  [req callback]
  (->sse-response
    req
    {on-open
     (fn [sse]
       ; Using future to do it in a separate thread, as the signal sender may sleep the thread to wait
       (future
         (callback (partial send! sse))
         ; TODO Figure out why this is necessary?
         ;      Without this the SSE connection closes before any signals are sent
         ;      It seems to depend on the time waited. With 1ms no events but with 10 they get sent
         (Thread/sleep 10)
         (d*/close-sse! sse))
       )}))

(defn close!
  [sse]
  (d*/close-sse! sse))

(defn ->sse-route-handler
  "Executes the passed in handler function only if and after a SSE connection has been opened.
  Expects the handler fn to return hiccup structure to be used for patching certain element in the DOM."
  [handler-fn]
  (fn [req]
    (->sse-response
     req
     {on-open
      (fn [sse] (d*/with-open-sse sse (send-html sse (handler-fn req))))})))
