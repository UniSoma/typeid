(ns typeid.error-semantics-test
  "Test suite for error data structure consistency across all validation functions.
   Ensures all errors follow the exception-based pattern with ex-info."
  (:require [clojure.test :refer [deftest is testing]]
    [typeid.core :as t]
    [typeid.validation :as v]))

;; T035: Error semantics test suite

(deftest error-structure-test
  (testing "All validation errors return consistent structure via explain"
    (let [errors [;; v/validate-prefix returns {:error {...}} structure
                  (:error (v/validate-prefix "User123"))      ; uppercase
                  (:error (v/validate-prefix "_invalid"))     ; starts with _
                  ;; t/explain returns error map directly
                  (t/explain "user_8zzzzzzzzzzzzzzzzzzzzzzzzz")  ; overflow
                  (t/explain "User_01h5fskfsk4fpeqwnsyz5hj55t") ; uppercase
                  (t/explain "user_tooshort")          ; short suffix
                  (t/explain 12345)]]                  ; non-string

      (doseq [error errors]
        (is (map? error) "Error should be a map")
        (is (contains? error :type) "Error should have :type")
        (is (keyword? (:type error)) "Error :type should be keyword")
        (is (contains? error :message) "Error should have :message")
        (is (string? (:message error)) "Error :message should be string")))))

(deftest error-types-test
  (testing "Specific error types are returned for each validation failure"
    ;; Prefix validation errors (extract from {:error {...}} structure)
    (is (= :invalid-prefix-format
          (:type (:error (v/validate-prefix "User123"))))
      "Uppercase in prefix should return :invalid-prefix-format")

    (is (= :invalid-prefix-format
          (:type (:error (v/validate-prefix "_invalid"))))
      "Prefix starting with underscore should return :invalid-prefix-format")

    (is (= :prefix-too-long
          (:type (:error (v/validate-prefix (apply str (repeat 64 "a"))))))
      "Prefix > 63 chars should return :prefix-too-long")

    ;; Parse validation errors (via explain since parse throws)
    (is (= :typeid/invalid-format
          (:type (t/explain "User_01h5fskfsk4fpeqwnsyz5hj55t")))
      "Uppercase TypeID should return :typeid/invalid-format")

    (is (= :typeid/invalid-length
          (:type (t/explain "user_tooshort")))
      "Short TypeID should return :typeid/invalid-length")

    (is (= :typeid/invalid-input-type
          (:type (t/explain 12345)))
      "Non-string input should return :typeid/invalid-input-type")

    (is (= :typeid/invalid-suffix
          (:type (t/explain "user_8zzzzzzzzzzzzzzzzzzzzzzzzz")))
      "Suffix starting with 8-z should return :typeid/invalid-suffix")

    (is (= :typeid/invalid-suffix
          (:type (t/explain "user_01h5fskfsk4fpeqwnsyz5hj5il")))
      "Suffix with invalid base32 chars should return :typeid/invalid-suffix")))

(deftest error-data-completeness-test
  (testing "Error data includes relevant debugging information"
    ;; Prefix errors
    (let [err-prefix (v/validate-prefix "User123")]
      (is (contains? (:error err-prefix) :data)
        "Prefix error should include :data")
      (is (contains? (:data (:error err-prefix)) :prefix)
        "Prefix error should include :prefix in data")
      (is (contains? (:data (:error err-prefix)) :pattern)
        "Prefix error should include :pattern in data"))

    ;; Overflow error
    (let [err-overflow (t/explain "user_8zzzzzzzzzzzzzzzzzzzzzzzzz")]
      (is (contains? err-overflow :input)
        "Overflow error should include :input"))

    ;; Length error
    (let [err-length (t/explain "user_tooshort")]
      (is (contains? err-length :input)
        "Length error should include :input")
      (is (contains? err-length :expected)
        "Length error should include :expected")
      (is (contains? err-length :actual)
        "Length error should include :actual"))

    ;; Type error
    (let [err-type (t/explain 12345)]
      (is (contains? err-type :input)
        "Type error should include :input")
      (is (contains? err-type :expected)
        "Type error should include :expected")
      (is (contains? err-type :actual)
        "Type error should include :actual"))))

(deftest actionable-messages-test
  (testing "Error messages are actionable and explain the problem"
    (let [err-prefix (:message (:error (v/validate-prefix "User123")))
          err-overflow (:message (t/explain "user_8zzzzzzzzzzzzzzzzzzzzzzzzz"))
          err-length (:message (t/explain "user_tooshort"))
          err-case (:message (t/explain "User_01h5fskfsk4fpeqwnsyz5hj55t"))]

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
  (testing "Parse throws and explain returns same error types for same invalid inputs"
    (let [invalid-inputs ["User_01h5fskfsk4fpeqwnsyz5hj55t"
                          "user_tooshort"
                          "user_8zzzzzzzzzzzzzzzzzzzzzzzzz"]]
      (doseq [input invalid-inputs]
        (let [explain-error (:type (t/explain input))
              parse-error (try
                            (t/parse input)
                            nil
                            (catch #?(:clj Exception :cljs js/Error) e
                              (:type (ex-data e))))]
          (is (= explain-error parse-error)
            (str "Parse and explain should return same error type for: " input))))))

  (testing "All success results have consistent structure"
    (let [valid-typeids ["user_01h5fskfsk4fpeqwnsyz5hj55t"
                         "01h5fskfsk4fpeqwnsyz5hj55t"
                         "my_type_01h5fskfsk4fpeqwnsyz5hj55t"]]
      (doseq [typeid valid-typeids]
        ;; Parse success
        (let [parse-result (t/parse typeid)]
          (is (map? parse-result) "Parse success should be a map with parsed data")
          (is (contains? parse-result :prefix) "Parse result should have :prefix")
          (is (contains? parse-result :suffix) "Parse result should have :suffix")
          (is (contains? parse-result :uuid) "Parse result should have :uuid"))

        ;; Explain success (returns nil for valid)
        (let [explain-result (t/explain typeid)]
          (is (nil? explain-result) "Explain success should return nil"))))))
