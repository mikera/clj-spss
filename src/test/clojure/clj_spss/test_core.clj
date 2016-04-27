(ns clj-spss.test-core
  (:use [clj-spss.core])
  (:require [clojure.core.matrix :as m])
  (:require [clojure.string :as str])
  (:require [clojure.core.matrix])
  (:use [clojure.test]))

(deftest test-load
  (testing "regular file path"
    (let [ds (load-spss "src/test/resources/org/opendatafoundation/data/spss/DatabaseTest.sav")]
      (is (= [200 11] (m/shape ds))))))

;(deftest test-strings
;  (let [ds (load-spss "src/test/resources/org/opendatafoundation/data/spss/VeryLongStringVariables.sav")]
;    (is (= [200 11] (shape ds)))))

(comment 
  (doseq [file ["src/test/resources/org/opendatafoundation/data/spss/DatabaseTest.sav"
                "src/test/resources/org/opendatafoundation/data/spss/StringCategories.sav"
                "src/test/resources/org/opendatafoundation/data/spss/TestNumber.sav"
                "src/test/resources/org/opendatafoundation/data/spss/VeryLongStrings.sav"
                "src/test/resources/org/opendatafoundation/data/spss/VeryLongStringVariables.sav"]]
    (write-csv (load-spss file) (str/replace file ".sav" ".csv")))
  
  (variable-info "src/test/resources/org/opendatafoundation/data/spss/VeryLongStrings.sav")
  )