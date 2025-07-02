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