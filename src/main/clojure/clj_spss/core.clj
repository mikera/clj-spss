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

(defn write-csv
  "Loads an SPSS .sav file into an SPSSFile"
  ([spssdata file]
    (with-open [out-file (io/writer file)]
      (csv/write-csv out-file
                     (concat
                       [(ds/column-names spssdata)]
                       (map m/eseq (m/slices spssdata)))))))

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
        (let [s (.getValueAsString v (int i) ffi)]
          (try
            (when-not (.isMissingValueCode v s) 
              s)
            (catch Throwable t
              (error "Can't read value: " s " from SPSS variable of type: " (class v) "with format: " format)))))
    (instance? SPSSNumericVariable v) 
      (let [format ^String (.getSPSSFormat v)]
        (cond
          (str/includes? (str/upper-case format) "DATE")
            (fn [^SPSSNumericVariable v i] 
              (try
                (let [d (.getValueAsDouble v (int i))]
                  (when-not (.isMissingValueCode v d)
                    (SPSSUtils/numericToCalendar d)))
                (catch Throwable t
                  (error "Can't read value from SPSS variable of type: " (class v) "with format: " format))))
          :else (fn [^SPSSNumericVariable v i] 
                  (let [s (.getValueAsString v (int i) ffi)]
                    (try
                      (when-not (empty? s) 
                        (read-string s))
                      (catch Throwable t
                        (error "Can't read value: " s " from SPSS variable of type: " (class v) "with format: " format)))))))
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
                      (let [conv (converter v ffi)]
                        (mapv 
                          (fn [i] (conv v i))
                          (range rowcount))))
                    variables)]
      (ds/dataset variable-names
                  (zipmap variable-names 
                          columns)))))