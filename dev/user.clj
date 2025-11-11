(ns user
  "Convenience functions for REPL development.

   This namespace is automatically loaded when starting a REPL
   from the project root with the :dev alias.

   Usage:
     clojure -M:dev
     user=> (refresh)  ; Reload changed namespaces
     user=> (test-all) ; Run all tests
     user=> (bench)    ; Run benchmarks"
  (:require [clojure.pprint :refer [pprint]]
    [clojure.repl :refer [doc source apropos dir]]
    [clojure.string :as str]
    [clojure.tools.namespace.repl :refer [refresh]]
    [typeid.core :as typeid]
    [typeid.impl.base32 :as base32]
    [typeid.impl.uuid :as uuid]
    [typeid.validation :as v]))

;; ============================================================================
;; REPL Utilities
;; ============================================================================

(defn reload
  "Reload all changed namespaces."
  []
  (refresh))

(defn test-all
  "Run all tests from the REPL."
  []
  (require 'kaocha.repl)
  ((resolve 'kaocha.repl/run-all)))

(defn test-watch
  "Watch for file changes and run tests automatically."
  []
  (require 'kaocha.repl)
  ((resolve 'kaocha.repl/run)))

(defn bench
  "Run performance benchmarks."
  []
  (require 'benchmarks.core-bench)
  ((resolve 'benchmarks.core-bench/-main)))

(defn lint
  "Run clj-kondo linting."
  []
  (require 'clj-kondo.main)
  ((resolve 'clj-kondo.main/main) "--lint" "src" "test"))

(defn format-code
  "Format all source files with cljfmt."
  []
  (require 'cljfmt.main)
  ((resolve 'cljfmt.main/main) "fix" "src" "test" "dev"))

;; ============================================================================
;; Quick Examples for REPL Exploration
;; ============================================================================

(defn examples
  "Print example usage for interactive exploration."
  []
  (println "
=== TypeID REPL Examples ===

;; Generate TypeIDs
(typeid/generate \"user\")
(typeid/generate \"\")  ; No prefix

;; Parse TypeIDs
(typeid/parse \"user_01h5fskfsk4fpeqwnsyz5hj55t\")
(typeid/parse \"01h5fskfsk4fpeqwnsyz5hj55t\")  ; No prefix

;; Validate TypeIDs
(typeid/validate \"user_01h5fskfsk4fpeqwnsyz5hj55t\")
(typeid/validate \"Invalid_TypeID\")

;; Convert between UUID and TypeID
(def uuid-bytes (uuid/generate-uuidv7))
(typeid/encode uuid-bytes \"user\")
(typeid/decode \"user_01h5fskfsk4fpeqwnsyz5hj55t\")

;; UUID format conversion
(typeid/uuid->hex uuid-bytes)
(typeid/hex->uuid \"01890a5d-ac96-774b-bcce-b302099a8057\")

;; Get all components
(typeid/typeid->map \"user_01h5fskfsk4fpeqwnsyz5hj55t\")

;; Validation predicates
(v/valid-prefix? \"user\")
(v/valid-prefix? \"User\")  ; false - must be lowercase
(v/valid-typeid-string? \"user_01h5fskfsk4fpeqwnsyz5hj55t\")
(v/valid-uuid-bytes? uuid-bytes)

;; Low-level operations
(base32/encode uuid-bytes)
(base32/decode \"01h5fskfsk4fpeqwnsyz5hj55t\")

=== REPL Utilities ===

(reload)          ; Reload changed namespaces
(test-all)        ; Run all tests
(test-watch)      ; Watch and run tests on changes
(bench)           ; Run performance benchmarks
(lint)            ; Run clj-kondo linting
(format-code)     ; Format source code with cljfmt
(examples)        ; Show this help message
"))

;; ============================================================================
;; Sample Data for Quick Testing
;; ============================================================================

(def sample-prefixes
  "Common prefix examples for testing."
  ["user" "post" "comment" "order" "product" "invoice" "customer" ""])

(def sample-uuid
  "Sample UUID bytes for testing."
  (uuid/generate-uuidv7))

(def sample-typeid
  "Sample TypeID for testing."
  (:ok (typeid/generate "user")))

(defn generate-samples
  "Generate sample TypeIDs with different prefixes."
  []
  (into {}
    (map (fn [prefix]
           (let [result (typeid/generate prefix)
                 key (if (str/blank? prefix) :no-prefix (keyword prefix))]
             [key (:ok result)]))
      sample-prefixes)))

;; ============================================================================
;; Welcome Message
;; ============================================================================

(println "
╔══════════════════════════════════════════════════════════════╗
║                                                              ║
║  TypeID Clojure Library - REPL Utilities Loaded             ║
║                                                              ║
║  Quick start:                                                ║
║    (examples)    - Show usage examples                       ║
║    (test-all)    - Run all tests                             ║
║    (bench)       - Run benchmarks                            ║
║    (reload)      - Reload changed namespaces                 ║
║                                                              ║
║  Sample data available:                                      ║
║    sample-uuid, sample-typeid, sample-prefixes               ║
║    (generate-samples) - Generate IDs with all prefixes       ║
║                                                              ║
╚══════════════════════════════════════════════════════════════╝
")
