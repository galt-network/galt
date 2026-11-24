(ns galt.learn.adapters.curriculum
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]))

(defn load-curriculum
  []
  ;; shortcut: the curriculum file is parsed on every call, cache it when it grows
  (edn/read-string (slurp (io/resource "learn/curriculum.edn"))))

(defn lesson-href
  [module-slug lesson-slug]
  (str "/learn/" module-slug "/" lesson-slug))

(defn- lessons-in-order
  [curriculum]
  (for [{:keys [slug] :as module} (:modules curriculum)
        lesson (:lessons module)]
    (assoc lesson :module module :href (lesson-href slug (:slug lesson)))))

(defn find-lesson
  [curriculum module-slug lesson-slug]
  (let [following (->> (lessons-in-order curriculum)
                       (drop-while #(not= (lesson-href module-slug lesson-slug) (:href %)))
                       seq)]
    (when-let [lesson (first following)]
      (assoc lesson :next-lesson (some-> (second following) (select-keys [:title :href]))))))
