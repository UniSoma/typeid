(ns typeid.compliance-test
  "Compliance tests using official reference test cases from valid.yml and invalid.yml.

   These tests ensure 100% specification compliance with the TypeID spec v0.3.0."
  {:clj-kondo/config '{:linters {:unused-namespace {:level :off}
                                 :unused-referred-var {:level :off}}}}
  (:require #?(:clj  [clojure.test :refer [deftest is testing]]
               :cljs [cljs.test :refer [deftest is testing]])
    #?(:clj [clj-yaml.core :as yaml])
    #?(:clj [clojure.java.io :as io])
    #?(:clj [clojure.string :as string])
    [typeid.codec :as codec]
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
               uuid-hex-clean #?(:clj (string/replace uuid-hex #"-" "")
                                 :cljs uuid-hex)
               uuid-bytes (codec/hex->uuid uuid-hex-clean)
               encoded-typeid (codec/encode uuid-bytes prefix)]
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
               expected-uuid-clean #?(:clj (string/replace expected-uuid #"-" "")
                                      :cljs expected-uuid)
               parsed (t/parse typeid-str)]
           (is (= expected-prefix (:prefix parsed))
             (str "Test case '" test-name "' prefix mismatch: "
               "expected=" expected-prefix " actual=" (:prefix parsed)))
           ;; Convert UUID bytes to hex for comparison
           (let [uuid-hex (codec/uuid->hex (:uuid parsed))]
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
               description (:description test-case)]
           (is (thrown? #?(:clj Exception :cljs js/Error)
                 (t/parse invalid-typeid))
             (str "Test case '" test-name "' should throw: " description))
           ;; Verify error details using explain
           (let [error (t/explain invalid-typeid)]
             (is (some? error)
               (str "Test case '" test-name "' should return an error: " description))
             (is (keyword? (:type error))
               (str "Test case '" test-name "' error should have a :type keyword"))
             (is (string? (:message error))
               (str "Test case '" test-name "' error should have a :message string"))))))))

;; Round-trip property test

#?(:clj
   (deftest valid-round-trip-test
     (testing "All valid test cases survive encode→decode→encode round-trip"
       (doseq [test-case valid-test-cases]
         (let [test-name (:name test-case)
               original-typeid (:typeid test-case)
               parsed (t/parse original-typeid)
               re-encoded (codec/encode (:uuid parsed) (:prefix parsed))]
           (is (= original-typeid re-encoded)
             (str "Test case '" test-name "' round-trip failed: "
               "original=" original-typeid " re-encoded=" re-encoded)))))))
