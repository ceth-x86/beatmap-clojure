(ns beatmap.beatmap
  (:gen-class)
  (:require [beatmap.config :as config]
            [beatmap.tokens :as tokens]))

(defn greet
  "Callable entry point to the application."
  [data]
  (let [app-config (config/merge-configs)
        app-name (config/get-config app-config :app :name)]
    (println (str "Hello, " (or (:name data) "World") "!"))
    (println (str "Running " app-name " application"))
    
    ;; Check if tokens are configured
    (if (tokens/validate-tokens)
      (println "✅ All tokens are configured")
      (do
        (println "⚠️  Some tokens are missing:")
        (doseq [[token-name _] (tokens/missing-tokens)]
          (println (str "   - " token-name)))))))

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (greet {:name (first args)}))
