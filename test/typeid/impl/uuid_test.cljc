(ns typeid.impl.uuid-test
  (:require [clojure.test :refer [deftest is testing]]
    [typeid.uuid :as uuid]
    [typeid.validation :as v]))

#?(:clj (set! *warn-on-reflection* true))

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
          timestamp #?(:clj (loop [i 0
                                   acc 0]
                              (if (< i 6)
                                (let [byte-val (bit-and (aget uuid-bytes i) 0xFF)]
                                  (recur (inc i)
                                    (+ (* acc 256) byte-val)))
                                acc))
                       :cljs (let [b0 (bit-and (aget uuid-bytes 0) 0xFF)
                                   b1 (bit-and (aget uuid-bytes 1) 0xFF)
                                   b2 (bit-and (aget uuid-bytes 2) 0xFF)
                                   b3 (bit-and (aget uuid-bytes 3) 0xFF)
                                   b4 (bit-and (aget uuid-bytes 4) 0xFF)
                                   b5 (bit-and (aget uuid-bytes 5) 0xFF)]
                               ;; Build 48-bit timestamp from bytes (big-endian)
                               ;; JavaScript can handle integers up to 2^53-1 safely
                               (+ (+ (* b0 1099511627776)  ; 2^40
                                    (* b1 4294967296))    ; 2^32
                                 (+ (* b2 16777216)       ; 2^24
                                   (* b3 65536))         ; 2^16
                                 (+ (* b4 256)            ; 2^8
                                   b5))))]
      (is (>= timestamp before-ms)
        (str "UUID timestamp " timestamp " should be >= " before-ms))
      (is (<= timestamp after-ms)
        (str "UUID timestamp " timestamp " should be <= " after-ms))))

  (testing "UUIDs generated in sequence have increasing timestamps or same timestamp"
    (let [uuid1 (uuid/generate-uuidv7)
          uuid2 (uuid/generate-uuidv7)
          extract-ts (fn [uuid-bytes]
                       #?(:clj (reduce (fn [acc b]
                                         (+ (* acc 256) (bit-and b 0xFF)))
                                 0
                                 (take 6 uuid-bytes))
                          :cljs (let [b0 (bit-and (aget uuid-bytes 0) 0xFF)
                                      b1 (bit-and (aget uuid-bytes 1) 0xFF)
                                      b2 (bit-and (aget uuid-bytes 2) 0xFF)
                                      b3 (bit-and (aget uuid-bytes 3) 0xFF)
                                      b4 (bit-and (aget uuid-bytes 4) 0xFF)
                                      b5 (bit-and (aget uuid-bytes 5) 0xFF)]
                                  (+ (+ (* b0 1099511627776)  ; 2^40
                                       (* b1 4294967296))    ; 2^32
                                    (+ (* b2 16777216)       ; 2^24
                                      (* b3 65536))         ; 2^16
                                    (+ (* b4 256)            ; 2^8
                                      b5)))))
          ts1 (extract-ts uuid1)
          ts2 (extract-ts uuid2)]
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

;; Timestamp-controlled UUIDv7 generation tests

(deftest generate-uuidv7-with-timestamp-test
  (testing "Generate UUIDv7 with explicit timestamp"
    (let [test-timestamp 1699564800000  ; 2023-11-10T00:00:00Z
          uuid-bytes (uuid/generate-uuidv7 test-timestamp)
          ;; Extract timestamp from first 48 bits
          extracted-ts #?(:clj (reduce (fn [acc b]
                                         (+ (* acc 256) (bit-and b 0xFF)))
                                 0
                                 (take 6 uuid-bytes))
                          :cljs (let [b0 (bit-and (aget uuid-bytes 0) 0xFF)
                                      b1 (bit-and (aget uuid-bytes 1) 0xFF)
                                      b2 (bit-and (aget uuid-bytes 2) 0xFF)
                                      b3 (bit-and (aget uuid-bytes 3) 0xFF)
                                      b4 (bit-and (aget uuid-bytes 4) 0xFF)
                                      b5 (bit-and (aget uuid-bytes 5) 0xFF)]
                                  (+ (+ (* b0 1099511627776)
                                       (* b1 4294967296))
                                    (+ (* b2 16777216)
                                      (* b3 65536))
                                    (+ (* b4 256)
                                      b5))))]
      (is (= test-timestamp extracted-ts)
        (str "Expected timestamp " test-timestamp ", got " extracted-ts))))

  (testing "Generate UUIDv7 with timestamp 0 (Unix epoch)"
    (let [uuid-bytes (uuid/generate-uuidv7 0)
          ;; First 6 bytes should all be 0
          first-six (take 6 uuid-bytes)]
      (is (every? zero? first-six)
        "Timestamp 0 should produce all-zero timestamp bytes")))

  (testing "Generate UUIDv7 with max valid timestamp"
    (let [max-ts 281474976710655  ; 2^48 - 1
          uuid-bytes (uuid/generate-uuidv7 max-ts)
          ;; All timestamp bytes should be 0xFF
          first-six (take 6 uuid-bytes)]
      (is (every? #(= 255 (bit-and % 0xFF)) first-six)
        "Max timestamp should produce all-0xFF timestamp bytes")))

  (testing "Generated UUID with custom timestamp has correct version and variant"
    (let [uuid-bytes (uuid/generate-uuidv7 1699564800000)
          version-byte (aget uuid-bytes 6)
          version (bit-and (bit-shift-right version-byte 4) 0x0F)
          variant-byte (bit-and (aget uuid-bytes 8) 0xFF)
          variant (bit-and (bit-shift-right variant-byte 6) 0x03)]
      (is (= 7 version) "UUID version should be 7")
      (is (= 2 variant) "UUID variant should be 2")))

  (testing "Generated UUID with custom timestamp passes validation"
    (let [uuid-bytes (uuid/generate-uuidv7 1699564800000)]
      (is (v/valid-uuidv7-bytes? uuid-bytes)
        "Generated UUID with custom timestamp should pass validation"))))

(deftest generate-uuidv7-timestamp-ordering-test
  (testing "UUIDs with different timestamps sort correctly"
    (let [ts1 1000
          ts2 2000
          ts3 3000
          uuid1 (uuid/generate-uuidv7 ts1)
          uuid2 (uuid/generate-uuidv7 ts2)
          uuid3 (uuid/generate-uuidv7 ts3)]
      ;; Compare byte sequences - earlier timestamps should sort first
      (is (neg? (compare (vec uuid1) (vec uuid2)))
        "UUID with earlier timestamp should sort before later")
      (is (neg? (compare (vec uuid2) (vec uuid3)))
        "UUID ordering should be transitive")))

  (testing "Same timestamp produces different UUIDs due to randomness"
    (let [ts 1699564800000
          uuid1 (uuid/generate-uuidv7 ts)
          uuid2 (uuid/generate-uuidv7 ts)]
      (is (not= (vec uuid1) (vec uuid2))
        "Same timestamp should still produce unique UUIDs"))))

(deftest generate-uuidv7-timestamp-validation-test
  (testing "Negative timestamp throws exception"
    (is (thrown-with-msg? #?(:clj clojure.lang.ExceptionInfo
                             :cljs js/Error)
          #"Timestamp out of valid range"
          (uuid/generate-uuidv7 -1)))

    (is (thrown-with-msg? #?(:clj clojure.lang.ExceptionInfo
                             :cljs js/Error)
          #"Timestamp out of valid range"
          (uuid/generate-uuidv7 -1000))))

  (testing "Timestamp exceeding 48-bit range throws exception"
    (is (thrown-with-msg? #?(:clj clojure.lang.ExceptionInfo
                             :cljs js/Error)
          #"Timestamp out of valid range"
          (uuid/generate-uuidv7 281474976710656)))  ; 2^48

    (is (thrown-with-msg? #?(:clj clojure.lang.ExceptionInfo
                             :cljs js/Error)
          #"Timestamp out of valid range"
          (uuid/generate-uuidv7 300000000000000))))  ; Well over max

  (testing "Exception contains structured error data"
    (try
      (uuid/generate-uuidv7 -100)
      (is false "Should have thrown exception")
      (catch #?(:clj clojure.lang.ExceptionInfo :cljs js/Error) e
        (let [data (ex-data e)]
          (is (= :typeid/invalid-timestamp (:type data)))
          (is (= -100 (:input data)))
          (is (= 0 (:min data)))
          (is (= 281474976710655 (:max data))))))))

;; T005-T010: bytes->uuid function tests

(deftest bytes->uuid-basic-test
  (testing "bytes->uuid converts 16 bytes to UUID object"
    (let [uuid-bytes #?(:clj (byte-array [0x01 0x8c 0x3f 0x9e 0x9e 0x4e 0x7a 0x8a
                                          0x8b 0x2a 0x7e 0x8e 0x9e 0x4e 0x7a 0x8a])
                        :cljs (js/Uint8Array. [0x01 0x8c 0x3f 0x9e 0x9e 0x4e 0x7a 0x8a
                                               0x8b 0x2a 0x7e 0x8e 0x9e 0x4e 0x7a 0x8a]))
          result (uuid/bytes->uuid uuid-bytes)]
      (is (some? result))
      #?(:clj (is (instance? java.util.UUID result))
         :cljs (is (uuid? result)))
      (is (= "018c3f9e-9e4e-7a8a-8b2a-7e8e9e4e7a8a" (str result)))))

  (testing "bytes->uuid requires exactly 16 bytes"
    (is (thrown-with-msg? #?(:clj clojure.lang.ExceptionInfo
                             :cljs js/Error)
          #"Invalid UUID bytes: expected exactly 16 bytes"
          (uuid/bytes->uuid #?(:clj (byte-array 15)
                               :cljs (js/Uint8Array. 15)))))

    (is (thrown-with-msg? #?(:clj clojure.lang.ExceptionInfo
                             :cljs js/Error)
          #"Invalid UUID bytes: expected exactly 16 bytes"
          (uuid/bytes->uuid #?(:clj (byte-array 17)
                               :cljs (js/Uint8Array. 17)))))))

(deftest bytes->uuid-round-trip-test
  (testing "bytes->uuid round-trips correctly with uuid->bytes"
    (let [original-uuid #?(:clj (java.util.UUID/fromString "018c3f9e-9e4e-7a8a-8b2a-7e8e9e4e7a8a")
                           :cljs (cljs.core/uuid "018c3f9e-9e4e-7a8a-8b2a-7e8e9e4e7a8a"))
          uuid-bytes (uuid/uuid->bytes original-uuid)
          recovered-uuid (uuid/bytes->uuid uuid-bytes)]
      (is (= original-uuid recovered-uuid)
        "Round-trip UUID -> bytes -> UUID should preserve value")))

  (testing "bytes->uuid with uuid->bytes produces identical UUIDs"
    (let [test-uuids [#?(:clj (java.util.UUID/fromString "00000000-0000-0000-0000-000000000000")
                         :cljs (cljs.core/uuid "00000000-0000-0000-0000-000000000000"))
                      #?(:clj (java.util.UUID/fromString "ffffffff-ffff-ffff-ffff-ffffffffffff")
                         :cljs (cljs.core/uuid "ffffffff-ffff-ffff-ffff-ffffffffffff"))
                      #?(:clj (java.util.UUID/randomUUID)
                         :cljs (random-uuid))]]
      (doseq [test-uuid test-uuids]
        (is (= test-uuid (-> test-uuid uuid/uuid->bytes uuid/bytes->uuid))
          (str "Round-trip should work for UUID: " test-uuid))))))

(deftest bytes->uuid-edge-cases-test
  (testing "bytes->uuid handles zero UUID"
    (let [zero-bytes #?(:clj (byte-array 16)
                        :cljs (js/Uint8Array. 16))
          zero-uuid (uuid/bytes->uuid zero-bytes)]
      (is (= "00000000-0000-0000-0000-000000000000" (str zero-uuid)))))

  (testing "bytes->uuid handles max UUID (all 0xFF)"
    (let [max-bytes #?(:clj (byte-array (repeat 16 -1))
                       :cljs (js/Uint8Array. (into-array (repeat 16 0xFF))))
          max-uuid (uuid/bytes->uuid max-bytes)]
      (is (= "ffffffff-ffff-ffff-ffff-ffffffffffff" (str max-uuid)))))

  (testing "bytes->uuid handles various UUID versions"
    ;; UUIDv1 example
    (let [v1-bytes #?(:clj (byte-array [0x6b 0xa7 0xb8 0x10 0x9d 0xad 0x11 0xd1
                                        0x80 0xb4 0x00 0xc0 0x4f 0xd4 0x30 0xc8])
                      :cljs (js/Uint8Array. [0x6b 0xa7 0xb8 0x10 0x9d 0xad 0x11 0xd1
                                             0x80 0xb4 0x00 0xc0 0x4f 0xd4 0x30 0xc8]))
          v1-uuid (uuid/bytes->uuid v1-bytes)]
      (is (some? v1-uuid)))

    ;; UUIDv4 example (random)
    (let [v4-bytes #?(:clj (byte-array [0x55 0x0e 0x84 0x00 0xe2 0x9b 0x41 0xd4
                                        0xa7 0x16 0x44 0x66 0x55 0x44 0x00 0x00])
                      :cljs (js/Uint8Array. [0x55 0x0e 0x84 0x00 0xe2 0x9b 0x41 0xd4
                                             0xa7 0x16 0x44 0x66 0x55 0x44 0x00 0x00]))
          v4-uuid (uuid/bytes->uuid v4-bytes)]
      (is (some? v4-uuid)))))

(deftest bytes->uuid-determinism-test
  (testing "bytes->uuid produces same UUID for same bytes"
    (let [test-bytes #?(:clj (byte-array [0x01 0x8c 0x3f 0x9e 0x9e 0x4e 0x7a 0x8a
                                          0x8b 0x2a 0x7e 0x8e 0x9e 0x4e 0x7a 0x8a])
                        :cljs (js/Uint8Array. [0x01 0x8c 0x3f 0x9e 0x9e 0x4e 0x7a 0x8a
                                               0x8b 0x2a 0x7e 0x8e 0x9e 0x4e 0x7a 0x8a]))
          uuid1 (uuid/bytes->uuid test-bytes)
          uuid2 (uuid/bytes->uuid test-bytes)]
      (is (= uuid1 uuid2)
        "Same bytes should produce equal UUIDs")))

  (testing "bytes->uuid is deterministic across multiple calls"
    (dotimes [_ 10]
      (let [test-uuid #?(:clj (java.util.UUID/randomUUID)
                         :cljs (random-uuid))
            uuid-bytes (uuid/uuid->bytes test-uuid)
            recovered1 (uuid/bytes->uuid uuid-bytes)
            recovered2 (uuid/bytes->uuid uuid-bytes)]
        (is (= recovered1 recovered2))
        (is (= test-uuid recovered1))))))
