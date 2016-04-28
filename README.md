# clj-spss

SPSS .sav file reader for Clojure, intended for use by Clojure Data Scientists working with SPSS .sav files.

Builds up work by the Open Data Foundation, see: https://github.com/daxplore/spssreader

## Example Usage

```clojure
(require '[clj-spss.core :as sav])

;; Load an SPSS file as a SPSSFile object
(def spss (sav/load-spssfile "src/test/resources/org/opendatafoundation/data/spss/DatabaseTest.sav"))


;; Inspect numeric values in an SPSS dataset
(get-value spss "id" 0)
=> 70.0


;; Inspect variables in a .sav file
(sav/variable-info "src/test/resources/org/opendatafoundation/data/spss/VeryLongStrings.sav")
=> [{:name "ID",
     :index 0
     :format "F6.0",
     :length 6}
     .....]


;; Convert an SPSS file into a core.matrix dataset
(def data (sav/dataset-from-spss spss))


;; Write out data as a .csv file
(sav/write-csv data "out.csv")

```
