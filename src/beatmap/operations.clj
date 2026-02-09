(ns beatmap.operations
  (:require [beatmap.apple-music.albums :as albums]
            [beatmap.apple-music.playlists :as playlists]
            [beatmap.csv.albums :as albums-csv]
            [beatmap.csv.playlists :as playlists-csv]
            [beatmap.csv.tracks :as tracks-csv]
            [beatmap.csv.artists :as artists-csv]
            [beatmap.entities :as entities]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.stacktrace :as stacktrace]
            [beatmap.chatgpt.artists :as chatgpt-artists]))

(defn process-apple-music-albums
  "Fetch, sort, and save Apple Music albums to CSV."
  [filename]
  (println "🎵 Fetching first 100 albums for main application...")
  (let [albums (albums/get-albums-with-pagination)]
    (if (empty? albums)
      (println "⚠️  No albums found in your library")
      (let [sorted-albums (entities/sort-albums-by-artist-year-name albums)
            file-path (albums-csv/write-albums-to-csv sorted-albums :filename filename)]
        (println (str "💾 Saving " (count sorted-albums) " albums to CSV file..."))
        (println (str "✅ Successfully saved albums to: " file-path))))))
(defn try-process-albums
  "Process Apple Music albums with error handling."
  [filename]
  (try
    (process-apple-music-albums filename)
    (catch Exception e
      (println (str "❌ Error during Apple Music integration: " (.getMessage e)))
      (println (str "❌ Exception type: " (type e)))
      (println (str "❌ Stack trace: " (with-out-str (stacktrace/print-stack-trace e)))))))

(defn process-apple-music-playlists-and-tracks
  "Fetch and save Apple Music playlists to separate CSV files AND tracks from editable playlists.
   
   This function combines playlist export and track export functionality:
   1. Exports all playlists to separate CSV files (editable/non-editable)
   2. Exports tracks from editable playlists to individual CSV files
   
   Args:
     editable-filename: Filename for editable playlists CSV
     non-editable-filename: Filename for non-editable playlists CSV
     tracks-output-dir: Directory for playlist tracks CSV files
   
   Returns:
     A map with summary information about both operations"
  [editable-filename non-editable-filename tracks-output-dir]
  (println "🎵 Fetching playlists and their tracks from your Apple Music library...")
  (let [playlists (playlists/get-playlists-with-pagination)]
    (if (empty? playlists)
      (println "⚠️  No playlists found in your library")
      (let [playlist-results (playlists-csv/export-playlists-to-csv playlists editable-filename non-editable-filename)
            tracks-summary (tracks-csv/export-playlist-tracks-to-csv playlists tracks-output-dir)]
        (println (str "💾 Successfully exported playlists and tracks"))
        {:playlists playlist-results
         :tracks tracks-summary}))))

(defn try-process-playlists-and-tracks
  "Process Apple Music playlists and tracks with error handling."
  [editable-filename non-editable-filename tracks-output-dir]
  (try
    (process-apple-music-playlists-and-tracks editable-filename non-editable-filename tracks-output-dir)
    (catch Exception e
      (println (str "❌ Error during Apple Music playlists and tracks processing: " (.getMessage e))))))

(defn process-generate-artists
  "Generate artists CSV from existing albums CSV."
  [& {:keys [albums-file artists-file] 
      :or {albums-file "resources/catalog/albums.csv" 
           artists-file "resources/catalog/generated/artists.csv"}}]
  (println (str "📖 Reading albums from " albums-file "..."))
  (let [artists (albums-csv/read-albums-csv albums-file)]
    (if (empty? artists)
      (println "⚠️  No artists found in albums file")
      (do
        (println (str "🎨 Found " (count artists) " unique artists"))
        ;; Create output directory if it doesn't exist
        (let [output-dir (-> artists-file io/file .getParent)]
          (when output-dir
            (.mkdirs (io/file output-dir))))
        (artists-csv/write-artists-to-csv artists :filename artists-file)))))

(defn try-process-generate-artists
  "Process generate artists with error handling."
  [& {:keys [albums-file artists-file] 
      :or {albums-file "resources/catalog/albums.csv" 
           artists-file "resources/catalog/generated/artists.csv"}}]
  (try
    (process-generate-artists :albums-file albums-file :artists-file artists-file)
    (catch Exception e
      (println (str "❌ Error generating artists: " (.getMessage e))))))

;; Sample country mapping for demonstration
;; In a real implementation, this could be loaded from a file or fetched from an API
(def artist-country-mapping
  {"The Beatles" "United Kingdom"
   "Pink Floyd" "United Kingdom" 
   "Led Zeppelin" "United Kingdom"
   "Queen" "United Kingdom"
   "The Rolling Stones" "United Kingdom"
   "Radiohead" "United Kingdom"
   "Oasis" "United Kingdom"
   "Coldplay" "United Kingdom"
   "Arctic Monkeys" "United Kingdom"
   "Blur" "United Kingdom"
   "Iron Maiden" "United Kingdom"
   "Black Sabbath" "United Kingdom"
   "Deep Purple" "United Kingdom"
   "Judas Priest" "United Kingdom"
   "Def Leppard" "United Kingdom"
   "AC/DC" "Australia"
   "INXS" "Australia"
   "Metallica" "United States"
   "Nirvana" "United States"
   "Pearl Jam" "United States"
   "Soundgarden" "United States"
   "Alice In Chains" "United States"
   "Red Hot Chili Peppers" "United States"
   "Green Day" "United States"
   "Foo Fighters" "United States"
   "Linkin Park" "United States"
   "System Of A Down" "United States"
   "Tool" "United States"
   "Nine Inch Nails" "United States"
   "Marilyn Manson" "United States"
   "Slipknot" "United States"
   "Korn" "United States"
   "Limp Bizkit" "United States"
   "Rage Against the Machine" "United States"
   "Audioslave" "United States"
   "Stone Temple Pilots" "United States"
   "Jane's Addiction" "United States"
   "Faith No More" "United States"
   "Living Colour" "United States"
   "Guns N' Roses" "United States"
   "Aerosmith" "United States"
   "Kiss" "United States"
   "Van Halen" "United States"
   "Journey" "United States"
   "Boston" "United States"
   "Foreigner" "United States"
   "REO Speedwagon" "United States"
   "Styx" "United States"
   "Chicago" "United States"
   "Eagles" "United States"
   "Fleetwood Mac" "United States"
   "The Doors" "United States"
   "The Beach Boys" "United States"
   "The Byrds" "United States"
   "Buffalo Springfield" "United States"
   "Crosby, Stills & Nash" "United States"
   "Bob Dylan" "United States"
   "Neil Young" "United States"
   "Bruce Springsteen" "United States"
   "Tom Petty and the Heartbreakers" "United States"
   "R.E.M." "United States"
   "U2" "Ireland"
   "The Cranberries" "Ireland"
   "Thin Lizzy" "Ireland"
   "Rory Gallagher" "Ireland"
   "The Pogues" "Ireland"
   "Sinéad O'Connor" "Ireland"
   "Enya" "Ireland"
   "Van Morrison" "Northern Ireland"
   "Rammstein" "Germany"
   "Scorpions" "Germany"
   "Accept" "Germany"
   "Helloween" "Germany"
   "Blind Guardian" "Germany"
   "Kraftwerk" "Germany"
   "Can" "Germany"
   "Neu!" "Germany"
   "Tangerine Dream" "Germany"
   "Klaus Schulze" "Germany"
   "Daft Punk" "France"
   "Justice" "France"
   "Air" "France"
   "Phoenix" "France"
   "Gojira" "France"
   "Alcest" "France"
   "Björk" "Iceland"
   "Sigur Rós" "Iceland"
   "Of Monsters and Men" "Iceland"
   "Kaleo" "Iceland"
   "Múm" "Iceland"
   "Emilíana Torrini" "Iceland"
   "GusGus" "Iceland"
   "Nightwish" "Finland"
   "Children of Bodom" "Finland"
   "Amorphis" "Finland"
   "Insomnium" "Finland"
   "Wintersun" "Finland"
   "Stratovarius" "Finland"
   "Sonata Arctica" "Finland"
   "HIM" "Finland"
   "The Rasmus" "Finland"
   "Apocalyptica" "Finland"
   "ABBA" "Sweden"
   "Roxette" "Sweden"
   "Ace of Base" "Sweden"
   "Europe" "Sweden"
   "Yngwie Malmsteen" "Sweden"
   "Opeth" "Sweden"
   "Dark Tranquillity" "Sweden"
   "In Flames" "Sweden"
   "At the Gates" "Sweden"
   "Entombed" "Sweden"
   "Bathory" "Sweden"
   "Marduk" "Sweden"
   "Darkthrone" "Norway"
   "Mayhem" "Norway"
   "Burzum" "Norway"
   "Emperor" "Norway"
   "Immortal" "Norway"
   "Satyricon" "Norway"
   "Dimmu Borgir" "Norway"
   "Enslaved" "Norway"
   "a-ha" "Norway"
   "Kings of Convenience" "Norway"
   "Röyksopp" "Norway"
   "Aurora" "Norway"})

(defn read-artists-from-csv
  "Read artists from CSV file.
   
   Args:
     file-path: Path to the artists CSV file
   
   Returns:
     Vector of artist names (excluding header)"
  [file-path]
  (try
    (with-open [reader (io/reader file-path)]
      (let [lines (line-seq reader)
            data-lines (rest lines)]  ; Skip header
        (vec (map str/trim data-lines))))
    (catch Exception e
      (println (str "❌ Error reading artists CSV file '" file-path "': " (.getMessage e)))
      [])))

(defn enrich-artist-with-country
  "Enrich an artist name with country information.
   
   Args:
     artist: Artist name
   
   Returns:
     Vector of [artist, country]"
  [artist]
  [artist (get artist-country-mapping artist "Unknown")])

(defn write-enriched-artists-to-csv
  "Write enriched artists to CSV file.
   
   Args:
     enriched-artists: Collection of [artist, country] pairs
     file-path: Output file path
   
   Returns:
     The file path where CSV was written"
  [enriched-artists file-path]
  (let [header ["Artist" "Country"]
        csv-lines (map #(str/join "," (map (fn [field] 
                                             (if (or (str/includes? field ",") 
                                                     (str/includes? field "\"") 
                                                     (str/includes? field "\n"))
                                               (str "\"" (str/replace field "\"" "\"\"") "\"")
                                               field)) %)) 
                       (cons header enriched-artists))
        csv-content (str/join "\n" csv-lines)]
    (with-open [writer (io/writer file-path)]
      (.write writer csv-content))
    (println (str "✅ Written " (count enriched-artists) " artists with country information to " file-path))
    file-path))

(defn process-enrich-artist-by-countries
  "Enrich artists CSV with country information.
   
   Args:
     artists-file: Path to input artists CSV file
     enriched-file: Path to output enriched CSV file
   
   Returns:
     The file path where enriched CSV was written"
  [& {:keys [artists-file enriched-file] 
      :or {artists-file "resources/catalog/generated/artists.csv" 
           enriched-file "resources/catalog/enriched/artists_with_countries.csv"}}]
  (println (str "📖 Reading artists from " artists-file "..."))
  (let [artists (read-artists-from-csv artists-file)]
    (if (empty? artists)
      (println "⚠️  No artists found in file")
      (do
        ;; Get country mappings using ChatGPT
        (let [country-mappings (chatgpt-artists/get-artist-countries artists :batch-size 25)
              enriched-artists (map (fn [artist]
                                     [artist (get country-mappings artist "Unknown")])
                                   artists)
              known-countries (count (filter #(not= (second %) "Unknown") enriched-artists))
              unknown-countries (- (count enriched-artists) known-countries)]
          (println (str "📊 Final country mapping results:"))
          (println (str "   Known countries: " known-countries " artists"))
          (println (str "   Unknown countries: " unknown-countries " artists"))
          ;; Create output directory if it doesn't exist
          (let [output-dir (-> enriched-file io/file .getParent)]
            (when output-dir
              (.mkdirs (io/file output-dir))))
          (write-enriched-artists-to-csv enriched-artists enriched-file))))))

(defn try-process-enrich-artist-by-countries
  "Process enrich artist by countries with error handling."
  [& {:keys [artists-file enriched-file]
      :or {artists-file "resources/catalog/generated/artists.csv"
           enriched-file "resources/catalog/enriched/artists_with_countries.csv"}}]
  (try
    (process-enrich-artist-by-countries :artists-file artists-file :enriched-file enriched-file)
    (catch Exception e
      (println (str "❌ Error enriching artists with countries: " (.getMessage e))))))

