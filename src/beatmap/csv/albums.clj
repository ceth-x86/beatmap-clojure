(ns beatmap.csv.albums
  (:require [clojure.string :as str]
            [clojure.java.io :as io]
            [beatmap.entities :as entities]
            [beatmap.csv.utils :as csv-utils]))

(defn album-to-csv-row
  "Convert album data to CSV row format [artist, year, album]"
  [album]
  (let [attributes (:attributes album)
        artist-name (:artistName attributes)
        album-name (:name attributes)
        release-date (:releaseDate attributes)
        year (entities/parse-release-date release-date)]
    [artist-name year album-name]))



(defn write-albums-to-csv
  "Write albums to CSV file with columns: Artist, Year, Album.
   Returns the file path where CSV was written."
  [albums & {:keys [filename] :or {filename "albums.csv"}}]
  (let [header ["Artist" "Year" "Album"]
        csv-rows (map album-to-csv-row albums)
        csv-lines (map #(str/join "," (map csv-utils/escape-csv-field %)) (cons header csv-rows))
        csv-content (str/join "\n" csv-lines)
        file-path filename]
    (with-open [writer (io/writer file-path)]
      (.write writer csv-content))
    (println (str "✅ Written " (count albums) " albums to " file-path))
    file-path))

(defn parse-csv-line
  "Parse a CSV line, handling quoted fields properly."
  [line]
  (let [trimmed-line (str/trim line)]
    (if (empty? trimmed-line)
      []
      (loop [chars (seq trimmed-line)
             current-field ""
             fields []
             in-quotes false]
        (cond
          (empty? chars)
          (conj fields current-field)
          
          (and (not in-quotes) (= (first chars) \,))
          (recur (rest chars) "" (conj fields current-field) false)
          
          (= (first chars) \")
          (if in-quotes
            (if (and (> (count chars) 1) (= (second chars) \"))
              (recur (drop 2 chars) (str current-field "\"") fields true)
              (recur (rest chars) current-field fields false))
            (recur (rest chars) current-field fields true))
          
          :else
          (recur (rest chars) (str current-field (first chars)) fields in-quotes))))))

(defn read-albums-csv
  "Read albums from CSV file and return unique artists.
   
   Args:
     file-path: Path to the albums CSV file
   
   Returns:
     Sorted set of unique artist names"
  [file-path]
  (try
    (with-open [reader (io/reader file-path)]
      (let [lines (line-seq reader)
            parsed-lines (map parse-csv-line lines)
            data-lines (rest parsed-lines)  ; Skip header
            artists (map first data-lines)  ; First column is artist
            unique-artists (set (filter #(and % (not (str/blank? %))) artists))]
        (sort unique-artists)))
    (catch Exception e
      (println (str "❌ Error reading albums CSV file '" file-path "': " (.getMessage e)))
      [])))

