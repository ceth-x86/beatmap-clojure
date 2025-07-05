(ns beatmap.apple-music.core
  (:require [clj-http.client :as http]
            [cheshire.core :as json]
            [beatmap.tokens :as tokens]
            [clojure.string :as str]))

(def apple-music-base-url "https://api.music.apple.com/v1")

(defn get-developer-token [] (tokens/get-developer-token))
(defn get-user-token [] (tokens/get-user-token))

(defn make-apple-music-request
  "Make a GET request to Apple Music API with proper headers and optional params.
   Now supports automatic retries on failure.
   
   Args:
     endpoint: The API endpoint path (e.g., '/me/library/albums')
     params (optional): Query parameters to include in the request
     retries (optional): Number of retry attempts (default 3)
     retry-delay-ms (optional): Delay between retries in ms (default 2000)
   
   Returns:
     Parsed JSON response as a Clojure map, or nil if the response body is empty.
   
   Throws:
     ex-info: If developer or user tokens are missing, or if the API request fails after all retries.
   
   Examples:
     (make-apple-music-request '/me/library/albums')
     (make-apple-music-request '/me/library/albums' :params {:limit 25 :offset 0})"
  [endpoint & {:keys [params retries retry-delay-ms] :or {retries 3 retry-delay-ms 2000}}]
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
                        request-opts)]
      (loop [attempt 1]
        (let [response (try
                         (http/get url final-opts)
                         (catch Exception e
                           {:status :exception :exception e}))]
          (if (and (map? response) (= 200 (:status response)))
            (if (some? (:body response))
              (json/parse-string (:body response) true)
              nil)
            (if (< attempt retries)
              (do
                (println (str "⚠️  Apple Music API request failed (attempt " attempt "/" retries ") for " endpoint ". Retrying in " retry-delay-ms "ms..."))
                (Thread/sleep retry-delay-ms)
                (recur (inc attempt)))
              (throw (ex-info "Apple Music API request failed after retries"
                              {:status (:status response)
                               :body (:body response)
                               :exception (:exception response)}))))))))) 