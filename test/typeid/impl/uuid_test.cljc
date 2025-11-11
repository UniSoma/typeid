(ns typeid.impl.uuid-test
  (:require [clojure.test :refer [deftest is testing]]
    [typeid.impl.uuid :as uuid]
    [typeid.validation :as v]))

;; T027: UUIDv7 generation tests

(deftest generate-uuidv7-basic-test
  (testing "Generate UUIDv7 returns 16 bytes"
    (let [uuid-bytes (uuid/generate-uuidv7)]
      (is (v/valid-uuid-bytes? uuid-bytes))
      (is (= 16 #?(:clj (alength uuid-bytes)
                   :cljs (.-length uuid-bytes))))))

  (testing "Generate multiple UUIDs are unique"
    (let [uuids (repeatedly 100 uuid/generate-uuidv7)]
      ;; Convert to vectors for comparison
      (is (= 100 (count (distinct (map #(vec (seq %)) uuids))))
        "All generated UUIDs should be unique"))))

(deftest generate-uuidv7-version-test
  (testing "Generated UUID has version 7"
    (let [uuid-bytes (uuid/generate-uuidv7)
          ;; Version is in bits 48-51 (byte 6, high nibble)
          version-byte (aget uuid-bytes 6)
          version (bit-and (bit-shift-right version-byte 4) 0x0F)]
      (is (= 7 version)
        (str "UUID version should be 7, got: " version))))

  (testing "Generated UUID has correct variant"
    (let [uuid-bytes (uuid/generate-uuidv7)
          ;; Variant is in bits 64-65 (byte 8, high 2 bits)
          ;; Mask to unsigned byte first
          variant-byte (bit-and (aget uuid-bytes 8) 0xFF)
          variant (bit-and (bit-shift-right variant-byte 6) 0x03)]
      (is (= 2 variant)
        (str "UUID variant should be 2 (10 in binary), got: " variant)))))

(deftest generate-uuidv7-timestamp-test
  (testing "UUID timestamp is recent"
    (let [before-ms #?(:clj (System/currentTimeMillis)
                       :cljs (.now js/Date))
          uuid-bytes (uuid/generate-uuidv7)
          after-ms #?(:clj (System/currentTimeMillis)
                      :cljs (.now js/Date))
          ;; Extract timestamp from first 48 bits (6 bytes)
          ts-bytes (take 6 uuid-bytes)
          timestamp (reduce (fn [acc b]
                              (+ (* acc 256)
                                (bit-and #?(:clj b :cljs b) 0xFF)))
                      0
                      ts-bytes)]
      (is (>= timestamp before-ms)
        (str "UUID timestamp " timestamp " should be >= " before-ms))
      (is (<= timestamp after-ms)
        (str "UUID timestamp " timestamp " should be <= " after-ms))))

  (testing "UUIDs generated in sequence have increasing timestamps or same timestamp"
    (let [uuid1 (uuid/generate-uuidv7)
          uuid2 (uuid/generate-uuidv7)
          ts1 (reduce (fn [acc b]
                        (+ (* acc 256) (bit-and #?(:clj b :cljs b) 0xFF)))
                0
                (take 6 uuid1))
          ts2 (reduce (fn [acc b]
                        (+ (* acc 256) (bit-and #?(:clj b :cljs b) 0xFF)))
                0
                (take 6 uuid2))]
      ;; ts2 should be >= ts1 (same millisecond or later)
      (is (>= ts2 ts1)
        (str "Second UUID timestamp " ts2 " should be >= first " ts1)))))

(deftest generate-uuidv7-randomness-test
  (testing "UUIDs have random components"
    (let [uuids (repeatedly 10 uuid/generate-uuidv7)
          ;; Extract random bytes (bytes 8-15)
          random-parts (map #(vec (drop 8 %)) uuids)]
      ;; All random parts should be different
      (is (= 10 (count (distinct random-parts)))
        "Random components of UUIDs should all be different")))

  (testing "Random bytes are not all zeros"
    (dotimes [_ 20]
      (let [uuid-bytes (uuid/generate-uuidv7)
            random-bytes (drop 8 uuid-bytes)]
        (is (not (every? zero? random-bytes))
          "Random bytes should not all be zero")))))

(deftest generate-uuidv7-validation-test
  (testing "Generated UUIDs pass strict UUIDv7 validation"
    (dotimes [_ 10]
      (let [uuid-bytes (uuid/generate-uuidv7)]
        (is (v/valid-uuidv7-bytes? uuid-bytes)
          "Generated UUID should pass strict UUIDv7 validation")))))
