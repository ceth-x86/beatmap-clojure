(ns beatmap.csv-export.utils-test
  (:require [clojure.test :refer :all]
            [beatmap.csv-export.utils :as csv-utils]
            [clojure.java.io :as io]
            [clojure.string :as str]))

(deftest escape-csv-field-simple-test
  (testing "simple fields are not escaped"
    (is (= "Simple text" (csv-utils/escape-csv-field "Simple text")))
    (is (= "123" (csv-utils/escape-csv-field 123)))
    (is (= "true" (csv-utils/escape-csv-field true)))))

(deftest escape-csv-field-comma-test
  (testing "fields with commas are escaped"
    (is (= "\"Hello, World\"" (csv-utils/escape-csv-field "Hello, World")))
    (is (= "\"Artist, Album, Year\"" (csv-utils/escape-csv-field "Artist, Album, Year")))))

(deftest escape-csv-field-quote-test
  (testing "fields with quotes are escaped"
    (is (= "\"Quote: \"\"text\"\"\"" (csv-utils/escape-csv-field "Quote: \"text\"")))
    (is (= "\"\"\"quoted\"\"\"" (csv-utils/escape-csv-field "\"quoted\"")))))

(deftest escape-csv-field-newline-test
  (testing "fields with newlines are escaped"
    (is (= "\"Line 1\nLine 2\"" (csv-utils/escape-csv-field "Line 1\nLine 2")))
    (is (= "\"Multi\nline\ntext\"" (csv-utils/escape-csv-field "Multi\nline\ntext")))))

(deftest escape-csv-field-multiple-special-chars-test
  (testing "fields with multiple special characters are escaped"
    (is (= "\"Hello, \"\"World\"\"\nTest\"" (csv-utils/escape-csv-field "Hello, \"World\"\nTest")))))

(deftest row-to-csv-line-test
  (testing "converts row to CSV line"
    (is (= "Artist,Year,Album" (csv-utils/row-to-csv-line ["Artist" "Year" "Album"])))
    (is (= "The Beatles,1969,Abbey Road" (csv-utils/row-to-csv-line ["The Beatles" 1969 "Abbey Road"])))
    (is (= "\"Hello, World\",\"Quote: \"\"text\"\"\",Simple" (csv-utils/row-to-csv-line ["Hello, World" "Quote: \"text\"" "Simple"])))))

(deftest write-csv-content-test
  (testing "writes CSV content to file"
    (let [test-file "test_output.csv"
          csv-lines ["header1,header2" "value1,value2" "\"quoted,value\",simple"]]
      (try
        (csv-utils/write-csv-content test-file csv-lines)
        (is (.exists (io/file test-file)))
        (let [content (slurp test-file)]
          (is (str/includes? content "header1,header2"))
          (is (str/includes? content "value1,value2"))
          (is (str/includes? content "\"quoted,value\",simple")))
        (finally
          (when (.exists (io/file test-file))
            (io/delete-file test-file))))))) 