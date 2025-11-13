# TypeID for Clojure

> Type-safe, K-sortable, globally unique identifiers inspired by Stripe IDs

[![Clojars Project](https://img.shields.io/clojars/v/io.github.unisoma/typeid.svg)](https://clojars.org/io.github.unisoma/typeid)
[![CI](https://github.com/UniSoma/typeid/workflows/CI/badge.svg)](https://github.com/UniSoma/typeid/actions)
[![License](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)

> **⚠️ Early Stage Development**
> This library is in active development (version 0.x). The API may change before reaching 1.0.0.
> While the library is production-ready and fully tested, you may encounter breaking changes in minor releases.
> See [CHANGELOG.md](CHANGELOG.md) for detailed change tracking.

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
{:deps {io.github.unisoma/typeid {:mvn/version "0.1.1"}}}
```

### Leiningen (project.clj)
```clojure
[io.github.unisoma/typeid "0.1.1"]
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

- **[API Reference (cljdoc)](https://cljdoc.org/d/io.github.unisoma/typeid)** - Full API documentation
- **[TypeID Specification](https://github.com/jetify-com/typeid)** - Official spec

## Usage Examples

### Basic Operations

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

## Common Patterns

### Generate ID for New Entity

```clojure
(defn create-user [name email]
  (let [user-id (typeid/create "user")]
    {:id user-id
     :name name
     :email email
     :created-at (System/currentTimeMillis)}))

(create-user "Alice" "alice@example.com")
;;=> {:id "user_01h5fskfsk4fpeqwnsyz5hj55t"
;;    :name "Alice"
;;    :email "alice@example.com"
;;    :created-at 1699564800000}
```

### Validate TypeID from User Input

```clojure
(defn get-user-by-id [typeid-str]
  (if-let [error (typeid/explain typeid-str)]
    {:error (str "Invalid TypeID: " (:message error))}
    (fetch-user-from-db typeid-str)))

(get-user-by-id "user_01h5fskfsk4fpeqwnsyz5hj55t")
;;=> {:id "user_..." :name "Alice" ...}

(get-user-by-id "invalid")
;;=> {:error "Invalid TypeID: Invalid format: does not match TypeID pattern"}
```

### Store UUID in Database, Return TypeID to Clients

```clojure
(require '[typeid.codec :as codec])

(defn save-and-return-order [order-data]
  (let [order-id (typeid/create "order")
        {:keys [uuid]} (typeid/parse order-id)
        uuid-hex (codec/uuid->hex uuid)]
    ;; Save uuid-hex to database (efficient storage)
    (save-to-db! {:uuid uuid-hex :data order-data})
    ;; Return TypeID to client (human-readable)
    {:id order-id :data order-data}))

(save-and-return-order {:items [...] :total 99.99})
;;=> {:id "order_01h5fskp7y2t3z9x8w6v5u4s3r" :data {...}}
```

### Fetch by TypeID, Query by UUID

```clojure
(require '[typeid.codec :as codec])

(defn fetch-order [typeid-str]
  (try
    (let [uuid-bytes (codec/decode typeid-str)
          uuid-hex (codec/uuid->hex uuid-bytes)]
      (query-db {:uuid uuid-hex}))
    (catch Exception e
      {:error (str "Invalid TypeID: " (ex-message e))})))

(fetch-order "order_01h5fskp7y2t3z9x8w6v5u4s3r")
;;=> {:uuid "..." :items [...] :total 99.99}
```

### Prefix-Based Type Checking

```clojure
(defn delete-user [typeid-str]
  (try
    (let [{:keys [prefix uuid]} (typeid/parse typeid-str)]
      (if (= "user" prefix)
        (do
          (delete-from-db! uuid)
          {:status :deleted :id typeid-str})
        {:error "Expected user TypeID, got different type"}))
    (catch Exception e
      {:error (str "Invalid TypeID: " (ex-message e))})))

(delete-user "user_01h5fskfsk4fpeqwnsyz5hj55t")
;;=> {:status :deleted :id "user_01h5fskfsk4fpeqwnsyz5hj55t"}

(delete-user "order_01h5fskp7y2t3z9x8w6v5u4s3r")
;;=> {:error "Expected user TypeID, got different type"}
```

### Integration with PostgreSQL

```clojure
(require '[next.jdbc :as jdbc]
         '[typeid.codec :as codec])

;; Store UUID as UUID column
(defn insert-user [ds name email]
  (let [user-id (typeid/create "user")
        {:keys [uuid]} (typeid/parse user-id)
        uuid-hex (codec/uuid->hex uuid)]
    (jdbc/execute-one! ds
      ["INSERT INTO users (id, name, email) VALUES (?::uuid, ?, ?)"
       uuid-hex name email])
    {:typeid user-id :name name :email email}))

(defn find-user [ds typeid-str]
  (let [uuid-bytes (codec/decode typeid-str)
        uuid-hex (codec/uuid->hex uuid-bytes)]
    (jdbc/execute-one! ds
      ["SELECT * FROM users WHERE id = ?::uuid" uuid-hex])))
```

### Integration with Web APIs (Ring)

```clojure
(require '[compojure.core :refer [defroutes GET POST]]
         '[ring.util.response :refer [response status]])

(defroutes app-routes
  (POST "/users" {body :body}
    (let [user-id (typeid/create "user")]
      (response {:id user-id
                 :name (:name body)
                 :email (:email body)})))

  (GET "/users/:id" [id]
    (if-let [error (typeid/explain id)]
      (-> (response {:error (:message error)})
          (status 400))
      (response (fetch-user-from-db id)))))
```

## Performance

The library is designed for high performance with the following **target benchmarks**:

| Operation | Target |
|-----------|--------|
| `create` | < 2μs |
| `parse` | < 2μs |
| `encode/decode` | < 1μs |
| `base32/encode` | < 1μs |
| `base32/decode` | < 1μs |
| `validate-prefix` | < 500ns |
| `uuid/generate-uuidv7` | < 500ns |

### Why These Targets?

These performance targets ensure TypeID operations remain negligible overhead in production systems:

**Real-world throughput:**
- **500K TypeIDs/second** at 2μs per create operation (single core)
- **1M operations/second** at 1μs for encode/decode
- Even at 10K requests/second (high traffic), ID generation consumes **<2% CPU time**

**Compared to typical operations:**
- Database queries: **1,000-10,000μs** (1-10ms) — **500-5000x slower**
- Network round trips: **10,000-100,000μs** (10-100ms) — **5,000-50,000x slower**
- File I/O: **1,000-100,000μs** — **500-50,000x slower**
- TypeID generation: **2μs** — **essentially free**

**When performance matters:**
- **Hot paths**: TypeIDs are often created for every new entity (users, orders, events)
- **Batch operations**: Generating thousands of IDs during data imports or migrations
- **High-frequency events**: Logging, analytics, or event sourcing systems
- **Latency-sensitive APIs**: Every microsecond counts in sub-millisecond response targets

**Bottom line:** At these speeds, TypeID operations are **negligible compared to I/O costs** (database, network, disk). You can generate IDs freely without worrying about performance bottlenecks.

Zero reflection warnings. All hot paths use type hints for optimal performance.

### Running Benchmarks

To measure actual performance on your system:

```bash
# Using Babashka (recommended)
bb bench

# Or manually with Clojure CLI
clojure -M:dev -m benchmarks.core-bench
```

The benchmark suite uses [Criterium](https://github.com/hugoduncan/criterium) and reports:
- **Time**: Mean execution time per operation
- **Throughput**: Operations per second
- **Variability**: Standard deviation as percentage of mean
- **Pass/Fail**: Indicator showing whether target is met

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

## FAQ

**Can I use TypeIDs as database primary keys?**

Yes! Store the UUID portion (16 bytes or hex string) as the primary key, and generate the TypeID string when sending data to clients. This gives you the best of both worlds: efficient storage and human-readable IDs in your API.

```clojure
;; Store UUID in database
(let [user-id (typeid/create "user")
      {:keys [uuid]} (typeid/parse user-id)]
  (save-to-db! {:id uuid :name "Alice"}))

;; Return TypeID to clients
(defn get-user [uuid]
  (let [user (fetch-from-db uuid)
        user-id (typeid/create "user" (:id user))]
    (assoc user :id user-id)))
```

**Are TypeIDs sortable?**

Yes! TypeIDs are chronologically sortable because they're based on UUIDv7, which encodes the timestamp in the first 48 bits. This means TypeIDs created later will sort after TypeIDs created earlier.

```clojure
(def id1 (typeid/create "user"))
(Thread/sleep 10)
(def id2 (typeid/create "user"))

(< id1 id2)  ;;=> true (id1 was created first)
```

**Can I have TypeIDs without a prefix?**

Yes! Use `nil` or no arguments to create prefix-less TypeIDs:

```clojure
(typeid/create nil)
;;=> "01h5fskfsk4fpeqwnsyz5hj55t"

(typeid/create)
;;=> "01h5fskfsk4fpeqwnsyz5hj55t"
```

**Are single-character prefixes allowed?**

Technically yes, but the spec recommends at least 3 characters for clarity. Use descriptive prefixes like "user" instead of "u" to make your IDs more self-documenting.

**Can I encode UUIDv4 (or other non-UUIDv7) as a TypeID?**

Yes! The `create` function with two arguments accepts any UUID:

```clojure
(typeid/create "legacy" #uuid "550e8400-e29b-41d4-a716-446655440000")
;;=> "legacy_2qeh85amd9ct4vr9px628gkdkr"
```

Note: Only the one-argument form generates UUIDv7. Other UUID versions won't be chronologically sortable.

**How do I migrate from UUIDs to TypeIDs?**

If you have existing UUIDs in your system, encode them with a prefix:

```clojure
(require '[typeid.codec :as codec])

;; From UUID object
(typeid/create "user" existing-uuid)

;; From hex string
(let [uuid-bytes (codec/hex->uuid "018c3f9e-9e4e-7a8a-8b2a-7e8e9e4e7a8a")]
  (codec/encode uuid-bytes "user"))
```

The underlying UUID remains the same, so your database queries continue to work.

**What's the performance impact?**

TypeID operations are extremely fast (< 2μs for create/parse). At 2μs per operation, you can generate 500K TypeIDs per second on a single core. This is negligible compared to typical I/O operations:
- Database queries: 1-10ms (500-5000x slower)
- Network requests: 10-100ms (5,000-50,000x slower)

Even at high traffic (10K req/s), ID generation consumes less than 2% CPU time.

**Do I need to validate TypeIDs from my own system?**

If you're generating TypeIDs within your system and storing them, you generally don't need to validate them again when reading from your database. However, **always validate** TypeIDs that come from:
- User input (URL parameters, form submissions)
- External APIs
- Untrusted sources

```clojure
;; From database (trusted) - no validation needed
(defn get-user-from-db [uuid]
  (let [user (fetch-user uuid)]
    (typeid/create "user" (:id user))))

;; From user input (untrusted) - validate first
(defn get-user-by-id [typeid-str]
  (if-let [error (typeid/explain typeid-str)]
    {:error (:message error)}
    (fetch-user typeid-str)))
```

**Can I customize the prefix separator?**

No, the TypeID specification requires the underscore (`_`) separator. This ensures consistency across all TypeID implementations and languages.

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
