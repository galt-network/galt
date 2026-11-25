(ns galt.core.infrastructure.import-cities-countries-test
  (:require
    [clojure.java.io :as io]
    [clojure.string :as str]
    [clojure.test :refer [deftest is testing]]
    [galt.core.infrastructure.db-migrations.import-cities-countries :as import]))

(deftest psql-only?-test
  (testing "lines only psql understands, or that fail without a postgres role"
    (is (true? (import/psql-only? "\\restrict abc")))
    (is (true? (import/psql-only? "\\unrestrict abc")))
    (is (true? (import/psql-only? "ALTER TABLE public.cities OWNER TO postgres;")))
    (is (true? (import/psql-only? "ALTER TABLE public.cities OWNER TO postgres;\r"))))
  (testing "SQL lines"
    (is (false? (import/psql-only? "INSERT INTO public.cities VALUES (1, 'OWNER TO;', 2);")))
    (is (false? (import/psql-only? "-- the importer drops the \\restrict lines")))
    (is (false? (import/psql-only? "ALTER TABLE ONLY public.cities")))))

(deftest statement-chunks-test
  (is (empty? (import/statement-chunks 3 [])))
  (is (= [["a;" "b;" "c;"] ["d;"]]
         (import/statement-chunks 3 ["a;" "b;" "c;" "d;"])))
  (testing "a statement split over lines is never cut"
    (is (= [["-- header" "ALTER TABLE ONLY t" "    ADD CONSTRAINT c FOREIGN KEY (a) REFERENCES x (id);"]
            ["SET x = 1;"]]
           (import/statement-chunks 1 ["-- header" "ALTER TABLE ONLY t"
                                       "    ADD CONSTRAINT c FOREIGN KEY (a) REFERENCES x (id);"
                                       "SET x = 1;"])))))

(deftest bundled-sample-test
  (with-open [lines (io/reader (io/resource import/sample))]
    (is (= 2933 (count (filter #(str/starts-with? % "INSERT INTO public.cities ") (line-seq lines)))))))
