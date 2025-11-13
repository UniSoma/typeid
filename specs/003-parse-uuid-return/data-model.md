# Data Model: Parse Function Returns Platform UUID

**Feature**: 003-parse-uuid-return
**Date**: 2025-11-12
**Phase**: 1 (Design & Contracts)

## Entity Changes

### Parsed TypeID Result (MODIFIED)

**Description**: The map returned by `parse` function representing a decomposed TypeID.

**Fields**:

| Field | Type (Before) | Type (After) | Description | Validation |
|-------|---------------|--------------|-------------|------------|
| `:prefix` | String | String | Type prefix (empty string if none) | No change |
| `:suffix` | String | String | 26-character base32 suffix | No change |
| `:uuid` | **byte[] / Uint8Array** | **java.util.UUID / cljs.core.UUID** | Platform-native UUID object | Must represent same 128-bit value as original bytes |
| `:typeid` | String | String | Original TypeID string | No change |

**Key Change**: `:uuid` field type changed from byte array to platform-native UUID object.

**Validation Rules**:
- All existing validation (prefix format, suffix format, length) unchanged
- UUID validation happens implicitly via platform type (cannot construct invalid UUID)
- Round-trip property: `(= uuid (-> typeid (parse) :uuid (uuid->bytes) (bytes->uuid)))` must hold

**State Transitions**: N/A (immutable value object)

---

### UUID Conversion Utilities (NEW)

**Description**: Helper functions for converting between byte arrays and platform UUID objects.

#### bytes->uuid

**Input**: 16-byte array (byte[] on JVM, Uint8Array on ClojureScript)
**Output**: Platform-native UUID object (java.util.UUID or cljs.core/UUID)

**Validation**:
- Input must be exactly 16 bytes
- All 128-bit values are valid (no range restrictions)
- Edge cases (all zeros, all ones) are valid

**Error Handling**:
- Throws `ex-info` if input is not 16 bytes
- Platform UUID constructors may throw for malformed input (rare)

**Properties**:
- **Deterministic**: Same bytes always produce equal UUID
- **Reversible**: `uuid->bytes` is the inverse function
- **Pure**: No side effects, no state

---

## Relationship Changes

### Before (Current State)

```
TypeID String
    ↓ parse
Parsed Result {:uuid byte[]}
    ↓ codec/uuid->hex
Hex String → (manual conversion) → java.util.UUID
```

Users must manually convert bytes to UUID for database operations.

### After (New State)

```
TypeID String
    ↓ parse
Parsed Result {:uuid UUID object}
    ↓ (direct use)
Database / Comparison / Serialization
```

Users can directly use UUID without conversion.

---

## Type Hierarchy

```
Platform UUID Objects
├── JVM: java.util.UUID
│   ├── Constructor: UUID(long msb, long lsb)
│   ├── Methods: getMostSignificantBits(), getLeastSignificantBits()
│   ├── Equality: Based on 128-bit value
│   └── Serialization: Standard Java serialization
│
└── ClojureScript: cljs.core.UUID
    ├── Constructor: uuid(hex-string)
    ├── Internal: String-based representation
    ├── Equality: Based on string value
    └── Serialization: String representation
```

**Common Operations**:
- Equality comparison: `=` works across both platforms
- String representation: `str` or `.toString()`
- Database storage: Native UUID column support (PostgreSQL, H2, etc.)

---

## Data Flow

### Parse Operation (Detailed)

```
Input: "user_01h455vb4pex5vsknk084sn02q"
    ↓
1. Validation (existing)
   - Check format, length, lowercase
   - Validate prefix pattern
   - Validate suffix format
    ↓
2. Decode suffix to bytes (existing)
   - Base32 decode → 16-byte array
    ↓
3. Convert bytes to UUID (NEW)
   - bytes->uuid function
   - Platform-specific construction
    ↓
4. Return parsed result (modified)
   - {:prefix "user"
      :suffix "01h455vb4pex5vsknk084sn02q"
      :uuid #uuid "018c3f9e-9e4e-7a8a-8b2a-7e8e9e4e7a8a"  ← Changed from bytes
      :typeid "user_01h455vb4pex5vsknk084sn02q"}
```

### Round-Trip Operation

```
Original UUID: #uuid "018c3f9e-9e4e-7a8a-8b2a-7e8e9e4e7a8a"
    ↓ uuid->bytes (existing)
16-byte array
    ↓ codec/encode (existing)
TypeID: "user_01h455vb4pex5vsknk084sn02q"
    ↓ parse (modified)
Parsed: {:uuid #uuid "018c3f9e-9e4e-7a8a-8b2a-7e8e9e4e7a8a"}
    ↓ equality check
(= original-uuid (:uuid parsed)) → true  ✅
```

---

## Edge Cases

### Zero UUID
```clojure
Input bytes: [0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0]
Output UUID: #uuid "00000000-0000-0000-0000-000000000000"
Valid: ✅ Yes
```

### Max UUID
```clojure
Input bytes: [255 255 ... 255] (16 bytes)
Output UUID: #uuid "ffffffff-ffff-ffff-ffff-ffffffffffff"
Valid: ✅ Yes
```

### UUIDv4 (Non-v7)
```clojure
Input bytes: [... variant v4 bits ...]
Output UUID: #uuid "550e8400-e29b-41d4-a716-446655440000"
Valid: ✅ Yes (TypeID spec allows encoding any UUID)
```

### Multiple Parse Calls
```clojure
(let [parsed1 (parse "user_01h455vb4pex5vsknk084sn02q")
      parsed2 (parse "user_01h455vb4pex5vsknk084sn02q")]
  (= (:uuid parsed1) (:uuid parsed2)))
→ true  ✅ (UUIDs compare equal)
```

---

## Performance Characteristics

| Operation | Before | After | Change |
|-----------|--------|-------|--------|
| `parse` | ~2μs | ~2.5μs | +500ns (bytes->uuid) |
| `bytes->uuid` | N/A | ~500ns | New operation |
| `codec/decode` | ~1μs | ~1μs | No change |
| Memory (parsed result) | ~160 bytes | ~150 bytes | Slight reduction (UUID object smaller than byte array wrapper) |

**Note**: Actual performance will be validated with benchmarks. Expected overhead is well within acceptable range.

---

## Validation Strategy

### Property-Based Tests

Using test.check to verify:

1. **Round-trip property**:
   ```clojure
   (prop/for-all [uuid (gen/uuid)]
     (= uuid (-> (create "test" uuid)
                 (parse)
                 :uuid)))
   ```

2. **Byte equivalence property**:
   ```clojure
   (prop/for-all [uuid-bytes (gen/vector (gen/choose 0 255) 16)]
     (= (vec uuid-bytes)
        (vec (-> (bytes->uuid uuid-bytes)
                 (uuid->bytes)))))
   ```

3. **Determinism property**:
   ```clojure
   (prop/for-all [typeid-str (gen/typeid)]
     (let [uuid1 (:uuid (parse typeid-str))
           uuid2 (:uuid (parse typeid-str))]
       (= uuid1 uuid2)))
   ```

### Manual Test Cases

- All zeros UUID
- All ones UUID
- UUIDv1, v4, v7 variants
- Edge timestamps (min, max)
- Existing compliance test suite (valid.yml, invalid.yml)

---

## Migration Impact

### Code That Breaks

```clojure
;; Direct byte array manipulation
(let [uuid-bytes (:uuid (parse "user_..."))]
  (aget uuid-bytes 0))  ; ❌ Breaks - uuid-bytes is now UUID object

;; Byte array validation
(let [uuid-val (:uuid (parse "user_..."))]
  (valid-uuid-bytes? uuid-val))  ; ❌ Breaks - expects bytes, gets UUID
```

### Code That Works

```clojure
;; Treating UUID opaquely (most common case)
(let [uuid (:uuid (parse "user_..."))]
  (save-to-db! {:id uuid}))  ; ✅ Works better - no conversion needed

;; UUID comparison
(let [uuid1 (:uuid (parse "user_1"))
      uuid2 (:uuid (parse "user_2"))]
  (= uuid1 uuid2))  ; ✅ Works - UUID equality

;; Round-trip conversion
(let [original #uuid "018c3f9e-9e4e-7a8a-8b2a-7e8e9e4e7a8a"
      typeid (create "user" original)
      recovered (:uuid (parse typeid))]
  (= original recovered))  ; ✅ Works - exact equality
```

---

## Summary

**Entity Changes**: 1 modified entity (Parsed TypeID Result)
**New Utilities**: 1 new function (`bytes->uuid`)
**Breaking Changes**: `:uuid` field type change
**Validation**: Property-based tests + manual edge cases
**Performance Impact**: Minimal (+500ns, well within budget)
**Migration**: Straightforward, most code unaffected or improved
