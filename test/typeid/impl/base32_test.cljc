(ns typeid.impl.base32-test
  (:require [clojure.test :refer [deftest is testing]]
    [typeid.impl.base32 :as base32]))

;; T026: Base32 encoding unit tests

(deftest encode-decode-round-trip-test
  (testing "Encode and decode round-trip preserves bytes"
    (let [uuid-bytes #?(:clj (byte-array (range 16))
                        :cljs (js/Uint8Array. (range 16)))
          encoded (base32/encode uuid-bytes)
          decoded (base32/decode encoded)]
      (is (= 26 (count encoded)))
      (is (= 16 #?(:clj (alength decoded)
                   :cljs (.-length decoded))))
      ;; Compare byte by byte
      (dotimes [i 16]
        (is (= #?(:clj (aget uuid-bytes i)
                  :cljs (aget uuid-bytes i))
              #?(:clj (aget decoded i)
                 :cljs (aget decoded i)))))))

  (testing "Multiple round-trips with different byte patterns"
    (let [test-cases [;; All zeros
                      (repeat 16 0)
                      ;; All ones (as bytes: -1)
                      (repeat 16 #?(:clj (unchecked-byte 0xFF) :cljs 0xFF))
                      ;; Alternating pattern
                      (take 16 (cycle [0 #?(:clj (unchecked-byte 0xFF) :cljs 0xFF)]))
                      ;; Sequential
                      (range 16)
                      ;; Reverse sequential
                      (reverse (range 16))]]
      (doseq [test-bytes test-cases]
        (let [uuid-bytes #?(:clj (byte-array test-bytes)
                            :cljs (js/Uint8Array. (clj->js test-bytes)))
              encoded (base32/encode uuid-bytes)
              decoded (base32/decode encoded)]
          (is (= 26 (count encoded)))
          (dotimes [i 16]
            (is (= #?(:clj (aget uuid-bytes i)
                      :cljs (aget uuid-bytes i))
                  #?(:clj (aget decoded i)
                     :cljs (aget decoded i))))))))))

(deftest encode-alphabet-test
  (testing "Encoded string only contains valid base32 characters"
    (let [valid-chars (set "0123456789abcdefghjkmnpqrstvwxyz")
          uuid-bytes #?(:clj (byte-array (range 16))
                        :cljs (js/Uint8Array. (range 16)))
          encoded (base32/encode uuid-bytes)]
      (is (every? valid-chars encoded))
      (is (= 26 (count encoded)))))

  (testing "Encoded string never contains excluded characters"
    (let [excluded-chars #{\i \l \o \u}
          uuid-bytes #?(:clj (byte-array (range 16))
                        :cljs (js/Uint8Array. (range 16)))
          encoded (base32/encode uuid-bytes)]
      (is (not-any? excluded-chars encoded)))))

(deftest encode-first-char-constraint-test
  (testing "Encoded string first character is 0-7 for valid UUIDs"
    ;; Test with bytes that should produce first char 0-7
    (dotimes [_ 20]
      (let [;; Create UUID with first byte <= 127 to ensure first char is 0-7
            first-byte (rand-int 32) ; Keep low to ensure first char is 0-7
            uuid-bytes #?(:clj (byte-array (cons first-byte (repeatedly 15 #(rand-int 256))))
                          :cljs (js/Uint8Array. (clj->js (cons first-byte (repeatedly 15 #(rand-int 256))))))
            encoded (base32/encode uuid-bytes)
            first-char (first encoded)]
        (is (contains? #{\0 \1 \2 \3 \4 \5 \6 \7} first-char)
          (str "First character should be 0-7, got: " first-char))))))

(deftest decode-validates-length-test
  (testing "Decode rejects strings that are too short"
    (is (thrown-with-msg? #?(:clj Exception :cljs js/Error)
          #"26 characters"
          (base32/decode "tooshort"))))

  (testing "Decode rejects strings that are too long"
    (is (thrown-with-msg? #?(:clj Exception :cljs js/Error)
          #"26 characters"
          (base32/decode "01234567890123456789012345extra")))))

(deftest decode-validates-overflow-test
  (testing "Decode rejects suffix starting with 8"
    (is (thrown-with-msg? #?(:clj Exception :cljs js/Error)
          #"(?i)overflow|0-7"
          (base32/decode "8zzzzzzzzzzzzzzzzzzzzzzzzz"))))

  (testing "Decode rejects suffix starting with 9-z"
    (doseq [first-char ["9" "a" "z"]]
      (is (thrown-with-msg? #?(:clj Exception :cljs js/Error)
            #"(?i)overflow|0-7"
            (base32/decode (str first-char "zzzzzzzzzzzzzzzzzzzzzzzzz")))))))

(deftest decode-validates-characters-test
  (testing "Decode rejects invalid base32 characters"
    (let [invalid-chars ["i" "l" "o" "u" "I" "L" "O" "U"]]
      (doseq [ch invalid-chars]
        (is (thrown-with-msg? #?(:clj Exception :cljs js/Error)
              #"(?i)invalid.*base32|character"
              (base32/decode (str "01234567890123456789012" ch "45")))
          (str "Should reject invalid character: " ch)))))

  (testing "Decode rejects uppercase letters"
    (is (thrown-with-msg? #?(:clj Exception :cljs js/Error)
          #"(?i)invalid.*base32|character"
          (base32/decode "01H5FSKFSK4FPEQWNSYZ5HJ55T")))))

(deftest encode-specific-patterns-test
  (testing "Encode handles all-zero bytes"
    (let [uuid-bytes #?(:clj (byte-array 16)
                        :cljs (js/Uint8Array. 16))
          encoded (base32/encode uuid-bytes)]
      (is (= 26 (count encoded)))
      (is (every? #{\0} encoded)))) ; All zeros

  (testing "Encode produces consistent output for same input"
    (let [uuid-bytes #?(:clj (byte-array (range 16))
                        :cljs (js/Uint8Array. (range 16)))
          encoded1 (base32/encode uuid-bytes)
          encoded2 (base32/encode uuid-bytes)]
      (is (= encoded1 encoded2)))))
