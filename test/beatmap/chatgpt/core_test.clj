(ns beatmap.chatgpt.core-test
  (:require [clojure.test :refer :all]
            [beatmap.chatgpt.core :as chatgpt]
            [clj-http.client]))

(deftest test-create-chat-request
  (testing "Create basic chat request"
    (let [messages [{:role "user" :content "Hello"}]
          request (chatgpt/create-chat-request messages)]
      (is (= (:model request) "gpt-3.5-turbo"))
      (is (= (:messages request) messages))
      (is (= (:temperature request) 0.1))
      (is (= (:max_tokens request) 1000))))
  
  (testing "Create chat request with custom parameters"
    (let [messages [{:role "user" :content "Hello"}]
          request (chatgpt/create-chat-request messages
                                              :model "gpt-4"
                                              :temperature 0.5
                                              :max-tokens 2000)]
      (is (= (:model request) "gpt-4"))
      (is (= (:temperature request) 0.5))
      (is (= (:max_tokens request) 2000)))))

(deftest test-send-chat-request-mocked
  (testing "Send chat request with mocked HTTP response"
    (with-redefs [clj-http.client/post 
                  (fn [url opts]
                    {:status 200
                     :body {:choices [{:message {:content "{\"result\": \"success\"}"}}]
                            :usage {:total_tokens 150}}})]
      (let [request {:model "gpt-3.5-turbo"
                     :messages [{:role "user" :content "Test"}]
                     :temperature 0.1
                     :max_tokens 1000}
            response (beatmap.chatgpt.core/send-chat-request request)]
        (is (:success response))
        (is (= (:content response) "{\"result\": \"success\"}"))
        (is (= (:tokens-used response) 150)))))
  
  (testing "Send chat request with API error"
    (with-redefs [clj-http.client/post 
                  (fn [url opts]
                    {:status 400
                     :body {:error {:message "Invalid request"}}})]
      (let [request {:model "gpt-3.5-turbo"
                     :messages [{:role "user" :content "Test"}]}
            response (beatmap.chatgpt.core/send-chat-request request)]
        (is (not (:success response)))
        (is (re-find #"API request failed" (:error response))))))
  
  (testing "Send chat request with network exception"
    (with-redefs [clj-http.client/post 
                  (fn [url opts]
                    (throw (Exception. "Network error")))]
      (let [request {:model "gpt-3.5-turbo"
                     :messages [{:role "user" :content "Test"}]}
            response (beatmap.chatgpt.core/send-chat-request request)]
        (is (not (:success response)))
        (is (re-find #"Request failed" (:error response)))))))

(deftest test-chat-completion-mocked
  (testing "Chat completion with mocked response"
    (with-redefs [beatmap.chatgpt.core/send-chat-request
                  (fn [request]
                    {:success true
                     :content "Mocked ChatGPT response"
                     :tokens-used 100})]
      (let [response (beatmap.chatgpt.core/chat-completion "Test prompt")]
        (is (:success response))
        (is (= (:content response) "Mocked ChatGPT response"))
        (is (= (:tokens-used response) 100))))))

(deftest test-parse-json-response
  (testing "Parse valid JSON"
    (let [json-str "{\"artist\": \"The Beatles\", \"country\": \"United Kingdom\"}"
          parsed (chatgpt/parse-json-response json-str)]
      (is (= parsed {"artist" "The Beatles" "country" "United Kingdom"}))))
  
  (testing "Parse JSON with markdown code blocks"
    (let [json-str "```json\n{\"artist\": \"AC/DC\", \"country\": \"Australia\"}\n```"
          parsed (chatgpt/parse-json-response json-str)]
      (is (= parsed {"artist" "AC/DC" "country" "Australia"}))))
  
  (testing "Parse invalid JSON returns nil"
    (let [invalid-json "not json at all"
          parsed (chatgpt/parse-json-response invalid-json)]
      (is (nil? parsed)))))