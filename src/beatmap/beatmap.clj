(ns beatmap.beatmap
  (:gen-class)
  (:require [beatmap.config :as config]
            [beatmap.tokens :as tokens]
            [beatmap.operations :as ops]))

(defn display-missing-tokens
  "Display information about missing tokens."
  []
  (println "⚠️  Some tokens are missing:")
  (doseq [[token-name _] (tokens/missing-tokens)]
    (println (str "   - " token-name))))

(defn display-help
  "Display help information about available commands."
  []
  (println "🎵 Beatmap - Apple Music Library Exporter")
  (println "")
  (println "Available commands:")
  (println "  albums     - Export your albums to CSV file")
  (println "  playlists  - Export playlists to separate CSV files (editable/non-editable)")
  (println "  help       - Show this help message")
  (println "")
  (println "Examples:")
  (println "  lein run albums")
  (println "  lein run playlists"))

(defn greet
  "Callable entry point to the application."
  [opts]
  (if (tokens/validate-tokens)
    (do
      (println "✅ All tokens are configured")
      (let [command (first (:args opts))]
        (case command
          "albums" (ops/try-process-albums "resources/catalog/albums.csv")
          "playlists" (ops/try-process-playlists "resources/catalog/playlists_personal.csv" "resources/catalog/playlists_apple_music.csv")
          "help" (display-help)
          (do
            (println "🎵 Welcome to Beatmap!")
            (display-help)))))
    (display-missing-tokens)))

(defn -main
  "Main entry point for the application."
  [& args]
  (greet {:args args}))
