(ns beatmap.operations-test
  (:require [clojure.test :refer :all]
            [beatmap.operations :as ops]
            [beatmap.apple-music.albums :as albums]
            [beatmap.apple-music.playlists :as playlists]
            [beatmap.csv-export.albums :as albums-csv]
            [beatmap.csv-export.playlists :as playlists-csv]
            [clojure.java.io :as io]))

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