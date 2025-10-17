(ns beatmap.spotify.core
  (:require [clj-http.client :as http]
            [cheshire.core :as json]
            [beatmap.tokens :as tokens]
            [clojure.string :as str]))

(def spotify-base-url "https://api.spotify.com/v1")

(defn get-access-token [] (tokens/get-spotify-token))

(defn build-request-opts
  "Build HTTP request options with headers and optional query parameters."
  [headers params]
  (let [request-opts {:headers headers
                      :throw-exceptions false}]
    (if params
      (assoc request-opts :query-params (into {} (map (fn [[k v]] [(name k) (str v)]) params)))
      request-opts)))

(defn should-retry?
  "Determine if a request should be retried based on status code."
  [status attempt retries]
  (and (< attempt retries)
       (not (contains? #{404 400 403 401} status))))

(defn make-single-request
  "Make a single HTTP request to the Spotify API."
  [url request-opts]
  (try
    (http/get url request-opts)
    (catch Exception e
      {:status :exception :exception e})))

(defn parse-successful-response
  "Parse successful API response."
  [response]
  (if (some? (:body response))
    (json/parse-string (:body response) true)
    nil))

(defn handle-request-with-retries
  "Handle API request with retry logic."
  [url request-opts endpoint retries retry-delay-ms]
  (loop [attempt 1]
    (let [response (make-single-request url request-opts)]
      (if (and (map? response) (= 200 (:status response)))
        (parse-successful-response response)
        (let [status (:status response)]
          (if (should-retry? status attempt retries)
            (do
              (println (str "⚠️  Spotify API request failed (attempt " attempt "/" retries ") for " endpoint " with status " status ". Retrying in " retry-delay-ms "ms..."))
              (Thread/sleep retry-delay-ms)
              (recur (inc attempt)))
            (throw (ex-info "Spotify API request failed after retries"
                            {:status (:status response)
                             :body (:body response)
                             :exception (:exception response)}))))))))

(defn make-spotify-request
  "Make a GET request to Spotify API with proper headers and optional params.
   Supports automatic retries on failure.

   Args:
     endpoint: The API endpoint path (e.g., '/me/following?type=artist')
     params (optional): Query parameters to include in the request
     retries (optional): Number of retry attempts (default 3)
     retry-delay-ms (optional): Delay between retries in ms (default 2000)

   Returns:
     Parsed JSON response as a Clojure map, or nil if the response body is empty.

   Throws:
     ex-info: If access token is missing, or if the API request fails after all retries.

   Examples:
     (make-spotify-request '/me/following' :params {:type 'artist' :limit 20})
     (make-spotify-request '/me/top/artists' :params {:limit 50})"
  [endpoint & {:keys [params retries retry-delay-ms] :or {retries 3 retry-delay-ms 2000}}]
  (let [access-token (get-access-token)
        url (str spotify-base-url endpoint)
        headers {"Authorization" (str "Bearer " access-token)}]
    (when (str/blank? access-token)
      (throw (ex-info "Spotify access token is required for Spotify API" {})))

    (let [request-opts (build-request-opts headers params)]
      (handle-request-with-retries url request-opts endpoint retries retry-delay-ms))))
