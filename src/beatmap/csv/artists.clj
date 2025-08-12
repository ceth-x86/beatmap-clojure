(ns beatmap.csv.artists
  (:require [clojure.string :as str]
            [clojure.java.io :as io]
            [beatmap.csv.utils :as csv-utils]))

(defn write-artists-to-csv
  "Write unique artists to CSV file.
   
   Args:
     artists: Collection of artist names
     filename: Output filename (optional, defaults to 'artists.csv')
   
   Returns:
     The file path where CSV was written"
  [artists & {:keys [filename] :or {filename "artists.csv"}}]
  (let [header ["Artist"]
        csv-rows (map vector artists)  ; Convert each artist to single-element vector
        csv-lines (map #(str/join "," (map csv-utils/escape-csv-field %)) (cons header csv-rows))
        csv-content (str/join "\n" csv-lines)
        file-path filename]
    (with-open [writer (io/writer file-path)]
      (.write writer csv-content))
    (println (str "✅ Written " (count artists) " unique artists to " file-path))
    file-path))
