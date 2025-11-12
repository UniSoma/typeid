# Data Model: TypeID API Simplification

**Feature**: 002-api-simplification
**Date**: 2025-11-11
**Status**: Complete

## Overview

This document describes the data structures used in the TypeID library API refactoring. Since this is a refactoring, most data structures remain unchanged from the existing implementation. Changes focus on error representation and function signatures.

## Core Entities

### 1. TypeID String

**Description**: External string representation of a TypeID, optionally prefixed.

**Format**: `[prefix_]suffix` where:
- `prefix`: Optional, 0-63 lowercase alphanumeric characters (a-z, 0-9)
- `_`: Separator (present only if prefix is non-empty)
- `suffix`: 26-character base32-encoded UUID (lowercase, excludes i, l, o, u for readability)

**Examples**:
```clojure
"user_01h455vb4pex5vsknk084sn02q"    ; With prefix
"01h455vb4pex5vsknk084sn02q"         ; Without prefix (no separator)
```

**Validation rules** (unchanged from original):
- Total length: 26-89 characters (26 for suffix-only, 1-63 for prefix + 1 separator + 26 for suffix)
- Prefix characters: Only lowercase a-z and 0-9
- Suffix characters: Only lowercase base32 alphabet (excludes i, l, o, u)
- Suffix length: Exactly 26 characters

**Validation predicate**:
```clojure
(defn valid-typeid-string? [s]
  (and (string? s)
       (or (valid-suffix? s)           ; Prefix-less TypeID
           (and (valid-prefixed? s)    ; Prefixed TypeID
                (has-valid-separator? s)))))
```

---

### 2. TypeID Components Map

**Description**: Decomposed representation of a TypeID with all constituent parts.

**Structure**:
```clojure
{:prefix   "user"                                           ; String (empty string if no prefix)
 :suffix   "01h455vb4pex5vsknk084sn02q"                    ; String (26 chars, base32)
 :uuid     #uuid "018c3f9e-9e4e-7a8a-8b2a-7e8e9e4e7a8a"    ; Platform-native UUID
 :typeid   "user_01h455vb4pex5vsknk084sn02q"}              ; String (full TypeID)
```

**Field specifications**:
- `:prefix` - String, lowercase alphanumeric, 0-63 characters (empty string if none)
- `:suffix` - String, base32-encoded UUID, exactly 26 characters
- `:uuid` - Platform-native UUID object (`java.util.UUID` on JVM, `cljs.core/UUID` on ClojureScript)
- `:typeid` - String, full TypeID representation (reconstructed from prefix + suffix)

**Validation predicate**:
```clojure
(defn valid-components-map? [m]
  (and (map? m)
       (string? (:prefix m))
       (valid-prefix? (:prefix m))
       (string? (:suffix m))
       (= 26 (count (:suffix m)))
       (uuid? (:uuid m))
       (string? (:typeid m))
       (valid-typeid-string? (:typeid m))))
```

**Usage**:
- Returned by `typeid.core/parse` for valid TypeID strings
- Used internally for decomposition/composition operations

---

### 3. Error Map

**Description**: Structured error information for validation failures and exceptions.

**Structure**:
```clojure
{:type     :typeid/invalid-prefix           ; Keyword error category
 :message  "Invalid prefix: contains ..."   ; Human-readable description
 :input    "User_123"                        ; Original input that caused error
 :expected "lowercase alphanumeric"          ; Expected format/value (optional)
 :actual   "uppercase characters"}           ; Actual problematic value (optional)
```

**Field specifications**:
- `:type` - Keyword identifying error category (see Error Types below)
- `:message` - String, human-readable error description
- `:input` - Any type, the original input that caused the error
- `:expected` - String (optional), description of expected format/value
- `:actual` - String (optional), description of actual problematic value

**Error Types**:

| Type | Description | Context Fields |
|------|-------------|----------------|
| `:typeid/invalid-prefix` | Prefix violates TypeID spec | `:input`, `:actual`, `:expected` |
| `:typeid/invalid-suffix` | Suffix is malformed or wrong length | `:input`, `:actual`, `:expected` |
| `:typeid/invalid-format` | Overall format doesn't match pattern | `:input`, `:expected` |
| `:typeid/invalid-input-type` | Non-string input to string-expecting function | `:input`, `:actual` (type), `:expected` (type) |
| `:typeid/invalid-uuid` | UUID format is invalid | `:input` |
| `:typeid/invalid-separator` | Missing or misplaced separator | `:input`, `:expected` |
| `:typeid/invalid-length` | String length violates constraints | `:input`, `:actual` (length), `:expected` (range) |

**Validation predicate**:
```clojure
(defn valid-error-map? [m]
  (and (map? m)
       (keyword? (:type m))
       (namespace (:type m) "typeid")
       (string? (:message m))
       (seq (:message m))
       (contains? m :input)))
```

**Usage contexts**:
1. **Returned by `explain`**: When TypeID is invalid
2. **Thrown via `ex-info`**: In `parse`, `create`, and codec functions
3. **Logged/displayed**: For debugging and user feedback

**Examples**:

```clojure
;; Invalid prefix (uppercase)
{:type :typeid/invalid-prefix
 :message "Invalid prefix: contains uppercase characters"
 :input "User_01h455vb4pex5vsknk084sn02q"
 :expected "lowercase alphanumeric characters (a-z, 0-9)"
 :actual "uppercase 'U'"}

;; Invalid suffix (wrong length)
{:type :typeid/invalid-suffix
 :message "Invalid suffix: wrong length"
 :input "user_01h455vb4pex"
 :expected "26 characters"
 :actual "14 characters"}

;; Invalid input type (explain function)
{:type :typeid/invalid-input-type
 :message "Invalid input type: expected string"
 :input 12345
 :expected "string"
 :actual "number"}

;; Invalid UUID format (create function)
{:type :typeid/invalid-uuid
 :message "Invalid UUID: malformed hex string"
 :input "not-a-uuid"}
```

---

### 4. UUID

**Description**: Platform-native universally unique identifier (128-bit value).

**Platform representations**:
- **JVM**: `java.util.UUID` object
- **ClojureScript**: `cljs.core/UUID` object (via `#uuid` reader literal)

**Conversion to bytes** (internal operation):
- **JVM**: Extract via `(.getMostSignificantBits uuid)` and `(.getLeastSignificantBits uuid)`
- **ClojureScript**: Access UUID bytes directly from object representation

**Version agnostic**:
- Accepts UUIDv1, UUIDv4, UUIDv7, or any valid UUID format
- No validation of UUID version or timestamp structure
- Edge cases accepted: all-zeros, all-ones, non-standard variants

**Usage**:
- Input to two-arity `create` function
- Output field in TypeID components map
- Internal representation for encoding/decoding

**Example values**:
```clojure
#uuid "018c3f9e-9e4e-7a8a-8b2a-7e8e9e4e7a8a"  ; UUIDv7 (time-ordered)
#uuid "550e8400-e29b-41d4-a716-446655440000"  ; UUIDv4 (random)
#uuid "00000000-0000-0000-0000-000000000000"  ; All zeros (edge case)
```

---

### 5. Prefix

**Description**: Optional type identifier prepended to TypeID strings.

**Type**: String, keyword, or nil

**Accepted formats**:
- **String**: `"user"`, `"org"`, `""` (empty string for prefix-less)
- **Keyword**: `:user`, `:org` (name extracted via `(name prefix)`)
- **Nil**: Treated as empty prefix (generates prefix-less TypeID)

**Validation rules**:
- Length: 0-63 characters
- Characters: Only lowercase a-z and 0-9
- No underscores or other special characters
- No uppercase letters

**Validation predicate**:
```clojure
(defn valid-prefix? [prefix]
  (or (nil? prefix)
      (and (string? prefix)
           (<= 0 (count prefix) 63)
           (every? #(or (Character/isLowerCase %)
                        (Character/isDigit %))
                   prefix))
      (keyword? prefix)))
```

**Normalization** (internal operation):
```clojure
(defn normalize-prefix [prefix]
  (cond
    (nil? prefix)     ""
    (keyword? prefix) (name prefix)
    (string? prefix)  prefix
    :else             (throw-invalid-prefix prefix)))
```

---

## Data Transformations

### TypeID String → Components Map

**Function**: `typeid.core/parse`

**Input**: TypeID string
**Output**: Components map (or throws exception)

**Transformation steps**:
1. Validate input is string (throw if not)
2. Split by `_` separator (if present)
3. Validate prefix (throw if invalid)
4. Validate suffix (throw if invalid)
5. Decode suffix to UUID bytes
6. Construct components map

**Example**:
```clojure
(parse "user_01h455vb4pex5vsknk084sn02q")
;; => {:prefix "user"
;;     :suffix "01h455vb4pex5vsknk084sn02q"
;;     :uuid #uuid "018c3f9e-9e4e-7a8a-8b2a-7e8e9e4e7a8a"
;;     :typeid "user_01h455vb4pex5vsknk084sn02q"}
```

---

### Prefix + UUID → TypeID String

**Function**: `typeid.core/create` (two-arity)

**Input**: Prefix (string/keyword/nil), UUID (platform-native)
**Output**: TypeID string (or throws exception)

**Transformation steps**:
1. Normalize prefix (keyword → string, nil → "")
2. Validate prefix (throw if invalid)
3. Validate UUID type (throw if not platform-native UUID)
4. Convert UUID to bytes
5. Encode bytes to base32 suffix
6. Combine prefix + separator + suffix (or just suffix if no prefix)

**Example**:
```clojure
(create "user" #uuid "018c3f9e-9e4e-7a8a-8b2a-7e8e9e4e7a8a")
;; => "user_01h455vb4pex5vsknk084sn02q"

(create nil #uuid "018c3f9e-9e4e-7a8a-8b2a-7e8e9e4e7a8a")
;; => "01h455vb4pex5vsknk084sn02q"

(create :org #uuid "018c3f9e-9e4e-7a8a-8b2a-7e8e9e4e7a8a")
;; => "org_01h455vb4pex5vsknk084sn02q"
```

---

### TypeID String → Validation Result

**Function**: `typeid.core/explain`

**Input**: Any value (typically string)
**Output**: `nil` (valid) or error map (invalid)

**Transformation steps**:
1. Check input type (return error map if not string)
2. Validate overall format (return error map if invalid)
3. Split by separator and validate parts (return error map on failure)
4. Return `nil` if all validations pass

**Example**:
```clojure
(explain "user_01h455vb4pex5vsknk084sn02q")
;; => nil  ; Valid

(explain "User_01h455vb4pex5vsknk084sn02q")
;; => {:type :typeid/invalid-prefix
;;     :message "Invalid prefix: contains uppercase characters"
;;     :input "User_01h455vb4pex5vsknk084sn02q"
;;     :expected "lowercase alphanumeric characters (a-z, 0-9)"
;;     :actual "uppercase 'U'"}

(explain 12345)
;; => {:type :typeid/invalid-input-type
;;     :message "Invalid input type: expected string"
;;     :input 12345
;;     :expected "string"
;;     :actual "number"}
```

---

## State Transitions

This refactoring introduces no stateful entities. All functions are pure transformations:

```
                              parse
TypeID String ───────────────────────────────────> Components Map
      ↑                                                    ↓
      │                                                    │
      │ create (2-arity)                                   │
      │                                                    │
Prefix + UUID                                       Decomposed for
                                                    application use

      explain
TypeID String ───────────────────────────────────> nil | Error Map
Any Input                                          (validation result)
```

## Validation Summary

### Manual Validation Predicates

Per constitution requirement (Principle IV), all validation uses manual predicate functions:

```clojure
;; String type validation
(defn string-input? [x] (string? x))

;; Prefix validation
(defn valid-prefix-chars? [s]
  (every? #(or (Character/isLowerCase %)
               (Character/isDigit %))
          s))

(defn valid-prefix-length? [s]
  (<= 0 (count s) 63))

(defn valid-prefix? [s]
  (and (valid-prefix-length? s)
       (valid-prefix-chars? s)))

;; Suffix validation
(defn valid-suffix-length? [s]
  (= 26 (count s)))

(defn valid-base32-chars? [s]
  (every? #(contains? base32-alphabet %) s))

(defn valid-suffix? [s]
  (and (valid-suffix-length? s)
       (valid-base32-chars? s)))

;; TypeID format validation
(defn valid-typeid-format? [s]
  (or (valid-suffix? s)                           ; No prefix
      (and (str/includes? s "_")                  ; Has separator
           (let [[prefix suffix] (str/split s #"_" 2)]
             (and (valid-prefix? prefix)
                  (valid-suffix? suffix))))))

;; UUID type validation (platform-specific)
(defn uuid? [x]
  #?(:clj (instance? java.util.UUID x)
     :cljs (instance? cljs.core/UUID x)))

;; Error map validation
(defn valid-error-type? [t]
  (and (keyword? t)
       (= "typeid" (namespace t))))

(defn valid-error-map? [m]
  (and (map? m)
       (valid-error-type? (:type m))
       (string? (:message m))
       (seq (:message m))
       (contains? m :input)))
```

All predicates are pure functions with no external dependencies, maintaining the constitution's zero runtime dependency requirement.

## Schema Documentation

While we don't use runtime schema libraries (per constitution), we document expected shapes:

### Function Input/Output Schemas

```clojure
;; typeid.core/explain
;; Input:  any
;; Output: nil | ErrorMap

;; typeid.core/parse
;; Input:  string
;; Output: ComponentsMap
;; Throws: ExceptionInfo with ErrorMap data

;; typeid.core/create (1-arity)
;; Input:  nil | string | keyword
;; Output: string (TypeID)
;; Throws: ExceptionInfo with ErrorMap data

;; typeid.core/create (2-arity)
;; Input:  (nil | string | keyword) × UUID
;; Output: string (TypeID)
;; Throws: ExceptionInfo with ErrorMap data

;; typeid.core/generate
;; Input:  nil | string | keyword
;; Output: string (TypeID)

;; typeid.codec/encode
;; Input:  bytes × (nil | string | keyword)
;; Output: string (TypeID)

;; typeid.codec/decode
;; Input:  string (TypeID)
;; Output: bytes (UUID)

;; typeid.codec/uuid->hex
;; Input:  bytes (UUID)
;; Output: string (32-char hex)

;; typeid.codec/hex->uuid
;; Input:  string (32-char hex)
;; Output: bytes (UUID)
```

## Performance Considerations

**Data structure overhead**:
- TypeID strings: 26-89 bytes (minimal memory footprint)
- Components maps: ~200 bytes (4 string pointers + 1 UUID object)
- Error maps: ~300 bytes (keyword + 2-4 strings + context)

**Allocation patterns** (no changes from original):
- Encoding: Allocates suffix string (26 chars) + full TypeID string
- Decoding: Allocates components map (4 fields) + UUID object
- Validation: Allocates error map only on failure (fast path = nil)

**Optimization notes**:
- `explain` fast path (valid input): No allocation (returns nil)
- `parse` does not pre-validate with `explain` (avoids double parsing)
- Prefix normalization uses string interning where possible

All performance goals maintained: <1μs encoding/decoding on typical hardware.
