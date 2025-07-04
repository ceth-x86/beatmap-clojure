(ns beatmap.operations
  (:require [beatmap.apple-music.albums :as albums]
            [beatmap.apple-music.playlists :as playlists]
            [beatmap.csv-export.albums :as albums-csv]
            [beatmap.csv-export.playlists :as playlists-csv]
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

(defn process-apple-music-playlists
  "Fetch and save Apple Music playlists to separate CSV files based on canEdit field."
  [editable-filename non-editable-filename]
  (println "🎵 Fetching playlists from your Apple Music library...")
  (let [playlists (playlists/get-playlists-with-pagination)]
    (if (empty? playlists)
      (println "⚠️  No playlists found in your library")
      (let [        file-paths (playlists-csv/write-playlists-separated-to-csv 
                    playlists 
                    editable-filename
                    non-editable-filename)]
        (println (str "💾 Successfully saved playlists to separate files"))
        file-paths))))

(defn try-process-playlists
  "Process Apple Music playlists with separation by canEdit field and error handling."
  [editable-filename non-editable-filename]
  (try
    (process-apple-music-playlists editable-filename non-editable-filename)
    (catch Exception e
      (println (str "❌ Error during Apple Music playlists separation: " (.getMessage e)))))) 