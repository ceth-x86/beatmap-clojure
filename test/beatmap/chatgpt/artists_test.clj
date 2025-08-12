(ns beatmap.chatgpt.artists-test
  (:require [clojure.test :refer :all]
            [beatmap.chatgpt.artists :as chatgpt-artists]))

(deftest test-create-artist-country-prompt
  (testing "Create prompt for artist country lookup"
    (let [artists ["The Beatles" "AC/DC" "U2"]
          prompt (chatgpt-artists/create-artist-country-prompt artists)]
      (is (re-find #"The Beatles" prompt))
      (is (re-find #"AC/DC" prompt))
      (is (re-find #"U2" prompt))
      (is (re-find #"JSON format" prompt))
      (is (re-find #"country of origin" prompt)))))

(deftest test-parse-artist-countries-response
  (testing "Parse successful response"
    (let [response {:success true
                   :content "{\"The Beatles\": \"United Kingdom\", \"AC/DC\": \"Australia\"}"}
          parsed (chatgpt-artists/parse-artist-countries-response response)]
      (is (= parsed {"The Beatles" "United Kingdom", "AC/DC" "Australia"}))))
  
  (testing "Parse failed response"
    (let [response {:success false
                   :error "API error"}
          parsed (chatgpt-artists/parse-artist-countries-response response)]
      (is (= parsed {}))))
  
  (testing "Parse response with invalid JSON"
    (let [response {:success true
                   :content "not valid json"}
          parsed (chatgpt-artists/parse-artist-countries-response response)]
      (is (= parsed {})))))

(deftest test-get-artist-countries
  (testing "Get artist countries for empty list"
    (let [result (chatgpt-artists/get-artist-countries [])]
      (is (= result {}))))
  
  (testing "Get artist countries with mocked ChatGPT response"
    (with-redefs [chatgpt-artists/batch-process-artists 
                  (fn [artists & {:keys [batch-size]}]
                    {"The Beatles" "United Kingdom" 
                     "AC/DC" "Australia"
                     "U2" "Ireland"})]
      (let [result (chatgpt-artists/get-artist-countries ["The Beatles" "AC/DC" "U2"])]
        (is (= result {"The Beatles" "United Kingdom" 
                       "AC/DC" "Australia"
                       "U2" "Ireland"}))))))