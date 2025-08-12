(ns beatmap.beatmap
  (:gen-class)
  (:require [beatmap.config :as config]
            [beatmap.tokens :as tokens]
            [beatmap.operations :as ops]
            [beatmap.generate :as generate]))

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
  (println "  playlists  - Export playlists AND tracks from editable playlists")
  (println "  generate   - Generate derived data from existing files")
  (println "  help       - Show this help message")
  (println "")
  (println "Examples:")
  (println "  lein run albums")
  (println "  lein run playlists")
  (println "  lein run generate artists"))

(defn greet
  "Callable entry point to the application."
  [opts]
  (if (tokens/validate-tokens)
    (do
      (println "✅ All tokens are configured")
      (let [command (first (:args opts))
            subcommand (second (:args opts))]
        (case command
          "albums" (ops/try-process-albums "resources/catalog/albums.csv")
          "playlists" (ops/try-process-playlists-and-tracks "resources/catalog/playlists_personal.csv" "resources/catalog/playlists_apple_music.csv" "resources/catalog/playlists")
          "generate" (generate/handle-generate-command subcommand)
          "help" (display-help)
          (do
            (println "🎵 Welcome to Beatmap!")
            (display-help)))))
    (display-missing-tokens)))

(defn -main
  "Main entry point for the application."
  [& args]
  (greet {:args args}))
