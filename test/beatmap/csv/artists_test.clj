(ns beatmap.csv.artists-test
  (:require [clojure.test :refer :all]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [beatmap.csv.artists :as artists-csv]
))

(deftest test-write-artists-to-csv
  (testing "Write unique artists to CSV file"
    (let [test-file "test_artists.csv"
          artists ["Aerosmith" "Pink Floyd" "The Beatles"]]
      (try
        (artists-csv/write-artists-to-csv artists :filename test-file)
        (let [content (slurp test-file)]
          (is (str/includes? content "Artist"))  ; Check header
          (is (str/includes? content "Aerosmith"))
          (is (str/includes? content "Pink Floyd"))
          (is (str/includes? content "The Beatles")))
        (finally
          (io/delete-file test-file))))))
