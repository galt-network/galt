#!/usr/bin/env bb
;; Local regtest BTCPay Server for Galt development.
;;
;;   bb scripts/btcpay.bb up
;;   bb scripts/btcpay.bb status
;;   bb scripts/btcpay.bb fund
;;   bb scripts/btcpay.bb pay <bolt11>
;;   bb scripts/btcpay.bb logs [container]
;;   bb scripts/btcpay.bb down
;;   bb scripts/btcpay.bb reset
;;
;; Clones a pinned btcpayserver-docker tree into .btcpay/ (gitignored),
;; generates compose with podman (not build.sh — that calls Docker), and
;; starts a payer Core Lightning node on the same network. clj-contain does
;; not start this stack. See docs/btcpay.md.

(ns btcpay
  (:require
   [babashka.fs :as fs]
   [babashka.process :as p]
   [cheshire.core :as json]
   [clojure.string :as str]))

(def root (-> *file* fs/absolutize fs/parent fs/parent str))
(def sha "cc57300cfe516524588ac3771ab4d51fed1be369")
(def btcpay-dir (str (fs/path root ".btcpay")))
(def src-dir (str (fs/path btcpay-dir "src")))
(def project "galt-btcpay")
(def host-name "btcpay.galt.test")
(def http-port "49392")
(def url (str "http://" host-name ":" http-port))
(def payer "galt_payer_clightning")
(def payer-volume "galt_payer_clightning")
(def btcpay-container "generated_btcpayserver_1")
(def bitcoind "btcpayserver_bitcoind")
(def merchant "btcpayserver_clightning_bitcoin")
(def wallet "galt")

(def compose-env
  {"DOCKER_HOST" (str "unix:///run/user/"
                      (str/trim (:out (p/shell {:out :string :err :string} "id" "-u")))
                      "/podman/podman.sock")
   "COMPOSE_PROJECT_NAME" project
   "NBITCOIN_NETWORK" "regtest"
   "BTCPAY_PROTOCOL" "http"
   "BTCPAY_HOST" host-name
   "NOREVERSEPROXY_HTTP_PORT" http-port
   "BTCPAY_ENABLE_SSH" "false"})

(def payer-opt
  (str/join "\n" ["developer"
                  "bitcoin-datadir=/etc/bitcoin"
                  "bitcoin-rpcconnect=bitcoind"
                  "database-upgrade=true"
                  "rpc-file-mode=0666"
                  "funding-confirms=1"
                  "dev-fast-gossip"
                  "dev-bitcoind-poll=1"]))

(defn die [msg]
  (throw (ex-info msg {})))

(defn capture
  [args & {:keys [dir env]}]
  (let [opts (cond-> {:continue true :out :string :err :string}
               dir (assoc :dir dir)
               env (assoc :extra-env env))]
    (apply p/shell opts args)))

(defn capture!
  [args & {:keys [dir env]}]
  (let [{:keys [exit out err]} (capture args :dir dir :env env)]
    (when-not (zero? exit)
      (die (str "failed (" exit "): " (str/join " " args)
                (when-not (str/blank? err) (str "\n" err))
                (when-not (str/blank? out) (str "\n" out)))))
    (str/trim out)))

(defn run!
  [args & {:keys [dir env]}]
  (let [opts (cond-> {:inherit true}
               dir (assoc :dir dir)
               env (assoc :extra-env env))
        proc @(apply p/process opts args)]
    (when-not (zero? (:exit proc))
      (die (str "failed (" (:exit proc) "): " (str/join " " args))))))

(defn wait-for [label attempts f]
  (loop [n attempts]
    (if (zero? n)
      (die (str "timed out waiting for " label))
      (let [v (try (f) (catch Exception _ nil))]
        (if v
          v
          (do (Thread/sleep 3000)
              (recur (dec n))))))))

(defn running? [name]
  (let [{:keys [exit out]} (capture ["podman" "inspect" "-f" "{{.State.Status}}" name])]
    (and (zero? exit) (= "running" (str/trim out)))))

(defn listening? [port]
  (let [{:keys [exit out]} (capture ["ss" "-H" "-ltn" (str "sport = :" port)])]
    (and (zero? exit) (str/includes? out (str ":" port)))))

(defn check-hosts! []
  (let [{:keys [exit out]} (capture ["getent" "hosts" host-name])]
    (when-not (and (zero? exit) (re-find #"\b127\.0\.0\.1\b" out))
      (die (str host-name " does not resolve to 127.0.0.1.\n"
                "Add this line to /etc/hosts (this script will not write it):\n"
                "127.0.0.1 " host-name)))))

(defn check-polar! []
  (when (and (listening? "9735") (not (running? merchant)))
    (die "port 9735 is in use. Stop Polar, then retry.")))

(defn ensure-src! []
  (when-not (fs/exists? (fs/path src-dir ".git"))
    (println "cloning btcpayserver-docker" sha)
    (fs/create-dirs btcpay-dir)
    (run! ["git" "clone" "--filter=blob:none"
           "https://github.com/btcpayserver/btcpayserver-docker.git" src-dir]))
  (let [head (capture! ["git" "-C" src-dir "rev-parse" "HEAD"])]
    (when-not (str/starts-with? head sha)
      (println "checking out" sha)
      (run! ["git" "-C" src-dir "fetch" "--depth" "1" "origin" sha])
      (run! ["git" "-C" src-dir "checkout" "--detach" sha]))))

(defn generate! []
  (println "generating compose")
  (fs/create-dirs (fs/path src-dir "Generated"))
  (run! ["podman" "pull" "docker.io/btcpayserver/docker-compose-generator"])
  (run! ["podman" "run" "--rm"
         "-v" (str src-dir "/Generated:/app/Generated")
         "-v" (str src-dir "/docker-compose-generator/docker-fragments:/app/docker-fragments")
         "-v" (str src-dir "/docker-compose-generator/crypto-definitions.json:/app/crypto-definitions.json")
         "-e" "BTCPAYGEN_CRYPTO1=btc"
         "-e" "BTCPAYGEN_LIGHTNING=clightning"
         "-e" "BTCPAYGEN_REVERSEPROXY=none"
         "-e" "BTCPAYGEN_EXCLUDE_FRAGMENTS=opt-add-tor;btcpay-host"
         "docker.io/btcpayserver/docker-compose-generator"])
  (run! ["bash" "./generate-secrets.sh" "Generated/manifest.json"] :dir src-dir)
  (let [path (str (fs/path src-dir "Generated/docker-compose.generated.yml"))
        yml (-> (slurp path)
                (str/replace #"(?m)(image:\s+)(?!docker\.io/)([A-Za-z0-9][A-Za-z0-9._-]*/[^\s]+)"
                             "$1docker.io/$2")
                (str/replace #":-(?!docker\.io/)([A-Za-z0-9][A-Za-z0-9._-]*/[^\s}]+)"
                             ":-docker.io/$1")
                (str/replace #"(?m)^    links:\n(?:    - .+\n)+" ""))]
    (spit path yml)
    (when (str/includes? yml "docker.sock")
      (die "generated compose mounts docker.sock; refusing to start"))
    (when-not (str/includes? yml btcpay-container)
      (die "generated compose is missing the btcpayserver container"))))

(defn write-runtime! []
  (spit (str (fs/path btcpay-dir "runtime.env"))
        (str/join "\n" ["NBITCOIN_NETWORK=regtest"
                        "BTCPAY_PROTOCOL=http"
                        (str "BTCPAY_HOST=" host-name)
                        (str "NOREVERSEPROXY_HTTP_PORT=" http-port)
                        "BTCPAY_ENABLE_SSH=false"
                        (str "COMPOSE_PROJECT_NAME=" project)
                        "LIGHTNING_ALIAS="
                        "BTCPAY_ANNOUNCEABLE_HOST="
                        "BTCPAY_ROOTPATH="
                        ""]))
  (spit (str (fs/path btcpay-dir "dev.override.yml"))
        (str "services:\n"
             "  btcpayserver:\n"
             "    environment:\n"
             "      BTCPAY_DISABLE_REGISTRATION: \"false\"\n"
             "      BTCPAY_DISABLESSRFPROTECTION: \"true\"\n")))

(defn compose-args [extra]
  (into ["podman" "compose"
         "-p" project
         "--project-directory" src-dir
         "-f" (str src-dir "/Generated/docker-compose.generated.yml")
         "-f" (str btcpay-dir "/dev.override.yml")
         "--env-file" (str btcpay-dir "/runtime.env")]
        extra))

(defn stack-network []
  (let [raw (capture! ["podman" "inspect" "--format" "{{json .NetworkSettings.Networks}}" btcpay-container])
        names (keys (json/parse-string raw))]
    (or (some #(when (str/includes? % project) %) names)
        (first names)
        (die "btcpayserver container has no network"))))

(defn bitcoin-volume []
  (let [names (str/split-lines (capture! ["podman" "volume" "ls" "--format" "{{.Name}}"]))
        hit (first (filter #(and (str/includes? % project)
                                 (str/includes? % "bitcoin_datadir")
                                 (not (str/includes? % "clightning")))
                           names))]
    (or hit (die (str "bitcoin datadir volume not found in " (pr-str names))))))

(defn lightning-image []
  (capture! ["podman" "inspect" "--format" "{{.ImageName}}" merchant]))

(defn ensure-payer! []
  (let [state (let [{:keys [exit out]} (capture ["podman" "inspect" "-f" "{{.State.Status}}" payer])]
                (when (zero? exit) (str/trim out)))]
    (cond
      (= "running" state) (println "payer already running")
      (some? state) (do (println "starting existing payer")
                        (run! ["podman" "start" payer]))
      :else
      (do (println "starting payer")
          (run! ["podman" "run" "-d"
                 "--name" payer
                 "--network" (stack-network)
                 "--restart" "unless-stopped"
                 "-v" (str payer-volume ":/root/.lightning")
                 "-v" (str (bitcoin-volume) ":/etc/bitcoin")
                 "-e" "LIGHTNINGD_NETWORK=regtest"
                 "-e" "LIGHTNINGD_CHAIN=btc"
                 "-e" (str "LIGHTNINGD_OPT=" payer-opt)
                 (lightning-image)])))))

(defn probe-dns! []
  (let [existed? (running? "galt-dev")
        image (lightning-image)]
    (when-not existed?
      (run! ["podman" "run" "-d" "--name" "galt-dev"
             "--network" (stack-network)
             "--entrypoint" "sleep" image "120"]))
    (try
      (let [{:keys [exit out err]} (capture ["podman" "exec" btcpay-container "getent" "hosts" "galt-dev"])]
        (if (zero? exit)
          (println "DNS ok:" (str/trim out))
          (println "DNS miss: BTCPay cannot resolve galt-dev\n" (str/trim err))))
      (finally
        (when-not existed?
          (capture ["podman" "rm" "-f" "galt-dev"]))))))

(defn up! []
  (check-hosts!)
  (check-polar!)
  (ensure-src!)
  (generate!)
  (write-runtime!)
  (println "starting stack")
  (run! (compose-args ["up" "-d"]) :env compose-env)
  (when-not (running? btcpay-container)
    (die "btcpayserver is not in podman. compose may have hit the Docker daemon."))
  (ensure-payer!)
  (println "waiting for" url)
  (try
    (wait-for "btcpay http" 90
              #(zero? (:exit (capture ["curl" "-fsS" "-o" "/dev/null" "-m" "5" (str url "/")]))))
    (catch Exception e
      (println (capture ["podman" "logs" "--tail" "80" btcpay-container]))
      (throw e)))
  (probe-dns!)
  (println)
  (println "BTCPay" url)
  (println "Register, create a store, create an API key with btcpay.store.cancreateinvoice.")
  (println "Put BTCPAY_API_KEY in .env. Do not put URLs in .env.")
  (println "clj-contain down does not stop this stack. bb scripts/btcpay.bb down does."))

(defn down! []
  (capture ["podman" "rm" "-f" payer])
  (when (fs/exists? (fs/path src-dir "Generated/docker-compose.generated.yml"))
    (run! (compose-args ["down"]) :env compose-env))
  (println "stopped (volumes kept)"))

(defn reset! []
  (capture ["podman" "rm" "-f" payer])
  (when (fs/exists? (fs/path src-dir "Generated/docker-compose.generated.yml"))
    (run! (compose-args ["down" "-v"]) :env compose-env))
  (capture ["podman" "volume" "rm" "-f" payer-volume])
  (println "removed stack and volumes"))

(defn status! []
  (doseq [line (str/split-lines (capture! ["podman" "ps" "-a" "--format" "{{.Names}}\t{{.Status}}\t{{.Ports}}"]))
          :when (or (str/includes? line "btcpay")
                    (str/includes? line "galt_payer")
                    (str/includes? line "galt-dev"))]
    (println line))
  (let [{:keys [exit]} (capture ["curl" "-fsS" "-o" "/dev/null" "-m" "5" (str url "/")])]
    (println (if (zero? exit) (str "http ok " url) (str "http down " url)))))

(defn logs! [name]
  (run! ["podman" "logs" "--tail" "80" (or name btcpay-container)]))

(defn bcli [& args]
  (capture! (into ["podman" "exec" bitcoind "bitcoin-cli" "-datadir=/data"] args)))

(defn bcli-json [& args]
  (json/parse-string (apply bcli args) true))

(def rpc-files (atom {}))

(defn rpc-file [container]
  (or (@rpc-files container)
      (let [candidates ["/root/.lightning/lightning-rpc"
                        "/root/.lightning/regtest/lightning-rpc"]
            found (or (some (fn [path]
                              (when (zero? (:exit (capture ["podman" "exec" container "test" "-S" path])))
                                path))
                            candidates)
                      (first (str/split-lines
                              (capture! ["podman" "exec" container "sh" "-c"
                                         "find /root/.lightning -name lightning-rpc -print -quit"]))))]
        (when (str/blank? found)
          (die (str "no lightning-rpc in " container)))
        (swap! rpc-files assoc container found)
        found)))

(defn lcli [container & args]
  (capture! (into ["podman" "exec" container "lightning-cli" "--rpc-file" (rpc-file container)] args)))

(defn lcli-json [container & args]
  (json/parse-string (apply lcli container args) true))

(defn msat [v]
  (cond
    (number? v) (long v)
    (string? v) (parse-long (str/replace v #"msat$" ""))
    :else 0))

(defn confirmed-sats [container]
  (reduce + 0
          (for [{:keys [status amount_msat]} (:outputs (lcli-json container "listfunds"))
                :when (= "confirmed" status)]
            (quot (msat amount_msat) 1000))))

(defn ensure-wallet! []
  (let [loaded (set (bcli-json "listwallets"))]
    (cond
      (loaded wallet) nil
      (some #(= wallet (:name %)) (:wallets (bcli-json "listwalletdir")))
      (bcli "loadwallet" wallet)
      :else (bcli "createwallet" wallet))))

(defn mine! [n]
  (let [addr (bcli (str "-rpcwallet=" wallet) "getnewaddress")]
    (bcli (str "-rpcwallet=" wallet) "generatetoaddress" (str n) addr)
    (println "mined" n "blocks, height" (bcli-json "getblockcount"))))

(defn ensure-height! []
  (let [h (bcli-json "getblockcount")]
    (when (< h 101)
      (mine! (- 101 h)))))

(defn ensure-onchain! [container]
  (when (< (confirmed-sats container) 100000)
    (let [addr (let [{:keys [bech32 p2tr]} (lcli-json container "newaddr")]
                 (or bech32 p2tr))]
      (println "funding" container addr)
      (bcli (str "-rpcwallet=" wallet) "-named" "sendtoaddress"
            (str "address=" addr) "amount=1" "fee_rate=1")
      (mine! 1)
      (wait-for (str container " on-chain funds") 40
                #(when (>= (confirmed-sats container) 100000) true)))))

(defn merchant-id []
  (:id (lcli-json merchant "getinfo")))

(defn peer-host []
  (if (zero? (:exit (capture ["podman" "exec" payer "getent" "hosts" "clightning_bitcoin"])))
    "clightning_bitcoin"
    merchant))

(defn channel-state [mid]
  (some (fn [{:keys [peer_id state]}]
          (when (= mid peer_id) state))
        (:channels (lcli-json payer "listfunds"))))

(defn ensure-channel! []
  (let [mid (merchant-id)]
    (when-not (#{"CHANNELD_NORMAL" "CHANNELD_AWAITING_LOCKIN"} (channel-state mid))
      (println "opening channel to" mid)
      (capture ["podman" "exec" payer "lightning-cli" "--rpc-file" (rpc-file payer)
                "connect" (str mid "@" (peer-host) ":9735")])
      (lcli payer "fundchannel" mid "50000000")
      (mine! 6))
    (wait-for "channel active" 40
              #(when (= "CHANNELD_NORMAL" (channel-state mid)) true))
    (println "channel active to" mid)))

(defn fund! []
  (wait-for "bitcoind" 40 #(try (bcli "getblockcount") true (catch Exception _ nil)))
  (ensure-wallet!)
  (ensure-height!)
  (wait-for "merchant lightning" 90 #(try (lcli merchant "getinfo") true (catch Exception _ nil)))
  (when-not (running? payer)
    (ensure-payer!))
  (wait-for "payer lightning" 60 #(try (lcli payer "getinfo") true (catch Exception _ nil)))
  (ensure-onchain! merchant)
  (ensure-onchain! payer)
  (ensure-channel!)
  (println "funded"))

(defn pay! [bolt11]
  (when (str/blank? bolt11)
    (die "usage: bb scripts/btcpay.bb pay <bolt11>"))
  (println (lcli payer "pay" bolt11)))

(defn help []
  (println "Usage: bb scripts/btcpay.bb <up|down|reset|status|fund|pay|logs>"))

(defn -main [& args]
  (try
    (case (first args)
      "up" (up!)
      "down" (down!)
      "reset" (reset!)
      "status" (status!)
      "fund" (fund!)
      "pay" (pay! (second args))
      "logs" (logs! (second args))
      (help))
    (catch Exception e
      (binding [*out* *err*]
        (println (or (ex-message e) (str e))))
      (System/exit 1))))

(when (= *file* (System/getProperty "babashka.file"))
  (apply -main *command-line-args*))
