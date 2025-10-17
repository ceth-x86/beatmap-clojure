(ns beatmap.csv.spotify-artists
  (:require [clojure.string :as str]
            [clojure.java.io :as io]
            [beatmap.csv.utils :as csv-utils]))

(defn extract-artist-info
  "Extract relevant information from a Spotify artist object.

   Args:
     artist: Spotify artist object from API

   Returns:
     Vector of [name, genres, popularity, followers]"
  [artist]
  (let [name (get artist :name "")
        genres (str/join "; " (get artist :genres []))
        popularity (get artist :popularity 0)
        followers (get-in artist [:followers :total] 0)]
    [name genres popularity followers]))

(defn write-spotify-artists-to-csv
  "Write Spotify artists to CSV file, sorted alphabetically by artist name.

   Args:
     artists: Collection of Spotify artist objects from API
     filename: Output filename (optional, defaults to 'resources/catalog/spotify/artists.csv')

   Returns:
     The file path where CSV was written"
  [artists & {:keys [filename] :or {filename "resources/catalog/spotify/artists.csv"}}]
  (let [header ["Artist" "Genres" "Popularity" "Followers"]
        ;; Sort artists alphabetically by name (case-insensitive)
        sorted-artists (sort-by #(str/lower-case (:name %)) artists)
        csv-rows (map extract-artist-info sorted-artists)
        csv-lines (map #(str/join "," (map csv-utils/escape-csv-field %)) (cons header csv-rows))
        csv-content (str/join "\n" csv-lines)
        file-path filename]
    ;; Create output directory if it doesn't exist
    (let [output-dir (-> file-path io/file .getParent)]
      (when output-dir
        (.mkdirs (io/file output-dir))))
    (with-open [writer (io/writer file-path)]
      (.write writer csv-content))
    (println (str "✅ Written " (count artists) " Spotify artists to " file-path))
    file-path))

(defn write-simple-artist-names-to-csv
  "Write simple list of Spotify artist names to CSV file, sorted alphabetically.

   Args:
     artists: Collection of Spotify artist objects from API
     filename: Output filename (optional, defaults to 'resources/catalog/spotify/artists_names.csv')

   Returns:
     The file path where CSV was written"
  [artists & {:keys [filename] :or {filename "resources/catalog/spotify/artists_names.csv"}}]
  (let [header ["Artist"]
        ;; Sort artists alphabetically by name (case-insensitive)
        sorted-artists (sort-by #(str/lower-case (:name %)) artists)
        artist-names (map :name sorted-artists)
        csv-rows (map vector artist-names)
        csv-lines (map #(str/join "," (map csv-utils/escape-csv-field %)) (cons header csv-rows))
        csv-content (str/join "\n" csv-lines)
        file-path filename]
    ;; Create output directory if it doesn't exist
    (let [output-dir (-> file-path io/file .getParent)]
      (when output-dir
        (.mkdirs (io/file output-dir))))
    (with-open [writer (io/writer file-path)]
      (.write writer csv-content))
    (println (str "✅ Written " (count artists) " Spotify artist names to " file-path))
    file-path))
