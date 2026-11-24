(ns galt.learn.adapters.curriculum-test
  (:require [clojure.test :refer [deftest is]]
            [galt.learn.adapters.curriculum :as curriculum]))

(deftest curriculum-data-is-consistent
  (let [modules (:modules (curriculum/load-curriculum))
        lessons (mapcat :lessons modules)]
    (is (= 3 (count modules)))
    (is (= 9 (count lessons)))
    (is (every? #(and (:slug %) (:title %)) modules))
    (is (every? #(and (:slug %) (:title %) (:status %)) lessons))
    (is (every? :summary (remove #(= :locked (:status %)) lessons)))
    (is (= (count modules) (count (set (map :slug modules)))))
    (is (= (count lessons) (count (set (map :slug lessons)))))
    (is (= (count lessons) (count (set (map :title lessons)))))))

(deftest lesson-href-joins-the-slugs
  (is (= "/learn/praxeology/value-theory" (curriculum/lesson-href "praxeology" "value-theory"))))

(deftest find-lesson-returns-lesson-with-navigation
  (let [lesson (curriculum/find-lesson (curriculum/load-curriculum) "praxeology" "value-theory")]
    (is (= "Intermediate Austrian Economics" (get-in lesson [:module :title])))
    (is (= "/learn/praxeology/value-theory" (:href lesson)))
    (is (= {:title "The Economic Calculation Problem" :href "/learn/praxeology/economic-calculation"}
           (:next-lesson lesson)))))

(deftest last-lesson-has-no-next-lesson
  (is (nil? (:next-lesson (curriculum/find-lesson (curriculum/load-curriculum)
                                                  "legal-theory" "polycentric-law")))))

(deftest unknown-lesson-is-nil
  (let [curriculum (curriculum/load-curriculum)]
    (doseq [[module-slug lesson-slug] [["nope" "nope"] ["praxeology" "sound-money"] [nil nil]]]
      (is (nil? (curriculum/find-lesson curriculum module-slug lesson-slug))))))
