(ns galt.payments.adapters.new-payment-test
  (:require
    [clojure.string :as str]
    [clojure.test :refer [deftest is]]
    [galt.core.infrastructure.web.helpers :refer [render-html]]
    [galt.payments.adapters.presentation.new-payment :as presentation]))

(deftest present-listens-for-signal-patch
  (let [html (render-html (presentation/present {:invoice-status "unpaid"
                                                 :datastar-action "@post('/datastar-sse?connection-id=lab')"
                                                 :return-to "/members/me"
                                                 :invoice-url "lnbc"}))]
    (is (str/includes? html "data-on-signal-patch"))
    (is (str/includes? html "data-init"))
    (is (str/includes? html "return-to="))
    (is (not (str/includes? html "data-on:signal-patch")))
    (is (not (str/includes? html "data-on:load")))
    (is (not (str/includes? html "signal-patch-filters")))))
