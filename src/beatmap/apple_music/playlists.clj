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

;; Example usage:
;; (get-user-playlists :limit 5)                    ; Single API call for 5 playlists
;; (get-user-playlists :limit 10 :offset 5)         ; Single API call with offset
;; (get-playlists-with-pagination :limit 50)        ; Get first 50 playlists with progress
;; (get-playlists-with-pagination :limit 100 :page-size 10)  ; Custom page size
;; (get-playlists-with-pagination)                  ; Get all playlists with progress