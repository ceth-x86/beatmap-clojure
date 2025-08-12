(ns beatmap.generate
  (:require [beatmap.operations :as ops]))

(defn display-generate-help
  "Display help for generate subcommands."
  []
  (println "🎵 Beatmap - Generate Commands")
  (println "")
  (println "Available generate subcommands:")
  (println "  artists    - Generate artists.csv from existing albums.csv")
  (println "")
  (println "Usage:")
  (println "  make run-cmd CMD=\"generate artists\"")
  (println "  clojure -M -m beatmap.beatmap generate artists"))

(defn try-generate-artists
  "Generate artists CSV with error handling."
  []
  (try
    (ops/try-process-generate-artists)
    (catch Exception e
      (println (str "❌ Error generating artists: " (.getMessage e))))))

(defn handle-generate-command
  "Handle generate command with subcommands.
   
   Args:
     subcommand: The generate subcommand to execute
   
   Examples:
     (handle-generate-command \"artists\")
     (handle-generate-command \"help\")"
  [subcommand]
  (case subcommand
    "artists" (try-generate-artists)
    "help" (display-generate-help)
    nil (display-generate-help)
    (do
      (println (str "❌ Unknown generate subcommand: " subcommand))
      (println "Run 'generate help' to see available subcommands."))))