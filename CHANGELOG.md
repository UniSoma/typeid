# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Changed

- **Documentation**: Added performance rationale section to README explaining why benchmark targets are appropriate
  - Real-world throughput implications (500K-1M ops/second)
  - Comparison to typical I/O operations (database, network, file)
  - Use case scenarios where performance matters
  - Clarifies that TypeID operations are negligible overhead in production systems

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
