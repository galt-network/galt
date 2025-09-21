(ns galt.payments.adapters.cln-gateway-http-responses)

(def invoices
  {:opts
   {:headers
    {"Rune" "A4sZXhnJuDkHNjGzEyzzm48Ki61GFff96cWRa1-EjKo9MA==",
     "Content-Type" "application/json"},
    :body "{\"label\":\"test-invoice-2\"}",
    :method :post,
    :url "http://localhost:8184/v1/listinvoices"},
   :body
   "{\"invoices\":[{\"amount_msat\":10123,\"amount_received_msat\":10123,\"bolt11\":\"lnbcrt101230p1p5vshxxsp5pfyc9xjey2e94a2qqe4x6he7pm4urqm09hzxczlth25ug9y79csspp5zngza58e052ym030glujf74ddykvp95ep8rzg945m4aq505ggh5qdpqgaq5c4pqd4jk6cn9wfeks6tsyp6x2um5xqyjw5qcqp29qxpqysgq97m36xew2er4dssxn8qqj2vz862h0dysz97zfr7pknyff783vdrz3p3sct96ae5l2aq4ex4fj9y99gy9spglvcvth28gm05xmjyj32sps3t3zp\",\"created_index\":6,\"description\":\"GALT membership test\",\"expires_at\":1758566214,\"label\":\"test-invoice-2\",\"paid_at\":1757961456,\"pay_index\":5,\"payment_hash\":\"14d02ed0f97d144dbe2f47f924faad692cc0969909c62416b4dd7a0a3e8845e8\",\"payment_preimage\":\"2e548c3a402368e5d8ba61166d600e713bc08182e4f648ae91269d56c9843fb8\",\"status\":\"paid\",\"updated_index\":5}]}",
   :headers
   {:access-control-allow-origin "*",
    :content-length "714",
    :content-security-policy
    "default-src 'self'; font-src 'self'; img-src 'self' data:; frame-src 'self'; style-src 'self' 'unsafe-inline'; script-src 'self' 'unsafe-inline';",
    :content-type "application/json",
    :date "Sat, 20 Sep 2025 13:25:28 GMT",
    :vary
    "origin, access-control-request-method, access-control-request-headers"},
   :status 201})
