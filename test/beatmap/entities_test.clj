(ns beatmap.entities-test
  (:require [clojure.test :refer :all]
            [beatmap.entities :as entities]))

(deftest sort-albums-by-artist-year-name-test
  (testing "sorts albums by artist, year, and album name"
    (let [albums [{:attributes {:artistName "The Beatles"
                                :name "Abbey Road"
                                :releaseDate "1969-09-26"}}
                   {:attributes {:artistName "The Beatles"
                                :name "Let It Be"
                                :releaseDate "1970-05-08"}}
                   {:attributes {:artistName "Pink Floyd"
                                :name "Dark Side of the Moon"
                                :releaseDate "1973-03-01"}}
                   {:attributes {:artistName "Pink Floyd"
                                :name "Wish You Were Here"
                                :releaseDate "1975-09-12"}}
                   {:attributes {:artistName "Aerosmith"
                                :name "Aerosmith"
                                :releaseDate "1973-01-05"}}]
          sorted (entities/sort-albums-by-artist-year-name albums)
          sorted-names (map #(get-in % [:attributes :name]) sorted)]
      (is (= ["Aerosmith" "Dark Side of the Moon" "Wish You Were Here" "Abbey Road" "Let It Be"]
             sorted-names)))))

(deftest sort-albums-by-artist-year-name-same-artist-test
  (testing "sorts albums by year when artist is the same"
    (let [albums [{:attributes {:artistName "The Beatles"
                                :name "Let It Be"
                                :releaseDate "1970-05-08"}}
                   {:attributes {:artistName "The Beatles"
                                :name "Abbey Road"
                                :releaseDate "1969-09-26"}}
                   {:attributes {:artistName "The Beatles"
                                :name "Sgt. Pepper's"
                                :releaseDate "1967-06-01"}}]
          sorted (entities/sort-albums-by-artist-year-name albums)
          sorted-names (map #(get-in % [:attributes :name]) sorted)]
      (is (= ["Sgt. Pepper's" "Abbey Road" "Let It Be"]
             sorted-names)))))

(deftest sort-albums-by-artist-year-name-same-year-test
  (testing "sorts albums by name when artist and year are the same"
    (let [albums [{:attributes {:artistName "The Beatles"
                                :name "Let It Be"
                                :releaseDate "1970-05-08"}}
                   {:attributes {:artistName "The Beatles"
                                :name "Abbey Road"
                                :releaseDate "1970-05-08"}}
                   {:attributes {:artistName "The Beatles"
                                :name "Sgt. Pepper's"
                                :releaseDate "1970-05-08"}}]
          sorted (entities/sort-albums-by-artist-year-name albums)
          sorted-names (map #(get-in % [:attributes :name]) sorted)]
      (is (= ["Abbey Road" "Let It Be" "Sgt. Pepper's"]
             sorted-names)))))

(deftest sort-albums-by-artist-year-name-empty-test
  (testing "returns empty list for empty input"
    (let [sorted (entities/sort-albums-by-artist-year-name [])]
      (is (= [] sorted)))))

(deftest sort-albums-by-artist-year-name-single-album-test
  (testing "returns single album unchanged"
    (let [albums [{:attributes {:artistName "The Beatles"
                                :name "Abbey Road"
                                :releaseDate "1969-09-26"}}]
          sorted (entities/sort-albums-by-artist-year-name albums)]
      (is (= albums sorted)))))

(deftest sort-albums-by-artist-year-name-missing-dates-test
  (testing "handles albums with missing or invalid release dates"
    (let [albums [{:attributes {:artistName "The Beatles"
                                :name "Abbey Road"
                                :releaseDate "1969-09-26"}}
                   {:attributes {:artistName "Unknown Artist"
                                :name "Unknown Album"
                                :releaseDate nil}}
                   {:attributes {:artistName "Another Artist"
                                :name "Another Album"
                                :releaseDate "Invalid Date"}}]
          sorted (entities/sort-albums-by-artist-year-name albums)
          sorted-names (map #(get-in % [:attributes :name]) sorted)]
      ;; Albums with valid dates should come first, then by artist name
      (is (= ["Another Album" "Abbey Road" "Unknown Album"]
             sorted-names)))))

(deftest sort-albums-by-artist-year-name-case-insensitive-test
  (testing "sorts case-insensitively by artist name"
    (let [albums [{:attributes {:artistName "aerosmith"
                                :name "Aerosmith"
                                :releaseDate "1973-01-05"}}
                   {:attributes {:artistName "Aerosmith"
                                :name "Aerosmith"
                                :releaseDate "1973-01-05"}}
                   {:attributes {:artistName "The Beatles"
                                :name "Abbey Road"
                                :releaseDate "1969-09-26"}}]
          sorted (entities/sort-albums-by-artist-year-name albums)
          sorted-names (map #(get-in % [:attributes :name]) sorted)]
      (is (= ["Aerosmith" "Aerosmith" "Abbey Road"]
             sorted-names)))))

;; Playlist sorting tests

(deftest sort-playlists-by-name-test
  (testing "sorts playlists alphabetically by name"
    (let [playlists [{:attributes {:name "Rock Classics"}}
                     {:attributes {:name "Chill Vibes"}}
                     {:attributes {:name "Workout Mix"}}
                     {:attributes {:name "Jazz Collection"}}]
          sorted (entities/sort-playlists-by-name playlists)
          sorted-names (map #(get-in % [:attributes :name]) sorted)]
      (is (= ["Chill Vibes" "Jazz Collection" "Rock Classics" "Workout Mix"]
             sorted-names)))))

(deftest sort-playlists-by-name-case-insensitive-test
  (testing "sorts playlists case-insensitively by name"
    (let [playlists [{:attributes {:name "rock classics"}}
                     {:attributes {:name "Chill Vibes"}}
                     {:attributes {:name "WORKOUT MIX"}}
                     {:attributes {:name "jazz collection"}}]
          sorted (entities/sort-playlists-by-name playlists)
          sorted-names (map #(get-in % [:attributes :name]) sorted)]
      (is (= ["Chill Vibes" "jazz collection" "rock classics" "WORKOUT MIX"]
             sorted-names)))))

(deftest sort-playlists-by-name-empty-test
  (testing "returns empty list for empty input"
    (let [sorted (entities/sort-playlists-by-name [])]
      (is (= [] sorted)))))

(deftest sort-playlists-by-name-single-playlist-test
  (testing "returns single playlist unchanged"
    (let [playlists [{:attributes {:name "Rock Classics"}}]
          sorted (entities/sort-playlists-by-name playlists)]
      (is (= playlists sorted)))))

(deftest sort-playlists-by-name-missing-names-test
  (testing "handles playlists with missing names"
    (let [playlists [{:attributes {:name "Rock Classics"}}
                     {:attributes {:name nil}}
                     {:attributes {}}
                     {:attributes {:name "Chill Vibes"}}]
          sorted (entities/sort-playlists-by-name playlists)
          sorted-names (map #(get-in % [:attributes :name]) sorted)]
      ;; Playlists with names should come first, then nil for missing names
      (is (= ["Chill Vibes" "Rock Classics" nil nil]
             sorted-names)))))

;; parse-release-date tests

(deftest parse-release-date-valid-test
  (testing "parses valid release dates correctly"
    (is (= "2020" (entities/parse-release-date "2020-01-15")))
    (is (= "1969" (entities/parse-release-date "1969-09-26")))
    (is (= "1973" (entities/parse-release-date "1973-03-01")))
    (is (= "2024" (entities/parse-release-date "2024-12-31")))))

(deftest parse-release-date-nil-test
  (testing "returns 'Unknown' for nil dates"
    (is (= "Unknown" (entities/parse-release-date nil)))))

(deftest parse-release-date-empty-test
  (testing "returns 'Unknown' for empty dates"
    (is (= "Unknown" (entities/parse-release-date "")))
    (is (= "Unknown" (entities/parse-release-date "   ")))))

(deftest parse-release-date-invalid-test
  (testing "returns 'Unknown' for invalid dates"
    (is (= "Unknown" (entities/parse-release-date "bad-date")))
    (is (= "Unknown" (entities/parse-release-date "202")))
    (is (= "Unknown" (entities/parse-release-date "not-a-date")))))

(deftest parse-release-date-edge-cases-test
  (testing "handles edge cases"
    (is (= "2020" (entities/parse-release-date "2020")))  ; Just year
    (is (= "2020" (entities/parse-release-date "2020-01")))  ; Year and month
    (is (= "2020" (entities/parse-release-date "2020-01-15-extra")))))  ; Extra parts 