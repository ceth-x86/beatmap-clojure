(ns beatmap.enrich
  (:require [beatmap.operations :as ops]))

(defn display-enrich-help
  "Display help for enrich subcommands."
  []
  (println "🎵 Beatmap - Enrich Commands")
  (println "")
  (println "Available enrich subcommands:")
  (println "  artist_by_countries - Enrich artists.csv with country information")
  (println "")
  (println "Usage:")
  (println "  make run-cmd CMD=\"enrich artist_by_countries\"")
  (println "  clojure -M -m beatmap.beatmap enrich artist_by_countries"))

(defn try-enrich-artist-by-countries
  "Enrich artists with country information with error handling."
  []
  (try
    (ops/try-process-enrich-artist-by-countries)
    (catch Exception e
      (println (str "❌ Error enriching artists with countries: " (.getMessage e))))))

(defn handle-enrich-command
  "Handle enrich command with subcommands.
   
   Args:
     subcommand: The enrich subcommand to execute
   
   Examples:
     (handle-enrich-command \"artist_by_countries\")
     (handle-enrich-command \"help\")"
  [subcommand]
  (case subcommand
    "artist_by_countries" (try-enrich-artist-by-countries)
    "help" (display-enrich-help)
    nil (display-enrich-help)
    (do
      (println (str "❌ Unknown enrich subcommand: " subcommand))
      (println "Run 'enrich help' to see available subcommands."))))