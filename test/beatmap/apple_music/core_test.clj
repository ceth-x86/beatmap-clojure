(ns beatmap.apple-music.core-test
  (:require [clojure.test :refer :all]
            [beatmap.apple-music.core :as core]))

(deftest make-apple-music-request-params-test
  (testing "make-apple-music-request builds params correctly"
    (with-redefs [core/get-developer-token (constantly "dev-token")
                  core/get-user-token (constantly "user-token")
                  clj-http.client/get (fn [url opts]
                                       (is (= "https://api.music.apple.com/v1/test-endpoint" url))
                                       (is (= "Bearer dev-token" (get-in opts [:headers "Authorization"])))
                                       (is (= "user-token" (get-in opts [:headers "Music-User-Token"])))
                                       (is (= {"limit" "5", "offset" "10"} (get opts :query-params)))
                                       {:status 200 :body "{\"data\":[]}"})]
      (core/make-apple-music-request "/test-endpoint" :params {:limit 5 :offset 10}))))

(deftest make-apple-music-request-no-params-test
  (testing "make-apple-music-request works without params"
    (with-redefs [core/get-developer-token (constantly "dev-token")
                  core/get-user-token (constantly "user-token")
                  clj-http.client/get (fn [url opts]
                                       (is (= "https://api.music.apple.com/v1/test-endpoint" url))
                                       (is (nil? (get opts :query-params)))
                                       {:status 200 :body "{\"data\":[]}"})]
      (core/make-apple-music-request "/test-endpoint"))))

(deftest make-apple-music-request-missing-tokens-test
  (testing "make-apple-music-request throws when tokens are missing"
    (with-redefs [core/get-developer-token (constantly "")
                  core/get-user-token (constantly "user-token")]
      (is (thrown? clojure.lang.ExceptionInfo 
                   (core/make-apple-music-request "/test-endpoint"))))
    
    (with-redefs [core/get-developer-token (constantly "dev-token")
                  core/get-user-token (constantly "")]
      (is (thrown? clojure.lang.ExceptionInfo 
                   (core/make-apple-music-request "/test-endpoint"))))))

(deftest make-apple-music-request-api-error-test
  (testing "make-apple-music-request throws on API error"
    (with-redefs [core/get-developer-token (constantly "dev-token")
                  core/get-user-token (constantly "user-token")
                  clj-http.client/get (fn [& _] {:status 500 :body "Server Error"})]
      (is (thrown? clojure.lang.ExceptionInfo 
                   (core/make-apple-music-request "/test-endpoint"))))))

(deftest make-apple-music-request-empty-response-test
  (testing "make-apple-music-request returns nil for empty response"
    (with-redefs [core/get-developer-token (constantly "dev-token")
                  core/get-user-token (constantly "user-token")
                  clj-http.client/get (fn [& _] {:status 200 :body nil})]
      (is (nil? (core/make-apple-music-request "/test-endpoint")))))) 