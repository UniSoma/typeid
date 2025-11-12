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
(require '[typeid.core :as typeid])

;; Create a new TypeID with a fresh UUIDv7
(typeid/create "user")
;;=> "user_01h5fskfsk4fpeqwnsyz5hj55t"

;; Create with keyword prefix (extracts name)
(typeid/create :user)
;;=> "user_01h5fskfsk4fpeqwnsyz5hj55t"

;; Create without prefix (nil or no args)
(typeid/create nil)
;;=> "01h5fskfsk4fpeqwnsyz5hj55t"

(typeid/create)
;;=> "01h5fskfsk4fpeqwnsyz5hj55t"

;; Create TypeID from an existing UUID
(typeid/create "user" #uuid "018c3f9e-9e4e-7a8a-8b2a-7e8e9e4e7a8a")
;;=> "user_01h455vb4pex5vsknk084sn02q"

;; Validate a TypeID (returns nil if valid, error map if invalid)
(typeid/explain "user_01h5fskfsk4fpeqwnsyz5hj55t")
;;=> nil  ; Valid!

(typeid/explain "User_01h5fskfsk4fpeqwnsyz5hj55t")
;;=> {:type :typeid/invalid-prefix
;;    :message "Invalid prefix: contains uppercase characters"
;;    :input "User_01h5fskfsk4fpeqwnsyz5hj55t"
;;    :expected "lowercase alphanumeric characters (a-z, 0-9)"
;;    :actual "uppercase 'U'"}

;; Parse a TypeID into components (throws exception if invalid)
(typeid/parse "user_01h5fskfsk4fpeqwnsyz5hj55t")
;;=> {:prefix "user"
;;    :suffix "01h5fskfsk4fpeqwnsyz5hj55t"
;;    :uuid #uuid "018c3f9e-9e4e-7a8a-8b2a-7e8e9e4e7a8a"
;;    :typeid "user_01h5fskfsk4fpeqwnsyz5hj55t"}

;; Low-level codec operations (typeid.codec namespace)
(require '[typeid.codec :as codec])

;; Encode UUID bytes to TypeID
(codec/encode uuid-bytes "user")
;;=> "user_01h5fskfsk4fpeqwnsyz5hj55t"

;; Decode TypeID to UUID bytes
(codec/decode "user_01h5fskfsk4fpeqwnsyz5hj55t")
;;=> #object["[B" ... (16-byte array)]

;; Convert UUID bytes to hex string
(codec/uuid->hex uuid-bytes)
;;=> "018c3f9e9e4e7a8a8b2a7e8e9e4e7a8a"

;; Convert hex string to UUID bytes
(codec/hex->uuid "018c3f9e-9e4e-7a8a-8b2a-7e8e9e4e7a8a")
;;=> #object["[B" ... (16-byte array)]
```

## Documentation

- **[API Reference (Codox)](https://unisoma.github.io/typeid/)** - Full API documentation
- **[Quickstart Guide](specs/001-typeid-implementation/quickstart.md)** - Step-by-step tutorial
- **[TypeID Specification](https://github.com/jetify-com/typeid)** - Official spec

## Usage Examples

### Basic Operations

See Quick Start above for common patterns.

### Error Handling

The library uses two patterns for error handling:

**1. Validation without exceptions (`explain`)**

Use `explain` when you want to check validity without catching exceptions:

```clojure
(require '[typeid.core :as typeid])

;; Valid TypeID returns nil
(typeid/explain "user_01h5fskfsk4fpeqwnsyz5hj55t")
;;=> nil

;; Invalid TypeID returns error map
(typeid/explain "User_01h5fskfsk4fpeqwnsyz5hj55t")
;;=> {:type :typeid/invalid-prefix
;;    :message "Invalid prefix: contains uppercase characters"
;;    :input "User_01h5fskfsk4fpeqwnsyz5hj55t"
;;    :expected "lowercase alphanumeric characters (a-z, 0-9)"
;;    :actual "uppercase 'U'"}

;; Gracefully handles non-string input
(typeid/explain 12345)
;;=> {:type :typeid/invalid-input-type
;;    :message "Invalid input type: expected string"
;;    :input 12345
;;    :expected "string"
;;    :actual "number"}
```

**2. Exception-based (`parse`, `create`)**

Functions like `parse` and `create` throw `ExceptionInfo` on invalid input:

```clojure
;; Parse valid TypeID
(typeid/parse "user_01h5fskfsk4fpeqwnsyz5hj55t")
;;=> {:prefix "user", :suffix "...", :uuid #uuid "...", :typeid "..."}

;; Invalid TypeID throws exception
(try
  (typeid/parse "invalid-typeid")
  (catch Exception e
    (ex-data e)))
;;=> {:type :typeid/invalid-format
;;    :message "Invalid format: does not match TypeID pattern"
;;    :input "invalid-typeid"
;;    :expected "TypeID format: [prefix_]suffix"}

;; Create with invalid prefix throws exception
(try
  (typeid/create "InvalidPrefix" #uuid "018c3f9e-9e4e-7a8a-8b2a-7e8e9e4e7a8a")
  (catch Exception e
    (ex-data e)))
;;=> {:type :typeid/invalid-prefix
;;    :message "Invalid prefix: contains uppercase characters"
;;    :input "InvalidPrefix"
;;    :expected "lowercase alphanumeric characters (a-z, 0-9)"
;;    :actual "uppercase 'I'"}
```

**Pattern: Validate before parsing (avoid exceptions)**

```clojure
(defn safe-parse [input]
  (if-let [error (typeid/explain input)]
    {:error error}
    {:success (typeid/parse input)}))

(safe-parse "user_01h5fskfsk4fpeqwnsyz5hj55t")
;;=> {:success {:prefix "user", ...}}

(safe-parse "invalid")
;;=> {:error {:type :typeid/invalid-format, ...}}
```

### Creating TypeIDs

The `create` function has three arities:

```clojure
;; Zero-arity: Generate new TypeID with no prefix
(typeid/create)
;;=> "01h5fskfsk4fpeqwnsyz5hj55t"

;; One-arity: Generate new TypeID with prefix
(typeid/create "user")
;;=> "user_01h5fskfsk4fpeqwnsyz5hj55t"

(typeid/create :org)  ; Keyword prefix works too
;;=> "org_01h5fskfsk4fpeqwnsyz5hj55t"

;; Two-arity: Create TypeID from existing UUID
(def my-uuid #uuid "018c3f9e-9e4e-7a8a-8b2a-7e8e9e4e7a8a")

(typeid/create "user" my-uuid)
;;=> "user_01h455vb4pex5vsknk084sn02q"

(typeid/create nil my-uuid)  ; No prefix
;;=> "01h455vb4pex5vsknk084sn02q"

;; Works with any UUID version
(typeid/create "order" #uuid "550e8400-e29b-41d4-a716-446655440000")
;;=> "order_2qeh85amd9ct4vr9px628gkdkr"
```

### Working with UUIDs and Codec Operations

For low-level operations, use the `typeid.codec` namespace:

```clojure
(require '[typeid.codec :as codec])

;; Convert UUID bytes to hex string
(def uuid-bytes (byte-array [0x01 0x8c 0x3f 0x9e 0x9e 0x4e 0x7a 0x8a
                              0x8b 0x2a 0x7e 0x8e 0x9e 0x4e 0x7a 0x8a]))

(codec/uuid->hex uuid-bytes)
;;=> "018c3f9e9e4e7a8a8b2a7e8e9e4e7a8a"

;; Convert hex string to UUID bytes (accepts hyphens and uppercase)
(codec/hex->uuid "018c3f9e-9e4e-7a8a-8b2a-7e8e9e4e7a8a")
;;=> #object["[B" ... (16-byte array)]

(codec/hex->uuid "018C3F9E9E4E7A8A8B2A7E8E9E4E7A8A")
;;=> #object["[B" ... (same byte array)]

;; Encode UUID bytes directly
(codec/encode uuid-bytes "user")
;;=> "user_01h455vb4pex5vsknk084sn02q"

;; Decode TypeID to UUID bytes
(codec/decode "user_01h455vb4pex5vsknk084sn02q")
;;=> #object["[B" ... (16-byte array)]
```

### Parsing TypeIDs

Extract components from a TypeID:

```clojure
;; Parse prefixed TypeID
(typeid/parse "user_01h5fskfsk4fpeqwnsyz5hj55t")
;;=> {:prefix "user"
;;    :suffix "01h5fskfsk4fpeqwnsyz5hj55t"
;;    :uuid #uuid "018c3f9e-9e4e-7a8a-8b2a-7e8e9e4e7a8a"
;;    :typeid "user_01h5fskfsk4fpeqwnsyz5hj55t"}

;; Parse prefix-less TypeID
(typeid/parse "01h5fskfsk4fpeqwnsyz5hj55t")
;;=> {:prefix ""
;;    :suffix "01h5fskfsk4fpeqwnsyz5hj55t"
;;    :uuid #uuid "018c3f9e-9e4e-7a8a-8b2a-7e8e9e4e7a8a"
;;    :typeid "01h5fskfsk4fpeqwnsyz5hj55t"}

;; Use destructuring to extract specific fields
(let [{:keys [prefix uuid]} (typeid/parse "user_01h5fskfsk4fpeqwnsyz5hj55t")]
  (println "Type:" prefix)
  (println "UUID:" uuid))
;;=> Type: user
;;=> UUID: 018c3f9e-9e4e-7a8a-8b2a-7e8e9e4e7a8a
```

## Migration Guide

If you're upgrading from an earlier version that used the data-as-error pattern (`{:ok ...}` / `{:error ...}`), here's how to migrate to the new exception-based API:

### API Changes Summary

| Old Function | New Function | Change |
|--------------|--------------|--------|
| `validate` | `explain` | Now returns `nil` for valid, error map for invalid (no wrapping) |
| `typeid->map` | `parse` | Now throws exception on invalid input (no `{:ok ...}` wrapping) |
| `generate` | `create` | Renamed to `create` with enhanced functionality (2-arity support) |
| N/A | `create` (2-arity) | **NEW**: Create TypeID from existing UUID |
| `encode` | `codec/encode` | Moved to `typeid.codec` namespace |
| `decode` | `codec/decode` | Moved to `typeid.codec` namespace |
| `uuid->hex` | `codec/uuid->hex` | Moved to `typeid.codec` namespace |
| `hex->uuid` | `codec/hex->uuid` | Moved to `typeid.codec` namespace |

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
clojure -M:test:cljs -m kaocha.runner --config-file tests.cljs.edn

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
