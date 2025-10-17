(ns beatmap.spotify
  (:require [beatmap.operations :as ops]))

(defn display-spotify-help
  "Display help for Spotify subcommands."
  []
  (println "🎵 Beatmap - Spotify Commands")
  (println "")
  (println "Available Spotify subcommands:")
  (println "  followed-artists  - Export your followed artists from Spotify")
  (println "  top-artists       - Export your top artists from Spotify")
  (println "")
  (println "Options:")
  (println "  --simple          - Export only artist names (without genres, popularity, followers)")
  (println "  --time-range=X    - For top-artists: 'short_term', 'medium_term' (default), or 'long_term'")
  (println "")
  (println "Usage:")
  (println "  make run-cmd CMD=\"spotify followed-artists\"")
  (println "  make run-cmd CMD=\"spotify followed-artists --simple\"")
  (println "  make run-cmd CMD=\"spotify top-artists\"")
  (println "  make run-cmd CMD=\"spotify top-artists --time-range=long_term\"")
  (println "  make run-cmd CMD=\"spotify top-artists --simple --time-range=short_term\""))

(defn parse-spotify-options
  "Parse command-line options for Spotify commands.

   Args:
     args: Vector of command-line arguments

   Returns:
     Map with :simple and :time-range keys"
  [args]
  (let [simple? (some #(= "--simple" %) args)
        time-range-arg (first (filter #(clojure.string/starts-with? % "--time-range=") args))
        time-range (when time-range-arg
                     (subs time-range-arg 13))]
    {:simple simple?
     :time-range (or time-range "medium_term")}))

(defn try-spotify-followed-artists
  "Export followed artists with error handling."
  [simple?]
  (try
    (let [filename (if simple?
                     "resources/catalog/spotify/followed_artists_names.csv"
                     "resources/catalog/spotify/followed_artists.csv")]
      (ops/try-process-spotify-followed-artists filename :detailed (not simple?)))
    (catch Exception e
      (println (str "❌ Error exporting followed artists: " (.getMessage e))))))

(defn try-spotify-top-artists
  "Export top artists with error handling."
  [simple? time-range]
  (try
    (let [time-suffix (case time-range
                        "long_term" "_long"
                        "short_term" "_short"
                        "_medium")
          filename (if simple?
                     (str "resources/catalog/spotify/top_artists" time-suffix "_names.csv")
                     (str "resources/catalog/spotify/top_artists" time-suffix ".csv"))]
      (ops/try-process-spotify-top-artists filename :time-range time-range :detailed (not simple?)))
    (catch Exception e
      (println (str "❌ Error exporting top artists: " (.getMessage e))))))

(defn handle-spotify-command
  "Handle Spotify command with subcommands.

   Args:
     subcommand: The Spotify subcommand to execute
     args: Additional command-line arguments

   Examples:
     (handle-spotify-command \"followed-artists\" [])
     (handle-spotify-command \"top-artists\" [\"--simple\"])
     (handle-spotify-command \"help\" [])"
  [subcommand & args]
  (let [options (parse-spotify-options args)]
    (case subcommand
      "followed-artists" (try-spotify-followed-artists (:simple options))
      "top-artists" (try-spotify-top-artists (:simple options) (:time-range options))
      "help" (display-spotify-help)
      nil (display-spotify-help)
      (do
        (println (str "❌ Unknown Spotify subcommand: " subcommand))
        (println "Run 'spotify help' to see available subcommands.")))))
