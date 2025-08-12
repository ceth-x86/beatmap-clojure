(ns beatmap.csv.playlists
  (:require [clojure.string :as str]
            [clojure.java.io :as io]
            [beatmap.entities :as entities]
            [beatmap.csv.utils :as csv-utils]))



(defn playlist-to-csv-row
  "Convert playlist data to CSV row format"
  [playlist]
  (let [attributes (:attributes playlist)
        name (or (:name attributes) "Untitled Playlist")
        description (let [desc (:description attributes)]
                      (cond
                        (string? desc) desc
                        (map? desc) (or (:standard desc) (first (vals desc)))
                        :else ""))
        last-modified-date (:lastModifiedDate attributes)
        can-edit (:canEdit attributes)]
    [name description last-modified-date can-edit]))

(defn write-playlists-to-csv
  "Write playlists to CSV file with columns: Playlist Name, Description, Last Modified, Can Edit.
   Returns the file path where CSV was written."
  [playlists & {:keys [filename] :or {filename "playlists.csv"}}]
  (let [header ["Playlist Name" "Description" "Last Modified" "Can Edit"]
        sorted-playlists (entities/sort-playlists-by-name playlists)
        csv-rows (map playlist-to-csv-row sorted-playlists)
        csv-lines (map #(str/join "," (map csv-utils/escape-csv-field %)) (cons header csv-rows))
        csv-content (str/join "\n" csv-lines)
        file-path filename]
    (with-open [writer (io/writer file-path)]
      (.write writer csv-content))
    (println (str "✅ Written " (count playlists) " playlists to " file-path))
    file-path))

(defn write-playlists-separated-to-csv
  "Write playlists to separate CSV files based on canEdit field.
   Returns a map with file paths for both files."
  [playlists editable-filename non-editable-filename]
  (let [sorted-playlists (entities/sort-playlists-by-name playlists)
        editable-playlists (filter #(get-in % [:attributes :canEdit]) sorted-playlists)
        non-editable-playlists (filter #(not (get-in % [:attributes :canEdit])) sorted-playlists)]
    
    ;; Export editable playlists
    (when (seq editable-playlists)
      (write-playlists-to-csv editable-playlists :filename editable-filename))
    
    ;; Export non-editable playlists
    (when (seq non-editable-playlists)
      (write-playlists-to-csv non-editable-playlists :filename non-editable-filename))
    
    (println (str "📊 Summary:"))
    (println (str "   Editable playlists: " (count editable-playlists) " -> " editable-filename))
    (println (str "   Non-editable playlists: " (count non-editable-playlists) " -> " non-editable-filename))
    
    {:editable editable-filename
     :non-editable non-editable-filename}))

(defn export-playlists-to-csv
  "Export playlists to separate CSV files based on canEdit field."
  [playlists editable-filename non-editable-filename]
  (write-playlists-separated-to-csv playlists editable-filename non-editable-filename))

;; Example usage:
;; (require '[beatmap.csv.playlists :as playlists-csv])
;; (playlists-csv/write-playlists-to-csv playlists :filename "my_playlists.csv")
;; (playlists-csv/write-playlists-separated-to-csv playlists) 