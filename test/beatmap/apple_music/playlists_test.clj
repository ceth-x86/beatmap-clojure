(ns beatmap.apple-music.playlists-test
  (:require [clojure.test :refer :all]
            [beatmap.apple-music.playlists :as playlists]
            [beatmap.apple-music.core :as core]))

;; Мокаем функцию make-apple-music-request для unit-тестов
(def mock-playlists
  (mapv (fn [i]
          {:id (str i)
           :attributes {:name (str "Playlist" i)
                        :description (str "Description for playlist " i)
                        :lastModifiedDate (str "2024-01-0" (mod i 9) "T10:00:00Z")
                        :canEdit (= 0 (mod i 2))}})
        (range 1 51)))

(def mock-tracks
  (mapv (fn [i]
          {:id (str "track-" i)
           :attributes {:name (str "Track " i)
                        :artistName (str "Artist " i)
                        :albumName (str "Album " i)
                        :releaseDate (str "2023-01-0" (mod i 9) "T00:00:00Z")
                        :genreNames [(str "Genre " i)]
                        :durationInMillis (* 180000 (+ 1 (mod i 10)))}})
        (range 1 101)))

(defn mock-get-user-playlists
  [& {:keys [limit offset]}]
  (let [offset (or offset 0)
        limit (or limit 25)]
    {:data (subvec mock-playlists offset (min (+ offset limit) (count mock-playlists)))}))

(defn mock-get-playlist-tracks
  [playlist-id & {:keys [limit offset]}]
  (let [offset (or offset 0)
        limit (or limit 25)]
    {:data (subvec mock-tracks offset (min (+ offset limit) (count mock-tracks)))}))

(deftest get-playlists-with-pagination-basic
  (with-redefs [playlists/get-user-playlists mock-get-user-playlists]
    (testing "Fetch first 10 playlists"
      (let [playlists (playlists/get-playlists-with-pagination :limit 10)]
        (is (= 10 (count playlists)))
        (is (= "Playlist1" (get-in (first playlists) [:attributes :name])))))
    (testing "Fetch all playlists (limit > available)"
      (let [playlists (playlists/get-playlists-with-pagination :limit 200)]
        (is (= 50 (count playlists)))))
    (testing "Fetch with custom page size"
      (let [playlists (playlists/get-playlists-with-pagination :limit 30 :page-size 7)]
        (is (= 30 (count playlists)))
        (is (= "Playlist30" (get-in (last playlists) [:attributes :name])))))))

(deftest get-playlists-with-pagination-empty
  (with-redefs [playlists/get-user-playlists (fn [& _] {:data []})]
    (testing "Returns empty when no playlists"
      (let [playlists (playlists/get-playlists-with-pagination :limit 10)]
        (is (empty? playlists))))))

(deftest get-playlists-with-pagination-error
  (with-redefs [playlists/get-user-playlists (fn [& _] nil)]
    (testing "Returns collected playlists on error"
      (let [playlists (playlists/get-playlists-with-pagination :limit 10)]
        (is (empty? playlists))))))

(deftest get-user-playlists-params
  (testing "get-user-playlists builds params correctly"
    (with-redefs [core/make-apple-music-request (fn [endpoint & {:keys [params]}] params)]
      (is (= {:limit 5} (playlists/get-user-playlists :limit 5)))
      (is (= {:limit 10 :offset 20} (playlists/get-user-playlists :limit 10 :offset 20)))
      (is (= {} (playlists/get-user-playlists))))))

(deftest get-playlist-tracks-params
  (testing "get-playlist-tracks builds params correctly"
    (with-redefs [core/make-apple-music-request (fn [endpoint & {:keys [params]}] params)]
      (is (= {:limit 5} (playlists/get-playlist-tracks "playlist-123" :limit 5)))
      (is (= {:limit 10 :offset 20} (playlists/get-playlist-tracks "playlist-123" :limit 10 :offset 20)))
      (is (= {} (playlists/get-playlist-tracks "playlist-123"))))))

(deftest get-playlist-tracks-endpoint
  (testing "get-playlist-tracks uses correct endpoint"
    (with-redefs [core/make-apple-music-request (fn [endpoint & _] endpoint)]
      (is (= "/me/library/playlists/playlist-123/tracks" 
             (playlists/get-playlist-tracks "playlist-123"))))))

(deftest get-playlist-tracks-with-pagination-basic
  (with-redefs [playlists/get-playlist-tracks mock-get-playlist-tracks]
    (testing "Fetch first 10 tracks"
      (let [tracks (playlists/get-playlist-tracks-with-pagination "playlist-123" "Test Playlist" :limit 10)]
        (is (= 10 (count tracks)))
        (is (= "Track 1" (get-in (first tracks) [:attributes :name])))))
    (testing "Fetch all tracks (limit > available)"
      (let [tracks (playlists/get-playlist-tracks-with-pagination "playlist-123" "Test Playlist" :limit 200)]
        (is (= 100 (count tracks)))))
    (testing "Fetch with custom page size"
      (let [tracks (playlists/get-playlist-tracks-with-pagination "playlist-123" "Test Playlist" :limit 30 :page-size 7)]
        (is (= 30 (count tracks)))
        (is (= "Track 30" (get-in (last tracks) [:attributes :name])))))))

(deftest get-playlist-tracks-with-pagination-empty
  (with-redefs [playlists/get-playlist-tracks (fn [& _] {:data []})]
    (testing "Returns empty when no tracks"
      (let [tracks (playlists/get-playlist-tracks-with-pagination "playlist-123" "Test Playlist" :limit 10)]
        (is (empty? tracks))))))

(deftest get-playlist-tracks-with-pagination-error
  (with-redefs [playlists/get-playlist-tracks (fn [& _] nil)]
    (testing "Returns collected tracks on error"
      (let [tracks (playlists/get-playlist-tracks-with-pagination "playlist-123" "Test Playlist" :limit 10)]
        (is (empty? tracks))))))

(deftest get-playlist-tracks-with-pagination-exception-handling
  (with-redefs [playlists/get-playlist-tracks (fn [& _] (throw (ex-info "API Error" {})))]
    (testing "Handles exceptions gracefully"
      (let [tracks (playlists/get-playlist-tracks-with-pagination "playlist-123" "Test Playlist" :limit 10)]
        (is (empty? tracks)))))) 