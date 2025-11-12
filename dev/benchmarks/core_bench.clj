(ns benchmarks.core-bench
  "Performance benchmarks for typeid library.

   Validates that performance targets are met:
   - generate: < 2μs total
   - base32/encode: < 1μs
   - base32/decode: < 1μs
   - prefix validation: < 500ns

   Usage:
     clojure -M:dev -m benchmarks.core-bench

   With reflection warnings check:
     clojure -M:dev -e \"(set! *warn-on-reflection* true) (require 'benchmarks.core-bench)\""
  (:require [criterium.core :as crit]
    [typeid.core :as typeid]
    [typeid.impl.base32 :as base32]
    [typeid.impl.uuid :as uuid]
    [typeid.validation :as v]))

(set! *warn-on-reflection* true)

;; ============================================================================
;; Benchmark Utilities
;; ============================================================================

(defn format-time
  "Format nanoseconds to human-readable time with target comparison."
  [mean-ns target-ns]
  (let [mean-us (/ mean-ns 1000.0)
        target-us (/ target-ns 1000.0)
        status (if (<= mean-ns target-ns) "✓ PASS" "✗ FAIL")]
    (format "%s %.2fμs (target: %.2fμs)" status mean-us target-us)))

(defn print-result
  "Print benchmark result with target comparison."
  [label result target-ns]
  (let [mean-ns (first (:mean result))]
    (println (format "%-30s %s" label (format-time mean-ns target-ns)))))

(defn run-benchmark
  "Run a benchmark with criterium and print results."
  [label f target-ns]
  (println (format "\nBenchmarking: %s" label))
  (let [result (crit/quick-benchmark* f nil)]
    (print-result label result target-ns)
    result))

;; ============================================================================
;; Test Data
;; ============================================================================

(def sample-uuid-bytes
  "Sample UUID bytes for testing."
  (uuid/generate-uuidv7))

(def sample-typeid
  "Sample TypeID string for testing."
  (let [{:keys [ok]} (typeid/generate "user")]
    ok))

(def sample-suffix
  "Sample base32 suffix for testing."
  (base32/encode sample-uuid-bytes))

(def sample-prefix "user")

;; ============================================================================
;; Benchmarks
;; ============================================================================

(defn bench-generate
  "Benchmark typeid.core/generate function.
   Target: < 2μs total (2000ns)"
  []
  (run-benchmark
    "typeid/generate"
    #(typeid/generate "user")
    2000))

(defn bench-generate-no-prefix
  "Benchmark typeid.core/generate with empty prefix.
   Target: < 2μs total (2000ns)"
  []
  (run-benchmark
    "typeid/generate (no prefix)"
    #(typeid/generate "")
    2000))

(defn bench-parse
  "Benchmark typeid.core/parse function.
   Target: < 2μs total (2000ns)"
  []
  (run-benchmark
    "typeid/parse"
    #(typeid/parse sample-typeid)
    2000))

(defn bench-validate
  "Benchmark typeid.core/validate function.
   Target: < 1μs (1000ns)"
  []
  (run-benchmark
    "typeid/validate"
    #(typeid/validate sample-typeid)
    1000))

(defn bench-encode
  "Benchmark typeid.core/encode function.
   Target: < 1μs (1000ns)"
  []
  (run-benchmark
    "typeid/encode"
    #(typeid/encode sample-uuid-bytes "user")
    1000))

(defn bench-decode
  "Benchmark typeid.core/decode function.
   Target: < 1μs (1000ns)"
  []
  (run-benchmark
    "typeid/decode"
    #(typeid/decode sample-typeid)
    1000))

(defn bench-base32-encode
  "Benchmark base32/encode function.
   Target: < 1μs (1000ns)"
  []
  (run-benchmark
    "base32/encode"
    #(base32/encode sample-uuid-bytes)
    1000))

(defn bench-base32-decode
  "Benchmark base32/decode function.
   Target: < 1μs (1000ns)"
  []
  (run-benchmark
    "base32/decode"
    #(base32/decode sample-suffix)
    1000))

(defn bench-uuidv7-generation
  "Benchmark UUIDv7 generation.
   Target: < 500ns"
  []
  (run-benchmark
    "uuid/generate-uuidv7"
    #(uuid/generate-uuidv7)
    500))

(defn bench-prefix-validation
  "Benchmark prefix validation.
   Target: < 500ns"
  []
  (run-benchmark
    "validate-prefix (valid)"
    #(v/validate-prefix "user")
    500))

(defn bench-prefix-validation-invalid
  "Benchmark prefix validation with invalid input.
   Target: < 500ns"
  []
  (run-benchmark
    "validate-prefix (invalid)"
    #(v/validate-prefix "User")
    500))

(defn bench-typeid-string-validation
  "Benchmark TypeID string format validation.
   Target: < 500ns"
  []
  (run-benchmark
    "valid-typeid-string?"
    #(v/valid-typeid-string? sample-typeid)
    500))

(defn bench-uuid-bytes-validation
  "Benchmark UUID bytes validation.
   Target: < 500ns"
  []
  (run-benchmark
    "valid-uuid-bytes?"
    #(v/valid-uuid-bytes? sample-uuid-bytes)
    500))

;; ============================================================================
;; Main Entry Point
;; ============================================================================

(defn -main
  "Run all benchmarks and report results."
  [& _args]
  (println "========================================")
  (println "TypeID Library Performance Benchmarks")
  (println "========================================")
  (println)
  (println "Running benchmarks with criterium...")
  (println "This may take several minutes.")
  (println)

  ;; Core API benchmarks
  (println "\n=== Core API Functions ===")
  (bench-generate)
  (bench-generate-no-prefix)
  (bench-parse)
  (bench-validate)
  (bench-encode)
  (bench-decode)

  ;; Low-level component benchmarks
  (println "\n=== Low-Level Components ===")
  (bench-base32-encode)
  (bench-base32-decode)
  (bench-uuidv7-generation)

  ;; Validation benchmarks
  (println "\n=== Validation Functions ===")
  (bench-prefix-validation)
  (bench-prefix-validation-invalid)
  (bench-typeid-string-validation)
  (bench-uuid-bytes-validation)

  (println "\n========================================")
  (println "Benchmarks Complete!")
  (println "========================================")
  (println)
  (println "Summary:")
  (println "- All times shown are mean execution time")
  (println "- ✓ PASS = meets performance target")
  (println "- ✗ FAIL = exceeds performance target")
  (println)
  (println "Note: Run with reflection warning check:")
  (println "  clojure -M:dev -e \"(set! *warn-on-reflection* true) (require 'benchmarks.core-bench)\"")
  (println))

;; Allow running from REPL
(comment
  ;; Run all benchmarks
  (-main)

  ;; Run individual benchmarks
  (bench-generate)
  (bench-base32-encode)
  (bench-prefix-validation))
