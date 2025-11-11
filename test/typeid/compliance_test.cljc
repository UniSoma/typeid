(ns typeid.compliance-test
  "Compliance tests using official reference test cases from valid.yml and invalid.yml.

   These tests ensure 100% specification compliance with the TypeID spec v0.3.0."
  (:require #?(:clj [clj-yaml.core :as yaml])
    #?(:clj [clojure.java.io :as io])
    [clojure.test :refer [deftest is testing]]
    [typeid.core :as t]))

#?(:clj (set! *warn-on-reflection* true))

;; T052: Load and parse YAML test files

#?(:clj
   (defn load-yaml-resource
     "Load and parse a YAML file from test/resources/"
     [filename]
     (when-let [resource (io/resource filename)]
       (yaml/parse-string (slurp resource)))))

#?(:clj
   (def valid-test-cases
     "Test cases from valid.yml"
     (load-yaml-resource "valid.yml")))

#?(:clj
   (def invalid-test-cases
     "Test cases from invalid.yml"
     (load-yaml-resource "invalid.yml")))

;; T053: Implement valid.yml test cases (encode UUID → expected TypeID)

#?(:clj
   (deftest valid-encode-test
     (testing "Encode UUID bytes with prefix produces expected TypeID"
       (doseq [test-case valid-test-cases]
         (let [test-name (:name test-case)
               expected-typeid (:typeid test-case)
               prefix (:prefix test-case)
               uuid-hex (:uuid test-case)
               ;; Parse UUID from hex string (with or without dashes)
               uuid-hex-clean (clojure.string/replace uuid-hex #"-" "")
               {uuid-bytes :ok} (t/hex->uuid uuid-hex-clean)
               {encoded-typeid :ok encode-error :error} (t/encode uuid-bytes prefix)]
           (is (nil? encode-error)
             (str "Test case '" test-name "' should not error on encode: "
               (:message encode-error)))
           (is (= expected-typeid encoded-typeid)
             (str "Test case '" test-name "' encode mismatch: "
               "expected=" expected-typeid " actual=" encoded-typeid)))))))

;; T054: Implement valid.yml test cases (decode TypeID → expected UUID)

#?(:clj
   (deftest valid-decode-test
     (testing "Decode TypeID produces expected UUID and prefix"
       (doseq [test-case valid-test-cases]
         (let [test-name (:name test-case)
               typeid-str (:typeid test-case)
               expected-prefix (:prefix test-case)
               expected-uuid (:uuid test-case)
               ;; Parse UUID from hex string (with or without dashes)
               expected-uuid-clean (clojure.string/replace expected-uuid #"-" "")
               {parsed :ok parse-error :error} (t/parse typeid-str)]
           (is (nil? parse-error)
             (str "Test case '" test-name "' should not error on parse: "
               (:message parse-error)))
           (is (= expected-prefix (:prefix parsed))
             (str "Test case '" test-name "' prefix mismatch: "
               "expected=" expected-prefix " actual=" (:prefix parsed)))
           ;; Convert UUID bytes to hex for comparison
           (let [{uuid-hex :ok} (t/uuid->hex (:uuid parsed))]
             (is (= expected-uuid-clean uuid-hex)
               (str "Test case '" test-name "' UUID mismatch: "
                 "expected=" expected-uuid-clean " actual=" uuid-hex))))))))

;; T055: Implement invalid.yml test cases (reject with appropriate errors)

#?(:clj
   (deftest invalid-test-cases-test
     (testing "Invalid TypeIDs are rejected with errors"
       (doseq [test-case invalid-test-cases]
         (let [test-name (:name test-case)
               invalid-typeid (:typeid test-case)
               description (:description test-case)
               {ok :ok error :error} (t/parse invalid-typeid)]
           (is (nil? ok)
             (str "Test case '" test-name "' should fail: " description))
           (is (some? error)
             (str "Test case '" test-name "' should return an error: " description))
           (is (keyword? (:type error))
             (str "Test case '" test-name "' error should have a :type keyword"))
           (is (string? (:message error))
             (str "Test case '" test-name "' error should have a :message string")))))))

;; Round-trip property test

#?(:clj
   (deftest valid-round-trip-test
     (testing "All valid test cases survive encode→decode→encode round-trip"
       (doseq [test-case valid-test-cases]
         (let [test-name (:name test-case)
               original-typeid (:typeid test-case)
               {parsed :ok} (t/parse original-typeid)
               {re-encoded :ok} (t/encode (:uuid parsed) (:prefix parsed))]
           (is (= original-typeid re-encoded)
             (str "Test case '" test-name "' round-trip failed: "
               "original=" original-typeid " re-encoded=" re-encoded)))))))
