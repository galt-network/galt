(ns galt-promo.names)

(def dictionary
  [{:initial "A" :adjectives ["Amber" "Agile" "Ancient"] :nouns ["Alpaca" "Antelope" "Archer"]}
   {:initial "B" :adjectives ["Brave" "Bright" "Bold"] :nouns ["Badger" "Beaver" "Bison"]}
   {:initial "C" :adjectives ["Calm" "Clever" "Cosmic"] :nouns ["Cougar" "Coyote" "Crane"]}
   {:initial "D" :adjectives ["Daring" "Dapper" "Dreamy"] :nouns ["Dolphin" "Dragon" "Duck"]}
   {:initial "E" :adjectives ["Eager" "Electric" "Elegant"] :nouns ["Eagle" "Echidna" "Elk"]}
   {:initial "F" :adjectives ["Fierce" "Friendly" "Frosty"] :nouns ["Falcon" "Fox" "Frog"]}
   {:initial "G" :adjectives ["Gentle" "Golden" "Grand"] :nouns ["Gazelle" "Gecko" "Goose"]}
   {:initial "H" :adjectives ["Happy" "Humble" "Hazy"] :nouns ["Hawk" "Heron" "Hippo"]}])

(defn- random-index [random n]
  (js/Math.floor (* (random) n)))

(defn generate-name [taken random]
  (let [{:keys [adjectives nouns]} (nth dictionary (random-index random (count dictionary)))
        candidates (vec (for [adjective adjectives noun nouns]
                          (str adjective " " noun)))
        offset (random-index random (count candidates))
        ordered (concat (drop offset candidates) (take offset candidates))]
    (or (first (remove taken ordered))
        (throw (ex-info "No unique names remain" {:taken (count taken)})))))
