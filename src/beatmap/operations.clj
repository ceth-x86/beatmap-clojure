(ns beatmap.operations
  (:require [beatmap.apple-music :as apple-music]
            [beatmap.csv-export :as csv]
            [beatmap.entities :as entities]))

(defn process-apple-music-albums
  "Fetch, sort, and save Apple Music albums to CSV."
  [filename]
  (println "🎵 Fetching first 100 albums for main application...")
  (let [albums (apple-music/get-albums-with-pagination)]
    (if (empty? albums)
      (println "⚠️  No albums found in your library")
      (let [sorted-albums (entities/sort-albums-by-artist-year-name albums)
            file-path (csv/write-albums-to-csv sorted-albums :filename filename)]
        (println (str "💾 Saving " (count sorted-albums) " albums to CSV file..."))
        (println (str "✅ Successfully saved albums to: " file-path))))))

(defn try-process-albums
  "Process Apple Music albums with error handling."
  [filename]
  (try
    (process-apple-music-albums filename)
    (catch Exception e
      (println (str "❌ Error during Apple Music integration: " (.getMessage e)))))) 