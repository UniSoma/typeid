(ns typeid.compliance-test
  "Compliance tests using official reference test cases from valid.yml and invalid.yml.

   These tests ensure 100% specification compliance with the TypeID spec v0.3.0."
  {:clj-kondo/config '{:linters {:unused-namespace {:level :off}
                                 :unused-referred-var {:level :off}}}}
  (:require [clj-yaml.core :as yaml]
    [clojure.java.io :as io]
    [clojure.string :as string]
    [clojure.test :refer [deftest is testing]]
    [typeid.codec :as codec]
    [typeid.core :as t]
    [typeid.impl.uuid :as uuid]))

(set! *warn-on-reflection* true)

;; T052: Load and parse YAML test files

(defn load-yaml-resource
  "Load and parse a YAML file from test/resources/"
  [filename]
  (when-let [resource (io/resource filename)]
    (yaml/parse-string (slurp resource))))

(def valid-test-cases
  "Test cases from valid.yml"
  (load-yaml-resource "valid.yml"))

(def invalid-test-cases
  "Test cases from invalid.yml"
  (load-yaml-resource "invalid.yml"))

;; T053: Implement valid.yml test cases (encode UUID → expected TypeID)

(deftest valid-encode-test
  (testing "Encode UUID bytes with prefix produces expected TypeID"
    (doseq [test-case valid-test-cases]
      (let [test-name (:name test-case)
            expected-typeid (:typeid test-case)
            prefix (:prefix test-case)
            uuid-hex (:uuid test-case)
            ;; Parse UUID from hex string (with or without dashes)
            uuid-hex-clean (string/replace uuid-hex #"-" "")
            uuid-bytes (codec/hex->uuid uuid-hex-clean)
            encoded-typeid (codec/encode uuid-bytes prefix)]
        (is (= expected-typeid encoded-typeid)
          (str "Test case '" test-name "' encode mismatch: "
            "expected=" expected-typeid " actual=" encoded-typeid))))))

;; T054: Implement valid.yml test cases (decode TypeID → expected UUID)

(deftest valid-decode-test
  (testing "Decode TypeID produces expected UUID and prefix"
    (doseq [test-case valid-test-cases]
      (let [test-name (:name test-case)
            typeid-str (:typeid test-case)
            expected-prefix (:prefix test-case)
            expected-uuid (:uuid test-case)
            ;; Parse UUID from hex string (with or without dashes)
            expected-uuid-clean (string/replace expected-uuid #"-" "")
            parsed (t/parse typeid-str)]
        (is (= expected-prefix (:prefix parsed))
          (str "Test case '" test-name "' prefix mismatch: "
            "expected=" expected-prefix " actual=" (:prefix parsed)))
        ;; Convert UUID object to bytes, then to hex for comparison
        (let [uuid-bytes (uuid/uuid->bytes (:uuid parsed))
              uuid-hex (codec/uuid->hex uuid-bytes)]
          (is (= expected-uuid-clean uuid-hex)
            (str "Test case '" test-name "' UUID mismatch: "
              "expected=" expected-uuid-clean " actual=" uuid-hex)))))))

;; T055: Implement invalid.yml test cases (reject with appropriate errors)

(deftest invalid-test-cases-test
  (testing "Invalid TypeIDs are rejected with errors"
    (doseq [test-case invalid-test-cases]
      (let [test-name (:name test-case)
            invalid-typeid (:typeid test-case)
            description (:description test-case)]
        (is (thrown? Exception (t/parse invalid-typeid))
          (str "Test case '" test-name "' should throw: " description))
        ;; Verify error details using explain
        (let [error (t/explain invalid-typeid)]
          (is (some? error)
            (str "Test case '" test-name "' should return an error: " description))
          (is (keyword? (:type error))
            (str "Test case '" test-name "' error should have a :type keyword"))
          (is (string? (:message error))
            (str "Test case '" test-name "' error should have a :message string")))))))

;; Round-trip property test

(deftest valid-round-trip-test
  (testing "All valid test cases survive encode→decode→encode round-trip"
    (doseq [test-case valid-test-cases]
      (let [test-name (:name test-case)
            original-typeid (:typeid test-case)
            parsed (t/parse original-typeid)
            ;; Convert UUID object to bytes for encoding
            uuid-bytes (uuid/uuid->bytes (:uuid parsed))
            re-encoded (codec/encode uuid-bytes (:prefix parsed))]
        (is (= original-typeid re-encoded)
          (str "Test case '" test-name "' round-trip failed: "
            "original=" original-typeid " re-encoded=" re-encoded))))))
