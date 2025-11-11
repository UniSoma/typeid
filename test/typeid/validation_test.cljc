(ns typeid.validation-test
    (:require [clojure.test :refer [deftest is testing are]]
              [typeid.validation :as v]))

;; T028: Validation unit tests

(deftest valid-prefix-test
  (testing "Valid prefixes"
    (are [prefix] (v/valid-prefix? prefix)
         ""                 ; Empty prefix
         "a"                ; Single char
         "user"             ; Simple prefix
         "my_type"          ; With underscore
         "user_account"     ; Multiple underscores
         "a_b_c"            ; Multiple underscores
         (apply str (repeat 63 "a")))) ; Max length

  (testing "Invalid prefixes"
    (are [prefix] (not (v/valid-prefix? prefix))
         "User"             ; Uppercase
         "user123"          ; Numbers
         "_user"            ; Starts with underscore
         "user_"            ; Ends with underscore
         "_"                ; Just underscore
         "user-type"        ; Hyphen
         "user.type"        ; Dot
         "user type"        ; Space
         (apply str (repeat 64 "a"))))) ; Too long

(deftest validate-prefix-test
  (testing "Valid prefix returns :ok"
    (let [result (v/validate-prefix "user")]
         (is (contains? result :ok))
         (is (= "user" (:ok result)))))

  (testing "Empty prefix is valid"
    (let [result (v/validate-prefix "")]
         (is (contains? result :ok))
         (is (= "" (:ok result)))))

  (testing "Invalid prefix returns :error with details"
    (let [result (v/validate-prefix "User123")]
         (is (contains? result :error))
         (is (= :invalid-prefix-format (:type (:error result))))
         (is (string? (:message (:error result))))
         (is (map? (:data (:error result))))))

  (testing "Too long prefix returns specific error"
    (let [result (v/validate-prefix (apply str (repeat 64 "a")))]
         (is (contains? result :error))
         (is (= :prefix-too-long (:type (:error result))))))

  (testing "Non-string prefix returns type error"
    (let [result (v/validate-prefix 123)]
         (is (contains? result :error))
         (is (= :invalid-prefix-type (:type (:error result)))))))

(deftest valid-base32-suffix-test
  (testing "Valid suffixes"
    (are [suffix] (v/valid-base32-suffix? suffix)
         "00000000000000000000000000" ; All zeros
         "01234567890123456789012345" ; Valid chars, starts with 0
         "7zzzzzzzzzzzzzzzzzzzzzzzzz" ; Starts with 7
         "0abcdefghjkmnpqrstvwxyz000")) ; All valid base32 chars

  (testing "Invalid suffixes - wrong length"
    (is (not (v/valid-base32-suffix? "tooshort")))
    (is (not (v/valid-base32-suffix? "toolong000000000000000000000000"))))

  (testing "Invalid suffixes - overflow (first char > 7)"
    (are [suffix] (not (v/valid-base32-suffix? suffix))
         "8zzzzzzzzzzzzzzzzzzzzzzzzz" ; Starts with 8
         "9zzzzzzzzzzzzzzzzzzzzzzzzz" ; Starts with 9
         "azzzzzzzzzzzzzzzzzzzzzzzzz" ; Starts with a
         "zzzzzzzzzzzzzzzzzzzzzzzzzz")) ; Starts with z

  (testing "Invalid suffixes - excluded characters"
    (are [suffix] (not (v/valid-base32-suffix? suffix))
         "01234567890123456789012i45" ; Contains 'i'
         "01234567890123456789012l45" ; Contains 'l'
         "01234567890123456789012o45" ; Contains 'o'
         "01234567890123456789012u45"))) ; Contains 'u'

(deftest valid-typeid-string-test
  (testing "Valid TypeID strings"
    (are [s] (v/valid-typeid-string? s)
         "user_01h5fskfsk4fpeqwnsyz5hj55t" ; With prefix
         "01h5fskfsk4fpeqwnsyz5hj55t"      ; Without prefix
         "a_01h5fskfsk4fpeqwnsyz5hj55t"    ; Short prefix
         (str (apply str (repeat 63 "a")) "_01h5fskfsk4fpeqwnsyz5hj55t"))) ; Max prefix

  (testing "Invalid TypeID strings - length"
    (is (not (v/valid-typeid-string? "tooshort")))
    (is (not (v/valid-typeid-string? (apply str (repeat 91 "a"))))))

  (testing "Invalid TypeID strings - case"
    (is (not (v/valid-typeid-string? "User_01h5fskfsk4fpeqwnsyz5hj55t")))
    (is (not (v/valid-typeid-string? "user_01H5FSKFSK4FPEQWNSYZ5HJ55T")))))

(deftest valid-uuid-bytes-test
  (testing "Valid UUID bytes"
    (is (v/valid-uuid-bytes? #?(:clj (byte-array 16)
                                :cljs (js/Uint8Array. 16)))))

  (testing "Invalid UUID bytes - wrong length"
    (is (not (v/valid-uuid-bytes? #?(:clj (byte-array 15)
                                     :cljs (js/Uint8Array. 15)))))
    (is (not (v/valid-uuid-bytes? #?(:clj (byte-array 17)
                                     :cljs (js/Uint8Array. 17))))))

  (testing "Invalid UUID bytes - wrong type"
    (is (not (v/valid-uuid-bytes? "not-bytes")))
    (is (not (v/valid-uuid-bytes? 12345)))
    (is (not (v/valid-uuid-bytes? [1 2 3 4 5 6 7 8 9 10 11 12 13 14 15 16])))))

(deftest valid-uuidv7-bytes-test
  (testing "Valid UUIDv7 bytes with correct version and variant"
    (let [uuid #?(:clj (byte-array 16)
                  :cljs (js/Uint8Array. 16))]
      ;; Set version to 7 (byte 6, high nibble)
         (aset uuid 6 #?(:clj (unchecked-byte 0x70) :cljs 0x70))
      ;; Set variant to 10 (byte 8, high 2 bits)
         (aset uuid 8 #?(:clj (unchecked-byte 0x80) :cljs 0x80))
         (is (v/valid-uuidv7-bytes? uuid))))

  (testing "Invalid UUIDv7 - wrong version"
    (let [uuid #?(:clj (byte-array 16)
                  :cljs (js/Uint8Array. 16))]
      ;; Set version to 4 (byte 6, high nibble)
         (aset uuid 6 #?(:clj (unchecked-byte 0x40) :cljs 0x40))
      ;; Set variant to 10 (byte 8, high 2 bits)
         (aset uuid 8 #?(:clj (unchecked-byte 0x80) :cljs 0x80))
         (is (not (v/valid-uuidv7-bytes? uuid)))))

  (testing "Invalid UUIDv7 - wrong variant"
    (let [uuid #?(:clj (byte-array 16)
                  :cljs (js/Uint8Array. 16))]
      ;; Set version to 7 (byte 6, high nibble)
         (aset uuid 6 #?(:clj (unchecked-byte 0x70) :cljs 0x70))
      ;; Set variant to 00 (byte 8, high 2 bits)
         (aset uuid 8 #?(:clj (unchecked-byte 0x00) :cljs 0x00))
         (is (not (v/valid-uuidv7-bytes? uuid))))))
