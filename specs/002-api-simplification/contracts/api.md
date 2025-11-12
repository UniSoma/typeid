# API Contract: TypeID Library

**Feature**: 002-api-simplification
**Date**: 2025-11-11
**Version**: 1.0.0 (Initial release)

## Overview

This document defines the public API contract for the TypeID Clojure library after the simplification refactoring. This is a library API (not REST/GraphQL), so contracts are specified as function signatures with pre/post-conditions.

## Namespaces

### typeid.core

Primary public API for TypeID operations.

**Functions**: `generate`, `create`, `explain`, `parse`

---

### typeid.codec

Low-level codec operations for advanced use cases.

**Functions**: `encode`, `decode`, `uuid->hex`, `hex->uuid`

---

## API Contracts

### typeid.core/generate

Generate a new TypeID with a UUIDv7 timestamp.

**Signature**:
```clojure
(generate)
(generate prefix)
```

**Parameters**:
- `prefix` (optional): `nil` | `string` | `keyword` - Type identifier (0-63 lowercase alphanumeric chars)

**Returns**: `string` - Generated TypeID

**Throws**: `ExceptionInfo` if prefix is invalid

**Preconditions**:
- If `prefix` is string: Must be 0-63 lowercase alphanumeric characters
- If `prefix` is keyword: Name must be 0-63 lowercase alphanumeric characters
- If `prefix` is nil or omitted: Generates prefix-less TypeID

**Postconditions**:
- Returns valid TypeID string conforming to spec v0.3.0
- Embedded UUID is valid UUIDv7 with monotonic timestamp
- If prefix provided: TypeID starts with `prefix_`
- If no prefix: TypeID has no separator

**Examples**:
```clojure
(generate)
;; => "01h455vb4pex5vsknk084sn02q"

(generate "user")
;; => "user_01h455vb4pex5vsknk084sn02q"

(generate :org)
;; => "org_01h455vb4pex5vsknk084sn02q"

(generate nil)
;; => "01h455vb4pex5vsknk084sn02q"

;; Invalid prefix
(generate "User")
;; => ExceptionInfo: {:type :typeid/invalid-prefix, :message "...", ...}
```

**Error conditions**:
| Condition | Error Type | Example |
|-----------|------------|---------|
| Prefix contains uppercase | `:typeid/invalid-prefix` | `"User"` |
| Prefix too long (>63 chars) | `:typeid/invalid-prefix` | `"a" × 64` |
| Prefix contains invalid chars | `:typeid/invalid-prefix` | `"user-123"` |

---

### typeid.core/create

Create a TypeID from components (prefix + optional existing UUID).

**Signatures**:
```clojure
(create prefix)
(create prefix uuid)
```

**Parameters**:
- `prefix` (optional in 1-arity): `nil` | `string` | `keyword` - Type identifier
- `uuid` (2-arity only): `java.util.UUID` (JVM) | `cljs.core/UUID` (ClojureScript) - Existing UUID to encode

**Returns**: `string` - TypeID encoded from components

**Throws**: `ExceptionInfo` if prefix or UUID is invalid

**Preconditions**:
- 1-arity: Same as `generate` (creates new UUIDv7)
- 2-arity: `uuid` must be platform-native UUID object (any version accepted)

**Postconditions**:
- Returns valid TypeID string
- 1-arity: Equivalent to `(generate prefix)`
- 2-arity: TypeID decodes back to the provided UUID

**Examples**:
```clojure
;; 1-arity (equivalent to generate)
(create "user")
;; => "user_01h455vb4pex5vsknk084sn02q"

;; 2-arity (from existing UUID)
(create "user" #uuid "018c3f9e-9e4e-7a8a-8b2a-7e8e9e4e7a8a")
;; => "user_01h455vb4pex5vsknk084sn02q"

(create nil #uuid "018c3f9e-9e4e-7a8a-8b2a-7e8e9e4e7a8a")
;; => "01h455vb4pex5vsknk084sn02q"

(create :org #uuid "550e8400-e29b-41d4-a716-446655440000")
;; => "org_2qeh85amd9ct4vr9px628gkdkr"  ; UUIDv4 accepted

;; Edge case: all-zeros UUID
(create "test" #uuid "00000000-0000-0000-0000-000000000000")
;; => "test_00000000000000000000000000"

;; Invalid UUID type
(create "user" "not-a-uuid")
;; => ExceptionInfo: {:type :typeid/invalid-uuid, ...}
```

**Error conditions**:
| Condition | Error Type | Example |
|-----------|------------|---------|
| Invalid prefix | `:typeid/invalid-prefix` | `"User"`, `"a" × 64` |
| Invalid UUID type | `:typeid/invalid-uuid` | `"string"`, `12345` |

---

### typeid.core/explain

Validate a TypeID and explain errors if invalid.

**Signature**:
```clojure
(explain input)
```

**Parameters**:
- `input`: `any` - Value to validate (typically string)

**Returns**:
- `nil` if `input` is a valid TypeID string
- `map` (error map) if invalid or non-string

**Throws**: Never throws (returns error map instead)

**Preconditions**: None (accepts any input)

**Postconditions**:
- Returns `nil` ⟺ input is valid TypeID string
- Returns error map ⟺ input is invalid or non-string
- Error map always includes `:type`, `:message`, `:input`

**Examples**:
```clojure
(explain "user_01h455vb4pex5vsknk084sn02q")
;; => nil  ; Valid

(explain "01h455vb4pex5vsknk084sn02q")
;; => nil  ; Valid (no prefix)

(explain "User_01h455vb4pex5vsknk084sn02q")
;; => {:type :typeid/invalid-prefix
;;     :message "Invalid prefix: contains uppercase characters"
;;     :input "User_01h455vb4pex5vsknk084sn02q"
;;     :expected "lowercase alphanumeric characters (a-z, 0-9)"
;;     :actual "uppercase 'U'"}

(explain "user_tooshort")
;; => {:type :typeid/invalid-suffix
;;     :message "Invalid suffix: wrong length"
;;     :input "user_tooshort"
;;     :expected "26 characters"
;;     :actual "8 characters"}

(explain 12345)
;; => {:type :typeid/invalid-input-type
;;     :message "Invalid input type: expected string"
;;     :input 12345
;;     :expected "string"
;;     :actual "number"}

(explain nil)
;; => {:type :typeid/invalid-input-type
;;     :message "Invalid input type: expected string"
;;     :input nil
;;     :expected "string"
;;     :actual "nil"}
```

**Error types returned**:
| Error Type | Condition |
|------------|-----------|
| `:typeid/invalid-input-type` | Input is not a string |
| `:typeid/invalid-format` | Overall format doesn't match TypeID pattern |
| `:typeid/invalid-prefix` | Prefix violates constraints |
| `:typeid/invalid-suffix` | Suffix is malformed or wrong length |
| `:typeid/invalid-separator` | Missing or misplaced `_` separator |
| `:typeid/invalid-length` | String length out of valid range |

---

### typeid.core/parse

Parse a TypeID string into components.

**Signature**:
```clojure
(parse typeid-str)
```

**Parameters**:
- `typeid-str`: `string` - TypeID to parse

**Returns**: `map` - Components map with keys:
- `:prefix` - `string` (empty if no prefix)
- `:suffix` - `string` (26 chars)
- `:uuid` - Platform-native UUID object
- `:typeid` - `string` (full TypeID, same as input)

**Throws**: `ExceptionInfo` if input is invalid

**Preconditions**:
- `typeid-str` must be a valid TypeID string

**Postconditions**:
- Returns components map with all 4 keys
- `:uuid` is platform-native UUID object
- Reconstructing TypeID from components yields original input

**Examples**:
```clojure
(parse "user_01h455vb4pex5vsknk084sn02q")
;; => {:prefix "user"
;;     :suffix "01h455vb4pex5vsknk084sn02q"
;;     :uuid #uuid "018c3f9e-9e4e-7a8a-8b2a-7e8e9e4e7a8a"
;;     :typeid "user_01h455vb4pex5vsknk084sn02q"}

(parse "01h455vb4pex5vsknk084sn02q")
;; => {:prefix ""
;;     :suffix "01h455vb4pex5vsknk084sn02q"
;;     :uuid #uuid "018c3f9e-9e4e-7a8a-8b2a-7e8e9e4e7a8a"
;;     :typeid "01h455vb4pex5vsknk084sn02q"}

;; Invalid TypeID
(parse "User_01h455vb4pex5vsknk084sn02q")
;; => ExceptionInfo: {:type :typeid/invalid-prefix, :message "...", ...}

(parse "not-a-typeid")
;; => ExceptionInfo: {:type :typeid/invalid-format, :message "...", ...}

(parse 12345)
;; => ExceptionInfo: {:type :typeid/invalid-input-type, :message "...", ...}
```

**Error conditions**: Same as `explain`, but thrown as `ExceptionInfo` instead of returned

**Relationship to `explain`**:
```clojure
;; These are equivalent error detection patterns:

;; Pattern 1: Using explain (no exception)
(if-let [error (explain input)]
  (handle-error error)
  (use-valid-typeid input))

;; Pattern 2: Using parse (exception-based)
(try
  (let [components (parse input)]
    (use-components components))
  (catch Exception e
    (handle-error (ex-data e))))
```

---

### typeid.codec/encode

Encode UUID bytes and prefix into TypeID string.

**Signature**:
```clojure
(encode uuid-bytes prefix)
```

**Parameters**:
- `uuid-bytes`: `bytes` (16-byte array) - UUID as byte array
- `prefix`: `nil` | `string` | `keyword` - Type identifier

**Returns**: `string` - TypeID string

**Throws**: `ExceptionInfo` if inputs are invalid

**Preconditions**:
- `uuid-bytes` must be byte array of length 16
- `prefix` must be valid (0-63 lowercase alphanumeric) or nil

**Postconditions**:
- Returns valid TypeID string
- Decoding result yields original `uuid-bytes`

**Examples**:
```clojure
(require '[typeid.codec :as codec])

(def uuid-bytes (byte-array [0x01 0x8c 0x3f 0x9e 0x9e 0x4e 0x7a 0x8a
                              0x8b 0x2a 0x7e 0x8e 0x9e 0x4e 0x7a 0x8a]))

(codec/encode uuid-bytes "user")
;; => "user_01h455vb4pex5vsknk084sn02q"

(codec/encode uuid-bytes nil)
;; => "01h455vb4pex5vsknk084sn02q"

;; Invalid byte array length
(codec/encode (byte-array 10) "user")
;; => ExceptionInfo: {:type :typeid/invalid-uuid, ...}
```

**Use cases**:
- Custom UUID generation logic
- Integration with systems that work with raw UUID bytes
- Testing with specific UUID values

---

### typeid.codec/decode

Decode TypeID string into UUID bytes.

**Signature**:
```clojure
(decode typeid-str)
```

**Parameters**:
- `typeid-str`: `string` - TypeID to decode

**Returns**: `bytes` (16-byte array) - UUID as byte array

**Throws**: `ExceptionInfo` if input is invalid

**Preconditions**:
- `typeid-str` must be valid TypeID string

**Postconditions**:
- Returns 16-byte array
- Encoding result yields original `typeid-str` (modulo prefix)

**Examples**:
```clojure
(require '[typeid.codec :as codec])

(codec/decode "user_01h455vb4pex5vsknk084sn02q")
;; => #object["[B" ... (16-byte array)]

(codec/decode "01h455vb4pex5vsknk084sn02q")
;; => #object["[B" ... (same 16-byte array)]

;; Invalid TypeID
(codec/decode "invalid")
;; => ExceptionInfo: {:type :typeid/invalid-format, ...}
```

**Use cases**:
- Extracting raw UUID bytes for database storage
- Integration with systems that expect byte arrays
- Custom UUID processing logic

---

### typeid.codec/uuid->hex

Convert UUID bytes to hexadecimal string.

**Signature**:
```clojure
(uuid->hex uuid-bytes)
```

**Parameters**:
- `uuid-bytes`: `bytes` (16-byte array) - UUID as byte array

**Returns**: `string` - 32-character lowercase hexadecimal string (no hyphens)

**Throws**: `ExceptionInfo` if input is invalid

**Preconditions**:
- `uuid-bytes` must be byte array of length 16

**Postconditions**:
- Returns 32-character string
- String contains only lowercase hex digits (0-9, a-f)
- No hyphens or formatting

**Examples**:
```clojure
(require '[typeid.codec :as codec])

(def uuid-bytes (byte-array [0x01 0x8c 0x3f 0x9e 0x9e 0x4e 0x7a 0x8a
                              0x8b 0x2a 0x7e 0x8e 0x9e 0x4e 0x7a 0x8a]))

(codec/uuid->hex uuid-bytes)
;; => "018c3f9e9e4e7a8a8b2a7e8e9e4e7a8a"

;; Invalid input
(codec/uuid->hex (byte-array 10))
;; => ExceptionInfo: {:type :typeid/invalid-uuid, ...}
```

**Use cases**:
- Logging/debugging (human-readable UUID format)
- Integration with systems that expect hex UUIDs
- Testing UUID encoding correctness

---

### typeid.codec/hex->uuid

Convert hexadecimal string to UUID bytes.

**Signature**:
```clojure
(hex->uuid hex-str)
```

**Parameters**:
- `hex-str`: `string` - 32-character hexadecimal string (with or without hyphens)

**Returns**: `bytes` (16-byte array) - UUID as byte array

**Throws**: `ExceptionInfo` if input is invalid

**Preconditions**:
- `hex-str` must be valid hex string (32 or 36 chars with hyphens)
- Characters must be valid hex digits (0-9, a-f, A-F)

**Postconditions**:
- Returns 16-byte array
- Round-trip: `(uuid->hex (hex->uuid x))` ≡ `(str/lower-case (str/replace x "-" ""))`

**Examples**:
```clojure
(require '[typeid.codec :as codec])

(codec/hex->uuid "018c3f9e9e4e7a8a8b2a7e8e9e4e7a8a")
;; => #object["[B" ... (16-byte array)]

;; With hyphens (UUID standard format)
(codec/hex->uuid "018c3f9e-9e4e-7a8a-8b2a-7e8e9e4e7a8a")
;; => #object["[B" ... (same 16-byte array)]

;; Uppercase accepted
(codec/hex->uuid "018C3F9E9E4E7A8A8B2A7E8E9E4E7A8A")
;; => #object["[B" ... (same 16-byte array)]

;; Invalid hex
(codec/hex->uuid "not-hex")
;; => ExceptionInfo: {:type :typeid/invalid-uuid, ...}

;; Wrong length
(codec/hex->uuid "018c3f9e")
;; => ExceptionInfo: {:type :typeid/invalid-uuid, ...}
```

**Use cases**:
- Parsing UUIDs from logs or external systems
- Testing with specific UUID values
- Integration with hex-based UUID systems

---

## Error Handling Contract

### Exception Structure

All functions that throw exceptions use `clojure.core/ex-info` (Clojure) or equivalent (ClojureScript) with structured data.

**Standard exception data map**:
```clojure
{:type     keyword     ; Namespaced keyword (e.g., :typeid/invalid-prefix)
 :message  string      ; Human-readable error description
 :input    any         ; Original input that caused error
 :expected string      ; Expected format/value (optional)
 :actual   string}     ; Actual problematic value (optional)
```

**Accessing exception data**:
```clojure
(try
  (parse "invalid-typeid")
  (catch Exception e
    (let [data (ex-data e)]
      (println "Error type:" (:type data))
      (println "Message:" (:message data))
      (println "Input:" (:input data)))))
```

### Error Type Hierarchy

All error types use `:typeid` namespace:

```
:typeid/invalid-input-type    ; Non-string where string expected
:typeid/invalid-format        ; Overall format doesn't match pattern
:typeid/invalid-prefix        ; Prefix constraint violation
:typeid/invalid-suffix        ; Suffix constraint violation
:typeid/invalid-separator     ; Missing or misplaced separator
:typeid/invalid-length        ; Length out of valid range
:typeid/invalid-uuid          ; UUID format/type invalid
```

### Error Messages

Error messages MUST be:
- **Human-readable**: Clear explanation of what went wrong
- **Actionable**: Indicate what's expected or how to fix
- **Consistent**: Follow standard format across all functions

**Message format**: `"Invalid {component}: {specific problem}"`

**Examples**:
- `"Invalid prefix: contains uppercase characters"`
- `"Invalid suffix: wrong length"`
- `"Invalid input type: expected string"`
- `"Invalid UUID: malformed hex string"`

---

## Contract Verification

### Property-Based Tests

All contracts are verified using `test.check` property-based tests:

**Invariants**:
1. **Round-trip encoding**: `(= uuid (parse (create prefix uuid)))`
2. **Round-trip decoding**: `(= typeid (encode (decode typeid) prefix))`
3. **Validation consistency**: `(nil? (explain x))` ⟺ `(not (throws? (parse x)))`
4. **Prefix normalization**: `(= (create "p" u) (create :p u))`
5. **UUID version agnostic**: `(valid? (create "p" uuid-v1))` ∧ `(valid? (create "p" uuid-v4))` ∧ `(valid? (create "p" uuid-v7))`

### Contract Tests (examples)

```clojure
(require '[clojure.test.check :as tc]
         '[clojure.test.check.generators :as gen]
         '[clojure.test.check.properties :as prop])

;; Round-trip property
(def round-trip-prop
  (prop/for-all [prefix (gen/elements ["user" "org" nil])
                 uuid gen-uuid]
    (let [typeid (create prefix uuid)
          parsed (parse typeid)]
      (= uuid (:uuid parsed)))))

;; Validation consistency property
(def validation-consistency-prop
  (prop/for-all [typeid gen-typeid]
    (= (nil? (explain typeid))
       (try (parse typeid) true
            (catch Exception _ false)))))

;; Prefix normalization property
(def prefix-normalization-prop
  (prop/for-all [prefix gen-valid-prefix
                 uuid gen-uuid]
    (= (create (str prefix) uuid)
       (create (keyword prefix) uuid))))
```

---

## Performance Contract

### Performance Budgets

Per constitution and feature spec, the following performance budgets apply:

| Operation | Budget (typical hardware) |
|-----------|---------------------------|
| `encode` | < 1μs per TypeID |
| `decode` | < 1μs per TypeID |
| `explain` (valid input) | < 500ns (fast path, returns nil) |
| `explain` (invalid input) | < 2μs (error map construction) |
| `parse` | < 1.5μs (includes decode + map construction) |
| `create` (2-arity) | < 1μs (reuses encode logic) |
| `generate` | < 5μs (includes UUIDv7 generation) |
| `uuid->hex` | < 500ns (simple hex formatting) |
| `hex->uuid` | < 500ns (hex parsing) |

### Memory Allocation

| Operation | Allocation |
|-----------|------------|
| `generate` / `create` | ~100 bytes (suffix string + full TypeID string) |
| `parse` | ~200 bytes (components map + strings) |
| `explain` (valid) | 0 bytes (returns nil) |
| `explain` (invalid) | ~300 bytes (error map + strings) |
| codec operations | ~50-100 bytes (strings or byte arrays) |

---

## Testing Requirements

Per constitution (Principle VII), all public functions MUST have:

1. **Unit tests**: Basic functionality coverage
2. **Property-based tests**: Generative tests with test.check
3. **Edge case tests**: Boundary conditions, invalid inputs
4. **Error tests**: Exception throwing and error map structure
5. **Cross-platform tests**: Verify behavior on JVM and ClojureScript

**Coverage goals**:
- Overall: >80%
- Critical paths (encode, decode, parse): 100%

---

## Documentation Requirements

Per constitution (Principle II), all public functions MUST have:

1. **Docstrings**: Purpose, parameters, return value, exceptions, examples
2. **README examples**: All major use cases covered
3. **API reference**: Generated via Codox

**Example docstring format**:
```clojure
(defn parse
  "Parse a TypeID string into its components.

  Takes a TypeID string and returns a map with decomposed parts:
  - :prefix (string, empty if no prefix)
  - :suffix (26-char base32 string)
  - :uuid (platform-native UUID object)
  - :typeid (full TypeID string)

  Throws ExceptionInfo with structured error data if the input is invalid.

  Examples:
    (parse \"user_01h455vb4pex5vsknk084sn02q\")
    ;; => {:prefix \"user\", :suffix \"...\", :uuid #uuid \"...\", :typeid \"...\"}

    (parse \"01h455vb4pex5vsknk084sn02q\")
    ;; => {:prefix \"\", :suffix \"...\", :uuid #uuid \"...\", :typeid \"...\"}

    (parse \"invalid\")
    ;; => ExceptionInfo: {:type :typeid/invalid-format, ...}

  See also: explain (for validation without exceptions)"
  [typeid-str]
  ...)
```

---

## Version History

**v1.0.0** (2025-11-11):
- Initial release
- Core API: `generate`, `create`, `explain`, `parse` in `typeid.core`
- Codec API: `encode`, `decode`, `uuid->hex`, `hex->uuid` in `typeid.codec`
- Exception-based error handling with structured `ex-info` data
- Cross-platform support: JVM (Clojure 1.11+) and ClojureScript
- Zero runtime dependencies
