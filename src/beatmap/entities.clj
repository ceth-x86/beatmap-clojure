(ns beatmap.entities
  (:require [beatmap.csv-export :as csv]
            [clojure.string :as str]))

;; Album functions
(defn sort-albums-by-artist-year-name
  "Sort albums by artist name, release year, and album name (case-insensitive)."
  [albums]
  (sort-by (fn [album]
             (let [attributes (:attributes album)
                   artist (str/lower-case (:artistName attributes))
                   year (csv/parse-release-date (:releaseDate attributes))
                   album-name (str/lower-case (:name attributes))]
               [artist year album-name]))
           albums))

