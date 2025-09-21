(ns galt.payments.adapters.cln-gateway-responses)

(def create-invoice
  {:bolt-11
 "lnbcrt9600n1p5v7pf4sp54gzmrra3x0z4y3vdzszdsazc4dwq258d6qe74dlg0rlushynfcsqpp59qstp0j35l35ut5pvag42njs8qyaautel40s4q5xkrf73fa5asqqdr8gaskcapqd4jk6cn9wfeks6tsypcxz7tdv4h8ggrxdaezqvp38yun2drrxykkyceexcknwvpkvsknsdtzxgkngc3evgcrzef38ymkxdcxqyjw5qcqp29qxpqysgqklxtz8rxveqj4l3vzduzlpt6fekdl9xwte4h9ky56xhlrzvhr6qymzfppxsdp7wk653gqwmr5frptwpc7tz89gz35pwr0g3z0624m3sq5mt9ag",
 :warning-deadends
 "Insufficient incoming capacity, once dead-end peers were excluded",
 :expires-at 1759002549,
 :payment-hash
 "2820b0be51a7e34e2e816751554e503809def179fd5f0a8286b0d3e8a7b4ec00",
 :payment-secret
 "aa05b18fb133c552458d1404d87458ab5c0550edd033eab7e878ffc85c934e20",
 :created-index 16})

(def responses
  {:invoice-raw create-invoice
   :invoice (select-keys create-invoice [:bolt-11 :expires-at :payment-hash :payment-secret :created-index])})
