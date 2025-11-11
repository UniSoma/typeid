# TypeID for Clojure

> Type-safe, K-sortable, globally unique identifiers inspired by Stripe IDs

[![Clojars Project](https://img.shields.io/clojars/v/unisoma.agilis/typeid.svg)](https://clojars.org/unisoma.agilis/typeid)
[![CI](https://github.com/UniSoma/typeid/workflows/CI/badge.svg)](https://github.com/UniSoma/typeid/actions)
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
{:deps {unisoma.agilis/typeid {:mvn/version "0.1.0"}}}
```

### Leiningen (project.clj)
```clojure
[unisoma.agilis/typeid "0.1.0"]
```

## Quick Start

```clojure
(require '[typeid.core :as t])

;; Generate a new TypeID
(t/generate "user")
;;=> {:ok "user_01h5fskfsk4fpeqwnsyz5hj55t"}

;; Parse a TypeID
(t/parse "user_01h5fskfsk4fpeqwnsyz5hj55t")
;;=> {:ok {:prefix "user"
;;         :suffix "01h5fskfsk4fpeqwnsyz5hj55t"
;;         :uuid #bytes[...]
;;         :typeid "user_01h5fskfsk4fpeqwnsyz5hj55t"}}

;; Validate a TypeID
(t/validate "user_01h5fskfsk4fpeqwnsyz5hj55t")
;;=> {:ok true}

;; Convert UUID to TypeID
(t/encode my-uuid-bytes "user")
;;=> {:ok "user_01h5fskfsk4fpeqwnsyz5hj55t"}

;; Extract UUID from TypeID
(t/decode "user_01h5fskfsk4fpeqwnsyz5hj55t")
;;=> {:ok #bytes[16 bytes]}
```

## Documentation

- **[API Reference (Codox)](https://unisoma.github.io/typeid/)** - Full API documentation
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
;;                   :first-char \8
;;                   :max-allowed-char \7}}}

(t/generate "InvalidPrefix")
;;=> {:error {:type :prefix-invalid-chars
;;            :message "Prefix must be lowercase alphanumeric"
;;            :data {:prefix "InvalidPrefix"
;;                   :invalid-chars [\I \P]}}}
```

### Validation Predicates

```clojure
(require '[typeid.validation :as v])

(v/valid-prefix? "user")        ;;=> true
(v/valid-prefix? "User")        ;;=> false (must be lowercase)

(v/valid-typeid-string? "user_01h5fskfsk4fpeqwnsyz5hj55t") ;;=> true
(v/valid-uuid-bytes? my-bytes)  ;;=> true if 16 bytes
```

### Working with UUIDs

```clojure
;; Convert UUID bytes to hex string
(t/uuid->hex uuid-bytes)
;;=> {:ok "01890a5d-ac96-774b-bcce-b302099a8057"}

;; Convert hex string to UUID bytes
(t/hex->uuid "01890a5d-ac96-774b-bcce-b302099a8057")
;;=> {:ok #bytes[...]}

;; Get all TypeID components as a map
(t/typeid->map "user_01h5fskfsk4fpeqwnsyz5hj55t")
;;=> {:ok {:prefix "user"
;;         :suffix "01h5fskfsk4fpeqwnsyz5hj55t"
;;         :uuid #bytes[...]
;;         :uuid-hex "01890a5d-ac96-774b-bcce-b302099a8057"
;;         :typeid "user_01h5fskfsk4fpeqwnsyz5hj55t"}}
```

## Performance

**Benchmarks** (measured with Criterium on JDK 17):
- `generate`: < 2μs per operation
- `parse`: < 2μs per operation
- `encode/decode`: < 1μs per operation
- `validate-prefix`: < 500ns per operation

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

**"Prefix too long (max 63 characters)"**
- TypeID prefixes must be 63 characters or fewer
- Consider using shorter, more concise prefixes

### Platform-Specific Issues

**ClojureScript**: UUID generation uses `js/Date.now()` and `crypto.getRandomValues()`, which require:
- Node.js 15+ or browser with Web Crypto API
- For older Node.js, add `ws` dependency (see package.json)

**JVM**: Uses `System.currentTimeMillis()` and `java.security.SecureRandom`
- No additional dependencies required
- Works on JDK 11+, tested on JDK 17 and 21

## Development

### Babashka Tasks (Recommended)

This project includes [Babashka](https://babashka.org/) tasks for common development workflows:

```bash
# Show all available tasks
bb tasks

# Show project information
bb info

# Testing
bb test              # Run all JVM tests
bb test:watch        # Run tests in watch mode
bb test:cljs         # Run ClojureScript tests
bb test:coverage     # Run with coverage report

# Code quality
bb lint              # Run clj-kondo
bb format            # Format code with cljfmt
bb quality           # Run all quality checks

# Performance
bb bench             # Run benchmarks

# Build
bb build             # Clean and build JAR
bb install           # Install to local Maven repo

# Documentation
bb docs              # Generate Codox docs

# CI workflows
bb ci:check          # Run all CI checks
bb release:check     # Verify release readiness

# Deploy
bb deploy            # Deploy to Clojars
```

### Manual Commands (Without Babashka)

```bash
# JVM tests
clojure -M:test -m kaocha.runner

# ClojureScript tests (requires Node.js)
npm install
clojure -M:test:cljs -m kaocha.runner :unit-cljs

# Test coverage
clojure -M:test:coverage

# Linting and formatting
clojure -M:lint
clojure -M:dev -m cljfmt.main fix src test

# Benchmarks
clojure -M:dev -m benchmarks.core-bench

# REPL
clojure -M:nrepl
```

See [CONTRIBUTING.md](CONTRIBUTING.md) for detailed development setup instructions.

## Contributing

Contributions are welcome! Please see [CONTRIBUTING.md](CONTRIBUTING.md) for:
- Development environment setup
- Code quality standards
- Testing requirements
- Pull request process

## License

Copyright © 2025 Jonas Rodrigues

Distributed under the MIT License. See [LICENSE](LICENSE) file for details.

## Acknowledgments

- [TypeID Specification](https://github.com/jetify-com/typeid) by Jetify
- Inspired by [Stripe IDs](https://stripe.com/docs/api/object_ids)
- Uses UUIDv7 as defined in [RFC 4122 Draft](https://datatracker.ietf.org/doc/html/draft-ietf-uuidrev-rfc4122bis)
