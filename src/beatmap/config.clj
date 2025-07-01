(ns beatmap.config
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]))

(defn load-config
  "Load configuration from resources/config.edn"
  []
  (try
    (-> "config.edn"
        io/resource
        slurp
        edn/read-string)
    (catch Exception e
      (println "Warning: Could not load config.edn:" (.getMessage e))
      {})))

(defn load-local-config
  "Load local configuration from config/local.edn (if exists)"
  []
  (try
    (let [local-config-file (io/file "config/local.edn")]
      (if (.exists local-config-file)
        (-> local-config-file slurp edn/read-string)
        {}))
    (catch Exception e
      (println "Warning: Could not load local config:" (.getMessage e))
      {})))

(defn merge-configs
  "Merge base config with local config"
  []
  (merge (load-config) (load-local-config)))

(defn get-config
  "Get configuration value by key path"
  [config & keys]
  (get-in config keys))

(defn get-secret
  "Get secret value by key"
  [config secret-key]
  (get-in config [:secrets secret-key]))

;; Example usage:
;; (def config (merge-configs))
;; (get-config config :api :base-url)
;; (get-secret config :api-token) 