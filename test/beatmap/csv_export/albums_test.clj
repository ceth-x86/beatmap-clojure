(ns beatmap.csv-export.albums-test
  (:require [clojure.test :refer :all]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [beatmap.csv-export.albums :as albums-csv]))

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

(deftest test-album-to-csv-row
  (testing "Convert album to CSV row format"
    (is (= ["Test Artist" "2020" "Test Album"]
           (albums-csv/album-to-csv-row sample-album)))
    (is (= ["No Date" "Unknown" "No Date Album"]
           (albums-csv/album-to-csv-row sample-album-no-date)))
    (is (= ["Invalid" "Unknown" "Invalid Album"]
           (albums-csv/album-to-csv-row sample-album-invalid-date)))))

(deftest test-write-albums-to-csv
  (testing "Write albums to CSV file"
    (let [test-file "test_albums.csv"
          albums [sample-album sample-album-no-date sample-album-invalid-date]]
      (try
        (albums-csv/write-albums-to-csv albums :filename test-file)
        (let [content (slurp test-file)]
          (is (str/includes? content "Artist,Year,Album"))  ; Check header
          (is (str/includes? content "Test Artist,2020,Test Album"))
          (is (str/includes? content "No Date,Unknown,No Date Album"))
          (is (str/includes? content "Invalid,Unknown,Invalid Album")))
        (finally
          (io/delete-file test-file))))))