(ns beatmap.tokens-test
  (:require [clojure.test :refer :all]
            [beatmap.tokens :as tokens]
            [beatmap.config :as config]))

;; Mock configuration data
(def mock-config-with-all-tokens
  {:secrets {:developer-token "dev-token-123"
             :user-token "user-token-456"
             :openai-api-key "openai-key-789"}})

(def mock-config-with-some-tokens
  {:secrets {:developer-token "dev-token-123"
             :user-token nil
             :openai-api-key "openai-key-789"}})

(def mock-config-with-no-tokens
  {:secrets {:developer-token nil
             :user-token nil
             :openai-api-key nil}})

(deftest get-tokens-test
  (testing "returns all tokens when all are present"
    (with-redefs [config/merge-configs (constantly mock-config-with-all-tokens)]
      (let [result (tokens/get-tokens)]
        (is (= "dev-token-123" (:developer-token result)))
        (is (= "user-token-456" (:user-token result)))
        (is (= "openai-key-789" (:openai-api-key result))))))
  
  (testing "returns nil for missing tokens"
    (with-redefs [config/merge-configs (constantly mock-config-with-some-tokens)]
      (let [result (tokens/get-tokens)]
        (is (= "dev-token-123" (:developer-token result)))
        (is (nil? (:user-token result)))
        (is (= "openai-key-789" (:openai-api-key result))))))
  
  (testing "returns nil for all tokens when none are present"
    (with-redefs [config/merge-configs (constantly mock-config-with-no-tokens)]
      (let [result (tokens/get-tokens)]
        (is (nil? (:developer-token result)))
        (is (nil? (:user-token result)))
        (is (nil? (:openai-api-key result)))))))

(deftest get-developer-token-test
  (testing "returns developer token when present"
    (with-redefs [config/merge-configs (constantly mock-config-with-all-tokens)]
      (is (= "dev-token-123" (tokens/get-developer-token)))))
  
  (testing "returns nil when developer token is missing"
    (with-redefs [config/merge-configs (constantly mock-config-with-no-tokens)]
      (is (nil? (tokens/get-developer-token))))))

(deftest get-user-token-test
  (testing "returns user token when present"
    (with-redefs [config/merge-configs (constantly mock-config-with-all-tokens)]
      (is (= "user-token-456" (tokens/get-user-token)))))
  
  (testing "returns nil when user token is missing"
    (with-redefs [config/merge-configs (constantly mock-config-with-no-tokens)]
      (is (nil? (tokens/get-user-token))))))

(deftest get-openai-api-key-test
  (testing "returns OpenAI API key when present"
    (with-redefs [config/merge-configs (constantly mock-config-with-all-tokens)]
      (is (= "openai-key-789" (tokens/get-openai-api-key)))))
  
  (testing "returns nil when OpenAI API key is missing"
    (with-redefs [config/merge-configs (constantly mock-config-with-no-tokens)]
      (is (nil? (tokens/get-openai-api-key))))))

(deftest validate-tokens-test
  (testing "returns true when all tokens are present"
    (with-redefs [config/merge-configs (constantly mock-config-with-all-tokens)]
      (is (true? (tokens/validate-tokens)))))
  
  (testing "returns false when some tokens are missing"
    (with-redefs [config/merge-configs (constantly mock-config-with-some-tokens)]
      (is (false? (tokens/validate-tokens)))))
  
  (testing "returns false when all tokens are missing"
    (with-redefs [config/merge-configs (constantly mock-config-with-no-tokens)]
      (is (false? (tokens/validate-tokens))))))

(deftest missing-tokens-test
  (testing "returns empty list when all tokens are present"
    (with-redefs [config/merge-configs (constantly mock-config-with-all-tokens)]
      (is (empty? (tokens/missing-tokens)))))
  
  (testing "returns list of missing tokens when some are missing"
    (with-redefs [config/merge-configs (constantly mock-config-with-some-tokens)]
      (let [missing (tokens/missing-tokens)]
        (is (= 1 (count missing)))
        (is (= :user-token (first (first missing))))
        (is (nil? (second (first missing)))))))
  
  (testing "returns all tokens when all are missing"
    (with-redefs [config/merge-configs (constantly mock-config-with-no-tokens)]
      (let [missing (tokens/missing-tokens)]
        (is (= 3 (count missing)))
        (is (some #(= :developer-token (first %)) missing))
        (is (some #(= :user-token (first %)) missing))
        (is (some #(= :openai-api-key (first %)) missing))))))