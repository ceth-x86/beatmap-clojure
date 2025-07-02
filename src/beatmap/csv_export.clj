(ns beatmap.csv-export
  (:require [clojure.string :as str]
            [clojure.java.io :as io]))

(defn parse-release-date
  "Parse release date string and extract year.
   Returns year as string or 'Unknown' if parsing fails."
  [date-str]
  (if (and date-str (str/blank? date-str))
    "Unknown"
    (try
      (let [year (subs date-str 0 4)]
        (if (and (= 4 (count year)) (re-matches #"\d{4}" year))
          year
          "Unknown"))
      (catch Exception _
        "Unknown"))))

(defn album-to-csv-row
  "Convert album data to CSV row format [artist, year, album]"
  [album]
  (let [attributes (:attributes album)
        artist-name (:artistName attributes)
        album-name (:name attributes)
        release-date (:releaseDate attributes)
        year (parse-release-date release-date)]
    [artist-name year album-name]))

(defn escape-csv-field
  "Escape a field for CSV format"
  [field]
  (let [field-str (str field)]
    (if (or (str/includes? field-str ",") 
            (str/includes? field-str "\"") 
            (str/includes? field-str "\n"))
      (str "\"" (str/replace field-str "\"" "\"\"") "\"")
      field-str)))

(defn write-albums-to-csv
  "Write albums to CSV file with columns: Artist, Year, Album.
   Returns the file path where CSV was written."
  [albums & {:keys [filename] :or {filename "albums.csv"}}]
  (let [csv-rows (map album-to-csv-row albums)
        csv-lines (map #(str/join "," (map escape-csv-field %)) csv-rows)
        csv-content (str/join "\n" csv-lines)
        file-path filename]
    (with-open [writer (io/writer file-path)]
      (.write writer csv-content))
    (println (str "✅ Written " (count albums) " albums to " file-path))
    file-path))

;; Example usage:
;; (require '[beatmap.csv-export :as csv])
;; (csv/write-albums-to-csv albums :filename "my_albums.csv") 