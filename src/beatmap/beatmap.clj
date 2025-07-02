(ns beatmap.beatmap
  (:gen-class)
  (:require [beatmap.config :as config]
            [beatmap.tokens :as tokens]
            [beatmap.operations :as ops]))

(defn display-missing-tokens
  "Display information about missing tokens."
  []
  (println "⚠️  Some tokens are missing:")
  (doseq [[token-name _] (tokens/missing-tokens)]
    (println (str "   - " token-name))))

(defn greet
  "Callable entry point to the application."
  [opts]
  (if (tokens/validate-tokens)
    (do
      (println "✅ All tokens are configured")
      (ops/try-process-albums "resources/catalog/albums.csv"))
    (display-missing-tokens)))

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (greet {}))
