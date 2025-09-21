(ns galt.payments.domain.use-cases.cln-payment-gateway-responses)

(def invoice-response
  {:bolt-11
   "lnbcrt1u1p5vkcw7sp5gqfrqm7ymn27melkayrqz8rx3xm0096srms8u5jkjmwk4jc590cspp5cypf5e505mxh25nsssl2kp2e4axg5hxwlm3y5zlv0yqpa4jj92qsdq2fpjkcmr0yyxqyjw5qcqp29qxpqysgqdmdhsey8wgdpvut75df6ks6z6mnvqz80g6craeqg6kr7l9qjl5trtqmt70w8amjpcw32efvmvx0cta0tgagdf5wdt9jz9840t9vxnygqnj9dfa",
   :warning-deadends
   "Insufficient incoming capacity, once dead-end peers were excluded",
   :expires-at 1758764126,
   :payment-hash
   "c1029a668fa6cd755270843eab0559af4c8a5ccefee24a0bec79001ed6522a81",
   :payment-secret
   "4012306fc4dcd5ede7f6e906011c6689b6f797501ee07e525696dd6acb142bf1",
   :created-index 10})

(def invoices-response
  [{:bolt-11
    "lnbcrt100n1p5vsk5csp5u0s0lr9s8cuekwqwu6nmzx8k0wm0dk4v2a7u3x7lcdktu5wpa5qqpp52ld6z72zm0w96jtltauhmgxltzfjg2ep32jpt4x7nhphgm79dg5qdqlgaq5c4pqd4jk6cn9wfeks6tsypnx2egxqyjw5qcqp29qxpqysgq780hyec54502qn9vynnugctynq0ac30dmggqzdwdcpev4z2p9r43gdwzm8w9gyu22r6xdsrs50ma2pzggmaufgty54d3e980vtvhmacqtyrdy3",
    :label "test-invoice-1",
    :amount-msat 10000,
    :expires-at 1758565656,
    :payment-hash
    "57dba17942dbdc5d497f5f797da0df5893242b218aa415d4de9dc3746fc56a28",
    :status "unpaid",
    :created-index 5,
    :description "GALT membership fee"}
   {:amount-received-msat 10123,
    :description "GALT membership test",
    :paid-at 1757961456,
    :expires-at 1758566214,
    :updated-index 5,
    :pay-index 5,
    :payment-preimage
    "2e548c3a402368e5d8ba61166d600e713bc08182e4f648ae91269d56c9843fb8",
    :amount-msat 10123,
    :payment-hash
    "14d02ed0f97d144dbe2f47f924faad692cc0969909c62416b4dd7a0a3e8845e8",
    :status "paid",
    :label "test-invoice-2",
    :bolt-11
    "lnbcrt101230p1p5vshxxsp5pfyc9xjey2e94a2qqe4x6he7pm4urqm09hzxczlth25ug9y79csspp5zngza58e052ym030glujf74ddykvp95ep8rzg945m4aq505ggh5qdpqgaq5c4pqd4jk6cn9wfeks6tsyp6x2um5xqyjw5qcqp29qxpqysgq97m36xew2er4dssxn8qqj2vz862h0dysz97zfr7pknyff783vdrz3p3sct96ae5l2aq4ex4fj9y99gy9spglvcvth28gm05xmjyj32sps3t3zp",
    :created-index 6}
 {  :bolt-11
    "lnbcrt1u1p5v4n7xsp5m8wqndedyvghzpa7frl85yxpp4fd9v7ellakfm3hqpjgrrz9f5uqpp5ucmqrywuva308q7v0qzuxatdchygqqlr5tfdyffu6jsl6rvd5znsdq2fpjkcmr0yyxqyjw5qcqp29qxpqysgqmvkt5nlxzumhfdhglmjasyjfj7fc2rw9xclu542ca0tp84qfyatymfe5guz73ttm386unmtev7zpucf5kgdyzhtn4c3tz803axzwgagqu6yndl",
    :label "pretty-unique2",
    :amount-msat 100000,
    :expires-at 1758726726,
    :payment-hash
    "e6360191dc6762f383cc7805c3756dc5c88003e3a2d2d2253cd4a1fd0d8da0a7",
    :status "unpaid",
    :created-index 9,
    :description "Hello!"}
 {  :bolt-11
    "lnbcrt1u1p5vkcw7sp5gqfrqm7ymn27melkayrqz8rx3xm0096srms8u5jkjmwk4jc590cspp5cypf5e505mxh25nsssl2kp2e4axg5hxwlm3y5zlv0yqpa4jj92qsdq2fpjkcmr0yyxqyjw5qcqp29qxpqysgqdmdhsey8wgdpvut75df6ks6z6mnvqz80g6craeqg6kr7l9qjl5trtqmt70w8amjpcw32efvmvx0cta0tgagdf5wdt9jz9840t9vxnygqnj9dfa",
    :label "pretty-unique3",
    :amount-msat 100000,
    :expires-at 1758764126,
    :payment-hash
    "c1029a668fa6cd755270843eab0559af4c8a5ccefee24a0bec79001ed6522a81",
    :status "unpaid",
    :created-index 10,
    :description "Hello!"}
 {  :bolt-11
    "lnbcrt9600n1p5vkercsp5za33vl2e638z4k3e589hkm8gxc4txmsjxtte2ty5ch3zewr08z9spp5m9lc2emja9373ndenndznsz60pn4zxvdp86x6e4jj7guwcm9357sdp9gaskcapqd4jk6cn9wfeks6tsypcxz7tdv4h8gxqyjw5qcqp29qxpqysgqyhfndjfveh3uuj4903xapegyvfv3gwvztyjwl3d084s7jh4z4h934xhr3e0efgr47uqvkgj4y3a6q66eqv6vncza8v4tdrjfm8z46hgpm4ttt5",
    :label "membership-payment-",
    :amount-msat 960000,
    :expires-at 1758764792,
    :payment-hash
    "d97f856772e963e8cdb99cda29c05a786751198d09f46d66b29791c763658d3d",
    :status "unpaid",
    :created-index 11,
    :description "Galt membership payment"}])
