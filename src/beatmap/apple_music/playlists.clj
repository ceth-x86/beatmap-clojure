(ns beatmap.apple-music.playlists
  (:require [beatmap.apple-music.core :as core]))

(defn get-user-playlists
  "Get user's playlists from Apple Music library.
   
   Makes a single API request to fetch playlists with optional pagination parameters.
   This is a low-level function that handles one API call at a time.
   
   Args:
     limit (optional): Maximum number of playlists to return in this request.
     offset (optional): Number of playlists to skip (for pagination).
   
   Returns:
     A map containing the API response with playlist data in the :data key,
     or nil if the request fails.
   
   Examples:
     (get-user-playlists :limit 25)           ; Get first 25 playlists
     (get-user-playlists :limit 10 :offset 25) ; Get 10 playlists starting from offset 25"
  [& {:keys [limit offset]}]
  (let [params (cond-> {}
                 limit (assoc :limit limit)
                 offset (assoc :offset offset))]
    (core/make-apple-music-request "/me/library/playlists" :params params)))

(defn get-playlists-with-pagination
  "Get playlists from user's library with pagination and progress display.
   
   This function fetches playlists from the Apple Music API using pagination to handle
   large libraries efficiently. It displays real-time progress information and
   includes a 1-second delay between requests to respect API rate limits.
   
   Args:
     limit (optional): Maximum number of playlists to fetch. If nil, fetches all available playlists.
     page-size (optional): Number of playlists per API request. Defaults to 25.
   
   Returns:
     A sequence of playlist maps from the user's library.
   
   Examples:
     (get-playlists-with-pagination :limit 50)     ; Get first 50 playlists
     (get-playlists-with-pagination :limit 25 :page-size 10)  ; Get 25 playlists, 10 per page
     (get-playlists-with-pagination)                ; Get all playlists
   
   Progress Output:
     - Shows current page being fetched
     - Displays playlists loaded per page
     - Shows running total of playlists collected
     - Indicates when limit is reached or no more playlists available"
  [& {:keys [limit page-size] :or {page-size 25}}]
  (loop [offset 0
         all-playlists []
         page-num 1]
    (if (and limit (>= (count all-playlists) limit))
      (do
        (println (str "✅ Reached limit of " limit " playlists. Total fetched: " (count all-playlists)))
        (take limit all-playlists))
      (do
        (println (str "📄 Fetching playlists page " page-num " (offset: " offset ")..."))
        (let [response (get-user-playlists :limit page-size :offset offset)]
          (if (and response (:data response))
            (let [playlists (:data response)
                  total-count (count playlists)]
              (println (str "✅ Page " page-num " loaded: " total-count " playlists"))
              (println (str "📊 Total playlists so far: " (+ (count all-playlists) total-count)))
              (if (zero? total-count)
                (do
                  (println "🎉 No more playlists available.")
                  all-playlists)
                (do
                  (Thread/sleep 1000)
                  (recur (+ offset page-size)
                         (concat all-playlists playlists)
                         (inc page-num)))))
            (do
              (println "❌ No response or data received for page" page-num)
              all-playlists)))))))

(defn get-playlist-tracks
  "Get tracks from a specific playlist.
   
   Makes a single API request to fetch tracks from a playlist with optional pagination parameters.
   This is a low-level function that handles one API call at a time.
   
   Args:
     playlist-id: The ID of the playlist to fetch tracks from.
     limit (optional): Maximum number of tracks to return in this request.
     offset (optional): Number of tracks to skip (for pagination).
   
   Returns:
     A map containing the API response with track data in the :data key,
     or nil if the request fails.
   
   Examples:
     (get-playlist-tracks \"playlist-id\" :limit 25)           ; Get first 25 tracks
     (get-playlist-tracks \"playlist-id\" :limit 10 :offset 25) ; Get 10 tracks starting from offset 25"
  [playlist-id & {:keys [limit offset]}]
  (let [params (cond-> {}
                 limit (assoc :limit limit)
                 offset (assoc :offset offset))]
    (core/make-apple-music-request (str "/me/library/playlists/" playlist-id "/tracks") :params params)))

(defn get-playlist-tracks-with-pagination
  "Get all tracks from a specific playlist with pagination and progress display.
   
   This function fetches tracks from a playlist using pagination to handle
   large playlists efficiently. It displays real-time progress information and
   includes a 1-second delay between requests to respect API rate limits.
   
   Args:
     playlist-id: The ID of the playlist to fetch tracks from.
     playlist-name: The name of the playlist (for progress display).
     limit (optional): Maximum number of tracks to fetch. If nil, fetches all available tracks.
     page-size (optional): Number of tracks per API request. Defaults to 25.
   
   Returns:
     A sequence of track maps from the playlist.
   
   Examples:
     (get-playlist-tracks-with-pagination \"playlist-id\" \"My Playlist\" :limit 50)
     (get-playlist-tracks-with-pagination \"playlist-id\" \"My Playlist\" :limit 25 :page-size 10)
     (get-playlist-tracks-with-pagination \"playlist-id\" \"My Playlist\")
   
   Progress Output:
     - Shows current page being fetched
     - Displays tracks loaded per page
     - Shows running total of tracks collected
     - Indicates when limit is reached or no more tracks available"
  [playlist-id playlist-name & {:keys [limit page-size] :or {page-size 25}}]
  (loop [offset 0
         all-tracks []
         page-num 1]
    (if (and limit (>= (count all-tracks) limit))
      (do
        (println (str "✅ Reached limit of " limit " tracks for playlist '" playlist-name "'. Total fetched: " (count all-tracks)))
        (take limit all-tracks))
      (do
        (println (str "📄 Fetching tracks page " page-num " for playlist '" playlist-name "' (offset: " offset ")..."))
        (let [response (try
                         (get-playlist-tracks playlist-id :limit page-size :offset offset)
                         (catch Exception e
                           nil))]
          (if (and response (:data response))
            (let [tracks (:data response)
                  total-count (count tracks)]
              (println (str "✅ Page " page-num " loaded: " total-count " tracks"))
              (println (str "📊 Total tracks so far: " (+ (count all-tracks) total-count)))
              (if (zero? total-count)
                (do
                  (println (str "🎉 No more tracks available for playlist '" playlist-name "'."))
                  all-tracks)
                (do
                  (Thread/sleep 1000)
                  (recur (+ offset page-size)
                         (concat all-tracks tracks)
                         (inc page-num)))))
            (do
              all-tracks)))))))

;; Example usage:
;; (get-user-playlists :limit 5)                    ; Single API call for 5 playlists
;; (get-user-playlists :limit 10 :offset 5)         ; Single API call with offset
;; (get-playlists-with-pagination :limit 50)        ; Get first 50 playlists with progress
;; (get-playlists-with-pagination :limit 100 :page-size 10)  ; Custom page size
;; (get-playlists-with-pagination)                  ; Get all playlists with progress
;; (get-playlist-tracks "playlist-id" :limit 5)     ; Single API call for 5 tracks
;; (get-playlist-tracks-with-pagination "playlist-id" "My Playlist" :limit 50) ; Get tracks with progress