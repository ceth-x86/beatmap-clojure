(ns beatmap.spotify.artists
  (:require [beatmap.spotify.core :as core]))

(defn get-followed-artists
  "Get user's followed artists from Spotify.

   Makes a single API request to fetch followed artists with optional pagination.
   This is a low-level function that handles one API call at a time.

   Args:
     limit (optional): Maximum number of artists to return in this request (max 50).
     after (optional): The last artist ID retrieved from the previous request (for pagination).

   Returns:
     A map containing the API response with artist data.

   Examples:
     (get-followed-artists :limit 50)
     (get-followed-artists :limit 50 :after \"artist-id-here\")"
  [& {:keys [limit after]}]
  (let [params (cond-> {:type "artist"}
                 limit (assoc :limit (min limit 50))
                 after (assoc :after after))]
    (core/make-spotify-request "/me/following" :params params)))

(defn get-all-followed-artists
  "Get all followed artists from user's Spotify account with pagination.

   This function fetches all followed artists using pagination to handle
   large numbers of followed artists. It displays real-time progress information
   and includes a 1-second delay between requests to respect API rate limits.

   Args:
     limit (optional): Maximum number of artists to fetch. If nil, fetches all followed artists.
     page-size (optional): Number of artists per API request. Defaults to 50 (max allowed).

   Returns:
     A sequence of artist maps.

   Examples:
     (get-all-followed-artists :limit 100)
     (get-all-followed-artists :limit 200 :page-size 50)
     (get-all-followed-artists)  ; Get all followed artists

   Progress Output:
     - Shows current page being fetched
     - Displays artists loaded per page
     - Shows running total of artists collected
     - Indicates when limit is reached or no more artists available"
  [& {:keys [limit page-size] :or {page-size 50}}]
  (loop [after nil
         all-artists []
         page-num 1]
    (if (and limit (>= (count all-artists) limit))
      (do
        (println (str "✅ Reached limit of " limit " artists. Total fetched: " (count all-artists)))
        (take limit all-artists))
      (do
        (println (str "📄 Fetching page " page-num (when after (str " (after: " after ")")) "..."))
        (let [response (get-followed-artists :limit page-size :after after)]
          (if (and response (get-in response [:artists :items]))
            (let [artists (get-in response [:artists :items])
                  total-count (count artists)
                  cursors (get-in response [:artists :cursors])
                  next-after (:after cursors)]
              (println (str "✅ Page " page-num " loaded: " total-count " artists"))
              (println (str "📊 Total artists so far: " (+ (count all-artists) total-count)))
              (if (zero? total-count)
                (do
                  (println "🎉 No more artists available.")
                  all-artists)
                (if (nil? next-after)
                  (do
                    (println "🎉 Fetched all followed artists.")
                    (concat all-artists artists))
                  (do
                    (Thread/sleep 1000)
                    (recur next-after
                           (concat all-artists artists)
                           (inc page-num))))))
            (do
              (println "❌ No response or data received for page" page-num)
              all-artists)))))))

(defn get-top-artists
  "Get user's top artists from Spotify.

   Fetches the user's top artists based on calculated affinity.

   Args:
     limit (optional): Maximum number of artists to return (max 50, default 20).
     time-range (optional): Time period for which to get top artists.
                           Valid values: 'long_term', 'medium_term', 'short_term'
                           Default: 'medium_term'
     offset (optional): Index of the first item to return (default 0).

   Returns:
     A map containing the API response with artist data.

   Examples:
     (get-top-artists :limit 50)
     (get-top-artists :limit 20 :time-range \"long_term\")
     (get-top-artists :limit 10 :offset 5)"
  [& {:keys [limit time-range offset] :or {limit 20 time-range "medium_term" offset 0}}]
  (let [params {:limit (min limit 50)
                :time_range time-range
                :offset offset}]
    (core/make-spotify-request "/me/top/artists" :params params)))

(defn get-all-top-artists
  "Get all top artists from user's Spotify account with pagination.

   Args:
     limit (optional): Maximum number of artists to fetch (default all available, max 50 per request).
     time-range (optional): Time period ('long_term', 'medium_term', 'short_term').
     page-size (optional): Number of artists per request (default 50).

   Returns:
     A sequence of artist maps."
  [& {:keys [limit time-range page-size] :or {time-range "medium_term" page-size 50}}]
  (loop [offset 0
         all-artists []
         page-num 1]
    (if (and limit (>= (count all-artists) limit))
      (do
        (println (str "✅ Reached limit of " limit " top artists. Total fetched: " (count all-artists)))
        (take limit all-artists))
      (do
        (println (str "📄 Fetching top artists page " page-num " (offset: " offset ")..."))
        (let [response (get-top-artists :limit page-size :time-range time-range :offset offset)]
          (if (and response (:items response))
            (let [artists (:items response)
                  total-count (count artists)]
              (println (str "✅ Page " page-num " loaded: " total-count " artists"))
              (println (str "📊 Total top artists so far: " (+ (count all-artists) total-count)))
              (if (zero? total-count)
                (do
                  (println "🎉 No more top artists available.")
                  all-artists)
                (do
                  (Thread/sleep 1000)
                  (recur (+ offset page-size)
                         (concat all-artists artists)
                         (inc page-num)))))
            (do
              (println "❌ No response or data received for page" page-num)
              all-artists)))))))

;; Example usage:
;; (get-followed-artists :limit 50)
;; (get-all-followed-artists)
;; (get-top-artists :limit 20 :time-range "long_term")
;; (get-all-top-artists :limit 100 :time-range "short_term")
