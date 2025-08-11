(ns beatmap.entities
  (:require [clojure.string :as str]))

(defn parse-release-date
  "Parse release date string and extract year.
   Returns year as string or 'Unknown' if parsing fails."
  [date-str]
  (if (or (nil? date-str) (str/blank? date-str))
    "Unknown"
    (try
      (let [year (subs date-str 0 4)]
        (if (and (= 4 (count year)) (re-matches #"\d{4}" year))
          year
          "Unknown"))
      (catch Exception _
        "Unknown"))))

;; Album functions
(defn sort-albums-by-artist-year-name
  "Sort albums by artist name, release year, and album name (case-insensitive)."
  [albums]
  (sort-by (fn [album]
             (let [attributes (:attributes album)
                   artist (str/lower-case (:artistName attributes))
                   year (parse-release-date (:releaseDate attributes))
                   album-name (str/lower-case (:name attributes))]
               [artist year album-name]))
           albums))

;; Playlist functions
(defn sort-playlists-by-name
  "Sort playlists alphabetically by name (case-insensitive)."
  [playlists]
  (sort-by #(str/lower-case (or (get-in % [:attributes :name]) "Untitled Playlist")) playlists))

