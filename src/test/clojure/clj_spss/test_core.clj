(ns clj-spss.test-core
  (:use [clj-spss.core])
  (:require [clojure.core.matrix :as m])
  (:require [clojure.string :as str])
  (:require [clojure.core.matrix])
  (:use [clojure.test]))

(deftest test-load
  (testing "regular file path"
    (let [sf (load-spssfile "src/test/resources/org/opendatafoundation/data/spss/DatabaseTest.sav")
          ds (dataset-from-spss sf)]
      (is (= [200 11] (m/shape ds)))
      (is (= 50.0 (m/mget ds 6 6) (get-value sf 6 6))))))

;(deftest test-strings
;  (let [ds (load-spss "src/test/resources/org/opendatafoundation/data/spss/VeryLongStringVariables.sav")]
;    (is (= [200 11] (shape ds)))))

(deftest test-lengths
  (let [vi (variable-info "src/test/resources/org/opendatafoundation/data/spss/VeryLongStrings.sav")]
    (is (= [ 6, 255, 255, 255, 8, 10, 1430, 1430, 6, 1430] (map :length vi)))))

(deftest test-number 
  (let [sf (load-spssfile "src/test/resources/org/opendatafoundation/data/spss/TestNumber.sav")
        v (variable sf "HEIGHT")]
    (is (= 1 (get-value sf 0 0))) ;; IID
    (is (= 1 (get-value sf "WEIGHT" 0))) ;; WEIGHT
    (is (= 137.34 (get-value v 0)))
    (is (= 123456789.34 (get-value sf 1 6)))
    (is (nil? (get-value v 3)))))

(deftest test-get-values
  (let [sf (load-spssfile "src/test/resources/org/opendatafoundation/data/spss/TestNumber.sav")
        v (variable sf "IID")]
    (is (= [1 2 3 4 5 6 7] (get-values v)))
    (is (= [1 2 3 4 5 6 7] (get-values sf 0)))))

(comment 
  (doseq [file ["src/test/resources/org/opendatafoundation/data/spss/DatabaseTest.sav"
                "src/test/resources/org/opendatafoundation/data/spss/StringCategories.sav"
                "src/test/resources/org/opendatafoundation/data/spss/TestNumber.sav"
                "src/test/resources/org/opendatafoundation/data/spss/VeryLongStrings.sav"
                "src/test/resources/org/opendatafoundation/data/spss/VeryLongStringVariables.sav"]]
    (write-csv (load-spssfile file) (str/replace file ".sav" ".csv") {:variable-labels true}))
  
  (variable-info "src/test/resources/org/opendatafoundation/data/spss/VeryLongStrings.sav")
  )