(ns beatmap.operations
  (:require [beatmap.apple-music.albums :as albums]
            [beatmap.apple-music.playlists :as playlists]
            [beatmap.csv-export.albums :as albums-csv]
            [beatmap.csv-export.playlists :as playlists-csv]
            [beatmap.csv-export.tracks :as tracks-csv]
            [beatmap.entities :as entities]))

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