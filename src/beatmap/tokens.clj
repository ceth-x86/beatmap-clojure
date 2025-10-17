(ns beatmap.tokens
  (:require [beatmap.config :as config]))

(defn get-tokens
  "Get all tokens from configuration"
  []
  (let [app-config (config/merge-configs)]
    {:developer-token (config/get-secret app-config :developer-token)
     :user-token (config/get-secret app-config :user-token)
     :openai-api-key (config/get-secret app-config :openai-api-key)
     :spotify-token (config/get-secret app-config :spotify-token)}))

(defn get-developer-token
  "Get developer token"
  []
  (let [app-config (config/merge-configs)]
    (config/get-secret app-config :developer-token)))

(defn get-user-token
  "Get user token"
  []
  (let [app-config (config/merge-configs)]
    (config/get-secret app-config :user-token)))

(defn get-openai-api-key
  "Get OpenAI API key"
  []
  (let [app-config (config/merge-configs)]
    (config/get-secret app-config :openai-api-key)))

(defn get-spotify-token
  "Get Spotify access token"
  []
  (let [app-config (config/merge-configs)]
    (config/get-secret app-config :spotify-token)))

(defn validate-tokens
  "Validate that all required tokens are present"
  []
  (let [tokens (get-tokens)]
    (every? some? (vals tokens))))

(defn missing-tokens
  "Get list of missing tokens"
  []
  (let [tokens (get-tokens)]
    (filter (fn [[k v]] (nil? v)) tokens)))

;; Example usage:
;; (require '[beatmap.tokens :as tokens])
;; (tokens/get-developer-token)
;; (tokens/get-openai-api-key)
;; (tokens/get-spotify-token)
;; (tokens/validate-tokens) 