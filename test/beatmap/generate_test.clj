(ns beatmap.generate-test
  (:require [clojure.test :refer :all]
            [clojure.java.io :as io]
            [beatmap.generate :as generate]))

(deftest test-handle-generate-command-artists
  (testing "Handle generate artists command"
    (let [albums-file "test_generate_albums.csv"
          artists-file "test-output/generated/test_artists.csv"
          albums-content "Artist,Year,Album\nThe Beatles,1969,Abbey Road\nPink Floyd,1973,Dark Side of the Moon\nThe Beatles,1970,Let It Be"]
      (try
        ;; Create test albums file
        (spit albums-file albums-content)
        ;; Test the generate artists command (this will use default paths, so we can't easily test the output)
        ;; For now, just ensure it doesn't throw an exception and returns a result
        (is (string? (generate/handle-generate-command "artists")))
        (finally
          (when (.exists (io/file albums-file)) 
            (io/delete-file albums-file)))))))

(deftest test-handle-generate-command-help
  (testing "Handle generate help command"
    ;; Should not throw an exception
    (is (nil? (generate/handle-generate-command "help")))
    (is (nil? (generate/handle-generate-command nil)))))

(deftest test-handle-generate-command-unknown
  (testing "Handle unknown generate command"
    ;; Should not throw an exception
    (is (nil? (generate/handle-generate-command "unknown-command")))))