(ns beatmap.apple-music.albums-test
  (:require [clojure.test :refer :all]
            [beatmap.apple-music.albums :as albums]
            [beatmap.apple-music.core :as core]))

;; Мокаем функцию make-apple-music-request для unit-тестов
(def mock-albums
  (mapv (fn [i]
          {:id (str i)
           :attributes {:artistName (str "Artist" i)
                        :name (str "Album" i)
                        :releaseDate (str (+ 2000 (mod i 20)) "-01-01")}})
        (range 1 101)))

(defn mock-get-user-albums
  [& {:keys [limit offset]}]
  (let [offset (or offset 0)
        limit (or limit 25)]
    {:data (subvec mock-albums offset (min (+ offset limit) (count mock-albums)))}))

(deftest get-albums-with-pagination-basic
  (with-redefs [albums/get-user-albums mock-get-user-albums]
    (testing "Fetch first 10 albums"
      (let [albums (albums/get-albums-with-pagination :limit 10)]
        (is (= 10 (count albums)))
        (is (= "Artist1" (get-in (first albums) [:attributes :artistName])))))
    (testing "Fetch all albums (limit > available)"
      (let [albums (albums/get-albums-with-pagination :limit 200)]
        (is (= 100 (count albums)))))
    (testing "Fetch with custom page size"
      (let [albums (albums/get-albums-with-pagination :limit 30 :page-size 7)]
        (is (= 30 (count albums)))
        (is (= "Artist30" (get-in (last albums) [:attributes :artistName])))))))

(deftest get-albums-with-pagination-empty
  (with-redefs [albums/get-user-albums (fn [& _] {:data []})]
    (testing "Returns empty when no albums"
      (let [albums (albums/get-albums-with-pagination :limit 10)]
        (is (empty? albums))))))

(deftest get-albums-with-pagination-error
  (with-redefs [albums/get-user-albums (fn [& _] nil)]
    (testing "Returns collected albums on error"
      (let [albums (albums/get-albums-with-pagination :limit 10)]
        (is (empty? albums))))))

(deftest get-user-albums-params
  (testing "get-user-albums builds params correctly"
    (with-redefs [core/make-apple-music-request (fn [endpoint & {:keys [params]}] params)]
      (is (= {:limit 5} (albums/get-user-albums :limit 5)))
      (is (= {:limit 10 :offset 20} (albums/get-user-albums :limit 10 :offset 20)))
      (is (= {} (albums/get-user-albums)))))) 