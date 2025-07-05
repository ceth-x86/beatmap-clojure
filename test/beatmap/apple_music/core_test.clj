(ns beatmap.apple-music.core-test
  (:require [clojure.test :refer :all]
            [beatmap.apple-music.core :as core]
            [beatmap.tokens :as tokens]
            [clojure.string :as str]
            [clj-http.client :as http]))

(deftest build-request-opts-test
  (testing "builds request options with headers only"
    (let [headers {"Authorization" "Bearer token"}
          result (core/build-request-opts headers nil)]
      (is (= {:headers headers :throw-exceptions false} result))))
  
  (testing "builds request options with headers and params"
    (let [headers {"Authorization" "Bearer token"}
          params {:limit 25 :offset 0}
          result (core/build-request-opts headers params)]
      (is (= {:headers headers 
              :throw-exceptions false
              :query-params {"limit" "25" "offset" "0"}} result))))
  
  (testing "converts keyword keys to strings in params"
    (let [headers {"Authorization" "Bearer token"}
          params {:limit 25 :offset 0 :type "albums"}
          result (core/build-request-opts headers params)]
      (is (= {"limit" "25" "offset" "0" "type" "albums"} 
             (:query-params result))))))

(deftest should-retry-test
  (testing "should retry on 5xx status codes"
    (is (true? (core/should-retry? 500 1 3)))
    (is (true? (core/should-retry? 502 1 3)))
    (is (true? (core/should-retry? 503 1 3))))
  
  (testing "should not retry on 4xx status codes"
    (is (false? (core/should-retry? 400 1 3)))
    (is (false? (core/should-retry? 401 1 3)))
    (is (false? (core/should-retry? 403 1 3)))
    (is (false? (core/should-retry? 404 1 3))))
  
  (testing "should not retry when max attempts reached"
    (is (false? (core/should-retry? 500 3 3)))
    (is (false? (core/should-retry? 502 2 2))))
  
  (testing "should retry when attempts remaining"
    (is (true? (core/should-retry? 500 1 3)))
    (is (true? (core/should-retry? 500 2 3)))))

(deftest parse-successful-response-test
  (testing "parses JSON body when present"
    (let [response {:status 200 :body "{\"data\":[{\"id\":\"123\"}]}"}
          result (core/parse-successful-response response)]
      (is (= {:data [{:id "123"}]} result))))
  
  (testing "returns nil when body is nil"
    (let [response {:status 200 :body nil}
          result (core/parse-successful-response response)]
      (is (nil? result))))
  
  (testing "returns nil when body is empty string"
    (let [response {:status 200 :body ""}
          result (core/parse-successful-response response)]
      (is (nil? result)))))

(deftest make-apple-music-request-test
  (testing "throws exception when developer token is missing"
    (with-redefs [tokens/get-developer-token (constantly nil)
                  tokens/get-user-token (constantly "user-token")]
      (is (thrown-with-msg? clojure.lang.ExceptionInfo 
                           #"Both developer and user tokens are required"
                           (core/make-apple-music-request "/test")))))
  
  (testing "throws exception when user token is missing"
    (with-redefs [tokens/get-developer-token (constantly "dev-token")
                  tokens/get-user-token (constantly nil)]
      (is (thrown-with-msg? clojure.lang.ExceptionInfo 
                           #"Both developer and user tokens are required"
                           (core/make-apple-music-request "/test")))))
  
  (testing "throws exception when both tokens are missing"
    (with-redefs [tokens/get-developer-token (constantly nil)
                  tokens/get-user-token (constantly nil)]
      (is (thrown-with-msg? clojure.lang.ExceptionInfo 
                           #"Both developer and user tokens are required"
                           (core/make-apple-music-request "/test")))))
  
  (testing "throws exception when both tokens are blank"
    (with-redefs [tokens/get-developer-token (constantly "")
                  tokens/get-user-token (constantly "")]
      (is (thrown-with-msg? clojure.lang.ExceptionInfo 
                           #"Both developer and user tokens are required"
                           (core/make-apple-music-request "/test"))))))

(deftest apple-music-base-url-test
  (testing "base URL is correct"
    (is (= "https://api.music.apple.com/v1" core/apple-music-base-url))))

(deftest make-single-request-test
  (testing "returns successful response"
    (with-redefs [http/get (fn [url opts] {:status 200 :body "{\"success\":true}"})]
      (let [result (core/make-single-request "http://test.com" {})]
        (is (= {:status 200 :body "{\"success\":true}"} result)))))
  
  (testing "returns exception response on error"
    (with-redefs [http/get (fn [url opts] (throw (ex-info "Network error" {})))]
      (let [result (core/make-single-request "http://test.com" {})]
        (is (= :exception (:status result)))
        (is (some? (:exception result)))))))

(deftest handle-request-with-retries-test
  (testing "returns parsed response on first success"
    (with-redefs [core/make-single-request (fn [url opts] {:status 200 :body "{\"data\":\"test\"}"})]
      (let [result (core/handle-request-with-retries "http://test.com" {} "/test" 3 100)]
        (is (= {:data "test"} result)))))
  
  (testing "retries on 5xx errors and succeeds"
    (let [call-count (atom 0)]
      (with-redefs [core/make-single-request (fn [url opts] 
                                              (swap! call-count inc)
                                              (if (= @call-count 1)
                                                {:status 500 :body "error"}
                                                {:status 200 :body "{\"data\":\"success\"}"}))]
        (let [result (core/handle-request-with-retries "http://test.com" {} "/test" 3 10)]
          (is (= {:data "success"} result))
          (is (= 2 @call-count))))))
  
  (testing "throws exception after max retries"
    (with-redefs [core/make-single-request (fn [url opts] {:status 500 :body "error"})]
      (is (thrown-with-msg? clojure.lang.ExceptionInfo 
                           #"Apple Music API request failed after retries"
                           (core/handle-request-with-retries "http://test.com" {} "/test" 2 10))))))
  
  (testing "does not retry on 4xx errors"
    (let [call-count (atom 0)]
      (with-redefs [core/make-single-request (fn [url opts] 
                                              (swap! call-count inc)
                                              {:status 404 :body "not found"})]
        (is (thrown-with-msg? clojure.lang.ExceptionInfo 
                             #"Apple Music API request failed after retries"
                             (core/handle-request-with-retries "http://test.com" {} "/test" 3 10)))
        (is (= 1 @call-count)))))
  
  (testing "returns nil when response body is empty"
    (with-redefs [core/make-single-request (fn [url opts] {:status 200 :body nil})]
      (let [result (core/handle-request-with-retries "http://test.com" {} "/test" 3 100)]
        (is (nil? result)))))

 