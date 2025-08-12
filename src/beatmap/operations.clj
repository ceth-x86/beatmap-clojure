(ns beatmap.operations
  (:require [beatmap.apple-music.albums :as albums]
            [beatmap.apple-music.playlists :as playlists]
            [beatmap.csv.albums :as albums-csv]
            [beatmap.csv.playlists :as playlists-csv]
            [beatmap.csv.tracks :as tracks-csv]
            [beatmap.csv.artists :as artists-csv]
            [beatmap.entities :as entities]
            [clojure.java.io :as io]))

(defn process-apple-music-albums
  "Fetch, sort, and save Apple Music albums to CSV."
  [filename]
  (println "🎵 Fetching first 100 albums for main application...")
  (let [albums (albums/get-albums-with-pagination)]
    (if (empty? albums)
      (println "⚠️  No albums found in your library")
      (let [sorted-albums (entities/sort-albums-by-artist-year-name albums)
            file-path (albums-csv/write-albums-to-csv sorted-albums :filename filename)]
        (println (str "💾 Saving " (count sorted-albums) " albums to CSV file..."))
        (println (str "✅ Successfully saved albums to: " file-path))))))

(defn try-process-albums
  "Process Apple Music albums with error handling."
  [filename]
  (try
    (process-apple-music-albums filename)
    (catch Exception e
      (println (str "❌ Error during Apple Music integration: " (.getMessage e))))))

(defn process-apple-music-playlists-and-tracks
  "Fetch and save Apple Music playlists to separate CSV files AND tracks from editable playlists.
   
   This function combines playlist export and track export functionality:
   1. Exports all playlists to separate CSV files (editable/non-editable)
   2. Exports tracks from editable playlists to individual CSV files
   
   Args:
     editable-filename: Filename for editable playlists CSV
     non-editable-filename: Filename for non-editable playlists CSV
     tracks-output-dir: Directory for playlist tracks CSV files
   
   Returns:
     A map with summary information about both operations"
  [editable-filename non-editable-filename tracks-output-dir]
  (println "🎵 Fetching playlists and their tracks from your Apple Music library...")
  (let [playlists (playlists/get-playlists-with-pagination)]
    (if (empty? playlists)
      (println "⚠️  No playlists found in your library")
      (let [playlist-results (playlists-csv/export-playlists-to-csv playlists editable-filename non-editable-filename)
            tracks-summary (tracks-csv/export-playlist-tracks-to-csv playlists tracks-output-dir)]
        (println (str "💾 Successfully exported playlists and tracks"))
        {:playlists playlist-results
         :tracks tracks-summary}))))

(defn try-process-playlists-and-tracks
  "Process Apple Music playlists and tracks with error handling."
  [editable-filename non-editable-filename tracks-output-dir]
  (try
    (process-apple-music-playlists-and-tracks editable-filename non-editable-filename tracks-output-dir)
    (catch Exception e
      (println (str "❌ Error during Apple Music playlists and tracks processing: " (.getMessage e))))))

(defn process-generate-artists
  "Generate artists CSV from existing albums CSV."
  [& {:keys [albums-file artists-file] 
      :or {albums-file "resources/catalog/albums.csv" 
           artists-file "resources/catalog/generated/artists.csv"}}]
  (println (str "📖 Reading albums from " albums-file "..."))
  (let [artists (albums-csv/read-albums-csv albums-file)]
    (if (empty? artists)
      (println "⚠️  No artists found in albums file")
      (do
        (println (str "🎨 Found " (count artists) " unique artists"))
        ;; Create output directory if it doesn't exist
        (let [output-dir (-> artists-file io/file .getParent)]
          (when output-dir
            (.mkdirs (io/file output-dir))))
        (artists-csv/write-artists-to-csv artists :filename artists-file)))))

(defn try-process-generate-artists
  "Process generate artists with error handling."
  [& {:keys [albums-file artists-file] 
      :or {albums-file "resources/catalog/albums.csv" 
           artists-file "resources/catalog/generated/artists.csv"}}]
  (try
    (process-generate-artists :albums-file albums-file :artists-file artists-file)
    (catch Exception e
      (println (str "❌ Error generating artists: " (.getMessage e))))))