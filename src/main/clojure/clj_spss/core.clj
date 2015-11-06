(ns clj-spss.core
  (:require [clojure.core.matrix :as m])
  (:require [clojure.core.matrix.dataset :as ds])
  (:require [mikera.cljutils.error :refer [error]])
  (:import [org.opendatafoundation.data.spss SPSSFile SPSSVariable SPSSStringVariable SPSSNumericVariable])
  (:import [org.opendatafoundation.data FileFormatInfo])
  (:import [java.io File]))

(set! *warn-on-reflection* true)

(defn load-spssfile
  "Loads an SPSS .sav file into an SPSSFile"
  (^SPSSFile [file]
    (load-spssfile file nil))
  (^SPSSFile [file options]
    (let [^File file (if (string? file) (File. (str file)) file)
        spssfile (SPSSFile. file)]
      (set! (.logFlag spssfile) (boolean (:log options))) 
      (.loadMetadata spssfile)
      (.loadData spssfile)
      spssfile)))

(defn converter 
  "Gets a function that parses an SPSS variable value" 
  [^SPSSVariable v]
  (cond 
    (instance? SPSSStringVariable v) identity
    (instance? SPSSNumericVariable v) 
      (let [format ^String (.getSPSSFormat v)]
        (cond
          :else (fn [s] (and s (read-string s)))))
    :else (error "Unable to recognise SPSS variable type")))

(defn load-spss
  "Loads a SPSS .sav file into a Clojure data structure"
  ([file]
    (load-spss file nil))
  ([file options]
    (let [spssfile (load-spssfile file)
          vcount (.getVariableCount spssfile)
          rowcount (.getRecordCount spssfile)
          variables (mapv #(.getVariable spssfile (int %)) (range vcount))
          variable-names (mapv #(.getName ^SPSSVariable %) variables)
          ^FileFormatInfo ffi (FileFormatInfo.)
          columns (mapv 
                    (fn [^SPSSVariable v]
                      (let [conv (converter v)]
                        (mapv 
                          (fn [i] (conv (.getValueAsString v (int i) ffi)))
                          (range rowcount))))
                    variables)]
      (ds/dataset variable-names
                  (zipmap variable-names 
                          columns)))))