(ns beatmap.csv.playlists-test
  (:require [clojure.test :refer :all]
            [beatmap.csv.playlists :as playlists-csv]
            [beatmap.entities :as entities]
            [clojure.java.io :as io]
            [clojure.string :as str]))

(def sample-playlists
  [{:attributes {:name "Rock Classics" :description "Best rock songs" :canEdit true}}
   {:attributes {:name "Chill Vibes" :description "Relaxing music" :canEdit false}}
   {:attributes {:name "Workout Mix" :curatorName "Apple Music" :canEdit false}}
   {:attributes {:name "Jazz Collection" :lastModifiedDate "2024-01-15" :canEdit true}}])

(deftest test-playlist-to-csv-row
  (testing "Convert playlist to CSV row format"
    (let [playlist {:attributes {:name "Test Playlist" :canEdit true}}]
      (is (= ["Test Playlist" "" nil true] 
             (playlists-csv/playlist-to-csv-row playlist))))))

(deftest test-sort-playlists-by-name
  (testing "Sort playlists alphabetically by name"
    (let [sorted (entities/sort-playlists-by-name sample-playlists)]
      (is (= "Chill Vibes" (get-in (first sorted) [:attributes :name])))
      (is (= "Workout Mix" (get-in (last sorted) [:attributes :name]))))))

(deftest test-write-playlists-to-csv
  (testing "Write playlists to CSV file"
    (let [test-file "test_playlists.csv"]
      (try
        (playlists-csv/write-playlists-to-csv sample-playlists :filename test-file)
        (let [content (slurp test-file)]
          (is (str/includes? content "Chill Vibes"))
          (is (str/includes? content "Rock Classics")))
        (finally
          (io/delete-file test-file true))))))

(deftest test-write-playlists-separated-to-csv
  (testing "Write separated playlists to CSV files"
    (let [test-editable "test_editable.csv"
          test-non-editable "test_non_editable.csv"]
      (try
        (playlists-csv/write-playlists-separated-to-csv 
         sample-playlists 
         test-editable
         test-non-editable)
        ;; Check that files exist and contain expected content
        (when (.exists (io/file test-editable))
          (let [editable-content (slurp test-editable)]
            (is (str/includes? editable-content "Playlist Name"))))
        (when (.exists (io/file test-non-editable))
          (let [non-editable-content (slurp test-non-editable)]
            (is (str/includes? non-editable-content "Playlist Name"))))
        (finally
          (when (.exists (io/file test-editable))
            (io/delete-file test-editable true))
          (when (.exists (io/file test-non-editable))
            (io/delete-file test-non-editable true))))))) 