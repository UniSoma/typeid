(ns typeid.error-semantics-test
  "Test suite for error data structure consistency across all validation functions.
   Ensures all errors follow the {:error {:type :message :data}} pattern."
  (:require [clojure.test :refer [deftest is testing]]
    [typeid.core :as t]
    [typeid.validation :as v]))

;; T035: Error semantics test suite

#_{:clj-kondo/ignore [:type-mismatch]}
(deftest error-structure-test
  (testing "All validation errors return consistent structure"
    (let [errors [(v/validate-prefix "User123")      ; uppercase
                  (v/validate-prefix "_invalid")     ; starts with _
                  (t/parse "user_8zzzzzzzzzzzzzzzzzzzzzzzzz")  ; overflow
                  (t/parse "User_01h5fskfsk4fpeqwnsyz5hj55t") ; uppercase
                  (t/parse "user_tooshort")          ; short suffix
                  (t/parse 12345)                    ; non-string
                  (t/validate "User_01h5fskfsk4fpeqwnsyz5hj55t") ; validate uppercase
                  (t/validate "user_tooshort")]]     ; validate short

      (doseq [result errors]
        (is (contains? result :error) "Error result should have :error key")
        (is (map? (:error result)) "Error should be a map")
        (is (contains? (:error result) :type) "Error should have :type")
        (is (keyword? (:type (:error result))) "Error :type should be keyword")
        (is (contains? (:error result) :message) "Error should have :message")
        (is (string? (:message (:error result))) "Error :message should be string")
        (is (contains? (:error result) :data) "Error should have :data")
        (is (map? (:data (:error result))) "Error :data should be map")))))

#_{:clj-kondo/ignore [:type-mismatch]}
(deftest error-types-test
  (testing "Specific error types are returned for each validation failure"
    ;; Prefix validation errors
    (is (= :invalid-prefix-format
          (:type (:error (v/validate-prefix "User123"))))
      "Uppercase in prefix should return :invalid-prefix-format")

    (is (= :invalid-prefix-format
          (:type (:error (v/validate-prefix "_invalid"))))
      "Prefix starting with underscore should return :invalid-prefix-format")

    (is (= :prefix-too-long
          (:type (:error (v/validate-prefix (apply str (repeat 64 "a"))))))
      "Prefix > 63 chars should return :prefix-too-long")

    ;; Parse validation errors
    (is (= :invalid-case
          (:type (:error (t/parse "User_01h5fskfsk4fpeqwnsyz5hj55t"))))
      "Uppercase TypeID should return :invalid-case")

    (is (= :invalid-length
          (:type (:error (t/parse "user_tooshort"))))
      "Short TypeID should return :invalid-length")

    (is (= :invalid-typeid-type
          (:type (:error (t/parse 12345))))
      "Non-string input should return :invalid-typeid-type")

    (is (= :invalid-suffix
          (:type (:error (t/parse "user_8zzzzzzzzzzzzzzzzzzzzzzzzz"))))
      "Suffix starting with 8-z should return :invalid-suffix")

    (is (= :invalid-suffix
          (:type (:error (t/parse "user_01h5fskfsk4fpeqwnsyz5hj5il"))))
      "Suffix with invalid base32 chars should return :invalid-suffix")

    ;; Validate function errors (should match parse errors)
    (is (= :invalid-case
          (:type (:error (t/validate "User_01h5fskfsk4fpeqwnsyz5hj55t"))))
      "Validate: uppercase should return :invalid-case")

    (is (= :invalid-length
          (:type (:error (t/validate "user_tooshort"))))
      "Validate: short TypeID should return :invalid-length")))

#_{:clj-kondo/ignore [:type-mismatch]}
(deftest error-data-completeness-test
  (testing "Error data includes relevant debugging information"
    ;; Prefix errors
    (let [err-prefix (v/validate-prefix "User123")]
      (is (contains? (:data (:error err-prefix)) :prefix)
        "Prefix error should include :prefix in data")
      (is (contains? (:data (:error err-prefix)) :pattern)
        "Prefix error should include :pattern in data"))

    ;; Overflow error
    (let [err-overflow (t/parse "user_8zzzzzzzzzzzzzzzzzzzzzzzzz")]
      (is (contains? (:data (:error err-overflow)) :suffix)
        "Overflow error should include :suffix in data"))

    ;; Length error
    (let [err-length (t/parse "user_tooshort")]
      (is (contains? (:data (:error err-length)) :typeid)
        "Length error should include :typeid in data")
      (is (contains? (:data (:error err-length)) :length)
        "Length error should include :length in data"))

    ;; Type error
    (let [err-type (t/parse 12345)]
      (is (contains? (:data (:error err-type)) :value)
        "Type error should include :value in data")
      (is (contains? (:data (:error err-type)) :value-type)
        "Type error should include :value-type in data"))))

(deftest actionable-messages-test
  (testing "Error messages are actionable and explain the problem"
    (let [err-prefix (:message (:error (v/validate-prefix "User123")))
          err-overflow (:message (:error (t/parse "user_8zzzzzzzzzzzzzzzzzzzzzzzzz")))
          err-length (:message (:error (t/parse "user_tooshort")))
          err-case (:message (:error (t/parse "User_01h5fskfsk4fpeqwnsyz5hj55t")))]

      ;; Messages should mention what's wrong
      (is (re-find #"(?i)pattern|format|lowercase" err-prefix)
        "Prefix error should mention pattern/format/lowercase")

      (is (or (re-find #"(?i)overflow|0-7|first" err-overflow)
            (re-find #"(?i)suffix" err-overflow))
        "Overflow error should mention overflow/0-7/first or suffix")

      (is (re-find #"(?i)26|90|length|character" err-length)
        "Length error should mention character count or length")

      (is (re-find #"(?i)lowercase" err-case)
        "Case error should mention lowercase"))))

(deftest error-consistency-test
  (testing "Parse and validate return same error types for same invalid inputs"
    (let [invalid-inputs ["User_01h5fskfsk4fpeqwnsyz5hj55t"
                          "user_tooshort"
                          "user_8zzzzzzzzzzzzzzzzzzzzzzzzz"
                          12345]]
      (doseq [input invalid-inputs]
        (let [parse-error (:type (:error (t/parse input)))
              validate-error (:type (:error (t/validate input)))]
          (is (= parse-error validate-error)
            (str "Parse and validate should return same error type for: " input))))))

  (testing "All success results have consistent structure"
    (let [valid-typeids ["user_01h5fskfsk4fpeqwnsyz5hj55t"
                         "01h5fskfsk4fpeqwnsyz5hj55t"
                         "my_type_01h5fskfsk4fpeqwnsyz5hj55t"]]
      (doseq [typeid valid-typeids]
        ;; Parse success
        (let [parse-result (t/parse typeid)]
          (is (contains? parse-result :ok) "Parse success should have :ok key")
          (is (not (contains? parse-result :error)) "Parse success should not have :error key")
          (is (map? (:ok parse-result)) "Parse :ok should be a map with parsed data"))

        ;; Validate success
        (let [validate-result (t/validate typeid)]
          (is (contains? validate-result :ok) "Validate success should have :ok key")
          (is (not (contains? validate-result :error)) "Validate success should not have :error key")
          (is (= true (:ok validate-result)) "Validate :ok should be true"))))))
