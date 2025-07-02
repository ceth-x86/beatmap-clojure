(ns beatmap.csv-export-test
  (:require [clojure.test :refer :all]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [beatmap.csv-export :refer :all]))

(def sample-album
  {:id "1"
   :attributes {:artistName "Test Artist"
                :name "Test Album"
                :releaseDate "2020-01-15"}})

(def sample-album-no-date
  {:id "2"
   :attributes {:artistName "No Date"
                :name "No Date Album"
                :releaseDate nil}})

(def sample-album-invalid-date
  {:id "3"
   :attributes {:artistName "Invalid"
                :name "Invalid Album"
                :releaseDate "bad-date"}})

(deftest test-parse-release-date
  (is (= "2020" (parse-release-date "2020-01-15")))
  (is (= "Unknown" (parse-release-date nil)))
  (is (= "Unknown" (parse-release-date "")))
  (is (= "Unknown" (parse-release-date "bad-date")))
  (is (= "Unknown" (parse-release-date "202"))))

(deftest test-album-to-csv-row
  (is (= ["Test Artist" "2020" "Test Album"]
         (album-to-csv-row sample-album)))
  (is (= ["No Date" "Unknown" "No Date Album"]
         (album-to-csv-row sample-album-no-date)))
  (is (= ["Invalid" "Unknown" "Invalid Album"]
         (album-to-csv-row sample-album-invalid-date))))

(deftest test-escape-csv-field
  (is (= "simple" (escape-csv-field "simple")))
  (is (= "\"with,comma\"" (escape-csv-field "with,comma")))
  (is (= "\"with\"\"quote\"" (escape-csv-field "with\"quote")))
  (is (= "\"with\nnewline\"" (escape-csv-field "with\nnewline"))))

(deftest test-write-albums-to-csv
  (let [test-file "test_albums.csv"
        albums [sample-album sample-album-no-date sample-album-invalid-date]]
    (try
      (write-albums-to-csv albums :filename test-file)
      (let [content (slurp test-file)]
        (is (str/includes? content "Test Artist,2020,Test Album"))
        (is (str/includes? content "No Date,Unknown,No Date Album"))
        (is (str/includes? content "Invalid,Unknown,Invalid Album")))
      (finally
        (io/delete-file test-file)))))
