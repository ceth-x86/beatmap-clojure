(ns beatmap.csv-export.tracks
  (:require [beatmap.csv-export.utils :as utils]
            [clojure.java.io :as io]
            [beatmap.apple-music.playlists :as playlists]))

(defn track-to-csv-row
  "Convert a track map to a CSV row.
   
   Args:
     track: A map containing track data from Apple Music API
   
   Returns:
     A vector representing a CSV row with track information"
  [track]
  (let [attributes (:attributes track)
        artist-name (get-in attributes [:artistName] "Unknown Artist")
        album-name (get-in attributes [:albumName] "Unknown Album")
        track-name (get-in attributes [:name] "Unknown Track")
        release-date (let [full-date (get-in attributes [:releaseDate] "Unknown")]
                       (if (and (string? full-date) (not= full-date "Unknown"))
                         (try
                           (subs full-date 0 4)  ; Extract year from YYYY-MM-DD format
                           (catch Exception _
                             full-date))
                         full-date))
        genre (get-in attributes [:genreNames] [])
        genre-str (if (seq genre) (first genre) "Unknown")
        duration-ms (get-in attributes [:durationInMillis] 0)
        duration-min (if (pos? duration-ms)
                      (clojure.string/replace (format "%.2f" (/ duration-ms 60000.0)) "," ".")
                      "0.00")]
    [artist-name album-name track-name release-date genre-str duration-min]))

(defn save-playlist-tracks-to-csv
  "Save tracks from a playlist to a CSV file.
   
   Args:
     playlist-id: The ID of the playlist
     playlist-name: The name of the playlist
     tracks: A sequence of track maps
     output-dir: Directory to save the CSV file (defaults to 'resources/catalog/playlist_tracks')
   
   Returns:
     The path to the created CSV file, or nil if failed
   
   The CSV file will be named: playlist_tracks_{playlist-id}.csv
   Columns: Artist, Album, Track, Release Date, Genre, Duration (minutes)"
  [playlist-id playlist-name tracks & {:keys [output-dir] :or {output-dir "resources/catalog/playlist_tracks"}}]
  (try
    ;; Create output directory if it doesn't exist
    (.mkdirs (io/file output-dir))
    
    ;; Create safe filename from playlist name
    (let [safe-name (-> playlist-name
                        (clojure.string/replace #"[^\w\s-]" "")  ; Remove special chars except spaces and hyphens
                        (clojure.string/replace #"\s+" "_")      ; Replace spaces with underscores
                        (clojure.string/trim-newline))               ; Trim newlines from start/end
          filename (str safe-name ".csv")
          filepath (str output-dir "/" filename)
          csv-rows (map track-to-csv-row tracks)
          header ["Artist" "Album" "Track" "Year" "Genre" "Duration (min)"]
          all-rows (cons header csv-rows)]
      
      (with-open [writer (io/writer filepath)]
        (doseq [row all-rows]
          (let [escaped-row (map utils/escape-csv-field row)]
            (.write writer (str (clojure.string/join "," escaped-row) "\n")))))
      
      (println (str "✅ Written " (count tracks) " tracks to " filepath))
      filepath)
    
    (catch Exception e
      (println (str "❌ Error saving tracks for playlist '" playlist-name "': " (.getMessage e)))
      nil)))

(defn process-playlist-tracks
  "Process tracks for a single playlist and save to CSV.
   
   Args:
     playlist: A playlist map containing :id and :attributes
     tracks: A sequence of track maps for this playlist
     output-dir: Directory to save the CSV file
   
   Returns:
     A map with :success boolean and :filepath string (if successful)"
  [playlist tracks & {:keys [output-dir] :or {output-dir "resources/catalog/playlist_tracks"}}]
  (let [playlist-id (:id playlist)
        playlist-name (get-in playlist [:attributes :name] "Unknown Playlist")]
    (println (str "🎵 Processing tracks for playlist: " playlist-name " (" (count tracks) " tracks)"))
    
    (if-let [filepath (save-playlist-tracks-to-csv playlist-id playlist-name tracks :output-dir output-dir)]
      {:success true :filepath filepath :track-count (count tracks)}
      {:success false :filepath nil :track-count 0})))

(defn process-all-playlist-tracks
  "Process tracks for all editable playlists and save each to a separate CSV file.
   
   Args:
     playlists: A sequence of playlist maps
     tracks-fn: A function that takes playlist-id and playlist-name and returns tracks
     output-dir: Directory to save CSV files
   
   Returns:
     A map with summary information about the processing
   
   Note: Only processes playlists with canEdit = true
   
   Example:
     (process-all-playlist-tracks playlists 
       (fn [playlist-id playlist-name] 
         (get-playlist-tracks-with-pagination playlist-id playlist-name)))"
  [playlists tracks-fn & {:keys [output-dir] :or {output-dir "resources/catalog/playlist_tracks"}}]
  (let [editable-playlists (filter #(get-in % [:attributes :canEdit] false) playlists)]
    (println (str "🎵 Processing tracks for " (count editable-playlists) " editable playlists (out of " (count playlists) " total)..."))
    
    (let [results (doall
                    (for [playlist editable-playlists]
                      (let [playlist-id (:id playlist)
                            playlist-name (get-in playlist [:attributes :name] "Unknown Playlist")
                            tracks (tracks-fn playlist-id playlist-name)]
                        (process-playlist-tracks playlist tracks :output-dir output-dir))))
          
          successful (filter :success results)
          failed (filter (complement :success) results)
          total-tracks (reduce + (map :track-count successful))]
      
      (println (str "📊 Playlist tracks processing summary:"))
      (println (str "   Total playlists: " (count playlists)))
      (println (str "   Editable playlists: " (count editable-playlists)))
      (println (str "   Successful: " (count successful) " playlists"))
      (println (str "   Failed: " (count failed) " playlists"))
      (println (str "   Total tracks processed: " total-tracks))
      (println (str "   Output directory: " output-dir))
      
      {:total-playlists (count playlists)
       :editable-playlists (count editable-playlists)
       :successful-count (count successful)
       :failed-count (count failed)
       :total-tracks total-tracks
       :output-dir output-dir
       :results results})))

(defn export-playlist-tracks-to-csv
  "Export tracks from editable playlists to individual CSV files."
  [playlists tracks-output-dir]
  (let [tracks-fn (fn [playlist-id playlist-name]
                    (playlists/get-playlist-tracks-with-pagination playlist-id playlist-name))]
    (process-all-playlist-tracks playlists tracks-fn :output-dir tracks-output-dir))) 