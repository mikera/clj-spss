(ns clj-spss.core
  (:require [clojure.core.matrix :as m])
  (:require [clojure.core.matrix.dataset :as ds])
  (:require [clojure.data.csv :as csv])
  (:require [clojure.java.io :as io])
  (:require [clojure.string :as str])
  (:require [mikera.cljutils.error :refer [error]])
  (:import [org.opendatafoundation.data.spss SPSSFile SPSSUtils SPSSVariable SPSSStringVariable SPSSNumericVariable])
  (:import [org.opendatafoundation.data FileFormatInfo])
  (:import [java.io File])
  (:import [java.net URI]))

(set! *warn-on-reflection* true)

(def ^FileFormatInfo DEFAULT-FILEFORMATINFO (FileFormatInfo.))

(defn to-file 
  "Coerces the argument is a java.io.File. Handles existing files, and Strings interpreted as file paths."
  (^java.io.File [file]
    (cond 
      (instance? File file) file
      (string? file) (File. (str file))
      (instance? URI file) (File. ^java.net.URI file)
      :else (error "Can't convert to File: " (class file)))))

(defn load-spssfile
  "Loads an SPSS .sav file into an SPSSFile"
  (^SPSSFile [file]
    (load-spssfile file nil))
  (^SPSSFile [file options]
    (let [file (to-file file)
        spssfile (SPSSFile. file)]
      (set! (.logFlag spssfile) (boolean (:log options))) 
      (.loadMetadata spssfile)
      (.loadData spssfile)
      spssfile)))

(defn to-spssfile 
  "Coerces to a SPSSFile in-memory representation."
  (^SPSSFile [file]
    (if (instance? SPSSFile file) 
      file
      (load-spssfile file))))

(defn converter 
  "Gets a function that converts an SPSS variable value to a Clojure value.

   Supported types:
    - Strings
    - Numbers
    - Dates 

   Missing values are returned as nil." 
  [^SPSSVariable v ^FileFormatInfo ffi]
  (cond 
    (instance? SPSSStringVariable v) 
      (fn [^SPSSVariable v i]
        (let [s (.getValueAsString v (int (inc i)) ffi)]
          (try
            (when-not (empty? s) 
              s)
            (catch Throwable t
              (error "Can't read value: " s " from SPSS variable of type: " (class v) "with format: " format)))))
    (instance? SPSSNumericVariable v) 
      (let [format ^String (.getSPSSFormat v)]
        (cond
          (str/includes? (str/upper-case format) "DATE")
            (fn [^SPSSNumericVariable v i] 
              (try
                (let [d (.getValueAsDouble v (int (inc i)))]
                  (when-not (.isMissingValueCode v d)
                    (SPSSUtils/numericToDate d)))
                (catch Throwable t
                  (error "Can't read value from SPSS variable of type: " (class v) "with format: " format))))
          :else (fn [^SPSSNumericVariable v i] 
                  (let [s (.getValueAsString v (int (inc i)) ffi)]
                    (try
                      (when-not (empty? s) 
                        (read-string s))
                      (catch Throwable t
                        (error "Can't read value: " s " from SPSS variable of type: " (class v) "with format: " format)))))))
    :else (error "Unable to recognise SPSS variable type")))

(defn variable-count 
  "Gets the number of variables in an SPSS file"
  ([^SPSSFile spssfile]
    (.getVariableCount spssfile)))

(defn record-count 
  "Gets the number of record in an SPSS file"
  ([^SPSSFile spssfile]
    (.getRecordCount spssfile)))

(defn variables 
  "Gets all variables from an SPSSFile"
  ([^SPSSFile spssfile]
    (let [vcount (variable-count spssfile)]
      (mapv #(.getVariable spssfile (int %)) (range vcount)))))

(defn variable
  "Gets a specific variable from an SPSSFile. 
   Variable may be specified by either a name or an integer index"
  (^SPSSVariable [^SPSSFile spssfile name-or-index]
    (if (string? name-or-index)
      (.getVariableByName spssfile (str name-or-index))
      (.getVariable spssfile (int name-or-index)))))

(defn get-value
  "Gets a value from an SPSS variable. 

   Indexed from first row = 0 (consistent with core.matrix slice numbering)"
  ([^SPSSFile spssfile var-name-or-index index]
    (get-value (variable spssfile var-name-or-index) index))
  ([^SPSSVariable spssvar index]
    ((converter spssvar DEFAULT-FILEFORMATINFO) spssvar index)))

(defn variable-info ([file]
  (let [spssfile (to-spssfile file)
        vars (variables spssfile)]
    (mapv 
      (fn [^SPSSVariable v i]
        {:name (.getName v)
         :index i
         :format (.getSPSSFormat v)
         :length (long (.getLength v))})
      vars
      (range (count vars))))))

(defn dataset-from-spss
  "Loads a SPSS .sav file into a Clojure core.matrix dataset structure"
  ([file]
    (dataset-from-spss file nil))
  ([file options]
    (let [spssfile (to-spssfile file)
          vcount (variable-count spssfile)
          rowcount (record-count spssfile)
          variables (variables spssfile)
          variable-names (mapv #(.getName ^SPSSVariable %) variables)
          ffi DEFAULT-FILEFORMATINFO
          columns (mapv 
                    (fn [^SPSSVariable v]
                      (let [conv (converter v ffi)]
                        (mapv 
                          (fn [i] (conv v i))
                          (range rowcount))))
                    variables)]
      (ds/dataset variable-names
                  (zipmap variable-names 
                          columns)))))

(defn write-csv
  "Writes a csv file. 

   Data may be either a core.matrix dataset or a loaded SPSSFile"
  ([spssdata file]
    (let [spssdata (cond 
                     (instance? SPSSFile spssdata) (dataset-from-spss spssdata)
                     :else spssdata)]
      (with-open [out-file (io/writer file)]
        (csv/write-csv out-file
                       (concat
                         [(ds/column-names spssdata)]
                         (map m/eseq (m/slices spssdata))))))))
