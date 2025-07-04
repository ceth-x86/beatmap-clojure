(ns beatmap.csv-export.albums
  (:require [clojure.string :as str]
            [clojure.java.io :as io]
            [beatmap.entities :as entities]
            [beatmap.csv-export.utils :as csv-utils]))

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
  (let [csv-rows (map album-to-csv-row albums)
        csv-lines (map #(str/join "," (map csv-utils/escape-csv-field %)) csv-rows)
        csv-content (str/join "\n" csv-lines)
        file-path filename]
    (with-open [writer (io/writer file-path)]
      (.write writer csv-content))
    (println (str "✅ Written " (count albums) " albums to " file-path))
    file-path))