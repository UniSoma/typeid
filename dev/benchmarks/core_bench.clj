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
  (:require
    [criterium.core :as crit]
    [typeid.codec :as codec]
    [typeid.core :as typeid]
    [typeid.impl.base32 :as base32]
    [typeid.uuid :as uuid]
    [typeid.validation :as v]))

(set! *warn-on-reflection* true)

;; ============================================================================
;; Benchmark Utilities
;; ============================================================================

(defn format-time
  "Format nanoseconds to human-readable time with appropriate unit."
  [time-ns]
  (cond
    (< time-ns 1) (format "%.3fns" (double time-ns))     ; Sub-nanosecond with 3 decimals
    (< time-ns 10) (format "%.2fns" (double time-ns))    ; Single digit ns with 2 decimals
    (< time-ns 1000) (format "%.1fns" (double time-ns))  ; Sub-microsecond with 1 decimal
    (< time-ns 1000000) (format "%.2fμs" (/ time-ns 1000.0))
    (< time-ns 1000000000) (format "%.2fms" (/ time-ns 1000000.0))
    :else (format "%.2fs" (/ time-ns 1000000000.0))))

(defn format-ops-per-sec
  "Format operations per second in human-readable form."
  [ops-per-sec]
  (cond
    (>= ops-per-sec 1000000000) (format "%.2fG ops/s" (/ ops-per-sec 1000000000.0))
    (>= ops-per-sec 1000000) (format "%.2fM ops/s" (/ ops-per-sec 1000000.0))
    (>= ops-per-sec 1000) (format "%.2fK ops/s" (/ ops-per-sec 1000.0))
    :else (format "%.0f ops/s" (double ops-per-sec))))

(defn print-result
  "Print benchmark result with target comparison and detailed metrics."
  [label result target-ns]
  (let [mean-sec (first (:mean result))         ; Criterium returns mean in seconds
        variance-sec2 (first (:variance result)) ; Variance in seconds²
        mean-ns (* mean-sec 1e9)                 ; Convert to nanoseconds
        std-dev-ns (* (Math/sqrt variance-sec2) 1e9) ; Convert std dev to ns
        ops-per-sec (/ 1 mean-sec)               ; Operations per second
        status (if (<= mean-ns target-ns) "✓" "✗")]
    (println (format "%-30s %s %8s  %14s  ±%.1f%%"
               label
               status
               (format-time mean-ns)
               (format-ops-per-sec ops-per-sec)
               (* 100 (/ std-dev-ns mean-ns))))))

(defn print-benchmark-header
  "Print column headers for benchmark results."
  []
  (println)
  (println (format "%-30s %s %8s  %14s  %s"
             "Operation"
             " "
             "Time"
             "Throughput"
             "Variability"))
  (println (apply str (repeat 75 "-"))))

(defn run-benchmark
  "Run a benchmark with criterium and print results."
  [label f target-ns]
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
  (typeid/create "user"))

(def sample-suffix
  "Sample base32 suffix for testing."
  (base32/encode sample-uuid-bytes))

;; ============================================================================
;; Benchmarks
;; ============================================================================

(defn bench-generate
  "Benchmark typeid.core/generate function.
   Target: < 2μs total (2000ns)"
  []
  (run-benchmark
    "typeid/create"
    #(typeid/create "user")
    2000))

(defn bench-generate-no-prefix
  "Benchmark typeid.core/generate with empty prefix.
   Target: < 2μs total (2000ns)"
  []
  (run-benchmark
    "typeid/create (no prefix)"
    #(typeid/create "")
    2000))

(defn bench-parse
  "Benchmark typeid.core/parse function.
   Target: < 2μs total (2000ns)"
  []
  (run-benchmark
    "typeid/parse"
    #(typeid/parse sample-typeid)
    2000))

(defn bench-encode
  "Benchmark typeid.core/encode function.
   Target: < 1μs (1000ns)"
  []
  (run-benchmark
    "typeid/encode"
    #(codec/encode sample-uuid-bytes "user")
    1000))

(defn bench-decode
  "Benchmark typeid.core/decode function.
   Target: < 1μs (1000ns)"
  []
  (run-benchmark
    "typeid/decode"
    #(codec/decode sample-typeid)
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

(defn bench-bytes-to-uuid
  "Benchmark bytes->uuid conversion.
   Target: < 500ns"
  []
  (run-benchmark
    "uuid/bytes->uuid"
    #(uuid/bytes->uuid sample-uuid-bytes)
    500))

(defn bench-uuid-to-bytes
  "Benchmark uuid->bytes conversion.
   Target: < 500ns"
  []
  (let [sample-uuid (uuid/bytes->uuid sample-uuid-bytes)]
    (run-benchmark
      "uuid/uuid->bytes"
      #(uuid/uuid->bytes sample-uuid)
      500)))

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

  ;; Core API benchmarks
  (println "\n=== Core API Functions ===")
  (print-benchmark-header)
  (bench-generate)
  (bench-generate-no-prefix)
  (bench-parse)
  (bench-encode)
  (bench-decode)

  ;; Low-level component benchmarks
  (println "\n\n=== Low-Level Components ===")
  (print-benchmark-header)
  (bench-base32-encode)
  (bench-base32-decode)
  (bench-uuidv7-generation)
  (bench-bytes-to-uuid)
  (bench-uuid-to-bytes)

  ;; Validation benchmarks
  (println "\n\n=== Validation Functions ===")
  (print-benchmark-header)
  (bench-prefix-validation)
  (bench-prefix-validation-invalid)
  (bench-typeid-string-validation)
  (bench-uuid-bytes-validation)

  (println "\n========================================")
  (println "Benchmarks Complete!")
  (println "========================================")
  (println)
  (println "Column descriptions:")
  (println "  ✓/✗         - Pass/Fail indicator (vs. target)")
  (println "  Time        - Mean execution time per operation")
  (println "  Throughput  - Operations per second")
  (println "  Variability - Standard deviation as % of mean")
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
