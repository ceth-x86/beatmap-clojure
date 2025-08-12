(ns beatmap.csv.tracks-test
  (:require [clojure.test :refer :all]
            [beatmap.csv.tracks :as tracks]
            [clojure.java.io :as io]))

(deftest track-to-csv-row-test
  (testing "Convert track map to CSV row"
    (let [track {:attributes {:artistName "Test Artist"
                             :albumName "Test Album"
                             :name "Test Track"
                             :releaseDate "2023-01-01"
                             :genreNames ["Rock"]
                             :durationInMillis 180000}}
          result (tracks/track-to-csv-row track)]
      (is (= ["Test Artist" "Test Album" "Test Track" "2023" "Rock" "3.00"] result))))
  
  (testing "Convert track with missing data to CSV row"
    (let [track {:attributes {:artistName "Test Artist"
                             :albumName "Test Album"
                             :name "Test Track"}}
          result (tracks/track-to-csv-row track)]
      (is (= ["Test Artist" "Test Album" "Test Track" "Unknown" "Unknown" "0.00"] result))))
  
  (testing "Convert track with empty genre to CSV row"
    (let [track {:attributes {:artistName "Test Artist"
                             :albumName "Test Album"
                             :name "Test Track"
                             :releaseDate "2023-01-01"
                             :genreNames []
                             :durationInMillis 120000}}
          result (tracks/track-to-csv-row track)]
      (is (= ["Test Artist" "Test Album" "Test Track" "2023" "Unknown" "2.00"] result)))))

(deftest save-playlist-tracks-to-csv-test
  (testing "Save tracks to CSV file"
    (let [test-dir "test-output"
          playlist-id "test-playlist-123"
          playlist-name "Test Playlist"
          tracks [{:attributes {:artistName "Artist 1"
                               :albumName "Album 1"
                               :name "Track 1"
                               :releaseDate "2023-01-01"
                               :genreNames ["Rock"]
                               :durationInMillis 180000}}
                  {:attributes {:artistName "Artist 2"
                               :albumName "Album 2"
                               :name "Track 2"
                               :releaseDate "2023-02-01"
                               :genreNames ["Pop"]
                               :durationInMillis 240000}}]
          result (tracks/save-playlist-tracks-to-csv playlist-id playlist-name tracks :output-dir test-dir)]
      (is (string? result))
      (is (.exists (io/file result)))
      (is (.contains result "Test_Playlist.csv"))
      
      ;; Clean up
      (.delete (io/file result))
      (.delete (io/file test-dir))))
  
  (testing "Handle special characters in playlist name"
    (let [test-dir "test-output"
          playlist-id "test-playlist-456"
          playlist-name "Test Playlist (Special) & More!"
          tracks [{:attributes {:artistName "Artist 1"
                               :albumName "Album 1"
                               :name "Track 1"
                               :releaseDate "2023-01-01"
                               :genreNames ["Rock"]
                               :durationInMillis 180000}}]
          result (tracks/save-playlist-tracks-to-csv playlist-id playlist-name tracks :output-dir test-dir)]
      (is (string? result))
      (is (.exists (io/file result)))
      (is (or (.contains result "Test_Playlist_Special__More.csv")
              (.contains result "Test_Playlist_Special_More.csv")))
      
      ;; Clean up
      (.delete (io/file result))
      (.delete (io/file test-dir)))))

(deftest process-playlist-tracks-test
  (testing "Process single playlist tracks"
    (let [test-dir "test-output"
          playlist {:id "test-playlist-789"
                   :attributes {:name "Test Playlist"}}
          tracks [{:attributes {:artistName "Artist 1"
                               :albumName "Album 1"
                               :name "Track 1"
                               :releaseDate "2023-01-01"
                               :genreNames ["Rock"]
                               :durationInMillis 180000}}]
          result (tracks/process-playlist-tracks playlist tracks :output-dir test-dir)]
      (is (:success result))
      (is (string? (:filepath result)))
      (is (.contains (:filepath result) "Test_Playlist.csv"))
      (is (= 1 (:track-count result)))
      
      ;; Clean up
      (.delete (io/file (:filepath result)))
      (.delete (io/file test-dir)))))

(deftest process-all-playlist-tracks-test
  (testing "Process multiple playlist tracks (only editable playlists)"
    (let [test-dir "test-output"
          playlists [{:id "playlist-1"
                     :attributes {:name "Playlist 1"
                                 :canEdit true}}
                    {:id "playlist-2"
                     :attributes {:name "Playlist 2"
                                 :canEdit false}}
                    {:id "playlist-3"
                     :attributes {:name "Playlist 3"
                                 :canEdit true}}]
          tracks-fn (fn [playlist-id playlist-name]
                     [{:attributes {:artistName "Artist"
                                   :albumName "Album"
                                   :name "Track"
                                   :releaseDate "2023-01-01"
                                   :genreNames ["Rock"]
                                   :durationInMillis 180000}}])
          result (tracks/process-all-playlist-tracks playlists tracks-fn :output-dir test-dir)]
      (is (= 3 (:total-playlists result)))
      (is (= 2 (:editable-playlists result)))
      (is (= 2 (:successful-count result)))
      (is (= 0 (:failed-count result)))
      (is (= 2 (:total-tracks result)))
      (is (= test-dir (:output-dir result)))
      
      ;; Clean up
      (doseq [file-result (:results result)]
        (when (:filepath file-result)
          (.delete (io/file (:filepath file-result)))))
      (.delete (io/file test-dir))))) 