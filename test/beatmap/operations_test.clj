(ns beatmap.operations-test
  (:require [clojure.test :refer :all]
            [beatmap.operations :as ops]
            [beatmap.apple-music.albums :as albums]
            [beatmap.apple-music.playlists :as playlists]
            [beatmap.csv.albums :as albums-csv]
            [beatmap.csv.playlists :as playlists-csv]
            [beatmap.csv.tracks :as tracks-csv]
            [clojure.java.io :as io]
            [clojure.string :as str]))

;; Mock data for testing
(def mock-albums
  [{:attributes {:artistName "The Beatles"
                 :name "Abbey Road"
                 :releaseDate "1969-09-26"}}
   {:attributes {:artistName "Pink Floyd"
                 :name "Dark Side of the Moon"
                 :releaseDate "1973-03-01"}}
   {:attributes {:artistName "Aerosmith"
                 :name "Aerosmith"
                 :releaseDate "1973-01-05"}}])

(def mock-sorted-albums
  [{:attributes {:artistName "Aerosmith"
                 :name "Aerosmith"
                 :releaseDate "1973-01-05"}}
   {:attributes {:artistName "Pink Floyd"
                 :name "Dark Side of the Moon"
                 :releaseDate "1973-03-01"}}
   {:attributes {:artistName "The Beatles"
                 :name "Abbey Road"
                 :releaseDate "1969-09-26"}}])

(deftest process-apple-music-albums-success-test
  (testing "successfully processes albums and saves to CSV"
    (with-redefs [albums/get-albums-with-pagination (fn [& _] mock-albums)
                  beatmap.entities/sort-albums-by-artist-year-name (constantly mock-sorted-albums)
                  albums-csv/write-albums-to-csv (fn [albums & {:keys [filename]}]
                                                          (is (= mock-sorted-albums albums))
                                                          (is (= "test_albums.csv" filename))
                                                          "test_albums.csv")]
      (let [result (ops/process-apple-music-albums "test_albums.csv")]
        (is (nil? result))))))

(deftest process-apple-music-albums-empty-test
  (testing "handles empty albums list"
    (with-redefs [albums/get-albums-with-pagination (fn [& _] [])]
      (let [result (ops/process-apple-music-albums "test_albums.csv")]
        (is (nil? result))))))

(deftest process-apple-music-albums-nil-test
  (testing "handles nil albums list"
    (with-redefs [albums/get-albums-with-pagination (fn [& _] nil)]
      (let [result (ops/process-apple-music-albums "test_albums.csv")]
        (is (nil? result))))))

(deftest try-process-albums-success-test
  (testing "successfully processes albums without errors"
    (with-redefs [albums/get-albums-with-pagination (fn [& _] mock-albums)
                  beatmap.entities/sort-albums-by-artist-year-name (constantly mock-sorted-albums)
                  albums-csv/write-albums-to-csv (fn [& _] "test_albums.csv")]
      (let [result (ops/try-process-albums "test_albums.csv")]
        (is (nil? result))))))

(deftest try-process-albums-error-test
  (testing "handles errors gracefully"
    (with-redefs [albums/get-albums-with-pagination (fn [& _] 
                                                                  (throw (ex-info "API Error" {:status 500})))]
      (let [result (ops/try-process-albums "test_albums.csv")]
        (is (nil? result))))))

(deftest try-process-albums-csv-error-test
  (testing "handles CSV writing errors gracefully"
    (with-redefs [albums/get-albums-with-pagination (fn [& _] mock-albums)
                  beatmap.entities/sort-albums-by-artist-year-name (constantly mock-sorted-albums)
                  albums-csv/write-albums-to-csv (fn [& _] 
                                                          (throw (ex-info "File write error" {:error "Permission denied"})))]
      (let [result (ops/try-process-albums "test_albums.csv")]
        (is (nil? result))))))

(deftest process-apple-music-albums-calls-dependencies-test
  (testing "calls all dependencies in correct order"
    (let [api-calls (atom 0)
          sort-calls (atom 0)
          csv-calls (atom 0)]
      (with-redefs [albums/get-albums-with-pagination (fn [& _] 
                                                                    (swap! api-calls inc)
                                                                    mock-albums)
                    beatmap.entities/sort-albums-by-artist-year-name (fn [albums]
                                                                      (swap! sort-calls inc)
                                                                      (is (= mock-albums albums))
                                                                      mock-sorted-albums)
                    albums-csv/write-albums-to-csv (fn [albums & {:keys [filename]}]
                                                            (swap! csv-calls inc)
                                                            (is (= mock-sorted-albums albums))
                                                            (is (= "test_albums.csv" filename))
                                                            "test_albums.csv")]
        (ops/process-apple-music-albums "test_albums.csv")
        (is (= 1 @api-calls) "API should be called once")
        (is (= 1 @sort-calls) "Sort should be called once")
        (is (= 1 @csv-calls) "CSV write should be called once")))))

(deftest process-apple-music-albums-filename-parameter-test
  (testing "uses provided filename parameter"
    (with-redefs [albums/get-albums-with-pagination (fn [& _] mock-albums)
                  beatmap.entities/sort-albums-by-artist-year-name (constantly mock-sorted-albums)
                  albums-csv/write-albums-to-csv (fn [albums & {:keys [filename]}]
                                                          (is (= "custom_filename.csv" filename))
                                                          filename)]
      (ops/process-apple-music-albums "custom_filename.csv"))))



(deftest process-apple-music-playlists-and-tracks-test
  (testing "Process playlists and tracks combined"
    (with-redefs [playlists/get-playlists-with-pagination (fn [] 
                                                            [{:id "playlist-1" 
                                                              :attributes {:name "Test Playlist" 
                                                                          :canEdit true}}])
                  playlists-csv/write-playlists-separated-to-csv (fn [playlists editable-filename non-editable-filename]
                                                                   {:editable-count 1 
                                                                    :non-editable-count 0})
                  tracks-csv/process-all-playlist-tracks (fn [playlists tracks-fn & {:keys [output-dir]}]
                                                          {:successful-count 1 
                                                           :total-tracks 25 
                                                           :output-dir output-dir})]
      (let [result (ops/process-apple-music-playlists-and-tracks "test_editable.csv" "test_non_editable.csv" "test_tracks")]
        (is (map? result))
        (is (contains? result :playlists))
        (is (contains? result :tracks))))))

(deftest try-process-playlists-and-tracks-test
  (testing "Process playlists and tracks with error handling"
    (with-redefs [ops/process-apple-music-playlists-and-tracks (fn [editable-filename non-editable-filename tracks-output-dir]
                                                               {:playlists {:editable-count 1} 
                                                                :tracks {:successful-count 1}})]
      (ops/try-process-playlists-and-tracks "test_editable.csv" "test_non_editable.csv" "test_tracks"))))

(deftest test-generate-artists-from-albums
  (testing "Generate artists.csv from albums.csv"
    (let [albums-file "test_albums_input.csv"
          artists-file "test-output/generated/artists.csv"
          albums-content "Artist,Year,Album\nThe Beatles,1969,Abbey Road\nPink Floyd,1973,Dark Side of the Moon\nThe Beatles,1970,Let It Be"]
      (try
        (spit albums-file albums-content)
        (ops/try-process-generate-artists :albums-file albums-file :artists-file artists-file)
        (let [artists-content (slurp artists-file)]
          (is (str/includes? artists-content "Artist"))  ; Check header
          (is (str/includes? artists-content "Pink Floyd"))
          (is (str/includes? artists-content "The Beatles")))
        (finally
          (when (.exists (io/file albums-file)) (io/delete-file albums-file))
          (when (.exists (io/file artists-file)) (io/delete-file artists-file)))))))