# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added

- **New arity**: `typeid.uuid/generate-uuidv7` now accepts an optional timestamp parameter
  - Zero-arity: Generate with current timestamp (unchanged behavior)
  - One-arity: Generate with explicit timestamp in milliseconds
  - Enables historical data migrations, deterministic testing, and audit logs with business timestamps
  - Validates timestamp is non-negative and within 48-bit range (0 to 281474976710655)
  - Throws `ExceptionInfo` with `:typeid/invalid-timestamp` type for invalid timestamps

  ```clojure
  ;; Generate with current time (existing behavior)
  (uuid/generate-uuidv7)

  ;; Generate with explicit timestamp
  (uuid/generate-uuidv7 1699564800000)  ; 2023-11-10T00:00:00Z

  ;; Complete workflow for historical data
  (let [uuid-bytes (uuid/generate-uuidv7 historical-timestamp)
        uuid-obj (uuid/bytes->uuid uuid-bytes)]
    (typeid/create "order" uuid-obj))
  ```

## [0.3.0] - 2025-11-13

### ⚠️ BREAKING CHANGES

- **UUID utilities moved from `typeid.impl.uuid` to `typeid.uuid` (public API)**

  The UUID utility namespace has been promoted from internal (`impl`) to public API.

  ```clojure
  ;; v0.2.x and earlier (old)
  (require '[typeid.impl.uuid :as uuid])

  ;; v0.3.0+ (new)
  (require '[typeid.uuid :as uuid])
  ```

  **Migration:**
  - Replace all `typeid.impl.uuid` requires with `typeid.uuid`
  - Function signatures are unchanged (`uuid->bytes`, `bytes->uuid`, `generate-uuidv7`)
  - See README "UUID Utility Functions" section for usage examples

### Added

- **Public API**: `typeid.uuid` namespace now part of official public API
  - `uuid->bytes` - Convert UUID objects to 16-byte arrays (JVM and ClojureScript)
  - `bytes->uuid` - Convert 16-byte arrays to UUID objects (with round-trip guarantee)
  - `generate-uuidv7` - Generate UUIDv7 bytes with timestamp-based ordering
- **Documentation**: Added UUID utilities section to README with comprehensive examples
- **Documentation**: Updated FAQ section to reference public `typeid.uuid` namespace

### Changed

- **Namespace**: Removed `typeid.impl.uuid` (private namespace)
- **Documentation**: All references to `typeid.impl.uuid` updated to `typeid.uuid`

## [0.2.0] - 2025-11-13

### ⚠️ BREAKING CHANGES

- **`parse` now returns platform-native UUID objects instead of byte arrays**

  The `:uuid` field in the result map is now a UUID object (`java.util.UUID` on JVM, `cljs.core/UUID` in ClojureScript) instead of a byte array.

  ```clojure
  ;; v0.1.x (old)
  (:uuid (typeid/parse "user_01h5fskfsk4fpeqwnsyz5hj55t"))
  ;;=> #object["[B" ... (byte array)]

  ;; v0.2.0 (new)
  (:uuid (typeid/parse "user_01h5fskfsk4fpeqwnsyz5hj55t"))
  ;;=> #uuid "018c3f9e-9e4e-7a8a-8b2a-7e8e9e4e7a8a"
  ```

  **Why this change?**
  - Direct database integration without conversion
  - Natural equality comparisons with `=`
  - Automatic JSON serialization
  - Standard UUID string formatting with `str`
  - Consistent API: `create` accepts UUIDs, `parse` returns UUIDs

  **Migration:**
  - Most code will work unchanged if you're passing UUIDs to database drivers
  - If you need byte arrays, use `typeid.impl.uuid/uuid->bytes`:
    ```clojure
    (require '[typeid.impl.uuid :as uuid])
    (let [{:keys [uuid]} (typeid/parse "user_...")]
      (uuid/uuid->bytes uuid))  ;; Get bytes if needed
    ```
  - See quickstart.md for detailed migration examples

### Added

- **New function**: `typeid.impl.uuid/bytes->uuid` - Convert 16-byte array to platform-native UUID
- **New function**: `typeid.impl.uuid/uuid->bytes` - Convert platform-native UUID to 16-byte array
- **Performance benchmarks**: Added benchmarks for UUID conversion operations
- **Test coverage**: Comprehensive round-trip tests for UUID equality across all UUID versions (v1, v4, v7)
- **Integration tests**: Database-ready UUID usage, JSON serialization, equality operators

### Changed

- **Documentation**: Updated all examples to show UUID objects in parse results
  - Database integration patterns now show direct UUID usage
  - Added FAQ section explaining how to get bytes if needed
  - Added migration guide in FAQ section
- **API documentation**: Updated docstrings to reflect UUID return types
- **Compliance tests**: Updated to work with UUID objects instead of byte arrays

### Performance

- **`bytes->uuid`**: < 500ns per operation (new function)
- **`uuid->bytes`**: < 500ns per operation (new function)
- **`parse`**: ~500ns additional overhead for UUID conversion (now < 3μs total)
- All operations remain well within performance budgets
- 73 tests, 836 assertions, 0 failures

## [0.1.1] - 2025-11-12

### Changed

- **Documentation**: Improved docstring formatting for better cljdoc rendering
  - Added structured sections with markdown headers
  - Added proper code fences with syntax highlighting
  - Added cross-references between functions
  - Enhanced all public namespaces: `typeid.core`, `typeid.validation`, `typeid.codec`

- **Build tooling**: Removed Codox in favor of automatic cljdoc generation
  - Removed `bb docs` tasks and `:codox` alias
  - API documentation now automatically generated on deployment to Clojars

## [0.1.0] - 2025-11-11

Initial release.

### Features

- **Core API** (`typeid.core`):
  - `create` - Generate new TypeIDs with optional prefixes, or create from existing UUIDs
  - `parse` - Parse TypeID strings into components
  - `explain` - Validate TypeID and get detailed error information

- **Codec API** (`typeid.codec`):
  - `encode` - Convert UUID bytes + prefix to TypeID string
  - `decode` - Extract UUID bytes from TypeID string
  - `uuid->hex` / `hex->uuid` - UUID format conversion helpers

- **Cross-platform support**: Identical API for Clojure JVM and ClojureScript
  - JVM: Uses `System.currentTimeMillis()` and `SecureRandom`
  - JS: Uses `js/Date.now()` and `crypto.getRandomValues()`

- **Comprehensive validation**:
  - Manual predicate functions (no external dependencies)
  - Detailed error messages with `:type`, `:message`, `:input`, `:expected`, `:actual`
  - Support for all TypeID specification v0.3.0 constraints

- **Testing infrastructure**:
  - 100% compliance with official TypeID specification tests
  - Property-based testing with test.check
  - >80% code coverage (100% for critical paths)
  - CI matrix testing on Clojure 1.11/1.12 and JDK 17/21

- **Documentation**:
  - Comprehensive README with examples and troubleshooting
  - API documentation via cljdoc
  - Quickstart guide with integration examples
  - CONTRIBUTING guide for developers

- **Build and tooling**:
  - deps.edn with :dev, :test, :build, and :coverage aliases
  - tools.build for JAR packaging and deployment
  - Babashka tasks for common workflows
  - clj-kondo configuration (zero-tolerance for warnings)
  - cljfmt configuration for consistent formatting
  - GitHub Actions CI/CD pipeline

### Performance

Benchmarks measured with Criterium on JDK 17:
- `create`: < 2μs per operation
- `parse`: < 2μs per operation
- `encode/decode`: < 1μs per operation
- `validate-prefix`: < 500ns per operation

### Technical Details

- **Zero external runtime dependencies** (only org.clojure/clojure and org.clojure/clojurescript)
- **Spec compliance**: 100% compliant with TypeID specification v0.3.0
- **Base32 encoding**: Crockford alphabet (case-insensitive, no ambiguous characters)
- **UUIDv7**: Timestamp-based (sortable) with cryptographic randomness
- **Type hints**: Zero reflection warnings, optimized hot paths
