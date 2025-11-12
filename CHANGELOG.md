# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Removed

- **REMOVED**: `validate` function - Previously deprecated in v1.0.0, now completely removed
  - The function was deprecated in favor of `explain` which provides cleaner semantics
  - Use `explain` for validation (returns `nil` for valid, error map for invalid)
  - Migration: Replace `(validate x)` with `(nil? (explain x))` or use `explain` directly

## [1.0.0] - 2025-11-12

### Breaking Changes

This is a major refactoring that simplifies the API by removing the data-as-error pattern and introducing exception-based error handling for most operations. **This release is NOT backward compatible** with v0.1.0.

#### API Changes

- **REMOVED**: `validate` function → Use `explain` instead
  - Old: `(validate typeid)` returned `{:ok true}` or `{:error ...}`
  - New: `(explain typeid)` returns `nil` (valid) or error map (invalid)

- **REMOVED**: `typeid->map` function → Use `parse` instead
  - Old: `(typeid->map typeid)` returned `{:ok components}` or `{:error ...}`
  - New: `(parse typeid)` returns components map or throws `ExceptionInfo`

- **RENAMED**: `generate` function → Use `create` instead
  - Enhanced with 2-arity support for creating TypeIDs from existing UUIDs
  - Throws exceptions on invalid input (instead of returning `{:error ...}`)

- **MOVED**: Codec functions to `typeid.codec` namespace
  - `encode`, `decode`, `uuid->hex`, `hex->uuid` now in `typeid.codec` (not `typeid.core`)
  - All codec functions now throw exceptions on invalid input instead of returning `{:error ...}`

- **NEW**: `create` function with three arities
  - Zero-arity: `(create)` - Generate TypeID with no prefix
  - One-arity: `(create prefix)` - Generate TypeID with prefix
  - Two-arity: `(create prefix uuid)` - Create TypeID from existing UUID (NEW functionality)

#### Error Handling Changes

All functions now use one of two patterns:

1. **Validation pattern** (`explain`): Returns `nil` for valid, error map for invalid
   - No exceptions thrown
   - Suitable for validation checks

2. **Exception pattern** (`parse`, `create`, codec functions): Throws `ExceptionInfo` on invalid input
   - Exception data includes `:type`, `:message`, `:input`, and context
   - More idiomatic Clojure error handling for parsing/creation

### Added

- **`explain` function**: Validation that returns `nil` for valid TypeIDs or detailed error map for invalid
  - Handles non-string inputs gracefully
  - Never throws exceptions
  - Replaces old `validate` function with cleaner semantics

- **`parse` function**: Exception-based TypeID parsing
  - Throws `ExceptionInfo` with structured error data on invalid input
  - Returns components map `{:prefix, :suffix, :uuid, :typeid}` for valid input
  - Replaces old `typeid->map` function

- **`create` function with 2-arity**: Create TypeID from existing UUID
  - Accepts platform-native UUID objects (`java.util.UUID` on JVM, `cljs.core/UUID` on ClojureScript)
  - Works with any UUID version (v1, v4, v7, etc.)
  - Enables migration scenarios and testing with specific UUIDs

- **`typeid.codec` namespace**: Separate namespace for low-level operations
  - `encode`: UUID bytes + prefix → TypeID string
  - `decode`: TypeID string → UUID bytes
  - `uuid->hex`: UUID bytes → hex string (32 chars, lowercase)
  - `hex->uuid`: Hex string → UUID bytes (accepts hyphens and uppercase)

### Changed

- **Error maps**: Consistent structure across all functions
  - `:type` - Namespaced keyword (e.g., `:typeid/invalid-prefix`)
  - `:message` - Human-readable error description
  - `:input` - Original input that caused the error
  - `:expected` - Expected format/value (optional)
  - `:actual` - Actual problematic value (optional)

- **Exception handling**: Functions use `ex-info` with structured data
  - All exceptions include the same error map structure
  - Error data can be extracted with `(ex-data e)`

- **Documentation**: Complete rewrite of README and API docs
  - New examples for `explain`, `parse`, and `create`
  - Migration guide from v0.1.0 to v1.0.0
  - Codec namespace usage examples
  - Error handling patterns

### Migration from v0.1.0

See the [Migration Guide](README.md#migration-guide) in the README for detailed migration instructions.

**Quick migration checklist**:

1. Replace `validate` with `explain` and update error handling
2. Replace `typeid->map` with `parse` and add exception handling
3. Rename `generate` to `create` (API is compatible for 0-arity and 1-arity)
4. Update codec function calls to use `typeid.codec` namespace
5. Consider using `create` (2-arity) for UUID encoding use cases
6. Update all error handling from `{:ok ...}` / `{:error ...}` pattern to exception handling

### Technical Details

- **API simplification**: Clean, focused API with 3 core functions (`typeid.core`) and 4 codec functions (`typeid.codec`)
- **Consistent error handling**: Mixed approach - validation returns data, parsing/creation throws exceptions
- **Zero runtime dependencies**: No external runtime dependencies
- **Cross-platform**: Full support for JVM and ClojureScript

## [0.1.0] - 2025-11-11

### Added
- **Core API**: Complete TypeID generation, parsing, validation, and conversion
  - `generate` - Generate new TypeIDs with optional prefixes
  - `parse` - Parse TypeID strings into components
  - `validate` - Validate TypeID format and constraints
  - `encode` - Convert UUID bytes to TypeID
  - `decode` - Extract UUID bytes from TypeID
  - `uuid->hex` / `hex->uuid` - UUID format conversion
  - `typeid->map` - Extract all TypeID components as a map

- **Cross-platform support**: Identical API for Clojure JVM and ClojureScript
  - JVM: Uses `System.currentTimeMillis()` and `SecureRandom`
  - JS: Uses `js/Date.now()` and `crypto.getRandomValues()`

- **Performance optimizations**: Sub-microsecond operations
  - Type hints throughout to eliminate reflection
  - Efficient base32 encoding/decoding with bit manipulation
  - Optimized UUIDv7 generation

- **Comprehensive validation**:
  - Manual predicate functions (no external dependencies)
  - Detailed error messages with `:type`, `:message`, and `:data`
  - Support for all TypeID specification v0.3.0 constraints

- **Testing infrastructure**:
  - 100% compliance with official TypeID specification tests
  - Property-based testing with test.check
  - Unit tests for all public API functions
  - >80% code coverage (100% for critical paths)
  - CI matrix testing on Clojure 1.11/1.12 and JDK 17/21

- **Documentation**:
  - Comprehensive README with examples and troubleshooting
  - API documentation with Codox
  - Quickstart guide with integration examples
  - CONTRIBUTING guide for developers
  - REPL utilities for interactive development

- **Build and tooling**:
  - deps.edn with :dev, :test, :build, :codox, :coverage aliases
  - tools.build for JAR packaging and deployment
  - clj-kondo configuration (zero-tolerance for warnings)
  - cljfmt configuration for consistent formatting
  - GitHub Actions CI/CD pipeline
  - Criterium benchmarks for performance validation

### Performance
- `generate`: < 2μs per operation
- `parse`: < 2μs per operation
- `encode/decode`: < 1μs per operation
- `validate-prefix`: < 500ns per operation

### Technical Details
- **Zero external runtime dependencies** (only org.clojure/clojure and org.clojure/clojurescript)
- **Spec compliance**: 100% compliant with TypeID specification v0.3.0
- **Base32 encoding**: Crockford alphabet (case-insensitive, no ambiguous characters)
- **UUIDv7**: Timestamp-based (sortable) with cryptographic randomness
