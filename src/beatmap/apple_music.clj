(ns beatmap.apple-music
  (:require [clj-http.client :as http]
            [cheshire.core :as json]
            [beatmap.tokens :as tokens]
            [clojure.string :as str]))

(def apple-music-base-url "https://api.music.apple.com/v1")

(defn get-developer-token [] (tokens/get-developer-token))
(defn get-user-token [] (tokens/get-user-token))

(defn make-apple-music-request
  "Make a GET request to Apple Music API with proper headers and optional params.
   
   This is the core function that handles all HTTP requests to the Apple Music API.
   It automatically includes authentication headers and handles response parsing.
   
   Args:
     endpoint: The API endpoint path (e.g., '/me/library/albums')
     params (optional): Query parameters to include in the request
   
   Returns:
     Parsed JSON response as a Clojure map, or nil if the response body is empty.
   
   Throws:
     ex-info: If developer or user tokens are missing, or if the API request fails.
   
   Examples:
     (make-apple-music-request '/me/library/albums')
     (make-apple-music-request '/me/library/albums' :params {:limit 25 :offset 0})"
  [endpoint & {:keys [params]}]
  (let [developer-token (get-developer-token)
        user-token (get-user-token)
        url (str apple-music-base-url endpoint)
        headers {"Authorization" (str "Bearer " developer-token)
                 "Music-User-Token" user-token}]
    (when (or (str/blank? developer-token) (str/blank? user-token))
      (throw (ex-info "Both developer and user tokens are required for Apple Music API" {})))
    (let [request-opts {:headers headers
                        :throw-exceptions false}
          final-opts (if params
                        (assoc request-opts :query-params (into {} (map (fn [[k v]] [(name k) (str v)]) params)))
                        request-opts)
          response (http/get url final-opts)]
      (if (= 200 (:status response))
        (if (some? (:body response))
          (json/parse-string (:body response) true)
          nil)
        (throw (ex-info "Apple Music API request failed"
                        {:status (:status response)
                         :body (:body response)}))))))

(defn get-user-albums
  "Get user's albums from Apple Music library.
   
   Makes a single API request to fetch albums with optional pagination parameters.
   This is a low-level function that handles one API call at a time.
   
   Args:
     limit (optional): Maximum number of albums to return in this request.
     offset (optional): Number of albums to skip (for pagination).
   
   Returns:
     A map containing the API response with album data in the :data key,
     or nil if the request fails.
   
   Examples:
     (get-user-albums :limit 25)           ; Get first 25 albums
     (get-user-albums :limit 10 :offset 25) ; Get 10 albums starting from offset 25"
  [& {:keys [limit offset]}]
  (let [params (cond-> {}
                 limit (assoc :limit limit)
                 offset (assoc :offset offset))]
    (make-apple-music-request "/me/library/albums" :params params)))

(defn get-albums-with-pagination
  "Get albums from user's library with pagination and progress display.
   
   This function fetches albums from the Apple Music API using pagination to handle
   large libraries efficiently. It displays real-time progress information and
   includes a 1-second delay between requests to respect API rate limits.
   
   Args:
     limit (optional): Maximum number of albums to fetch. If nil, fetches all available albums.
     page-size (optional): Number of albums per API request. Defaults to 25.
   
   Returns:
     A sequence of album maps from the user's library.
   
   Examples:
     (get-albums-with-pagination :limit 100)     ; Get first 100 albums
     (get-albums-with-pagination :limit 50 :page-size 10)  ; Get 50 albums, 10 per page
     (get-albums-with-pagination)                ; Get all albums
   
   Progress Output:
     - Shows current page being fetched
     - Displays albums loaded per page
     - Shows running total of albums collected
     - Indicates when limit is reached or no more albums available"
  [& {:keys [limit page-size] :or {page-size 25}}]
  (loop [offset 0
         all-albums []
         page-num 1]
    (if (and limit (>= (count all-albums) limit))
      (do
        (println (str "✅ Reached limit of " limit " albums. Total fetched: " (count all-albums)))
        (take limit all-albums))
      (do
        (println (str "📄 Fetching page " page-num " (offset: " offset ")..."))
        (let [response (get-user-albums :limit page-size :offset offset)]
          (if (and response (:data response))
            (let [albums (:data response)
                  total-count (count albums)]
              (println (str "✅ Page " page-num " loaded: " total-count " albums"))
              (println (str "📊 Total albums so far: " (+ (count all-albums) total-count)))
              (if (zero? total-count)
                (do
                  (println "🎉 No more albums available.")
                  all-albums)
                (do
                  (Thread/sleep 1000)
                  (recur (+ offset page-size)
                         (concat all-albums albums)
                         (inc page-num)))))
            (do
              (println "❌ No response or data received for page" page-num)
              all-albums)))))))



;; Example usage:
;; (get-user-albums :limit 5)                    ; Single API call for 5 albums
;; (get-user-albums :limit 10 :offset 5)         ; Single API call with offset
;; (get-albums-with-pagination :limit 50)        ; Get first 50 albums with progress
;; (get-albums-with-pagination :limit 100 :page-size 10)  ; Custom page size
;; (get-albums-with-pagination)                  ; Get all albums with progress 