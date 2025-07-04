(ns beatmap.csv-export.utils
  (:require [clojure.string :as str]))

(defn escape-csv-field
  "Escape a field for CSV format.
   
   This function handles CSV field escaping according to RFC 4180:
   - If the field contains comma, double quote, or newline, it must be enclosed in double quotes
   - Any double quotes within the field must be escaped by doubling them
   
   Args:
     field: The field value to escape (will be converted to string)
   
   Returns:
     Properly escaped CSV field as a string
   
   Examples:
     (escape-csv-field \"Hello, World\")     ; => \"\\\"Hello, World\\\"\"
     (escape-csv-field \"Quote: \\\"text\\\"\") ; => \"\\\"Quote: \\\"\\\"text\\\"\\\"\\\"\"
     (escape-csv-field \"Simple text\")      ; => \"Simple text\""
  [field]
  (let [field-str (str field)]
    (if (or (str/includes? field-str ",") 
            (str/includes? field-str "\"") 
            (str/includes? field-str "\n"))
      (str "\"" (str/replace field-str "\"" "\"\"") "\"")
      field-str)))

(defn row-to-csv-line
  "Convert a sequence of fields to a CSV line.
   
   Args:
     fields: Sequence of field values to convert
   
   Returns:
     CSV line as a string with proper field escaping
   
   Examples:
     (row-to-csv-line [\"Artist\" \"Year\" \"Album\"])
     (row-to-csv-line [\"The Beatles\" 1969 \"Abbey Road\"])"
  [fields]
  (str/join "," (map escape-csv-field fields)))

(defn write-csv-content
  "Write CSV content to a file.
   
   Args:
     file-path: Path to the output file
     csv-lines: Sequence of CSV lines (strings)
   
   Returns:
     The file path where CSV was written
   
   Examples:
     (write-csv-content \"output.csv\" [\"header1,header2\" \"value1,value2\"])"
  [file-path csv-lines]
  (let [csv-content (str/join "\n" csv-lines)]
    (with-open [writer (clojure.java.io/writer file-path)]
      (.write writer csv-content))
    file-path)) 