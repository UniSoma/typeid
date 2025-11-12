(ns typeid.core-test
  (:require [clojure.string :as str]
    [clojure.test :refer [deftest is testing]]
    [clojure.test.check.clojure-test :refer [defspec]]
    [clojure.test.check.generators :as gen]
    [clojure.test.check.properties :as prop]
    [typeid.codec :as codec]
    [typeid.core :as t]
    [typeid.impl.uuid :as uuid]
    [typeid.validation :as v]))

#?(:clj (set! *warn-on-reflection* true))

;; T023: Unit tests for create function
(deftest create-with-prefix-test
  (testing "Generate TypeID with valid prefix"
    (let [typeid (t/create "user")]
      (is (string? typeid))
      (is (= 31 (count typeid)))        ; "user" (4) + "_" (1) + suffix (26)
      (is (.startsWith typeid "user_"))
      (is (= 26 (count (subs typeid 5)))))) ; suffix is 26 chars

  (testing "Generate TypeID with empty prefix"
    (let [typeid (t/create "")]
      (is (string? typeid))
      (is (= 26 (count typeid)))              ; just suffix, no prefix
      (is (not (str/includes? typeid "_"))))) ; no separator

  (testing "Generate TypeID with various valid prefixes"
    (doseq [prefix ["a" "abc" "my_type" "user_account"]]
      (let [typeid (t/create prefix)]
        (is (.startsWith typeid (str prefix "_")))
        (is (= (+ (count prefix) 1 26) (count typeid))))))

  (testing "Generated TypeIDs are unique"
    (let [id1 (t/create "user")
          id2 (t/create "user")]
      (is (not= id1 id2))
      (is (.startsWith id1 "user_"))
      (is (.startsWith id2 "user_"))))

  (testing "Generated TypeID suffix starts with 0-7"
    (dotimes [_ 20]                     ; Test multiple times due to randomness
      (let [typeid (t/create "test")
            suffix (subs typeid 5)      ; Skip "test_"
            first-char (first suffix)]
        (is (contains? #{\0 \1 \2 \3 \4 \5 \6 \7} first-char)
          (str "First character of suffix must be 0-7, got: " first-char))))))

(deftest create-with-invalid-prefix-test
  (testing "Reject prefix with uppercase"
    (is (thrown-with-msg? #?(:clj Exception :cljs js/Error)
          #"(?i)prefix.*pattern|lowercase"
          (t/create "User"))))

  (testing "Reject prefix with numbers"
    (is (thrown-with-msg? #?(:clj Exception :cljs js/Error)
          #"(?i)prefix.*pattern"
          (t/create "user123"))))

  (testing "Reject prefix that's too long"
    (is (thrown-with-msg? #?(:clj Exception :cljs js/Error)
          #"(?i)prefix.*63|too long"
          (t/create (apply str (repeat 64 "a"))))))

  (testing "Reject prefix starting with underscore"
    (is (thrown-with-msg? #?(:clj Exception :cljs js/Error)
          #"(?i)prefix.*pattern"
          (t/create "_user")))))

(deftest create-with-no-args-test
  (testing "Generate TypeID with no arguments"
    (let [typeid (t/create)]
      (is (string? typeid))
      (is (= 26 (count typeid))) ; just suffix, no prefix
      (is (not (str/includes? typeid "_"))))) ; no separator

  (testing "Generate TypeID with nil prefix"
    (let [typeid (t/create nil)]
      (is (string? typeid))
      (is (= 26 (count typeid))) ; just suffix, no prefix
      (is (not (str/includes? typeid "_"))))) ; no separator

  (testing "No-args and nil generate unique TypeIDs"
    (let [id1 (t/create)
          id2 (t/create nil)]
      (is (not= id1 id2))
      (is (= 26 (count id1)))
      (is (= 26 (count id2))))))

(deftest create-with-keyword-prefix-test
  (testing "Generate TypeID with keyword prefix"
    (let [typeid (t/create :user)]
      (is (string? typeid))
      (is (= 31 (count typeid))) ; "user" (4) + "_" (1) + suffix (26)
      (is (.startsWith typeid "user_"))
      (is (= 26 (count (subs typeid 5)))))) ; suffix is 26 chars

  (testing "Generate TypeID with various keyword prefixes"
    (doseq [prefix [:a :abc :my_type :user_account]]
      (let [typeid (t/create prefix)
            prefix-str (name prefix)]
        (is (.startsWith typeid (str prefix-str "_")))
        (is (= (+ (count prefix-str) 1 26) (count typeid))))))

  (testing "Keyword and string prefixes produce same format"
    (let [id1 (t/create "user")
          id2 (t/create :user)]
      (is (not= id1 id2)) ; Different UUIDs
      (is (.startsWith id1 "user_"))
      (is (.startsWith id2 "user_"))
      (is (= (count id1) (count id2)))))

  (testing "Reject invalid keyword prefix"
    (is (thrown-with-msg? #?(:clj Exception :cljs js/Error)
          #"(?i)prefix.*pattern|lowercase"
          (t/create :User))))

  (testing "Namespaced keyword uses name part only"
    (let [typeid (t/create :ns/user)]
      (is (string? typeid))
      (is (.startsWith typeid "user_")) ; namespace is ignored
      (is (= 31 (count typeid))))))

;; T024: Unit tests for parse function
(deftest parse-valid-typeid-test
  (testing "Parse TypeID with prefix"
    (let [typeid "user_01h5fskfsk4fpeqwnsyz5hj55t"
          parsed (t/parse typeid)]
      (is (map? parsed))
      (is (= "user" (:prefix parsed)))
      (is (= "01h5fskfsk4fpeqwnsyz5hj55t" (:suffix parsed)))
      (is (= typeid (:typeid parsed)))
      (is (some? (:uuid parsed)))
      (is (v/valid-uuid-bytes? (:uuid parsed)))))

  (testing "Parse TypeID without prefix"
    (let [typeid "01h5fskfsk4fpeqwnsyz5hj55t"
          parsed (t/parse typeid)]
      (is (= "" (:prefix parsed)))
      (is (= "01h5fskfsk4fpeqwnsyz5hj55t" (:suffix parsed)))
      (is (= typeid (:typeid parsed)))
      (is (v/valid-uuid-bytes? (:uuid parsed)))))

  (testing "Parse TypeID with underscores in prefix"
    (let [typeid "my_user_type_01h5fskfsk4fpeqwnsyz5hj55t"
          parsed (t/parse typeid)]
      (is (= "my_user_type" (:prefix parsed)))
      (is (= "01h5fskfsk4fpeqwnsyz5hj55t" (:suffix parsed))))))

(deftest parse-invalid-typeid-test
  (testing "Reject TypeID with uppercase"
    (is (thrown? #?(:clj Exception :cljs js/Error)
          (t/parse "User_01h5fskfsk4fpeqwnsyz5hj55t")))
    (let [error (t/explain "User_01h5fskfsk4fpeqwnsyz5hj55t")]
      (is (some? error))
      (is (= :typeid/invalid-format (:type error)))))

  (testing "Reject TypeID that's too short"
    (is (thrown? #?(:clj Exception :cljs js/Error)
          (t/parse "user_tooshort")))
    (let [error (t/explain "user_tooshort")]
      (is (some? error))
      (is (= :typeid/invalid-length (:type error)))))

  (testing "Reject TypeID that's too long"
    (let [long-typeid (str "user_" (apply str (repeat 100 "a")))]
      (is (thrown? #?(:clj Exception :cljs js/Error)
            (t/parse long-typeid)))
      (let [error (t/explain long-typeid)]
        (is (some? error))
        (is (= :typeid/invalid-length (:type error))))))

  (testing "Reject TypeID with invalid base32 character"
    (is (thrown? #?(:clj Exception :cljs js/Error)
          (t/parse "user_01h5fskfsk4fpeqwnsyz5hj5il"))) ; 'i' and 'l' are invalid
    (let [error (t/explain "user_01h5fskfsk4fpeqwnsyz5hj5il")]
      (is (some? error))
      (is (= :typeid/invalid-suffix (:type error)))))

  (testing "Reject TypeID with suffix starting with 8-z"
    (is (thrown? #?(:clj Exception :cljs js/Error)
          (t/parse "user_8zzzzzzzzzzzzzzzzzzzzzzzzz")))
    (let [error (t/explain "user_8zzzzzzzzzzzzzzzzzzzzzzzzz")]
      (is (some? error))
      (is (= :typeid/invalid-suffix (:type error)))))

  (testing "Reject non-string input"
    (is (thrown? #?(:clj Exception :cljs js/Error)
          (t/parse 12345)))
    (let [error (t/explain 12345)]
      (is (some? error))
      (is (= :typeid/invalid-input-type (:type error))))))

(deftest create-parse-round-trip-test
  (testing "Generate and parse round-trip preserves data"
    (let [prefixes ["user" "order" "session" "" "a" "my_type"]]
      (doseq [prefix prefixes]
        (let [typeid (t/create prefix)
              parsed (t/parse typeid)]
          (is (= prefix (:prefix parsed)))
          (is (= typeid (:typeid parsed)))
          (is (= 26 (count (:suffix parsed))))
          (is (v/valid-uuid-bytes? (:uuid parsed)))))))

  (testing "Parse and re-generate produces different UUID but same prefix"
    (let [typeid1 (t/create "user")
          parsed1 (t/parse typeid1)
          typeid2 (t/create "user")
          parsed2 (t/parse typeid2)]
      (is (= "user" (:prefix parsed1) (:prefix parsed2)))
      (is (not= typeid1 typeid2)) ; Different UUIDs
      (is (not= (:suffix parsed1) (:suffix parsed2))))))

;; T034: Unit tests for validate function (User Story 2)
(deftest validate-valid-typeid-test
  (testing "Validate correct TypeID with prefix"
    (is (nil? (t/explain "user_01h5fskfsk4fpeqwnsyz5hj55t"))))

  (testing "Validate correct TypeID without prefix"
    (is (nil? (t/explain "01h5fskfsk4fpeqwnsyz5hj55t"))))

  (testing "Validate TypeID with underscores in prefix"
    (is (nil? (t/explain "my_user_type_01h5fskfsk4fpeqwnsyz5hj55t")))))

(deftest validate-invalid-typeid-test
  (testing "Reject TypeID with uppercase"
    (let [error (t/explain "User_01h5fskfsk4fpeqwnsyz5hj55t")]
      (is (some? error))
      (is (= :typeid/invalid-format (:type error)))))

  (testing "Reject TypeID that's too short"
    (let [error (t/explain "user_tooshort")]
      (is (some? error))
      (is (= :typeid/invalid-length (:type error)))))

  (testing "Reject TypeID that's too long"
    (let [error (t/explain (str "user_" (apply str (repeat 100 "a"))))]
      (is (some? error))
      (is (= :typeid/invalid-length (:type error)))))

  (testing "Reject TypeID with invalid base32 character"
    (let [error (t/explain "user_01h5fskfsk4fpeqwnsyz5hj5il")] ; 'i' and 'l' are invalid
      (is (some? error))
      (is (= :typeid/invalid-suffix (:type error)))))

  (testing "Reject TypeID with suffix starting with 8-z"
    (let [error (t/explain "user_8zzzzzzzzzzzzzzzzzzzzzzzzz")]
      (is (some? error))
      (is (= :typeid/invalid-suffix (:type error)))))

  (testing "Reject non-string input"
    (let [error (t/explain 12345)]
      (is (some? error))
      (is (= :typeid/invalid-input-type (:type error))))))

(deftest validate-vs-parse-test
  (testing "Validate should accept same inputs as parse"
    (let [typeids ["user_01h5fskfsk4fpeqwnsyz5hj55t"
                   "01h5fskfsk4fpeqwnsyz5hj55t"
                   "my_type_01h5fskfsk4fpeqwnsyz5hj55t"]]
      (doseq [typeid typeids]
        (let [explain-result (t/explain typeid)
              parsed (t/parse typeid)]
          (is (nil? explain-result))
          (is (some? parsed)))))))

;; T036: Edge case tests
(deftest edge-case-prefix-tests
  (testing "Maximum length prefix (63 characters)"
    (let [max-prefix (str "a" (apply str (repeat 61 "b")) "z") ; 63 chars: a + 61 b's + z
          typeid (t/create max-prefix)]
      (is (string? typeid))
      (is (= (+ 63 1 26) (count typeid))) ; 63 prefix + 1 separator + 26 suffix
      (is (.startsWith typeid max-prefix))

      ;; Should parse successfully
      (let [parsed (t/parse typeid)]
        (is (= max-prefix (:prefix parsed))))

      ;; Should validate successfully
      (is (nil? (t/explain typeid)))))

  (testing "Consecutive underscores in prefix"
    (let [prefix "my__type__name" ; Multiple consecutive underscores
          typeid (t/create prefix)]
      (is (string? typeid))
      (is (.startsWith typeid prefix))

      ;; Should parse successfully
      (let [parsed (t/parse typeid)]
        (is (= prefix (:prefix parsed))))

      ;; Should validate successfully
      (is (nil? (t/explain typeid)))))

  (testing "Prefix boundary characters"
    ;; Single character prefix (minimum non-empty)
    (let [typeid-a (t/create "a")]
      (is (= 28 (count typeid-a))) ; 1 + 1 + 26
      (let [parsed (t/parse typeid-a)]
        (is (= "a" (:prefix parsed)))))

    ;; Two character prefix
    (let [typeid-ab (t/create "ab")]
      (is (= 29 (count typeid-ab))) ; 2 + 1 + 26
      (let [parsed (t/parse typeid-ab)]
        (is (= "ab" (:prefix parsed)))))

    ;; Three character prefix (recommended minimum)
    (let [typeid-abc (t/create "abc")]
      (is (= 30 (count typeid-abc))) ; 3 + 1 + 26
      (let [parsed (t/parse typeid-abc)]
        (is (= "abc" (:prefix parsed))))))

  (testing "Prefix must start and end with letter (not underscore)"
    ;; Valid: starts and ends with letter
    (let [valid-prefixes ["a" "ab" "a_b" "abc_def" "user_account_type"]]
      (doseq [prefix valid-prefixes]
        (let [typeid (t/create prefix)]
          (is (string? typeid) (str "Should generate TypeID for prefix: " prefix))
          (let [parsed (t/parse typeid)]
            (is (= prefix (:prefix parsed)))))))

    ;; Invalid: starts with underscore
    (is (thrown? #?(:clj Exception :cljs js/Error)
          (t/create "_invalid")))

    ;; Invalid: ends with underscore
    (is (thrown? #?(:clj Exception :cljs js/Error)
          (t/create "invalid_")))))

(deftest edge-case-suffix-tests
  (testing "Suffix boundary: first character must be 0-7"
    ;; Valid: first char in 0-7
    (doseq [first-char [\0 \1 \2 \3 \4 \5 \6 \7]]
      (let [valid-suffix (str first-char (apply str (repeat 25 "z")))
            valid-typeid (str "test_" valid-suffix)
            parsed (t/parse valid-typeid)]
        (is (some? parsed) (str "Should accept suffix starting with: " first-char))))

    ;; Invalid: first char 8-z
    (doseq [first-char [\8 \9 \a \b \c \d \e \f \g \h \j \k \m \n \p \q \r \s \t \v \w \x \y \z]]
      (let [invalid-suffix (str first-char (apply str (repeat 25 "z")))
            invalid-typeid (str "test_" invalid-suffix)
            error (t/explain invalid-typeid)]
        (is (some? error) (str "Should reject suffix starting with: " first-char))
        (is (= :typeid/invalid-suffix (:type error))))))

  (testing "Suffix must be exactly 26 characters"
    ;; Too short (25 chars)
    (is (thrown? #?(:clj Exception :cljs js/Error)
          (t/parse "test_01h5fskfsk4fpeqwnsyz5hj5")))
    (let [error (t/explain "test_01h5fskfsk4fpeqwnsyz5hj5")]
      (is (some? error))
      (is (= :typeid/invalid-suffix (:type error))))

    ;; Too long (28 chars)
    (is (thrown? #?(:clj Exception :cljs js/Error)
          (t/parse "test_01h5fskfsk4fpeqwnsyz5hj55tt")))
    (let [error (t/explain "test_01h5fskfsk4fpeqwnsyz5hj55tt")]
      (is (some? error))
      (is (= :typeid/invalid-suffix (:type error))))))

(deftest edge-case-typeid-length-tests
  (testing "Minimum TypeID length (26 chars - no prefix)"
    (let [min-typeid "01h5fskfsk4fpeqwnsyz5hj55t"
          parsed (t/parse min-typeid)]
      (is (= "" (:prefix parsed)))
      (is (= 26 (count min-typeid)))))

  (testing "Maximum TypeID length (90 chars - 63 prefix + 1 sep + 26 suffix)"
    (let [max-prefix (str "a" (apply str (repeat 61 "b")) "z") ; 63 chars
          max-typeid (t/create max-prefix)
          parsed (t/parse max-typeid)]
      (is (= max-prefix (:prefix parsed)))
      (is (= 90 (count max-typeid)))))

  (testing "Length boundaries"
    ;; 25 chars: too short
    (is (thrown? #?(:clj Exception :cljs js/Error)
          (t/parse "0h5fskfsk4fpeqwnsyz5hj55t")))
    (let [error (t/explain "0h5fskfsk4fpeqwnsyz5hj55t")]
      (is (some? error))
      (is (= :typeid/invalid-length (:type error))))

    ;; 91 chars: too long (64-char prefix + sep + 26 suffix)
    (let [too-long-prefix (apply str (repeat 64 "a"))
          too-long-typeid (str too-long-prefix "_01h5fskfsk4fpeqwnsyz5hj55t")]
      (is (thrown? #?(:clj Exception :cljs js/Error)
            (t/parse too-long-typeid)))
      (let [error (t/explain too-long-typeid)]
        (is (some? error))
        (is (= :typeid/invalid-length (:type error)))))))

;; T041-T045: User Story 3 - UUID Conversion Tests

(deftest encode-function-test
  (testing "Encode UUID bytes with prefix"
    (let [uuid-bytes #?(:clj (byte-array (map unchecked-byte [0x01 0x88 0xe5 0xf5 0xf3 0x4a 0x7b 0x3d
                                                              0x9f 0x2a 0x1c 0x5d 0xe6 0x7f 0xa8 0xc1]))
                        :cljs (js/Uint8Array. (clj->js [0x01 0x88 0xe5 0xf5 0xf3 0x4a 0x7b 0x3d
                                                        0x9f 0x2a 0x1c 0x5d 0xe6 0x7f 0xa8 0xc1])))
          encoded (codec/encode uuid-bytes "user")]
      (is (string? encoded))
      (is (.startsWith encoded "user_"))
      (is (= 31 (count encoded)))))

  (testing "Encode UUID bytes without prefix"
    (let [uuid-bytes #?(:clj (byte-array (map unchecked-byte [0x01 0x88 0xe5 0xf5 0xf3 0x4a 0x7b 0x3d
                                                              0x9f 0x2a 0x1c 0x5d 0xe6 0x7f 0xa8 0xc1]))
                        :cljs (js/Uint8Array. (clj->js [0x01 0x88 0xe5 0xf5 0xf3 0x4a 0x7b 0x3d
                                                        0x9f 0x2a 0x1c 0x5d 0xe6 0x7f 0xa8 0xc1])))
          encoded (codec/encode uuid-bytes "")]
      (is (string? encoded))
      (is (= 26 (count encoded)))))

  (testing "Reject invalid UUID length"
    (let [short-uuid #?(:clj (byte-array (repeat 8 0))
                        :cljs (js/Uint8Array. 8))]
      (is (thrown-with-msg? #?(:clj Exception :cljs js/Error)
            #"(?i)16 bytes"
            (codec/encode short-uuid "user")))))

  (testing "Reject invalid prefix"
    (let [uuid-bytes #?(:clj (byte-array (repeat 16 0))
                        :cljs (js/Uint8Array. 16))]
      (is (thrown? #?(:clj Exception :cljs js/Error)
            (codec/encode uuid-bytes "User123"))))))

(deftest decode-function-test
  (testing "Decode valid TypeID to UUID bytes"
    (let [uuid-bytes (codec/decode "user_01h5fskfsk4fpeqwnsyz5hj55t")]
      (is (some? uuid-bytes))
      (is (= 16 (alength uuid-bytes)))))

  (testing "Decode TypeID without prefix"
    (let [uuid-bytes (codec/decode "01h5fskfsk4fpeqwnsyz5hj55t")]
      (is (some? uuid-bytes))
      (is (= 16 (alength uuid-bytes)))))

  (testing "Decode rejects invalid TypeID"
    (is (thrown? #?(:clj Exception :cljs js/Error)
          (codec/decode "invalid")))))

(deftest uuid-hex-conversion-test
  (testing "Convert UUID bytes to hex string"
    (let [uuid-bytes #?(:clj (byte-array (map unchecked-byte [0x01 0x88 0xe5 0xf5 0xf3 0x4a 0x7b 0x3d
                                                              0x9f 0x2a 0x1c 0x5d 0xe6 0x7f 0xa8 0xc1]))
                        :cljs (js/Uint8Array. (clj->js [0x01 0x88 0xe5 0xf5 0xf3 0x4a 0x7b 0x3d
                                                        0x9f 0x2a 0x1c 0x5d 0xe6 0x7f 0xa8 0xc1])))
          hex-str (codec/uuid->hex uuid-bytes)]
      (is (string? hex-str))
      (is (= 32 (count hex-str)))
      (is (= "0188e5f5f34a7b3d9f2a1c5de67fa8c1" hex-str))
      (is (re-matches #"^[0-9a-f]{32}$" hex-str))))

  (testing "Convert hex string to UUID bytes"
    (let [uuid-bytes (codec/hex->uuid "0188e5f5f34a7b3d9f2a1c5de67fa8c1")]
      (is (some? uuid-bytes))
      (is (= 16 (alength uuid-bytes)))
      (is (= (map unchecked-byte [0x01 0x88 0xe5 0xf5 0xf3 0x4a 0x7b 0x3d
                                  0x9f 0x2a 0x1c 0x5d 0xe6 0x7f 0xa8 0xc1])
            (vec uuid-bytes)))))

  (testing "Reject invalid hex length"
    (is (thrown? #?(:clj Exception :cljs js/Error)
          (codec/hex->uuid "0188e5"))))

  (testing "Reject invalid hex characters"
    (is (thrown? #?(:clj Exception :cljs js/Error)
          (codec/hex->uuid "0188e5f5f34a7b3d9f2a1c5de67fa8cz"))))

  (testing "UUID->hex->UUID round-trip"
    (let [original-uuid #?(:clj (byte-array (map unchecked-byte [0x01 0x88 0xe5 0xf5 0xf3 0x4a 0x7b 0x3d
                                                                 0x9f 0x2a 0x1c 0x5d 0xe6 0x7f 0xa8 0xc1]))
                           :cljs (js/Uint8Array. (clj->js [0x01 0x88 0xe5 0xf5 0xf3 0x4a 0x7b 0x3d
                                                           0x9f 0x2a 0x1c 0x5d 0xe6 0x7f 0xa8 0xc1])))
          hex-str (codec/uuid->hex original-uuid)
          recovered-uuid (codec/hex->uuid hex-str)]
      (is (= (vec original-uuid) (vec recovered-uuid)))))

  ;; T062: Test hex->uuid with hyphens and uppercase
  (testing "hex->uuid accepts hyphenated UUID format"
    (let [uuid-bytes (codec/hex->uuid "0188e5f5-f34a-7b3d-9f2a-1c5de67fa8c1")]
      (is (= 16 (alength uuid-bytes)))
      (is (= (map unchecked-byte [0x01 0x88 0xe5 0xf5 0xf3 0x4a 0x7b 0x3d
                                  0x9f 0x2a 0x1c 0x5d 0xe6 0x7f 0xa8 0xc1])
            (vec uuid-bytes)))))

  (testing "hex->uuid accepts uppercase hex"
    (let [uuid-bytes (codec/hex->uuid "0188E5F5F34A7B3D9F2A1C5DE67FA8C1")]
      (is (= 16 (alength uuid-bytes)))
      (is (= (map unchecked-byte [0x01 0x88 0xe5 0xf5 0xf3 0x4a 0x7b 0x3d
                                  0x9f 0x2a 0x1c 0x5d 0xe6 0x7f 0xa8 0xc1])
            (vec uuid-bytes)))))

  (testing "hex->uuid accepts mixed case with hyphens"
    (let [uuid-bytes (codec/hex->uuid "0188E5f5-F34a-7B3d-9F2a-1C5dE67fA8c1")]
      (is (= 16 (alength uuid-bytes)))
      (is (= (map unchecked-byte [0x01 0x88 0xe5 0xf5 0xf3 0x4a 0x7b 0x3d
                                  0x9f 0x2a 0x1c 0x5d 0xe6 0x7f 0xa8 0xc1])
            (vec uuid-bytes))))))

(deftest typeid-map-conversion-test
  (testing "Convert TypeID to map (unwrapped parse)"
    (let [result (t/parse "user_01h5fskfsk4fpeqwnsyz5hj55t")]
      (is (map? result))
      (is (= "user" (:prefix result)))
      (is (= "01h5fskfsk4fpeqwnsyz5hj55t" (:suffix result)))
      (is (= "user_01h5fskfsk4fpeqwnsyz5hj55t" (:typeid result)))
      (is (some? (:uuid result)))))

  (testing "parse throws on invalid input"
    (is (thrown? #?(:clj Exception :cljs js/Error)
          (t/parse "invalid")))))

(deftest encode-decode-round-trip-test
  (testing "Encode→decode round-trip preserves UUID"
    (let [original-uuid #?(:clj (byte-array (map unchecked-byte [0x01 0x88 0xe5 0xf5 0xf3 0x4a 0x7b 0x3d
                                                                 0x9f 0x2a 0x1c 0x5d 0xe6 0x7f 0xa8 0xc1]))
                           :cljs (js/Uint8Array. (clj->js [0x01 0x88 0xe5 0xf5 0xf3 0x4a 0x7b 0x3d
                                                           0x9f 0x2a 0x1c 0x5d 0xe6 0x7f 0xa8 0xc1])))
          typeid-str (codec/encode original-uuid "order")
          recovered-uuid (codec/decode typeid-str)]
      (is (= (vec original-uuid) (vec recovered-uuid)))))

  (testing "Generate→decode→encode round-trip"
    (let [typeid1 (t/create "session")
          uuid-bytes (codec/decode typeid1)
          typeid2 (codec/encode uuid-bytes "session")]
      ;; TypeIDs should be identical (same UUID)
      (is (= typeid1 typeid2))
      ;; Both should be valid
      (is (some? uuid-bytes))
      (is (string? typeid2)))))

(deftest suffix-extraction-test
  (testing "Extract suffix from TypeID with prefix"
    (let [typeid "user_01h5fskfsk4fpeqwnsyz5hj55t"
          parsed (t/parse typeid)]
      (is (= "01h5fskfsk4fpeqwnsyz5hj55t" (:suffix parsed)))))

  (testing "Extract suffix from TypeID without prefix"
    (let [typeid "01h5fskfsk4fpeqwnsyz5hj55t"
          parsed (t/parse typeid)]
      (is (= "01h5fskfsk4fpeqwnsyz5hj55t" (:suffix parsed))))))

;; T034-T045: User Story 3 - Create TypeIDs with Flexible Options Tests

(deftest create-zero-arity-test
  (testing "Create TypeID with no arguments generates prefix-less TypeID"
    (let [typeid (t/create)]
      (is (string? typeid))
      (is (= 26 (count typeid))) ; No prefix, just 26-char suffix
      (is (not (str/includes? typeid "_"))))) ; No separator

  (testing "Multiple zero-arity calls generate unique TypeIDs"
    (let [id1 (t/create)
          id2 (t/create)]
      (is (not= id1 id2))
      (is (= 26 (count id1)))
      (is (= 26 (count id2))))))

(deftest create-one-arity-string-test
  (testing "Create TypeID with string prefix generates new UUID"
    (let [typeid (t/create "user")]
      (is (string? typeid))
      (is (.startsWith typeid "user_"))
      (is (= 31 (count typeid))))) ; "user" (4) + "_" (1) + suffix (26)

  (testing "Create TypeID with empty string generates prefix-less TypeID"
    (let [typeid (t/create "")]
      (is (string? typeid))
      (is (= 26 (count typeid)))
      (is (not (str/includes? typeid "_")))))

  (testing "Multiple one-arity calls with same prefix generate unique TypeIDs"
    (let [id1 (t/create "user")
          id2 (t/create "user")]
      (is (not= id1 id2))
      (is (.startsWith id1 "user_"))
      (is (.startsWith id2 "user_")))))

(deftest create-one-arity-keyword-test
  (testing "Create TypeID with keyword prefix"
    (let [typeid (t/create :user)]
      (is (string? typeid))
      (is (.startsWith typeid "user_"))
      (is (= 31 (count typeid)))))

  (testing "Keyword and string prefixes produce same format"
    (let [id1 (t/create "order")
          id2 (t/create :order)]
      (is (not= id1 id2)) ; Different UUIDs
      (is (.startsWith id1 "order_"))
      (is (.startsWith id2 "order_"))
      (is (= (count id1) (count id2))))))

(deftest create-one-arity-nil-test
  (testing "Create TypeID with nil prefix generates prefix-less TypeID"
    (let [typeid (t/create nil)]
      (is (string? typeid))
      (is (= 26 (count typeid)))
      (is (not (str/includes? typeid "_"))))))

(deftest create-two-arity-string-prefix-test
  (testing "Create TypeID from string prefix and UUID"
    (let [the-uuid #uuid "018c3f9e-9e4e-7a8a-8b2a-7e8e9e4e7a8a"
          typeid (t/create "user" the-uuid)]
      (is (string? typeid))
      (is (.startsWith typeid "user_"))
      (is (= 31 (count typeid)))
      ;; Parse back and verify UUID bytes match
      (let [parsed (t/parse typeid)
            parsed-uuid-bytes (:uuid parsed)
            uuid-bytes (uuid/uuid->bytes the-uuid)]
        (is (= (vec uuid-bytes) (vec parsed-uuid-bytes)))))))

(deftest create-two-arity-keyword-prefix-test
  (testing "Create TypeID from keyword prefix and UUID"
    (let [the-uuid #uuid "018c3f9e-9e4e-7a8a-8b2a-7e8e9e4e7a8a"
          typeid (t/create :order the-uuid)]
      (is (string? typeid))
      (is (.startsWith typeid "order_"))
      (is (= 32 (count typeid)))))) ; "order" (5) + "_" (1) + suffix (26)

(deftest create-two-arity-nil-prefix-test
  (testing "Create TypeID from nil prefix and UUID"
    (let [the-uuid #uuid "018c3f9e-9e4e-7a8a-8b2a-7e8e9e4e7a8a"
          typeid (t/create nil the-uuid)]
      (is (string? typeid))
      (is (= 26 (count typeid)))
      (is (not (str/includes? typeid "_")))
      ;; Parse back and verify UUID bytes match
      (let [parsed (t/parse typeid)
            parsed-uuid-bytes (:uuid parsed)
            uuid-bytes (uuid/uuid->bytes the-uuid)]
        (is (= (vec uuid-bytes) (vec parsed-uuid-bytes)))))))

(deftest create-uuid-versions-test
  (testing "Create accepts UUIDv7 (time-ordered)"
    (let [the-uuid #uuid "018c3f9e-9e4e-7a8a-8b2a-7e8e9e4e7a8a"
          typeid (t/create "test" the-uuid)]
      (is (string? typeid))
      (is (.startsWith typeid "test_"))))

  (testing "Create accepts UUIDv4 (random)"
    (let [the-uuid #uuid "550e8400-e29b-41d4-a716-446655440000"
          typeid (t/create "test" the-uuid)]
      (is (string? typeid))
      (is (.startsWith typeid "test_"))))

  (testing "Create accepts UUIDv1 (time-based)"
    (let [the-uuid #uuid "c232ab00-9414-11ec-b3c8-9f68deced846"
          typeid (t/create "test" the-uuid)]
      (is (string? typeid))
      (is (.startsWith typeid "test_")))))

(deftest create-edge-case-uuids-test
  (testing "Create accepts all-zeros UUID"
    (let [the-uuid #uuid "00000000-0000-0000-0000-000000000000"
          typeid (t/create "test" the-uuid)]
      (is (string? typeid))
      (is (.startsWith typeid "test_"))
      (is (= "test_00000000000000000000000000" typeid))))

  (testing "Create accepts all-ones UUID (max value)"
    (let [the-uuid #uuid "ffffffff-ffff-ffff-ffff-ffffffffffff"
          typeid (t/create "test" the-uuid)]
      (is (string? typeid))
      (is (.startsWith typeid "test_")))))

(deftest create-invalid-uuid-type-test
  (testing "Create throws on non-UUID argument"
    (is (thrown? #?(:clj Exception :cljs js/Error)
          (t/create "user" "not-a-uuid"))))

  (testing "Create throws on number instead of UUID"
    (is (thrown? #?(:clj Exception :cljs js/Error)
          (t/create "user" 12345))))

  (testing "Create throws on nil UUID in two-arity"
    (is (thrown? #?(:clj Exception :cljs js/Error)
          (t/create "user" nil)))))

(deftest create-invalid-prefix-test
  (testing "Create throws on uppercase prefix"
    (is (thrown-with-msg? #?(:clj Exception :cljs js/Error)
          #"(?i)prefix.*pattern|lowercase"
          (t/create "User"))))

  (testing "Create throws on prefix with numbers"
    (is (thrown-with-msg? #?(:clj Exception :cljs js/Error)
          #"(?i)prefix.*pattern"
          (t/create "user123"))))

  (testing "Create throws on too-long prefix"
    (is (thrown-with-msg? #?(:clj Exception :cljs js/Error)
          #"(?i)prefix.*63|too long"
          (t/create (apply str (repeat 64 "a"))))))

  (testing "Create throws on prefix starting with underscore"
    (is (thrown-with-msg? #?(:clj Exception :cljs js/Error)
          #"(?i)prefix.*pattern"
          (t/create "_user")))))

;; Generator for valid TypeID prefixes (used by property-based tests)
(def gen-valid-prefix
  "Generator for valid TypeID prefixes matching the pattern [a-z]([a-z_]{0,61}[a-z])?"
  (gen/one-of
    [(gen/return "") ; Empty prefix
    ;; Single letter prefix (a-z)
     (gen/fmap str (gen/elements (map char (range 97 123))))
    ;; Multi-character prefix: starts with a-z, middle can be a-z or _, ends with a-z
     (gen/fmap (fn [[first-char middle-chars last-char]]
                 (str first-char (apply str middle-chars) last-char))
       (gen/tuple
         (gen/elements (map char (range 97 123))) ; First: a-z
         (gen/vector (gen/elements (concat (map char (range 97 123)) [\_])) 0 60) ; Middle: a-z or _ (max 60 to allow first+last)
         (gen/elements (map char (range 97 123)))))]))

;; T045: Property-based test for create round-trip with parse
(defspec create-parse-round-trip-property
  {:num-tests 100
   :seed 54321} ; Different seed from parse test
  (prop/for-all [prefix gen-valid-prefix]
    ;; Generate a UUID to use
    (let [the-uuid #?(:clj (java.util.UUID/randomUUID)
                      :cljs (random-uuid))
          ;; Create TypeID from UUID
          typeid (t/create (if (empty? prefix) nil prefix) the-uuid)
          ;; Parse it back
          parsed (t/parse typeid)
          parsed-uuid-bytes (:uuid parsed)
          uuid-bytes (uuid/uuid->bytes the-uuid)]
      ;; Verify round-trip consistency
      (and
       ;; Prefix should match
        (= (if (empty? prefix) "" prefix) (:prefix parsed))
       ;; UUID bytes should match
        (= (vec uuid-bytes) (vec parsed-uuid-bytes))
       ;; Should be valid TypeID
        (nil? (t/explain typeid))))))

;; T064: Property-based test for codec encode/decode round-trip
(defspec codec-encode-decode-round-trip-property
  {:num-tests 100
   :seed 11111} ; Deterministic seed
  (prop/for-all [prefix gen-valid-prefix]
    ;; Generate random UUID bytes
    (let [uuid-bytes #?(:clj (let [ba (byte-array 16)]
                               (dotimes [i 16]
                                 (aset ba i (unchecked-byte (rand-int 256))))
                               ba)
                        :cljs (let [arr (js/Uint8Array. 16)]
                                (dotimes [i 16]
                                  (aset arr i (rand-int 256)))
                                arr))
          ;; Encode with prefix
          typeid (codec/encode uuid-bytes (if (empty? prefix) nil prefix))
          ;; Decode back
          decoded-bytes (codec/decode typeid)]
      ;; Verify round-trip
      (= (vec uuid-bytes) (vec decoded-bytes)))))

;; T065: Property-based test for uuid->hex/hex->uuid round-trip
(defspec uuid-hex-round-trip-property
  {:num-tests 100
   :seed 22222} ; Deterministic seed
  (prop/for-all [_dummy (gen/return nil)] ; We generate our own random bytes
    ;; Generate random UUID bytes
    (let [uuid-bytes #?(:clj (let [ba (byte-array 16)]
                               (dotimes [i 16]
                                 (aset ba i (unchecked-byte (rand-int 256))))
                               ba)
                        :cljs (let [arr (js/Uint8Array. 16)]
                                (dotimes [i 16]
                                  (aset arr i (rand-int 256)))
                                arr))
          ;; Convert to hex
          hex-str (codec/uuid->hex uuid-bytes)
          ;; Convert back to bytes
          recovered-bytes (codec/hex->uuid hex-str)]
      ;; Verify round-trip
      (and
       ;; Hex string should be 32 chars
        (= 32 (count hex-str))
       ;; Hex string should be lowercase
        (= hex-str (str/lower-case hex-str))
       ;; UUID bytes should match
        (= (vec uuid-bytes) (vec recovered-bytes))))))

;; T026: Property-based test for parse round-trip consistency
(defspec parse-round-trip-property
  {:num-tests 100
   :seed 12345} ; Deterministic seed for reproducibility
  (prop/for-all [prefix gen-valid-prefix]
    ;; Generate a TypeID with the given prefix
    (let [typeid (t/create prefix)
          ;; Parse it back
          parsed (t/parse typeid)
          ;; Extract components
          parsed-prefix (:prefix parsed)
          parsed-typeid (:typeid parsed)]
      ;; Verify round-trip consistency
      (and
       ;; The parsed prefix should match the original (accounting for empty string vs "")
        (= (if (empty? prefix) "" prefix) parsed-prefix)
       ;; The parsed typeid should match the original
        (= typeid parsed-typeid)
       ;; The suffix should be exactly 26 characters
        (= 26 (count (:suffix parsed)))
       ;; The UUID should be valid (16 bytes)
        (v/valid-uuid-bytes? (:uuid parsed))
       ;; Re-generating from the same UUID should produce the same TypeID
        (= typeid (codec/encode (:uuid parsed) parsed-prefix))))))
