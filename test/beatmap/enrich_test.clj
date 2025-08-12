(ns beatmap.enrich-test
  (:require [clojure.test :refer :all]
            [beatmap.enrich :as enrich]
            [beatmap.operations :as ops]))

(deftest display-enrich-help-test
  (testing "Display enrich help text"
    (let [output (with-out-str (enrich/display-enrich-help))]
      (is (re-find #"Beatmap - Enrich Commands" output))
      (is (re-find #"artist_by_countries" output))
      (is (re-find #"enrich artist_by_countries" output)))))

(deftest handle-enrich-command-help-test
  (testing "Handle enrich help command"
    (let [output (with-out-str (enrich/handle-enrich-command "help"))]
      (is (re-find #"Beatmap - Enrich Commands" output)))))

(deftest handle-enrich-command-nil-test
  (testing "Handle enrich command with nil subcommand shows help"
    (let [output (with-out-str (enrich/handle-enrich-command nil))]
      (is (re-find #"Beatmap - Enrich Commands" output)))))

(deftest handle-enrich-command-unknown-test
  (testing "Handle unknown enrich subcommand"
    (let [output (with-out-str (enrich/handle-enrich-command "unknown"))]
      (is (re-find #"Unknown enrich subcommand: unknown" output))
      (is (re-find #"enrich help" output)))))

(deftest handle-enrich-command-artist-by-countries-test
  (testing "Handle artist_by_countries subcommand"
    (with-redefs [ops/try-process-enrich-artist-by-countries (fn [] "mock-success")]
      (enrich/handle-enrich-command "artist_by_countries"))))