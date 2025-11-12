# Data Model: TypeID Components

**Feature**: TypeID Clojure/ClojureScript Library
**Date**: 2025-11-10
**Purpose**: Define data structures, validation rules, and state transitions for TypeID entities

## Overview

This document defines the core data structures used in the TypeID library. The library works with three primary entities: **TypeID strings** (external representation), **Parsed TypeIDs** (internal decomposed form), and **UUIDs** (raw byte representation). All entities are immutable and validated via Malli schemas.

## Core Entities

### 1. TypeID String (External Representation)

**Description**: The string representation of a TypeID as exposed to library users and stored in databases/logs.

**Format**:
```
[prefix]_[suffix]    (if prefix non-empty)
[suffix]             (if prefix empty)
```

**Components**:
- **Prefix**: 0-63 lowercase ASCII characters `[a-z_]` matching pattern `^([a-z]([a-z_]{0,61}[a-z])?)?$`
- **Separator**: Single underscore `_` (omitted if prefix is empty)
- **Suffix**: Exactly 26 base32 characters using Crockford alphabet `0-9a-z` (excluding `i,l,o,u`)

**Constraints**:
- Minimum length: 26 characters (prefix-less TypeID)
- Maximum length: 90 characters (63 prefix + 1 separator + 26 suffix)
- First suffix character must be `0-7` (prevents 128-bit overflow)
- Case-sensitive: all lowercase required

**Examples**:
```clojure
"user_01h5fskfsk4fpeqwnsyz5hj55t"     ;; Valid: prefix + separator + suffix
"01h5fskfsk4fpeqwnsyz5hj55t"          ;; Valid: prefix-less
"my__type_01h5fskfsk4fpeqwnsyz5hj55t" ;; Valid: consecutive underscores allowed
"abc_01h5fskfsk4fpeqwnsyz5hj55t"      ;; Valid: 3-char prefix (minimum recommended)
"a_01h5fskfsk4fpeqwnsyz5hj55t"        ;; Valid but discouraged: single-char prefix
```

**Invalid Examples**:
```clojure
"User_01h5fskfsk4fpeqwnsyz5hj55t"     ;; Invalid: uppercase in prefix
"user_8zzzzzzzzzzzzzzzzzzzzzzzzz"     ;; Invalid: first suffix char > 7 (overflow)
"_prefix_01h5fskfsk4fpeqwnsyz5hj55t"  ;; Invalid: prefix starts with underscore
"prefix_01h5fskfsk4fpeqwnsyz5hj5"     ;; Invalid: suffix too short (< 26 chars)
```

**Validation Predicate**:
```clojure
(defn valid-typeid-string?
  "Check if string is a valid TypeID format."
  [s]
  (and (string? s)
       (<= 26 (count s) 90)
       (= s (clojure.string/lower-case s))))
```

---

### 2. Parsed TypeID (Internal Representation)

**Description**: The decomposed form of a TypeID after parsing, used internally for manipulation and validation.

**Structure**:
```clojure
{:prefix   string         ;; Type prefix (empty string if none)
 :suffix   string         ;; 26-character base32 suffix
 :uuid     bytes          ;; 16-byte UUID (decoded from suffix)
 :typeid   string}        ;; Original TypeID string (for round-trip)
```

**Field Specifications**:

| Field | Type | Constraints | Description |
|-------|------|-------------|-------------|
| `:prefix` | `string` | 0-63 chars, pattern `^([a-z]([a-z_]{0,61}[a-z])?)?$` | Type identifier (e.g., "user", "order") |
| `:suffix` | `string` | Exactly 26 chars, pattern `^[0-7][0-9a-z]{25}$` | Base32-encoded UUID |
| `:uuid` | `bytes` | Exactly 16 bytes | Raw UUID bytes (big-endian) |
| `:typeid` | `string` | Valid TypeID string | Complete TypeID for serialization |

**Validation Rules**:
1. `:prefix` must match prefix regex or be empty
2. `:suffix` must be exactly 26 base32 characters with first char ≤ `7`
3. `:uuid` must be exactly 16 bytes
4. `:typeid` must equal `(str prefix (when-not (empty? prefix) "_") suffix)`
5. Decoding `:suffix` must produce `:uuid` (round-trip verification)

**Examples**:
```clojure
;; Parsed TypeID with prefix
{:prefix "user"
 :suffix "01h5fskfsk4fpeqwnsyz5hj55t"
 :uuid   #bytes[16, 1, 136, 229, ...]
 :typeid "user_01h5fskfsk4fpeqwnsyz5hj55t"}

;; Parsed TypeID without prefix
{:prefix ""
 :suffix "01h5fskfsk4fpeqwnsyz5hj55t"
 :uuid   #bytes[16, 1, 136, 229, ...]
 :typeid "01h5fskfsk4fpeqwnsyz5hj55t"}
```

**Validation Predicates**:
```clojure
(defn valid-parsed-typeid?
  "Check if map is a valid parsed TypeID."
  [{:keys [prefix suffix uuid typeid]}]
  (and (valid-prefix? prefix)
       (valid-base32-suffix? suffix)
       (valid-uuid-bytes? uuid)
       (valid-typeid-string? typeid)
       (= typeid (str prefix (when-not (empty? prefix) "_") suffix))))
```

**State Transitions**: None (immutable)

---

### 3. UUID (Raw Bytes)

**Description**: 128-bit universally unique identifier, typically UUIDv7 for generated TypeIDs.

**Format**: 16 bytes in big-endian order

**Structure (UUIDv7)**:
```
Byte  0-5:  Unix timestamp in milliseconds (48 bits)
Byte  6:    Sub-millisecond precision + sequence (12 bits) + version (4 bits = 0111)
Byte  7:    Variant (2 bits = 10) + random (6 bits)
Byte  8-15: Random data (62 bits total with byte 7)
```

**Bit Layout**:
```
 0                   1                   2                   3
 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
|                         unix_ts_ms (48 bits)                  |
+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
|  rand_a (12 bits) | ver |  variant  |      rand_b (62 bits)   |
+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
|                       rand_b (continued)                       |
+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
```

**Validation Rules**:
- **Length**: Must be exactly 16 bytes
- **Version** (bits 48-51 for UUIDv7): Should be `0111` (= 7) for generated TypeIDs
- **Variant** (bits 64-65): Should be `10` for RFC 4122 compliance
- **Note**: Library accepts non-v7 UUIDs for encoding (FR-014 supports custom UUID variants)

**Examples**:
```clojure
;; UUIDv7 bytes (hex representation for readability)
#bytes[0x01 0x88 0xe5 0xf5 0xf3 0x4a 0x7b 0x3d
       0x9f 0x2a 0x1c 0x5d 0xe6 0x7f 0xa8 0xc1]

;; UUIDv4 bytes (also accepted for encoding)
#bytes[0x3d 0x7f 0xa8 0xc1 0x1c 0x5d 0x4e 0x6a
       0x8f 0x2a 0xe5 0xf5 0xf3 0x4a 0x7b 0x3d]
```

**Validation Predicates**:
```clojure
(defn valid-uuid-bytes?
  "Check if bytes represent a valid UUID (exactly 16 bytes)."
  [b]
  (and (bytes? b)
       (= 16 (count b))))

(defn valid-uuidv7-bytes?
  "Stricter validation for generated UUIDs (version 7, variant 10)."
  [b]
  (and (valid-uuid-bytes? b)
       (= 7 (bit-and (bit-shift-right (aget b 6) 4) 0x0F))  ; Version bits
       (= 2 (bit-and (bit-shift-right (aget b 8) 6) 0x03)))) ; Variant bits
```

**State Transitions**: None (immutable)

---

### 4. Base32 Encoding Alphabet

**Description**: Lookup tables for encoding/decoding between 5-bit values and base32 characters.

**Encode Alphabet** (index → character):
```
Index: 0  1  2  3  4  5  6  7  8  9  10 11 12 13 14 15 16 17 18 19 20 21 22 23 24 25 26 27 28 29 30 31
Char:  0  1  2  3  4  5  6  7  8  9  a  b  c  d  e  f  g  h  j  k  m  n  p  q  r  s  t  v  w  x  y  z
```

**Decode Map** (character → index):
```clojure
{\0 0, \1 1, \2 2, \3 3, \4 4, \5 5, \6 6, \7 7, \8 8, \9 9,
 \a 10, \b 11, \c 12, \d 13, \e 14, \f 15, \g 16, \h 17,
 \j 18, \k 19, \m 20, \n 21, \p 22, \q 23, \r 24, \s 25,
 \t 26, \v 27, \w 28, \x 29, \y 30, \z 31}
```

**Excluded Characters**: `i`, `l`, `o`, `u` (ambiguous/confusing)

**Validation Rules**:
- All characters in suffix must exist in decode map
- First character must be `0-7` (decimal value 0-7) to prevent overflow

---

## Data Flow

### Generate TypeID

```
User Input: prefix (string)
    ↓
1. Validate prefix (Malli schema)
    ↓
2. Generate UUIDv7 bytes (16 bytes with timestamp + random)
    ↓
3. Encode UUID to base32 suffix (26 chars)
    ↓
4. Combine prefix + separator + suffix
    ↓
Output: TypeID string
```

### Parse TypeID

```
User Input: typeid (string)
    ↓
1. Validate string format (length, case)
    ↓
2. Split on last underscore → prefix + suffix
    ↓
3. Validate prefix (regex match)
    ↓
4. Validate suffix (26 chars, first ≤ 7)
    ↓
5. Decode suffix to UUID bytes
    ↓
Output: ParsedTypeID {:prefix, :suffix, :uuid, :typeid}
```

### Encode UUID to TypeID

```
User Input: uuid-bytes (16 bytes), prefix (string)
    ↓
1. Validate uuid-bytes (16 bytes)
    ↓
2. Validate prefix (Malli schema)
    ↓
3. Encode uuid-bytes to base32 suffix
    ↓
4. Combine prefix + separator + suffix
    ↓
Output: TypeID string
```

### Decode TypeID to UUID

```
User Input: typeid (string)
    ↓
1. Parse TypeID → ParsedTypeID
    ↓
2. Extract :uuid field
    ↓
Output: UUID bytes (16 bytes)
```

---

## Validation Rules Summary

| Entity | Rule | Enforced By | Error Type |
|--------|------|-------------|------------|
| TypeID String | Length 26-90 chars | Malli schema | `:invalid-length` |
| TypeID String | All lowercase | Parse function | `:invalid-case` |
| Prefix | Match regex `^([a-z]([a-z_]{0,61}[a-z])?)?$` | Malli schema | `:invalid-prefix-format` |
| Prefix | 0-63 characters | Malli schema | `:prefix-too-long` |
| Suffix | Exactly 26 characters | Parse function | `:invalid-suffix-length` |
| Suffix | First char `0-7` | Decode function | `:suffix-overflow` |
| Suffix | All chars in base32 alphabet | Decode function | `:invalid-base32-char` |
| UUID | Exactly 16 bytes | Malli schema | `:invalid-uuid-length` |
| UUIDv7 (generated) | Version bits = 0111 | Generate function | `:invalid-uuid-version` |
| UUIDv7 (generated) | Variant bits = 10 | Generate function | `:invalid-uuid-variant` |

---

## Error Data Structure

All validation errors return a map with structured error information:

```clojure
{:error
 {:type    keyword          ;; Error type (e.g., :invalid-prefix-format)
  :message string           ;; Human-readable error message
  :data    map              ;; Contextual data (input value, rule violated, etc.)
  :path    vector}}         ;; Malli error path (if applicable)
```

**Example**:
```clojure
;; Invalid prefix error
{:error
 {:type :invalid-prefix-format
  :message "Prefix must match pattern [a-z]([a-z_]{0,61}[a-z])? or be empty"
  :data {:prefix "User123"
         :pattern "^([a-z]([a-z_]{0,61}[a-z])?)?$"}}}

;; Suffix overflow error
{:error
 {:type :suffix-overflow
  :message "First character of suffix must be 0-7 to prevent 128-bit overflow"
  :data {:suffix "8zzzzzzzzzzzzzzzzzzzzzzzzz"
         :first-char \8
         :max-allowed-char \7}}}
```

---

## Performance Considerations

### Memory Allocation

- **TypeID strings**: Immutable strings, no additional allocation
- **Parsed TypeIDs**: Single map allocation with 4 keys
- **UUID bytes**: Byte arrays (16 bytes each)
- **Base32 encoding**: Use transients for intermediate buffers to reduce GC pressure

### Hot Path Optimization

Critical functions for performance (targets: < 1μs each):
1. **base32/encode**: Use bit-shifting and pre-allocated lookup tables
2. **base32/decode**: Use lookup map and bit manipulation (no string ops)
3. **uuid/generate-uuidv7**: Minimize allocations; use platform-native time/random sources
4. **Validation**: Short-circuit on first error; regex compiled once

### Caching Strategy

**No caching required**:
- All operations are pure functions
- No expensive computations that benefit from memoization
- Lookup tables (encode alphabet, decode map) are compile-time constants

---

## Open Questions

1. **Optional validation bypass**: Should library provide a `*validate*` dynamic var to disable validation in production for maximum performance? (Trade-off: performance vs safety)
2. **UUID format utilities**: Should library include `uuid->hex-string` and `hex-string->uuid` helper functions for user convenience? (Not required by spec but commonly requested)
3. **Prefix naming conventions**: Should library warn about discouraged patterns (e.g., single-char prefixes) or strictly enforce minimum 3-char recommendation?

---

## References

- [TypeID Specification v0.3.0](../../../typeid.md) - Sections: Type Prefix, UUID Suffix, Base32 Encoding
- [spec.md](spec.md) - Functional requirements FR-001 through FR-020
- [research.md](research.md) - UUIDv7 generation strategy, base32 encoding approach
