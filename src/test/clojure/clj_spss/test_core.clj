(ns clj-spss.test-core
  (:use [clj-spss.core])
  (:use [clojure.core.matrix])
  (:use [clojure.test]))

(deftest test-load
  (let [ds (load-spss "src/test/resources/org/opendatafoundation/data/spss/DatabaseTest.sav")]
    (is (= [200 11] (shape ds)))))