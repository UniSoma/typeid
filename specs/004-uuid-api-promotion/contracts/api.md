# API Contract: typeid.uuid

**Feature**: 004-uuid-api-promotion
**Date**: 2025-11-13
**Namespace**: `typeid.uuid`

## Overview

This document defines the public API contract for the `typeid.uuid` namespace. All functions are pure (no side effects) and work with platform-native data types.

## Namespace

```clojure
(ns typeid.uuid
  "UUID utility functions for TypeID library.

  Provides cross-platform utilities for working with UUID objects:
  - Convert UUIDs to/from byte arrays
  - Generate UUIDv7 (timestamp-ordered UUIDs)

  All functions work with platform-native UUID types:
  - JVM: java.util.UUID
  - ClojureScript: cljs.core/UUID

  Examples:

    ;; Convert UUID to bytes
    (uuid->bytes #uuid \"018c3f9e-9e4e-7a8a-8b2a-7e8e9e4e7a8a\")
    ;; => #object[\"[B\" ...] (16-byte array)

    ;; Convert bytes to UUID
    (bytes->uuid (uuid->bytes my-uuid))
    ;; => #uuid \"018c3f9e-9e4e-7a8a-8b2a-7e8e9e4e7a8a\"

    ;; Generate new UUIDv7
    (generate-uuidv7)
    ;; => #uuid \"018d5e9e-...\" (timestamp-based)")
```

## Public Functions

### uuid->bytes

**Signature**:
```clojure
(uuid->bytes uuid) => byte-array
```

**Description**: Converts a platform-native UUID object to a 16-byte array.

**Parameters**:
- `uuid` (UUID object): Platform-native UUID (java.util.UUID on JVM, cljs.core/UUID on ClojureScript)

**Returns**:
- `byte-array`: 16-byte array representation (byte[] on JVM, Uint8Array on ClojureScript)

**Throws**:
- `ex-info` with type `:typeid/invalid-uuid` if input is not a valid UUID object

**Properties**:
- Pure function (no side effects)
- Deterministic (same UUID always produces same bytes)
- Cross-platform compatible (works on JVM and ClojureScript)
- Big-endian byte order

**Examples**:
```clojure
;; JVM
(uuid->bytes #uuid "018c3f9e-9e4e-7a8a-8b2a-7e8e9e4e7a8a")
;; => #object["[B" 0x... (16-byte array)]

;; ClojureScript
(uuid->bytes #uuid "018c3f9e-9e4e-7a8a-8b2a-7e8e9e4e7a8a")
;; => #object[Uint8Array [1 140 63 158 158 78 122 138 139 42 126 142 158 78 122 138]]

;; Error case
(uuid->bytes "not-a-uuid")
;; => ExceptionInfo Invalid UUID: expected platform-native UUID object
```

**Contract**:
```clojure
;; Preconditions
(valid-uuid? uuid) ;; true

;; Postconditions
(valid-uuid-bytes? result) ;; true
(= 16 (count result))      ;; true

;; Invariants
(= uuid (bytes->uuid (uuid->bytes uuid))) ;; round-trip property
```

---

### bytes->uuid

**Signature**:
```clojure
(bytes->uuid uuid-bytes) => UUID
```

**Description**: Converts a 16-byte array to a platform-native UUID object.

**Parameters**:
- `uuid-bytes` (byte-array): 16-byte array (byte[] on JVM, Uint8Array on ClojureScript)

**Returns**:
- `UUID`: Platform-native UUID object (java.util.UUID on JVM, cljs.core/UUID on ClojureScript)

**Throws**:
- `ex-info` with type `:typeid/invalid-uuid` if input is not exactly 16 bytes

**Properties**:
- Pure function (no side effects)
- Deterministic (same bytes always produce same UUID)
- Cross-platform compatible (works on JVM and ClojureScript)
- Inverse operation of `uuid->bytes`

**Examples**:
```clojure
;; JVM
(def bytes (byte-array [0x01 0x8c 0x3f 0x9e 0x9e 0x4e 0x7a 0x8a
                         0x8b 0x2a 0x7e 0x8e 0x9e 0x4e 0x7a 0x8a]))
(bytes->uuid bytes)
;; => #uuid "018c3f9e-9e4e-7a8a-8b2a-7e8e9e4e7a8a"

;; ClojureScript
(def bytes (js/Uint8Array. [0x01 0x8c 0x3f 0x9e 0x9e 0x4e 0x7a 0x8a
                             0x8b 0x2a 0x7e 0x8e 0x9e 0x4e 0x7a 0x8a]))
(bytes->uuid bytes)
;; => #uuid "018c3f9e-9e4e-7a8a-8b2a-7e8e9e4e7a8a"

;; Round-trip
(= uuid (bytes->uuid (uuid->bytes uuid)))
;; => true

;; Error cases
(bytes->uuid (byte-array 15)) ;; too short
;; => ExceptionInfo Invalid UUID bytes: expected exactly 16 bytes

(bytes->uuid "not-bytes")
;; => ExceptionInfo UUID must be exactly 16 bytes
```

**Contract**:
```clojure
;; Preconditions
(valid-uuid-bytes? uuid-bytes) ;; true
(= 16 (count uuid-bytes))      ;; true

;; Postconditions
(valid-uuid? result) ;; true

;; Invariants
(java.util.Arrays/equals
  uuid-bytes
  (uuid->bytes (bytes->uuid uuid-bytes))) ;; round-trip property
```

---

### generate-uuidv7

**Signature**:
```clojure
(generate-uuidv7) => UUID
```

**Description**: Generates a new UUIDv7 (RFC 9562) with timestamp-based ordering.

**Parameters**: None

**Returns**:
- `UUID`: Platform-native UUID object with UUIDv7 structure

**Throws**: None (cannot fail under normal operation)

**Properties**:
- Impure function (depends on system time and randomness)
- Non-deterministic (each call produces unique UUID)
- Cross-platform compatible (works on JVM and ClojureScript)
- Chronologically sortable (later calls produce lexicographically greater UUIDs)
- 74 bits of randomness ensure uniqueness

**UUIDv7 Structure**:
- Bytes 0-5 (48 bits): Unix timestamp in milliseconds
- Byte 6 (4 bits): Version = 7 (0111)
- Bytes 6-7 (12 bits): Random data
- Byte 8 (2 bits): Variant = 2 (10)
- Bytes 8-15 (62 bits): Random data

**Examples**:
```clojure
;; Generate new UUIDv7
(generate-uuidv7)
;; => #uuid "018c3f9e-9e4e-7a8a-8b2a-7e8e9e4e7a8a"

;; Each call produces unique UUID
(= (generate-uuidv7) (generate-uuidv7))
;; => false

;; Later calls produce greater UUIDs (sortable)
(def uuid1 (generate-uuidv7))
(Thread/sleep 10)
(def uuid2 (generate-uuidv7))
(< (compare (str uuid1) (str uuid2)) 0)
;; => true

;; Can be used with TypeID
(require '[typeid.core :as typeid])
(typeid/create "user" (generate-uuidv7))
;; => "user_01h455vb4pex5vsknk084sn02q"
```

**Contract**:
```clojure
;; No preconditions (accepts no arguments)

;; Postconditions
(valid-uuid? result)           ;; true
(uuidv7? result)               ;; true (version bits = 7)
(uuid? result)                 ;; true (platform-native type)

;; Properties
;; Sequential calls produce monotonically increasing UUIDs
(let [u1 (generate-uuidv7)
      u2 (generate-uuidv7)]
  (<= (compare (str u1) (str u2)) 0)) ;; true (u1 <= u2)
```

## Platform-Specific Notes

### JVM (Clojure)

**UUID Type**: `java.util.UUID`

**Byte Array Type**: `byte[]` (primitive array)

**Time Source**: `System/currentTimeMillis()`

**Random Source**: `java.security.SecureRandom`

**Performance**:
- `uuid->bytes`: ~500ns per call
- `bytes->uuid`: ~500ns per call
- `generate-uuidv7`: ~1μs per call

### ClojureScript

**UUID Type**: `cljs.core/UUID`

**Byte Array Type**: `js/Uint8Array`

**Time Source**: `js/Date.now()`

**Random Source**: `crypto.getRandomValues()` (browser) or Node.js `crypto` module

**Performance**:
- `uuid->bytes`: ~1μs per call
- `bytes->uuid`: ~1μs per call
- `generate-uuidv7`: ~2μs per call

**Browser Compatibility**: Requires Web Crypto API (all modern browsers)

**Node.js Compatibility**: Requires Node.js 15+ or `crypto` module

## Error Handling

All errors are thrown as `ex-info` with structured error data:

**Error Data Schema**:
```clojure
{:type     keyword          ;; Error category (e.g., :typeid/invalid-uuid)
 :message  string           ;; Human-readable error message
 :input    any              ;; The invalid input that caused error
 :expected string           ;; What was expected
 :actual   string}          ;; What was actually received
```

**Error Types**:

| Error Type | Thrown By | Condition |
|------------|-----------|-----------|
| `:typeid/invalid-uuid` | `uuid->bytes` | Input is not a UUID object |
| `:typeid/invalid-uuid` | `bytes->uuid` | Input is not exactly 16 bytes |

**Example Error**:
```clojure
(try
  (uuid->bytes "not-a-uuid")
  (catch Exception e
    (ex-data e)))
;; => {:type :typeid/invalid-uuid
;;     :message "Invalid UUID: expected platform-native UUID object"
;;     :input "not-a-uuid"
;;     :expected "java.util.UUID"
;;     :actual "class java.lang.String"}
```

## Migration from typeid.impl.uuid

**Old (0.2.x and earlier)**:
```clojure
(require '[typeid.impl.uuid :as uuid])

(uuid/uuid->bytes #uuid "...")
(uuid/bytes->uuid byte-array)
(uuid/generate-uuidv7)
```

**New (0.3.0+)**:
```clojure
(require '[typeid.uuid :as uuid])

(uuid/uuid->bytes #uuid "...")
(uuid/bytes->uuid byte-array)
(uuid/generate-uuidv7)
```

**Breaking Change**: The `typeid.impl.uuid` namespace has been removed. Update all imports to `typeid.uuid`.

## Compatibility

**Minimum Versions**:
- Clojure 1.11+
- ClojureScript 1.12+

**Tested Platforms**:
- JDK 17, 21
- Node.js 18+
- Modern browsers (Chrome, Firefox, Safari, Edge)

## Performance Budgets

| Function | JVM | ClojureScript | Target |
|----------|-----|---------------|--------|
| `uuid->bytes` | ~500ns | ~1μs | < 1μs |
| `bytes->uuid` | ~500ns | ~1μs | < 1μs |
| `generate-uuidv7` | ~1μs | ~2μs | < 2μs |

All targets met in benchmarks on typical hardware (see `dev/benchmarks/` for details).

## See Also

- `typeid.core` - Main TypeID API (create, parse, explain)
- `typeid.codec` - Low-level encoding/decoding operations
- [RFC 9562](https://datatracker.ietf.org/doc/html/rfc9562) - UUIDv7 specification
