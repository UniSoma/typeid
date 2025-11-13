# Data Model: UUID API Promotion

**Feature**: 004-uuid-api-promotion
**Date**: 2025-11-13
**Status**: Complete

## Overview

This feature does not introduce new data entities or modify existing data structures. It promotes existing UUID utility functions to a public namespace. This document describes the data types handled by the public API for reference.

## Core Data Types

### UUID Object

**Description**: Platform-native UUID representation used across the TypeID library.

**Platform Variants**:
- **JVM**: `java.util.UUID` - Java's standard UUID class
- **ClojureScript**: `cljs.core/UUID` - ClojureScript's UUID type

**Properties**:
- Immutable value type
- 128-bit identifier
- Standard string representation: `xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx`
- Supports all UUID versions (v1, v4, v7, etc.)

**Validation Rules**:
- Must be valid UUID object (platform-specific type check)
- Must be non-nil

**Example**:
```clojure
;; JVM
#uuid "018c3f9e-9e4e-7a8a-8b2a-7e8e9e4e7a8a"

;; ClojureScript
#uuid "018c3f9e-9e4e-7a8a-8b2a-7e8e9e4e7a8a"
```

### Byte Array

**Description**: Raw 16-byte representation of a UUID.

**Platform Variants**:
- **JVM**: `byte[]` (Java primitive byte array)
- **ClojureScript**: `js/Uint8Array` (JavaScript typed array)

**Properties**:
- Fixed length: exactly 16 bytes
- Represents UUID in big-endian byte order
- Bytes 0-7: Most significant bits
- Bytes 8-15: Least significant bits

**Validation Rules**:
- Must be exactly 16 bytes in length
- Must be valid byte array type for platform
- No byte value constraints (any 0-255 valid)

**Example**:
```clojure
;; JVM
(byte-array [0x01 0x8c 0x3f 0x9e 0x9e 0x4e 0x7a 0x8a
             0x8b 0x2a 0x7e 0x8e 0x9e 0x4e 0x7a 0x8a])

;; ClojureScript
(js/Uint8Array. [0x01 0x8c 0x3f 0x9e 0x9e 0x4e 0x7a 0x8a
                  0x8b 0x2a 0x7e 0x8e 0x9e 0x4e 0x7a 0x8a])
```

### UUIDv7

**Description**: Specialized UUID variant with timestamp-based ordering (RFC 9562).

**Structure** (128 bits total):
- **Bits 0-47** (6 bytes): Unix timestamp in milliseconds
- **Bits 48-51** (4 bits): Version = 0111 (7)
- **Bits 52-63** (12 bits): Random data
- **Bits 64-65** (2 bits): Variant = 10
- **Bits 66-127** (62 bits): Random data

**Properties**:
- Chronologically sortable by creation time
- Compatible with standard UUID operations
- Represented as standard UUID object (not a separate type)

**Validation Rules** (for generation, not conversion):
- Timestamp must be valid Unix millisecond timestamp
- Version bits must be set to 0111 (7)
- Variant bits must be set to 10

**Example**:
```clojure
;; UUIDv7 generated at 2024-01-15 12:00:00 UTC
#uuid "018c3f9e-9e4e-7a8a-8b2a-7e8e9e4e7a8a"
;;     └─────┬────┘
;;     timestamp portion (first 48 bits)
```

## Data Transformations

### UUID ↔ Bytes Conversion

**Operation**: Bidirectional conversion between UUID objects and byte arrays

**Invariants**:
- **Round-trip property**: `(= uuid (bytes->uuid (uuid->bytes uuid)))`
- **Byte order consistency**: Big-endian across all platforms
- **Length preservation**: Always 16 bytes, regardless of UUID version

**Error Conditions**:

| Condition | Function | Error Type | Error Message |
|-----------|----------|------------|---------------|
| Non-UUID input | `uuid->bytes` | `ex-info` | "Invalid UUID: expected platform-native UUID object" |
| Wrong byte count | `bytes->uuid` | `ex-info` | "Invalid UUID bytes: expected exactly 16 bytes" |
| Wrong type | `bytes->uuid` | `ex-info` | "UUID must be exactly 16 bytes" |
| Nil input | Either | `ex-info` | Type-specific error message |

### UUIDv7 Generation

**Operation**: Generate new UUIDv7 with current timestamp

**Inputs**: None (uses system time and random source)

**Outputs**: Platform-native UUID object with UUIDv7 structure

**Properties**:
- **Monotonicity**: Sequential calls produce sortable UUIDs
- **Randomness**: 74 bits of random data ensure uniqueness
- **Time precision**: Millisecond-level timestamp accuracy

**Platform-Specific Behavior**:

| Platform | Time Source | Random Source |
|----------|-------------|---------------|
| JVM | `System/currentTimeMillis()` | `java.security.SecureRandom` |
| ClojureScript | `js/Date.now()` | `crypto.getRandomValues()` or Node.js crypto |

## State Transitions

**N/A** - All functions are stateless and pure. No state machines or transitions involved.

## Relationships

```text
TypeID (string)
    ↓ parse
┌───────────────┐
│   UUID Object │ ←──── generate-uuidv7()
│ (java.util or │
│  cljs.core)   │
└───────┬───────┘
        │
        │ uuid->bytes ↓
        │ bytes->uuid ↑
        │
┌───────┴───────┐
│  Byte Array   │
│ (16 bytes)    │
└───────────────┘
```

**Explanation**:
- `generate-uuidv7()` creates new UUID objects
- `uuid->bytes` converts UUID → bytes
- `bytes->uuid` converts bytes → UUID
- Both conversion functions work with any UUID version
- UUID objects are used by `typeid.core/parse` (returns UUID) and `typeid.core/create` (accepts UUID)

## Schema Validation (Predicates)

Per constitutional requirement (Principle IV), all validation uses manual predicates:

### UUID Object Validation

```clojure
(defn valid-uuid? [x]
  #?(:clj  (instance? java.util.UUID x)
     :cljs (uuid? x)))
```

### Byte Array Validation

```clojure
(defn valid-uuid-bytes? [x]
  #?(:clj  (and (bytes? x) (= 16 (alength ^bytes x)))
     :cljs (and (instance? js/Uint8Array x) (= 16 (.-length x)))))
```

### UUIDv7 Structure Validation

```clojure
(defn uuidv7? [uuid]
  (and (valid-uuid? uuid)
       (let [bytes (uuid->bytes uuid)]
         ;; Check version bits (byte 6, high nibble = 0111)
         (= 7 (bit-shift-right (bit-and (aget bytes 6) 0xF0) 4)))))
```

## Summary

This feature involves no new data entities. It exposes existing data transformation functions for:
1. Converting UUIDs to bytes and back (bidirectional, lossless)
2. Generating UUIDv7 objects with timestamp-based ordering

All data types are platform-native (no custom types introduced). All validation uses manual predicates per constitutional requirements.
