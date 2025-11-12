# Research: TypeID Implementation Decisions

**Feature**: TypeID Clojure/ClojureScript Library
**Date**: 2025-11-10
**Purpose**: Document technology choices, implementation strategies, and rationale for key decisions

## Overview

This document captures research findings and decisions for implementing the TypeID specification in Clojure/ClojureScript. The primary challenges are: (1) UUIDv7 generation with proper timestamp and randomness, (2) custom base32 encoding matching the reference implementation, (3) cross-platform compatibility between JVM and JavaScript, and (4) achieving sub-microsecond performance targets.

## Research Areas

### 1. UUIDv7 Generation Strategy

**Decision**: Implement custom UUIDv7 generator using platform-specific time/random sources via reader conditionals

**Rationale**:
- Java's `java.util.UUID` only supports v3, v4, v5 (not v7)
- JavaScript's `crypto.randomUUID()` generates v4 UUIDs only
- UUIDv7 requires:
  - 48 bits: Unix timestamp in milliseconds
  - 12 bits: Sub-millisecond precision + sequence counter for monotonicity
  - 2 bits: Version (0111)
  - 62 bits: Random data
  - 2 bits: Variant (10)

**Implementation Approach**:
```clojure
;; src/typeid/impl/uuid.cljc
(ns typeid.impl.uuid
  #?(:clj (:import [java.time Instant]
                   [java.security SecureRandom])
     :cljs (:require [goog.crypt :as crypt])))

(defn- current-timestamp-ms []
  #?(:clj (System/currentTimeMillis)
     :cljs (.now js/Date)))

(defn- random-bytes [n]
  #?(:clj (let [rng (SecureRandom.)
                bytes (byte-array n)]
            (.nextBytes rng bytes)
            bytes)
     :cljs (let [arr (js/Uint8Array. n)]
             (.getRandomValues js/crypto arr)
             arr)))

(defn generate-uuidv7 []
  ;; 1. Get timestamp (48 bits)
  ;; 2. Generate random bits for sub-ms + random section
  ;; 3. Set version bits (48-51) to 0111
  ;; 4. Set variant bits (64-65) to 10
  ;; 5. Return 16 bytes
  ...)
```

**Alternatives Considered**:
- **Use existing UUID libraries**: No Clojure library supports UUIDv7 natively; adding dependency violates "no runtime deps" constraint
- **Timestamp-only UUIDs**: Insufficient randomness for high-throughput scenarios
- **Use UUIDv4 and modify bits**: Loses timestamp ordering property which is core to TypeID value

**Monotonicity Strategy**:
- Maintain atom with last timestamp + sequence counter (dev/REPL use acceptable)
- For production: rely on millisecond timestamp + random bits
- Document that sub-millisecond uniqueness depends on random quality

### 2. Base32 Encoding (Crockford Alphabet)

**Decision**: Implement custom base32 encoder/decoder using lookup tables and bit manipulation

**Rationale**:
- Standard base32 (RFC 4648) uses different alphabet (`A-Z2-7`)
- Crockford base32 uses `0-9a-z` excluding `i,l,o,u` (32 chars)
- TypeID encoding:
  - Prepend 2 zero bits to 128-bit UUID → 130 bits
  - Split into 26 groups of 5 bits each
  - Map each 5-bit group to base32 alphabet

**Implementation Approach**:
```clojure
;; src/typeid/impl/base32.cljc
(ns typeid.impl.base32)

(def ^:private encode-alphabet
  "0123456789abcdefghjkmnpqrstvwxyz")

(def ^:private decode-map
  ;; Build reverse lookup map: char -> 5-bit value
  (into {} (map-indexed (fn [idx ch] [ch idx]) encode-alphabet)))

(defn encode
  "Encode 16 bytes (128 bits) as 26-character base32 string.
   Prepends 2 zero bits, splits into 26 5-bit groups."
  [uuid-bytes]
  ;; 1. Treat bytes as big-endian 128-bit integer
  ;; 2. Prepend 2 zero bits -> 130 bits
  ;; 3. Extract 5 bits at a time (26 iterations)
  ;; 4. Map to alphabet
  ;; 5. Return string
  ...)

(defn decode
  "Decode 26-character base32 string to 16 bytes (128 bits).
   Validates first char <= '7' (overflow check)."
  [base32-str]
  ;; 1. Map each char to 5-bit value via decode-map
  ;; 2. Validate first char <= 7 (ensures 130 bits with 2 leading zeros)
  ;; 3. Concatenate 5-bit groups -> 130 bits
  ;; 4. Drop 2 leading zero bits -> 128 bits
  ;; 5. Return 16 bytes
  ...)
```

**Performance Considerations**:
- Use bit-shifting instead of string operations
- Preallocate result buffers (use transients)
- Inline alphabet lookups (no function calls in hot loop)
- Target: < 500ns for encode, < 500ns for decode

**Alternatives Considered**:
- **Use Apache Commons Codec**: Wrong alphabet, not available in ClojureScript
- **Convert via BigInteger**: Works but slower (heap allocation, arbitrary precision overhead)
- **Hand-rolled string manipulation**: Less efficient than bit ops

### 2a. Reflection Elimination Strategy

**Decision**: Enable reflection warnings globally and add type hints to hot paths

**Rationale**:
- Reflection calls add significant overhead (10-100x slower)
- Performance targets (< 1μs) require avoiding reflection in critical paths
- Type hints are free at runtime but enable direct method invocation
- ClojureScript benefits from proper externs/type hints for advanced compilation

**Implementation Requirements**:

**1. Enable Reflection Warnings in All Source Namespaces**:
```clojure
(ns typeid.impl.base32
  (:require ...)
  (:import ...))

;; Enable reflection warnings at namespace level
(set! *warn-on-reflection* true)
```

**2. Type Hints for Hot Paths**:

**Base32 Encoding (Bit Manipulation)**:
```clojure
(defn encode-5bit-group
  "Encode 5-bit value to base32 character."
  ^char [^long value]
  (aget ^chars encode-alphabet value))

(defn decode-base32-char
  "Decode base32 character to 5-bit value."
  ^long [^char c]
  (or (get decode-map c)
      (throw (ex-info "Invalid base32 character" {:char c}))))

(defn extract-bits
  "Extract 5 bits from byte array at bit offset."
  ^long [^bytes uuid-bytes ^long bit-offset]
  (let [byte-idx (quot bit-offset 8)
        bit-in-byte (rem bit-offset 8)
        byte-val (bit-and (aget uuid-bytes byte-idx) 0xFF)]
    (bit-and (bit-shift-right byte-val bit-in-byte) 0x1F)))
```

**UUID Byte Access**:
```clojure
(defn get-uuid-byte
  "Get byte from UUID at index, treating as unsigned."
  ^long [^bytes uuid-bytes ^long index]
  (bit-and (aget uuid-bytes index) 0xFF))

(defn set-version-bits
  "Set UUIDv7 version bits (48-51) to 0111."
  [^bytes uuid-bytes]
  (aset uuid-bytes 6
        (unchecked-byte
          (bit-or (bit-and (aget uuid-bytes 6) 0x0F)
                  0x70)))
  uuid-bytes)
```

**String Operations**:
```clojure
(defn build-typeid
  "Combine prefix and suffix into TypeID string."
  ^String [^String prefix ^String suffix]
  (if (empty? prefix)
    suffix
    (str prefix "_" suffix)))

(defn split-typeid
  "Split TypeID into prefix and suffix."
  [[^String prefix ^String suffix] ^String typeid-str]
  (let [last-underscore (.lastIndexOf typeid-str (int \_))]
    (if (= -1 last-underscore)
      ["" typeid-str]
      [(.substring typeid-str 0 last-underscore)
       (.substring typeid-str (inc last-underscore))])))
```

**3. ClojureScript Externs/Type Hints**:
```clojure
;; Use ^js type hints for JS interop
(defn current-timestamp-ms []
  #?(:clj (System/currentTimeMillis)
     :cljs (.now js/Date)))

(defn random-bytes [n]
  #?(:clj (let [rng (java.security.SecureRandom.)
                ^bytes bytes (byte-array n)]
            (.nextBytes rng bytes)
            bytes)
     :cljs (let [^js arr (js/Uint8Array. n)]
             (.getRandomValues (.-crypto js/window) arr)
             arr)))
```

**4. CI Reflection Check**:

Add to .github/workflows/ci.yml:
```yaml
jobs:
  lint:
    # ... existing steps ...
    - name: Check for reflection warnings
      run: |
        OUTPUT=$(clojure -M -e "(require 'typeid.core)" 2>&1)
        if echo "$OUTPUT" | grep -q "Reflection warning"; then
          echo "❌ Reflection warnings found:"
          echo "$OUTPUT" | grep "Reflection warning"
          exit 1
        fi
        echo "✅ No reflection warnings"
```

**5. Performance Validation**:

Benchmarks must verify reflection elimination:
```clojure
;; dev/benchmarks/core_bench.clj
(require '[criterium.core :as crit])

(defn bench-encode []
  (let [uuid-bytes (byte-array (range 16))]
    (crit/quick-bench
      (typeid/encode uuid-bytes "user"))))

;; Target: < 1μs (1000ns)
;; If > 1μs, check for reflection with (set! *warn-on-reflection* true)
```

**Alternatives Considered**:
- **No type hints**: Simpler but misses performance targets
- **Selective hints**: Harder to maintain, easy to miss hot paths
- **Global hints**: Too aggressive, hinders development

**Trade-offs**:
- **Pro**: Meets sub-microsecond performance targets
- **Pro**: Catches reflection issues at compile time
- **Con**: More verbose code (type hints on every function)
- **Con**: Requires discipline to maintain

### 3. Cross-Platform Strategy (CLJC + Reader Conditionals)

**Decision**: Use `.cljc` files with reader conditionals for platform-specific code

**Rationale**:
- Most code (base32, prefix validation, parsing) is platform-agnostic
- Only UUIDv7 generation needs platform-specific APIs (time, random)
- CLJC allows single codebase with conditional compilation

**Platform-Specific Code**:

| Functionality | JVM | JavaScript | Shared |
|---------------|-----|------------|--------|
| Time source | `System.currentTimeMillis()` | `js/Date.now()` | No |
| Random source | `SecureRandom` | `crypto.getRandomValues()` | No |
| Base32 encoding | Pure Clojure | Pure ClojureScript | Yes (CLJC) |
| Prefix validation | Pure Clojure | Pure ClojureScript | Yes (CLJC) |
| String manipulation | Pure Clojure | Pure ClojureScript | Yes (CLJC) |

**Testing Strategy**:
- Unit tests in `.cljc` run on both platforms
- Platform-specific tests for UUIDv7 generation
- Compliance tests (valid.yml, invalid.yml) run on both platforms

**Alternatives Considered**:
- **Separate .clj and .cljs files**: Duplicates logic, harder to maintain
- **JVM-only library**: Doesn't meet requirement FR-019 (ClojureScript support)
- **Macros for platform dispatch**: More complex than reader conditionals

### 4. Validation Strategy (Manual Predicates)

**Decision**: Use hand-written predicate functions for runtime validation (zero external dependencies)

**Rationale**:
- **Zero runtime dependencies**: Constitution requires no external dependencies at runtime
- Predicates are simple, explicit, and fast
- Full control over error messages and validation logic
- No additional library to learn or maintain compatibility with
- Validation code is easily auditable and debuggable

**Predicate Definitions**:
```clojure
;; src/typeid/impl/validation.cljc
(ns typeid.impl.validation)

(def ^:private prefix-pattern #"^([a-z]([a-z_]{0,61}[a-z])?)?$")
(def ^:private base32-chars (set "0123456789abcdefghjkmnpqrstvwxyz"))

(defn valid-prefix?
  "Check if prefix matches pattern [a-z]([a-z_]{0,61}[a-z])? or is empty."
  [s]
  (and (string? s)
       (<= (count s) 63)
       (re-matches prefix-pattern s)))

(defn valid-base32-suffix?
  "Check if suffix is 26 chars, all base32, first char <= 7."
  [s]
  (and (string? s)
       (= 26 (count s))
       (<= (int (first s)) (int \7))
       (every? base32-chars s)))

(defn valid-typeid-string?
  "Check if string is a valid TypeID (length, format, components)."
  [s]
  (and (string? s)
       (<= 26 (count s) 90)
       (= s (clojure.string/lower-case s))))

(defn valid-uuid-bytes?
  "Check if bytes represent a valid UUID (exactly 16 bytes)."
  [b]
  (and (bytes? b)
       (= 16 (count b))))

(defn validate-prefix
  "Validate prefix, return {:ok prefix} or {:error error-map}."
  [prefix]
  (cond
    (not (string? prefix))
    {:error {:type :invalid-prefix-type
             :message "Prefix must be a string"
             :data {:prefix prefix :type (type prefix)}}}

    (> (count prefix) 63)
    {:error {:type :prefix-too-long
             :message "Prefix must be at most 63 characters"
             :data {:prefix prefix :length (count prefix)}}}

    (not (re-matches prefix-pattern prefix))
    {:error {:type :invalid-prefix-format
             :message "Prefix must match pattern [a-z]([a-z_]{0,61}[a-z])? or be empty"
             :data {:prefix prefix :pattern (str prefix-pattern)}}}

    :else
    {:ok prefix}))
```

**Error Handling**:
- Public functions return `{:ok result}` or `{:error {:type :message :data}}`
- Errors include context: what validation failed, what value was provided
- Validation predicates provide detailed error types and messages

**Advantages Over Malli**:
- **Zero dependencies**: No external library at runtime
- **Explicit**: Validation logic is visible and auditable
- **Fast**: No schema interpretation overhead
- **Simple**: Easy to understand, debug, and modify
- **Portable**: Works identically on JVM and JS without library compatibility concerns

**Dev/Test Usage**:
- Malli or clojure.spec MAY be used in dev/test for generative testing
- Test generators can use Malli schemas to generate test data
- Schemas stay in test namespace, never shipped to users

### 5. Testing Strategy (Kaocha + test.check + Reference Tests)

**Decision**: Three-tier testing strategy

**Tier 1: Unit Tests (Kaocha)**
- Test each function in isolation
- Cover happy paths and edge cases
- Fast, deterministic, run on every commit

**Tier 2: Property-Based Tests (test.check)**
- Generative tests for round-trip properties:
  - `(parse (generate prefix)) => prefix + valid UUID`
  - `(encode (decode base32)) => base32`
  - `(validate (generate prefix)) => true`
- Generate random valid prefixes, random UUIDs
- Find edge cases automatically

**Tier 3: Compliance Tests (valid.yml + invalid.yml)**
- Load reference test files from spec repo
- Iterate each test case:
  - Valid: encode UUID => expected TypeID string
  - Valid: decode TypeID => expected UUID
  - Invalid: reject with appropriate error
- 100% pass rate required (SC-002, SC-003)

**Tier 4: Error Semantics Tests**
- Verify error data structure consistency across all validation functions
- Test that each error type is returned in the correct scenarios
- Validate error messages are actionable and include context
- Test error data includes all relevant debugging information

**Error Semantics Test Suite**:
```clojure
(ns typeid.error-semantics-test
  (:require [clojure.test :refer [deftest is testing]]
            [typeid.core :as t]
            [typeid.validation :as v]))

(deftest error-structure-test
  (testing "All validation errors return consistent structure"
    (let [errors [(v/validate-prefix "User123")      ; uppercase
                  (v/validate-prefix "_invalid")     ; starts with _
                  (t/parse "user_8zzzzzzzzzzzzzzzzzzzzzzzzz")  ; overflow
                  (t/parse "User_01h5fskfsk4fpeqwnsyz5hj55t") ; uppercase
                  (t/parse "user_tooshort")]]        ; short suffix

      (doseq [result errors]
        (is (contains? result :error))
        (is (map? (:error result)))
        (is (contains? (:error result) :type))
        (is (keyword? (:type (:error result))))
        (is (contains? (:error result) :message))
        (is (string? (:message (:error result))))
        (is (contains? (:error result) :data))
        (is (map? (:data (:error result))))))))

(deftest error-types-test
  (testing "Specific error types are returned for each validation failure"
    (is (= :invalid-prefix-format
           (:type (:error (v/validate-prefix "User123")))))

    (is (= :invalid-prefix-format
           (:type (:error (v/validate-prefix "_invalid")))))

    (is (= :prefix-too-long
           (:type (:error (v/validate-prefix (apply str (repeat 64 "a")))))))

    (is (= :suffix-overflow
           (:type (:error (t/parse "user_8zzzzzzzzzzzzzzzzzzzzzzzzz")))))

    (is (= :invalid-case
           (:type (:error (t/parse "User_01h5fskfsk4fpeqwnsyz5hj55t")))))

    (is (= :invalid-suffix-length
           (:type (:error (t/parse "user_tooshort")))))))

(deftest error-data-completeness-test
  (testing "Error data includes relevant debugging information"
    (let [err-prefix (v/validate-prefix "User123")
          err-overflow (t/parse "user_8zzzzzzzzzzzzzzzzzzzzzzzzz")
          err-length (t/parse "user_tooshort")]

      ;; Prefix error includes prefix and pattern
      (is (contains? (:data (:error err-prefix)) :prefix))
      (is (contains? (:data (:error err-prefix)) :pattern))

      ;; Overflow error includes suffix and first-char
      (is (contains? (:data (:error err-overflow)) :suffix))
      (is (contains? (:data (:error err-overflow)) :first-char))

      ;; Length error includes length and expected
      (is (contains? (:data (:error err-length)) :suffix)))))

(deftest actionable-messages-test
  (testing "Error messages are actionable and explain the problem"
    (let [err-prefix (:message (:error (v/validate-prefix "User123")))
          err-overflow (:message (:error (t/parse "user_8zzzzzzzzzzzzzzzzzzzzzzzzz")))]

      ;; Messages should mention what's wrong
      (is (re-find #"(?i)pattern|format|lowercase" err-prefix))
      (is (re-find #"(?i)overflow|0-7|first character" err-overflow)))))
```

**test.check Generators**:
```clojure
(ns typeid.properties-test
  (:require [clojure.test.check :as tc]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.properties :as prop]))

(def gen-valid-prefix
  "Generate valid TypeID prefixes."
  (gen/one-of
    [(gen/return "")  ; Empty prefix
     (gen/fmap (fn [chars]
                 (str (first chars)
                      (apply str (rest chars))
                      (last chars)))
               (gen/vector (gen/elements "abcdefghijklmnopqrstuvwxyz_") 1 63))]))

(def gen-uuid-bytes
  "Generate random 16-byte UUID."
  (gen/fmap byte-array (gen/vector gen/byte 16)))
```

**Test Coverage Goals**:

Coverage measured with cloverage, enforced in CI:
- **Overall coverage**: ≥80% (CI fails below this threshold)
- **Critical paths**: 100% coverage required for:
  - `typeid.impl.base32/encode` and `/decode`
  - `typeid.impl.uuid/generate-uuidv7`
  - `typeid.validation/validate-prefix`
  - All public functions in `typeid.core`
- **Error paths**: All error conditions must have test coverage
- **Platform-specific**: Reader conditionals tested on both JVM and JS

**Coverage Reporting**:
- Local: `clojure -M:coverage` generates HTML report in `target/coverage/`
- CI: Uploads to Codecov on every push (Java 17 + Clojure 1.11.1 matrix only)
- Badge: Shows coverage % on README
- Threshold: Build fails if coverage drops below 80%

**Alternatives Considered**:
- **Only unit tests**: Miss edge cases, no round-trip verification
- **Only property tests**: Don't verify spec compliance (valid.yml cases)
- **Manual test cases**: Time-consuming, incomplete coverage
- **No coverage tooling**: Can't track coverage regression over time

### 6. Build & Release Automation

**Decision**: deps.edn + tools.build + GitHub Actions

**Build Configuration**:
```clojure
;; deps.edn
{:deps {org.clojure/clojure {:mvn/version "1.11.1"}}
 ;; ZERO runtime dependencies beyond Clojure itself

 :aliases
 {:dev {:extra-deps {criterium/criterium {:mvn/version "0.4.6"}
                     dev.weavejester/cljfmt {:mvn/version "0.12.0"}}
        :extra-paths ["dev"]}

  :test {:extra-deps {lambdaisland/kaocha {:mvn/version "1.91.1392"}
                      org.clojure/test.check {:mvn/version "1.1.1"}
                      ;; Malli ONLY for test data generation (NOT runtime)
                      metosin/malli {:mvn/version "0.16.4"}}
         :extra-paths ["test"]}

  :coverage {:extra-deps {cloverage/cloverage {:mvn/version "1.2.4"}}
             :main-opts ["-m" "cloverage.coverage"
                         "--src-ns-path" "src"
                         "--test-ns-path" "test"
                         "--codecov"
                         "--fail-threshold" "80"]}

  :build {:deps {io.github.clojure/tools.build {:mvn/version "0.10.5"}
                 slipset/deps-deploy {:mvn/version "0.2.2"}}
          :ns-default build}

  :cljs {:extra-deps {org.clojure/clojurescript {:mvn/version "1.11.132"}
                      lambdaisland/kaocha-cljs {:mvn/version "1.5.154"}}}

  :codox {:extra-deps {codox/codox {:mvn/version "0.10.8"}}
          :exec-fn codox.main/generate-docs}}}

;; build.clj
(ns build
  (:require [clojure.tools.build.api :as b]))

(def lib 'com.example/typeid)
(def version (or (System/getenv "RELEASE_VERSION") "0.1.0-SNAPSHOT"))
(def class-dir "target/classes")
(def basis (delay (b/create-basis {:project "deps.edn"})))
(def jar-file (format "target/%s-%s.jar" (name lib) version))

(defn clean
  "Remove target directory."
  [_]
  (b/delete {:path "target"}))

(defn pom
  "Generate pom.xml for Maven/Clojars deployment."
  [_]
  (b/write-pom {:class-dir class-dir
                :lib lib
                :version version
                :basis @basis
                :src-dirs ["src"]
                :scm {:url "https://github.com/example/typeid"
                      :connection "scm:git:git://github.com/example/typeid.git"
                      :developerConnection "scm:git:ssh://git@github.com/example/typeid.git"
                      :tag (str "v" version)}
                :pom-data [[:description "Type-safe, K-sortable unique identifiers for Clojure"]
                           [:url "https://github.com/example/typeid"]
                           [:licenses
                            [:license
                             [:name "MIT License"]
                             [:url "https://opensource.org/licenses/MIT"]]]
                           [:developers
                            [:developer
                             [:name "TypeID Contributors"]]]]}))

(defn jar
  "Build library jar file."
  [_]
  (clean nil)
  (pom nil)
  (b/copy-dir {:src-dirs ["src"]
               :target-dir class-dir})
  (b/jar {:class-dir class-dir
          :jar-file jar-file}))

(defn install
  "Install jar to local Maven repository (~/.m2)."
  [_]
  (jar nil)
  (b/install {:basis @basis
              :lib lib
              :version version
              :jar-file jar-file
              :class-dir class-dir}))

(defn deploy
  "Deploy jar to Clojars.
   Requires CLOJARS_USERNAME and CLOJARS_PASSWORD environment variables."
  [{:keys [sign] :or {sign true}}]
  (jar nil)
  (let [pom-file (b/pom-path {:lib lib :class-dir class-dir})]
    ((requiring-resolve 'deps-deploy.deps-deploy/deploy)
     {:installer :remote
      :artifact jar-file
      :pom-file pom-file
      :sign-releases? sign})))
```

**CI Workflow (GitHub Actions)**:
```yaml
# .github/workflows/ci.yml
name: CI
on: [push, pull_request]
jobs:
  test-clj:
    name: Test Clojure (JVM)
    runs-on: ubuntu-latest
    strategy:
      matrix:
        java: [17, 21]
        clojure: [1.11.1, 1.12.0]
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with: {java-version: ${{ matrix.java }}}
      - name: Run tests
        run: clojure -M:test -m kaocha.runner
      - name: Run linter
        run: clojure -M:dev -m clj-kondo.main --lint src test
      - name: Build jar
        run: clojure -T:build jar
      - name: Run coverage (Java 17, Clojure 1.11.1 only)
        if: matrix.java == 17 && matrix.clojure == '1.11.1'
        run: clojure -M:coverage
      - name: Upload coverage to Codecov
        if: matrix.java == 17 && matrix.clojure == '1.11.1'
        uses: codecov/codecov-action@v4
        with:
          files: ./target/coverage/codecov.json
          fail_ci_if_error: false

  test-cljs:
    name: Test ClojureScript (Node.js)
    runs-on: ubuntu-latest
    strategy:
      matrix:
        node: [18, 20]
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-node@v4
        with: {node-version: ${{ matrix.node }}}
      - uses: actions/setup-java@v4
        with: {java-version: 17}
      - name: Install ClojureScript dependencies
        run: npm install
      - name: Run ClojureScript tests
        run: clojure -M:test:cljs -m kaocha.runner --config-file tests.cljs.edn
      - name: Lint ClojureScript
        run: clojure -M:dev -m clj-kondo.main --lint src test
```

**Release Workflow**:
```yaml
# .github/workflows/release.yml
name: Release
on:
  push:
    tags: ['v*']
jobs:
  deploy:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with: {java-version: 17}
      - run: clojure -T:build jar
      - run: clojure -T:build deploy
        env:
          CLOJARS_USERNAME: ${{ secrets.CLOJARS_USERNAME }}
          CLOJARS_PASSWORD: ${{ secrets.CLOJARS_PASSWORD }}
```

**ClojureScript Test Configuration**:

Create `tests.cljs.edn` for Kaocha ClojureScript testing:
```clojure
#kaocha/v1
{:tests
 [{:id :unit-cljs
   :type :kaocha.type/cljs
   :test-paths ["test"]
   :cljs/timeout 10000
   :cljs/repl-env cljs.repl.node/repl-env}]

 :plugins [:kaocha.plugin/profiling]
 :color? true
 :fail-fast? false}
```

Create `package.json` for Node.js dependencies:
```json
{
  "name": "typeid-cljs-tests",
  "version": "1.0.0",
  "private": true,
  "dependencies": {},
  "devDependencies": {
    "ws": "^8.16.0"
  }
}
```

**Alternatives Considered**:
- **Leiningen**: Older, less aligned with Clojure CLI ecosystem
- **Manual builds**: Error-prone, not reproducible
- **Travis CI / CircleCI**: GitHub Actions integrates better with GitHub repos

### 7. Documentation Strategy (README + Codox + Quickstart)

**Decision**: Multi-layered documentation approach

**README.md** (High-Level):

Complete outline for repository root README.md:

```markdown
# TypeID for Clojure

> Type-safe, K-sortable, globally unique identifiers inspired by Stripe IDs

[![Clojars Project](https://img.shields.io/clojars/v/com.example/typeid.svg)](https://clojars.org/com.example/typeid)
[![CI](https://github.com/example/typeid/workflows/CI/badge.svg)](https://github.com/example/typeid/actions)
[![codecov](https://codecov.io/gh/example/typeid/branch/main/graph/badge.svg)](https://codecov.io/gh/example/typeid)
[![License](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)

## What is TypeID?

TypeID is a modern, type-safe extension of UUIDv7. It adds an optional type prefix to UUIDs, making them more readable and self-documenting.

**Example**: `user_01h5fskfsk4fpeqwnsyz5hj55t`

- **Type prefix**: `user` (optional, 0-63 lowercase characters)
- **Separator**: `_` (omitted if no prefix)
- **Suffix**: `01h5fskfsk4fpeqwnsyz5hj55t` (base32-encoded UUIDv7)

**Key Features**:
- ✅ Type-safe: Prefixes prevent mixing IDs from different domains
- ✅ K-sortable: Chronologically ordered by creation time (UUIDv7)
- ✅ Compact: 26-character suffix (vs 36-character UUID string)
- ✅ URL-safe: No special characters, case-insensitive alphabet
- ✅ Cross-platform: Works on JVM and JavaScript (ClojureScript)
- ✅ Zero dependencies: Pure Clojure/ClojureScript implementation

## Installation

### Clojure CLI (deps.edn)
```clojure
{:deps {com.example/typeid {:mvn/version "0.1.0"}}}
```

### Leiningen (project.clj)
```clojure
[com.example/typeid "0.1.0"]
```

## Quick Start

```clojure
(require '[typeid.core :as t])

;; Generate a new TypeID
(t/generate "user")
;;=> "user_01h5fskfsk4fpeqwnsyz5hj55t"

;; Parse a TypeID
(t/parse "user_01h5fskfsk4fpeqwnsyz5hj55t")
;;=> {:ok {:prefix "user"
;;         :suffix "01h5fskfsk4fpeqwnsyz5hj55t"
;;         :uuid #bytes[...]
;;         :typeid "user_01h5fskfsk4fpeqwnsyz5hj55t"}}

;; Validate a TypeID
(t/validate "user_01h5fskfsk4fpeqwnsyz5hj55t")
;;=> {:ok true}

;; Extract UUID from TypeID
(t/to-uuid "user_01h5fskfsk4fpeqwnsyz5hj55t")
;;=> {:ok #bytes[16 bytes]}

;; Create TypeID from existing UUID
(t/from-uuid my-uuid-bytes "user")
;;=> {:ok "user_01h5fskfsk4fpeqwnsyz5hj55t"}
```

## Documentation

- **[API Reference (Codox)](https://example.github.io/typeid/)** - Full API documentation
- **[Quickstart Guide](specs/001-typeid-implementation/quickstart.md)** - Step-by-step tutorial
- **[TypeID Specification](https://github.com/jetify-com/typeid)** - Official spec

## Usage Examples

### Basic Operations

See Quick Start above for common patterns.

### Error Handling

All functions return `{:ok result}` or `{:error error-map}`:

```clojure
(t/parse "Invalid_8zzzzzzzzzzzzzzzzzzzzzzzzz")
;;=> {:error {:type :suffix-overflow
;;            :message "First character of suffix must be 0-7 to prevent overflow"
;;            :data {:suffix "8zzzzzzzzzzzzzzzzzzzzzzzzz"
;;                   :first-char \\8
;;                   :max-allowed-char \\7}}}
```

### Validation Predicates

```clojure
(require '[typeid.validation :as v])

(v/valid-prefix? "user")        ;;=> true
(v/valid-prefix? "User")        ;;=> false (must be lowercase)

(v/valid-typeid-string? "user_01h5fskfsk4fpeqwnsyz5hj55t") ;;=> true
(v/valid-uuid-bytes? my-bytes)  ;;=> true if 16 bytes
```

## Performance

**Benchmarks** (measured with Criterium on JDK 17):
- `generate`: < 1μs per operation
- `parse`: < 1μs per operation
- `encode/decode`: < 500ns per operation
- `validate-prefix`: < 200ns per operation

Zero reflection warnings. All hot paths use type hints.

## Comparison with Alternatives

| Feature | TypeID | UUID | ULID | Nano ID |
|---------|--------|------|------|---------|
| Type prefix | ✅ | ❌ | ❌ | ❌ |
| K-sortable | ✅ | ❌ (v4), ⚠️ (v7) | ✅ | ❌ |
| Compact | ✅ (26 chars) | ❌ (36 chars) | ✅ (26 chars) | ✅ (21 chars) |
| URL-safe | ✅ | ⚠️ (with hyphens) | ✅ | ✅ |
| Spec compliance | ✅ v0.3.0 | ✅ RFC 4122 | ✅ | ❌ |
| Cross-platform | ✅ | ✅ | ✅ | ✅ |

## Troubleshooting

### Common Errors

**"Prefix must be lowercase"**
- TypeID prefixes are case-sensitive and must be all lowercase
- Use `clojure.string/lower-case` to normalize input if needed

**"First character of suffix must be 0-7"**
- TypeID suffixes cannot start with `8-z` to prevent 128-bit overflow
- This typically indicates corrupted data or manual TypeID construction

**"Suffix must be exactly 26 characters"**
- TypeID suffixes are always 26 base32 characters (representing 128 bits)
- Ensure you're not truncating the suffix

### Platform-Specific Issues

**ClojureScript**: UUID generation uses `js/Date.now()` and `crypto.getRandomValues()`, which require:
- Node.js 15+ or browser with Web Crypto API
- For older Node.js, add `ws` dependency (see package.json)

**JVM**: Uses `System.currentTimeMillis()` and `java.security.SecureRandom`
- No additional dependencies required
- Works on JDK 11+, tested on JDK 17 and 21

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md) for development setup and guidelines.

## License

Copyright © 2025 TypeID Contributors

Distributed under the MIT License. See [LICENSE](LICENSE) for details.

## References

- [TypeID Specification](https://github.com/jetify-com/typeid) - Official specification v0.3.0
- [UUIDv7 (RFC 9562)](https://datatracker.ietf.org/doc/html/rfc9562) - UUID version 7
- [Crockford Base32](https://www.crockford.com/base32.html) - Base32 encoding scheme
```

**Codox (API Reference)**:
- Auto-generated from docstrings
- Function signatures with examples
- Malli schemas embedded in docs
- Published to GitHub Pages via CI

**quickstart.md** (Step-by-Step)**:
- Install library
- Add to `deps.edn`
- REPL walkthrough for each user story
- Integration examples (e.g., use with datomic, PostgreSQL)

**Docstring Format**:
```clojure
(defn generate
  "Generate a new TypeID with the given prefix.

   The prefix must be 0-63 lowercase characters matching [a-z]([a-z_]{0,61}[a-z])?,
   or an empty string for prefix-less TypeIDs.

   Returns a TypeID string like \"user_01h5fskfsk4fpeqwnsyz5hj55t\".

   Example:
     (generate \"user\")
     ;;=> \"user_01h5fskfsk4fpeqwnsyz5hj55t\"

     (generate \"\")
     ;;=> \"01h5fskfsk4fpeqwnsyz5hj55t\"

   See also: `parse`, `validate`"
  [prefix]
  ...)
```

**Alternatives Considered**:
- **Docstrings only**: No high-level overview, hard to discover examples
- **Wiki pages**: Separate from code, gets out of sync
- **No docs**: Violates constitution Principle II (exhaustive examples required)

## Key Decisions Summary

| Area | Decision | Rationale |
|------|----------|-----------|
| UUIDv7 Generation | Custom implementation with reader conditionals | No existing library supports v7; cross-platform required |
| Base32 Encoding | Lookup tables + bit manipulation | Matches reference implementation; performance target < 1μs |
| Cross-Platform | CLJC with reader conditionals | Maximizes code reuse; single source of truth |
| Validation | Malli schemas at public API | Clear errors, executable documentation, lightweight |
| Testing | Kaocha + test.check + compliance tests | Unit + property + spec compliance = comprehensive coverage |
| Build/Release | deps.edn + tools.build + GitHub Actions | Standard Clojure tooling; automated, reproducible |
| Documentation | README + Codox + quickstart | Multi-level: overview, API reference, tutorial |

## Open Questions & Future Research

1. **ClojureScript Optimization**: Investigate if typed arrays (`js/Uint8Array`) provide measurable performance improvement over Clojure vectors for byte manipulation
2. **Babashka Support**: Determine if Babashka's limited Java interop affects UUIDv7 generation (may need separate implementation path)
3. **Monotonic Clock**: Research if Java's `System.nanoTime()` or JavaScript's `performance.now()` can improve sub-millisecond uniqueness guarantees
4. **Advanced Validation**: Consider optional strict mode that warns about discouraged patterns (e.g., single-char prefixes, very short prefixes)

## References

- [TypeID Specification v0.3.0](../../../typeid.md)
- [UUIDv7 Draft Spec (RFC 9562)](https://datatracker.ietf.org/doc/html/rfc9562)
- [Crockford Base32](https://www.crockford.com/base32.html)
- [Reference Implementation (Go)](https://github.com/jetify-com/typeid-go)
- [Malli Documentation](https://github.com/metosin/malli)
- [Kaocha Documentation](https://cljdoc.org/d/lambdaisland/kaocha/)
- [tools.build Guide](https://clojure.org/guides/tools_build)
