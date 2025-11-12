# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

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
