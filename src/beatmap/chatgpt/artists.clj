(ns beatmap.chatgpt.artists
  (:require [beatmap.chatgpt.core :as chatgpt]
            [beatmap.config :as config]
            [clojure.string :as str]
            [clojure.data.json :as json]
            [clojure.java.io :as io]))

(defn enriched-cache-file []
  (str (config/get-catalog-dir) "/enriched/artists_with_countries.csv"))

(defn load-cached-countries
  "Load existing artist-country mappings from cache file.
   
   Returns:
     Map of artist -> country from existing cache, or empty map if file doesn't exist"
  []
  (if (.exists (io/file (enriched-cache-file)))
    (try
      (with-open [reader (io/reader (enriched-cache-file))]
        (let [lines (line-seq reader)
              data-lines (rest lines)] ; Skip header "Artist,Country"
          (into {} (map (fn [line]
                         (let [parts (str/split line #"," 2)] ; Split into max 2 parts
                           [(first parts) (second parts)]))
                       data-lines))))
      (catch Exception e
        (println (str "⚠️ Failed to load cache file: " (.getMessage e)))
        {}))
    (do
      (println "📋 No existing cache file found, will create new one")
      {})))

(defn filter-uncached-artists
  "Filter out artists that already have known countries in cache.
   
   Args:
     artists: Collection of artist names
     cache: Map of artist -> country from cache
   
   Returns:
     Vector of artists that need to be processed (not in cache or have 'Unknown' country)"
  [artists cache]
  (let [uncached (filter (fn [artist]
                          (let [cached-country (get cache artist)]
                            (or (nil? cached-country)
                                (= cached-country "Unknown"))))
                        artists)]
    (when (seq uncached)
      (println (str "🔍 Found " (count uncached) " artists needing lookup (out of " (count artists) " total)")))
    (vec uncached)))

(defn merge-with-cache
  "Merge new results with existing cache data.
   
   Args:
     cache: Existing artist -> country mappings
     new-results: New artist -> country mappings
   
   Returns:
     Combined map with new results taking precedence"
  [cache new-results]
  (merge cache new-results))

(defn save-intermediate-results
  "Save intermediate results to cache file during processing.
   
   Args:
     cache: Existing cache data
     new-results: New results from current batch
     all-artists: Complete list of artists being processed
   
   Returns:
     Updated combined results"
  [cache new-results all-artists]
  (let [combined-results (merge-with-cache cache new-results)
        csv-rows (map (fn [artist]
                       [artist (get combined-results artist "Unknown")])
                     all-artists)
        csv-lines (cons ["Artist" "Country"] csv-rows)
        csv-content (str/join "\n" (map #(str/join "," %) csv-lines))]
    (try
      (spit (enriched-cache-file) csv-content)
      (println (str "💾 Saved intermediate results to " (enriched-cache-file) " (" (count combined-results) " total entries)"))
      combined-results
      (catch Exception e
        (println (str "⚠️ Failed to save intermediate results: " (.getMessage e)))
        combined-results))))

(defn create-artist-country-prompt
  "Create a prompt for ChatGPT to determine countries of origin for music artists.
   
   Args:
     artists: Collection of artist names
   
   Returns:
     Formatted prompt string"
  [artists]
  (let [artist-list (str/join "\n" (map #(str "- " %) artists))]
    (str "I need to determine the country of origin for the following music artists/bands. "
         "Please provide the information in JSON format where each key is the exact artist name "
         "and the value is the country name. Use full country names (e.g., 'United States', 'United Kingdom'). "
         "If an artist's country is unknown or ambiguous, use 'Unknown'.\n\n"
         "Artists:\n" artist-list "\n\n"
         "Please respond with only valid JSON in this format:\n"
         "{\n"
         "  \"Artist Name\": \"Country Name\",\n"
         "  \"Another Artist\": \"Another Country\"\n"
         "}")))

(defn parse-artist-countries-response
  "Parse ChatGPT response for artist countries.
   
   Args:
     response: Response map from ChatGPT API
   
   Returns:
     Map of artist -> country, or empty map if parsing fails"
  [response]
  (if (:success response)
    (if-let [parsed-data (chatgpt/parse-json-response (:content response))]
      parsed-data
      (do
        (println "⚠️ Failed to parse artist countries JSON response")
        {}))
    (do
      (println (str "❌ ChatGPT API request failed: " (:error response)))
      {})))

(defn batch-process-artists-with-logging
  "Process artists in batches to avoid token limits with detailed logging and intermediate saving.
   
   Args:
     artists: Collection of artist names
     batch-size: Number of artists per batch (default: 20)
     cache: Existing cache data
     all-artists: Complete list of all artists for final output
   
   Returns:
     Map of artist -> country for all processed artists"
  [artists & {:keys [batch-size cache all-artists] :or {batch-size 20 cache {} all-artists artists}}]
  (println (str "🤖 Processing " (count artists) " artists with ChatGPT in batches of " batch-size "..."))
  (let [artist-batches (partition-all batch-size artists)
        total-batches (count artist-batches)
        start-time (System/currentTimeMillis)]
    (println (str "📊 Batch processing details:"))
    (println (str "   Total batches: " total-batches))
    (println (str "   Batch size: " batch-size " artists"))
    (println (str "   Estimated tokens per batch: ~" (* batch-size 50) " tokens"))
    
    (loop [batches artist-batches
           batch-num 1
           results {}
           total-tokens 0]
      (if (empty? batches)
        (do
          (let [elapsed-time (/ (- (System/currentTimeMillis) start-time) 1000.0)]
            (println (str "🏁 Batch processing completed in " elapsed-time " seconds"))
            (println (str "💰 Total tokens used: " total-tokens))
            (when (> total-tokens 0)
              (println (str "💵 Estimated cost: $" (format "%.4f" (* total-tokens 0.0000015))))))
          results)
        (let [current-batch (first batches)
              remaining-batches (rest batches)
              batch-artists-str (str/join ", " (take 3 current-batch))
              batch-artists-display (if (> (count current-batch) 3)
                                     (str batch-artists-str "...")
                                     batch-artists-str)]
          (println (str "🔄 Processing batch " batch-num "/" total-batches))
          (println (str "   📋 Artists: " batch-artists-display))
          (println (str "   ⏱️ Batch " batch-num " of " total-batches " (" (count current-batch) " artists)..."))
          
          (let [prompt (create-artist-country-prompt current-batch)
                response (chatgpt/chat-completion prompt
                                                :system-message "You are a music expert with detailed knowledge about artists and bands from around the world. Always respond with valid JSON."
                                                :temperature 0.1
                                                :max-tokens 2000)
                batch-results (parse-artist-countries-response response)
                batch-tokens (or (:tokens-used response) 0)]
            
            (if (empty? batch-results)
              (do
                (println (str "❌ Batch " batch-num " failed, continuing with next batch..."))
                (recur remaining-batches (inc batch-num) results total-tokens))
              (let [updated-results (merge results batch-results)
                    combined-cache (save-intermediate-results cache updated-results all-artists)
                    success-count (count (filter #(not= (val %) "Unknown") batch-results))]
                (println (str "✅ Batch " batch-num " completed successfully"))
                (println (str "   📊 Results: " success-count "/" (count batch-results) " artists mapped"))
                (println (str "   💰 Tokens used this batch: " batch-tokens))
                (println (str "   📈 Progress: " (int (* 100 (/ batch-num total-batches))) "% complete"))
                (recur remaining-batches (inc batch-num) updated-results (+ total-tokens batch-tokens))))))))))

(defn print-enrichment-start 
  "Print start information for enrichment process."
  [total-count]
  (println "🎯 Starting artist country enrichment process")
  (println (str "📊 Total unique artists to process: " total-count))
  (println (str "💾 Loading existing cache from " (enriched-cache-file) "...")))

(defn print-cache-analysis
  "Print cache analysis information."
  [cache-size cached-known cached-unknown total-count uncached-count]
  (println "💾 Cache status:")
  (println (str "   Total entries in cache: " cache-size))
  (println (str "   Artists with known countries: " (count cached-known)))
  (println (str "   Artists with unknown countries: " (count cached-unknown)))
  (println (str "   Artists not in cache: " (- total-count (count cached-known) (count cached-unknown))))
  
  (println "📊 Processing breakdown:")
  (println (str "   ✅ Already cached (known): " (count cached-known) " artists"))
  (println (str "   🔄 Need ChatGPT lookup: " uncached-count " artists"))
  (println (str "   📈 Cache hit rate: " (int (* 100 (/ (count cached-known) total-count))) "%"))
  
  (when (seq cached-known)
    (println (str "📋 Sample cached artists: " (str/join ", " (take 5 cached-known))))))

(defn print-final-summary
  "Print final processing summary."
  [new-results combined-results]
  (let [success-count (count (filter #(not= (val %) "Unknown") new-results))
        unknown-count (- (count new-results) success-count)
        total-known (count (filter #(not= (val %) "Unknown") combined-results))
        total-unknown (- (count combined-results) total-known)]
    
    (println "📊 Final processing summary:")
    (println (str "   📥 New artists processed: " (count new-results)))
    (println (str "   ✅ Successfully mapped: " success-count))
    (println (str "   ❓ Unknown/failed: " unknown-count))
    (when (> success-count 0)
      (println (str "   📈 Success rate: " (int (* 100 (/ success-count (count new-results)))) "%")))
    
    (println "🎯 Overall enrichment results:")
    (println (str "   📊 Total artists in final dataset: " (count combined-results)))
    (println (str "   ✅ Artists with known countries: " total-known))
    (println (str "   ❓ Artists with unknown countries: " total-unknown))
    (println (str "   📈 Overall completion rate: " (int (* 100 (/ total-known (count combined-results)))) "%"))
    (println (str "   💾 Final cache size: " (count combined-results) " entries"))))

(defn process-cached-artists
  "Handle case where all artists are cached."
  [cache unique-artists]
  (println "✅ All artists found in cache with known countries, no ChatGPT calls needed!")
  (println (str "💰 Estimated API cost savings: $" (format "%.2f" (* (count unique-artists) 0.002))))
  cache)

(defn process-uncached-artists
  "Process artists that need ChatGPT lookup."
  [uncached-artists batch-size cache unique-artists]
  (println (str "🚀 Starting ChatGPT processing for " (count uncached-artists) " artists..."))
  (let [new-results (batch-process-artists-with-logging uncached-artists 
                                                       :batch-size batch-size 
                                                       :cache cache 
                                                       :all-artists unique-artists)
        combined-results (merge-with-cache cache new-results)]
    (print-final-summary new-results combined-results)
    combined-results))

(defn get-artist-countries
  "Get countries for a list of artists using ChatGPT with caching."
  [artists & {:keys [batch-size] :or {batch-size 20}}]
  (if (empty? artists)
    {}
    (let [unique-artists (distinct artists)
          total-count (count unique-artists)]
      
      (print-enrichment-start total-count)
      
      (let [cache (load-cached-countries)
            cache-size (count cache)
            cached-known (filter #(and (contains? cache %) 
                                      (not= (get cache %) "Unknown")) 
                                unique-artists)
            cached-unknown (filter #(and (contains? cache %) 
                                        (= (get cache %) "Unknown")) 
                                  unique-artists)
            uncached-artists (filter-uncached-artists unique-artists cache)]
        
        (print-cache-analysis cache-size cached-known cached-unknown total-count (count uncached-artists))
        
        (if (empty? uncached-artists)
          (process-cached-artists cache unique-artists)
          (process-uncached-artists uncached-artists batch-size cache unique-artists))))))