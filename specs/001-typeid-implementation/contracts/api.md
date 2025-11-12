# API Contracts: TypeID Public Functions

**Feature**: TypeID Clojure/ClojureScript Library
**Date**: 2025-11-10
**Purpose**: Define the public API surface, function signatures, input/output contracts, and behavior specifications

## Overview

This document specifies the public API of the TypeID library exposed via the `typeid.core` namespace. All functions are pure, accept data as input, and return data as output (no side effects). Functions validate inputs using Malli schemas and return either successful results or structured error maps.

## Namespace: `typeid.core`

All public functions reside in the `typeid.core` namespace. Implementation details are in `typeid.impl.*` namespaces and are not part of the public API.

---

## Function: `generate`

**Purpose**: Generate a new TypeID with a UUIDv7 timestamp and the specified prefix.

**Signature**:
```clojure
(generate prefix)
```

**Parameters**:
| Name | Type | Constraints | Description |
|------|------|-------------|-------------|
| `prefix` | `string` | 0-63 chars, pattern `^([a-z]([a-z_]{0,61}[a-z])?)?$` | Type identifier (empty string for prefix-less TypeID) |

**Returns**:
| Success | Failure |
|---------|---------|
| `{:ok typeid-string}` | `{:error error-map}` |

**Return Types**:
- **Success**: `typeid-string` is a valid TypeID string (26-90 characters)
- **Failure**: `error-map` contains `:type`, `:message`, `:data` keys

**Behavior**:
1. Validate `prefix` using hand-written validation predicates
2. Generate UUIDv7 with current timestamp + random bits
3. Set version bits (48-51) to `0111` and variant bits (64-65) to `10`
4. Encode UUID to 26-character base32 suffix
5. Combine prefix + separator (if prefix non-empty) + suffix
6. Return `{:ok typeid-string}`

**Error Conditions**:
| Error Type | Trigger | Example |
|------------|---------|---------|
| `:invalid-prefix-format` | Prefix contains uppercase, digits, or invalid chars | `prefix = "User123"` |
| `:prefix-too-long` | Prefix exceeds 63 characters | `prefix = (apply str (repeat 64 "a"))` |
| `:prefix-starts-with-underscore` | Prefix begins with `_` | `prefix = "_invalid"` |
| `:prefix-ends-with-underscore` | Prefix ends with `_` | `prefix = "invalid_"` |

**Examples**:
```clojure
(require '[typeid.core :as typeid])

;; Generate TypeID with prefix
(typeid/generate "user")
;;=> {:ok "user_01h5fskfsk4fpeqwnsyz5hj55t"}

;; Generate prefix-less TypeID
(typeid/generate "")
;;=> {:ok "01h5fskfsk4fpeqwnsyz5hj55t"}

;; Error: invalid prefix
(typeid/generate "User")
;;=> {:error {:type :invalid-prefix-format
;;            :message "Prefix must contain only lowercase letters and underscores"
;;            :data {:prefix "User"}}}
```

**User Story Mapping**: US1 (Generate and Parse TypeIDs)

---

## Function: `parse`

**Purpose**: Parse a TypeID string into its components (prefix, suffix, UUID).

**Signature**:
```clojure
(parse typeid-string)
```

**Parameters**:
| Name | Type | Constraints | Description |
|------|------|-------------|-------------|
| `typeid-string` | `string` | 26-90 chars, lowercase | Complete TypeID string to parse |

**Returns**:
| Success | Failure |
|---------|---------|
| `{:ok parsed-map}` | `{:error error-map}` |

**Return Types**:
- **Success**: `parsed-map` contains `:prefix`, `:suffix`, `:uuid`, `:typeid` keys (see data-model.md)
- **Failure**: `error-map` contains `:type`, `:message`, `:data` keys

**Behavior**:
1. Validate string length (26-90 characters)
2. Validate all lowercase
3. Split on last `_` to extract prefix and suffix
4. Validate prefix format (regex match or empty)
5. Validate suffix (26 chars, all base32, first char ≤ `7`)
6. Decode suffix to UUID bytes
7. Return `{:ok {:prefix ... :suffix ... :uuid ... :typeid ...}}`

**Error Conditions**:
| Error Type | Trigger | Example |
|------------|---------|---------|
| `:invalid-length` | String length < 26 or > 90 | `"short"` or very long string |
| `:invalid-case` | Contains uppercase characters | `"User_01h5fskfsk4fpeqwnsyz5hj55t"` |
| `:invalid-prefix-format` | Prefix violates format rules | `"_invalid_01h5fskfsk4fpeqwnsyz5hj55t"` |
| `:invalid-suffix-length` | Suffix not exactly 26 chars | `"user_01h5fskfsk"` |
| `:invalid-base32-char` | Suffix contains non-base32 char | `"user_01h5fskfsk4fpeqwnsyz5hj5il"` (contains `i`, `l`) |
| `:suffix-overflow` | First suffix char > `7` | `"user_8zzzzzzzzzzzzzzzzzzzzzzzzz"` |

**Examples**:
```clojure
;; Parse valid TypeID
(typeid/parse "user_01h5fskfsk4fpeqwnsyz5hj55t")
;;=> {:ok {:prefix "user"
;;         :suffix "01h5fskfsk4fpeqwnsyz5hj55t"
;;         :uuid #bytes[...]
;;         :typeid "user_01h5fskfsk4fpeqwnsyz5hj55t"}}

;; Parse prefix-less TypeID
(typeid/parse "01h5fskfsk4fpeqwnsyz5hj55t")
;;=> {:ok {:prefix ""
;;         :suffix "01h5fskfsk4fpeqwnsyz5hj55t"
;;         :uuid #bytes[...]
;;         :typeid "01h5fskfsk4fpeqwnsyz5hj55t"}}

;; Error: suffix overflow
(typeid/parse "user_8zzzzzzzzzzzzzzzzzzzzzzzzz")
;;=> {:error {:type :suffix-overflow
;;            :message "First character of suffix must be 0-7"
;;            :data {:suffix "8zzzzzzzzzzzzzzzzzzzzzzzzz"
;;                   :first-char \8}}}
```

**User Story Mapping**: US1 (Generate and Parse TypeIDs)

---

## Function: `validate`

**Purpose**: Validate a TypeID string without parsing it (faster check).

**Signature**:
```clojure
(validate typeid-string)
```

**Parameters**:
| Name | Type | Constraints | Description |
|------|------|-------------|-------------|
| `typeid-string` | `string` | Any string | String to validate as TypeID |

**Returns**:
| Success | Failure |
|---------|---------|
| `{:ok true}` | `{:error error-map}` |

**Return Types**:
- **Success**: `{:ok true}` indicates valid TypeID
- **Failure**: `error-map` contains `:type`, `:message`, `:data` keys

**Behavior**:
1. Validate length (26-90)
2. Validate all lowercase
3. Validate prefix format (if present)
4. Validate suffix format (26 chars, base32 alphabet, first ≤ `7`)
5. **Note**: Does NOT decode suffix to UUID (lighter weight than `parse`)

**Error Conditions**: Same as `parse` function

**Examples**:
```clojure
;; Validate correct TypeID
(typeid/validate "user_01h5fskfsk4fpeqwnsyz5hj55t")
;;=> {:ok true}

;; Validate incorrect TypeID
(typeid/validate "User_01h5fskfsk4fpeqwnsyz5hj55t")
;;=> {:error {:type :invalid-case
;;            :message "TypeID must be all lowercase"
;;            :data {:typeid "User_01h5fskfsk4fpeqwnsyz5hj55t"}}}
```

**User Story Mapping**: US2 (Validate TypeIDs)

---

## Function: `encode`

**Purpose**: Encode a UUID (as bytes) with a prefix into a TypeID string.

**Signature**:
```clojure
(encode uuid-bytes prefix)
```

**Parameters**:
| Name | Type | Constraints | Description |
|------|------|-------------|-------------|
| `uuid-bytes` | `bytes` | Exactly 16 bytes | Raw UUID bytes (big-endian) |
| `prefix` | `string` | 0-63 chars, pattern `^([a-z]([a-z_]{0,61}[a-z])?)?$` | Type identifier |

**Returns**:
| Success | Failure |
|---------|---------|
| `{:ok typeid-string}` | `{:error error-map}` |

**Return Types**:
- **Success**: `typeid-string` is a valid TypeID string
- **Failure**: `error-map` contains `:type`, `:message`, `:data` keys

**Behavior**:
1. Validate `uuid-bytes` (must be exactly 16 bytes)
2. Validate `prefix` using validation predicates
3. Encode `uuid-bytes` to 26-character base32 suffix
4. Combine prefix + separator (if prefix non-empty) + suffix
5. Return `{:ok typeid-string}`

**Error Conditions**:
| Error Type | Trigger | Example |
|------------|---------|---------|
| `:invalid-uuid-length` | UUID bytes not 16 bytes | `uuid-bytes = (byte-array 8)` |
| `:invalid-prefix-format` | Prefix violates format rules | `prefix = "User123"` |

**Examples**:
```clojure
;; Encode UUIDv7 bytes
(def uuid-bytes (byte-array [0x01 0x88 0xe5 0xf5 0xf3 0x4a 0x7b 0x3d
                              0x9f 0x2a 0x1c 0x5d 0xe6 0x7f 0xa8 0xc1]))
(typeid/encode uuid-bytes "user")
;;=> {:ok "user_01h5fskfsk4fpeqwnsyz5hj55t"}

;; Encode UUIDv4 (non-v7 also supported)
(typeid/encode uuid-v4-bytes "session")
;;=> {:ok "session_3d7fa8c11c5d4e6a8f2ae5f5f34a7b3d"}
```

**User Story Mapping**: US3 (Convert Between TypeID and UUID Formats)

---

## Function: `decode`

**Purpose**: Decode a TypeID string to extract the UUID bytes.

**Signature**:
```clojure
(decode typeid-string)
```

**Parameters**:
| Name | Type | Constraints | Description |
|------|------|-------------|-------------|
| `typeid-string` | `string` | 26-90 chars, lowercase | Complete TypeID string |

**Returns**:
| Success | Failure |
|---------|---------|
| `{:ok uuid-bytes}` | `{:error error-map}` |

**Return Types**:
- **Success**: `uuid-bytes` is a 16-byte array (big-endian)
- **Failure**: `error-map` contains `:type`, `:message`, `:data` keys

**Behavior**:
1. Parse TypeID using `parse` function
2. Extract `:uuid` field from parsed result
3. Return `{:ok uuid-bytes}`

**Error Conditions**: Same as `parse` function

**Examples**:
```clojure
;; Decode TypeID to UUID bytes
(typeid/decode "user_01h5fskfsk4fpeqwnsyz5hj55t")
;;=> {:ok #bytes[0x01 0x88 0xe5 0xf5 0xf3 0x4a 0x7b 0x3d
;;               0x9f 0x2a 0x1c 0x5d 0xe6 0x7f 0xa8 0xc1]}

;; Error: invalid TypeID
(typeid/decode "invalid")
;;=> {:error {:type :invalid-suffix-length ...}}
```

**User Story Mapping**: US3 (Convert Between TypeID and UUID Formats)

---

## Function: `uuid->hex`

**Purpose**: Convert UUID bytes to hexadecimal string representation (utility function).

**Signature**:
```clojure
(uuid->hex uuid-bytes)
```

**Parameters**:
| Name | Type | Constraints | Description |
|------|------|-------------|-------------|
| `uuid-bytes` | `bytes` | Exactly 16 bytes | Raw UUID bytes |

**Returns**:
| Success | Failure |
|---------|---------|
| `{:ok hex-string}` | `{:error error-map}` |

**Return Types**:
- **Success**: `hex-string` is a 32-character lowercase hex string
- **Failure**: `error-map` for invalid input

**Behavior**:
1. Validate `uuid-bytes` (16 bytes)
2. Convert each byte to 2-character hex representation
3. Return `{:ok hex-string}`

**Examples**:
```clojure
(def uuid-bytes (byte-array [0x01 0x88 0xe5 0xf5 0xf3 0x4a 0x7b 0x3d
                              0x9f 0x2a 0x1c 0x5d 0xe6 0x7f 0xa8 0xc1]))
(typeid/uuid->hex uuid-bytes)
;;=> {:ok "0188e5f5f34a7b3d9f2a1c5de67fa8c1"}
```

**User Story Mapping**: US3 (Convert Between TypeID and UUID Formats)

---

## Function: `hex->uuid`

**Purpose**: Convert hexadecimal string to UUID bytes (utility function).

**Signature**:
```clojure
(hex->uuid hex-string)
```

**Parameters**:
| Name | Type | Constraints | Description |
|------|------|-------------|-------------|
| `hex-string` | `string` | 32 hex chars `[0-9a-f]` | Hexadecimal UUID representation |

**Returns**:
| Success | Failure |
|---------|---------|
| `{:ok uuid-bytes}` | `{:error error-map}` |

**Return Types**:
- **Success**: `uuid-bytes` is a 16-byte array
- **Failure**: `error-map` for invalid input

**Behavior**:
1. Validate `hex-string` (32 chars, all `[0-9a-f]`)
2. Parse each pair of hex chars as byte
3. Return `{:ok uuid-bytes}`

**Error Conditions**:
| Error Type | Trigger | Example |
|------------|---------|---------|
| `:invalid-hex-length` | String not 32 chars | `"0188e5"` |
| `:invalid-hex-char` | Contains non-hex chars | `"0188e5f5f34a7b3d9f2a1c5de67fa8cz"` |

**Examples**:
```clojure
(typeid/hex->uuid "0188e5f5f34a7b3d9f2a1c5de67fa8c1")
;;=> {:ok #bytes[0x01 0x88 0xe5 0xf5 0xf3 0x4a 0x7b 0x3d
;;               0x9f 0x2a 0x1c 0x5d 0xe6 0x7f 0xa8 0xc1]}
```

**User Story Mapping**: US3 (Convert Between TypeID and UUID Formats)

---

## Function: `typeid->map`

**Purpose**: Convert TypeID string to a map with all components (alias for `parse`, returns map instead of `{:ok ...}` wrapper for REPL convenience).

**Signature**:
```clojure
(typeid->map typeid-string)
```

**Parameters**: Same as `parse`

**Returns**: Parsed map (unwrapped) or throws exception on error

**Note**: This function is provided for REPL/debugging convenience. Production code should use `parse` which returns `{:ok ...}` or `{:error ...}`.

**Examples**:
```clojure
(typeid/typeid->map "user_01h5fskfsk4fpeqwnsyz5hj55t")
;;=> {:prefix "user"
;;    :suffix "01h5fskfsk4fpeqwnsyz5hj55t"
;;    :uuid #bytes[...]
;;    :typeid "user_01h5fskfsk4fpeqwnsyz5hj55t"}
```

---

## Error Handling Convention

All public functions return either `{:ok result}` or `{:error error-map}`. This enables consistent error handling:

```clojure
(let [{:keys [ok error]} (typeid/generate "user")]
  (if ok
    (println "Generated:" ok)
    (println "Error:" (:message error))))
```

**Error Map Structure**:
```clojure
{:type    keyword    ;; Error type (e.g., :invalid-prefix-format)
 :message string     ;; Human-readable error message
 :data    map}       ;; Contextual data (input value, violated rule, etc.)
```

---

## Validation Predicates (Public)

The library exposes validation predicates in `typeid.validation` namespace for users who want to validate data independently:

```clojure
(require '[typeid.validation :as v])

v/valid-prefix?          ;; Check if prefix is valid
v/valid-base32-suffix?   ;; Check if suffix is valid
v/valid-typeid-string?   ;; Check if TypeID string is valid
v/valid-uuid-bytes?      ;; Check if UUID bytes are valid
v/valid-uuidv7-bytes?    ;; Check if UUID is valid v7
```

**Example Usage**:
```clojure
(require '[typeid.validation :as v])

(v/valid-prefix? "user")
;;=> true

(v/valid-prefix? "User123")
;;=> false

;; More detailed validation with error messages
(require '[typeid.impl.validation :as impl])

(impl/validate-prefix "User123")
;;=> {:error {:type :invalid-prefix-format
;;            :message "Prefix must match pattern [a-z]([a-z_]{0,61}[a-z])? or be empty"
;;            :data {:prefix "User123" :pattern "..."}}}
```

---

## Performance Targets

| Function | Target Latency | Notes |
|----------|----------------|-------|
| `generate` | < 1μs | Includes UUID generation + base32 encoding |
| `parse` | < 1μs | Includes validation + base32 decoding |
| `validate` | < 500ns | Validation only, no decoding |
| `encode` | < 500ns | Base32 encoding only |
| `decode` | < 500ns | Base32 decoding only |
| `uuid->hex` | < 100ns | Simple byte-to-hex conversion |
| `hex->uuid` | < 100ns | Simple hex-to-byte conversion |

---

## Cross-Platform Compatibility

All public functions work identically on JVM Clojure and ClojureScript. Platform-specific implementation details (UUIDv7 generation) are hidden via reader conditionals.

**Guarantees**:
- Same input produces same output on both platforms (given same UUID bytes)
- Same error types and messages on both platforms
- Same Malli schemas on both platforms

**Platform Differences** (internal only):
- UUIDv7 generation uses `System.currentTimeMillis()` on JVM, `js/Date.now()` on JS
- Random bytes from `SecureRandom` on JVM, `crypto.getRandomValues()` on JS

---

## Deprecation Policy

Per Constitution Principle I (Stable public API surface):
- Breaking changes require MAJOR version bump
- Deprecation warnings MUST precede removal by at least one MINOR version
- Deprecated functions will log warnings for one minor version before removal

---

## References

- [spec.md](../spec.md) - User stories and functional requirements
- [data-model.md](../data-model.md) - Data structures and validation rules
- [TypeID Specification v0.3.0](../../../typeid.md) - Official TypeID spec
