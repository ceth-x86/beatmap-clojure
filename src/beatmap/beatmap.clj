(ns beatmap.beatmap
  (:gen-class)
  (:require [beatmap.config :as config]
            [beatmap.tokens :as tokens]
            [beatmap.operations :as ops]
            [beatmap.generate :as generate]
            [beatmap.enrich :as enrich]))

(defn display-missing-tokens
  "Display information about missing tokens."
  []
  (println "⚠️  Some tokens are missing:")
  (doseq [[token-name _] (tokens/missing-tokens)]
    (println (str "   - " token-name))))

(defn display-help
  "Display help information about available commands."
  []
  (println "🎵 Beatmap - Music Library Exporter")
  (println "")
  (println "Available commands:")
  (println "  albums     - Export your albums to CSV file (Apple Music)")
  (println "  playlists  - Export playlists AND tracks from editable playlists (Apple Music)")
  (println "  generate   - Generate derived data from existing files")
  (println "  enrich     - Enrich existing data with additional information")
  (println "  help       - Show this help message")
  (println "")
  (println "Examples:")
  (println "  make run-cmd CMD=albums")
  (println "  make run-cmd CMD=playlists")
  (println "  make run-cmd CMD=\"generate artists\"")
  (println "  make run-cmd CMD=\"enrich artist_by_countries\""))

(defn greet
  "Callable entry point to the application."
  [opts]
  (if (tokens/validate-tokens)
    (do
      (println "✅ All tokens are configured")
      (let [command (first (:args opts))
            subcommand (second (:args opts))
            rest-args (drop 2 (:args opts))]
        (case command
          "albums" (ops/try-process-albums "resources/catalog/albums.csv")
          "playlists" (ops/try-process-playlists-and-tracks "resources/catalog/playlists_personal.csv" "resources/catalog/playlists_apple_music.csv" "resources/catalog/playlists")
          "generate" (generate/handle-generate-command subcommand)
          "enrich" (enrich/handle-enrich-command subcommand)
          "help" (display-help)
          (do
            (println "🎵 Welcome to Beatmap!")
            (display-help)))))
    (display-missing-tokens)))

(defn -main
  "Main entry point for the application."
  [& args]
  (greet {:args args}))
