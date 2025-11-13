# API Contract: Parse Function Changes

**Feature**: 003-parse-uuid-return
**Date**: 2025-11-12
**Phase**: 1 (Design & Contracts)

## Contract Changes

### `typeid.core/parse` (BREAKING CHANGE)

**Function Signature**:
```clojure
(parse typeid-str) → {:prefix String, :suffix String, :uuid UUID, :typeid String}
```

**Change**: Return value `:uuid` field type changed from `byte[]`/`Uint8Array` to `java.util.UUID`/`cljs.core/UUID`.

---

#### Input Contract

**Parameters**:
- `typeid-str` (String): Valid TypeID string

**Preconditions** (unchanged):
- Must be a string
- Length must be 26-90 characters
- Must be all lowercase
- Must not start with underscore
- Prefix (if present) must match `[a-z]([a-z_]{0,61}[a-z])?`
- Suffix must be exactly 26 base32 characters
- Suffix must start with 0-7 (to prevent overflow)

**Validation** (unchanged):
- Throws `ExceptionInfo` with structured error data if invalid
- Error data includes `:type`, `:message`, `:input`, and context fields

---

#### Output Contract (CHANGED)

**Before (v0.1.x)**:
```clojure
{:prefix "user"                              ; String
 :suffix "01h455vb4pex5vsknk084sn02q"       ; String
 :uuid #object["[B" ...]                     ; byte[] (JVM) or Uint8Array (CLJS)
 :typeid "user_01h455vb4pex5vsknk084sn02q"} ; String
```

**After (v0.2.0)**:
```clojure
{:prefix "user"                              ; String
 :suffix "01h455vb4pex5vsknk084sn02q"       ; String
 :uuid #uuid "018c3f9e-9e4e-7a8a-8b2a-7e8e9e4e7a8a"  ; UUID object
 :typeid "user_01h455vb4pex5vsknk084sn02q"} ; String
```

**Return Value Fields**:

| Field | Type | Required | Description | Validation |
|-------|------|----------|-------------|------------|
| `:prefix` | String | Yes | Type prefix (empty string if none) | Matches input |
| `:suffix` | String | Yes | 26-character base32 suffix | Exactly 26 chars, base32 alphabet |
| `:uuid` | **UUID** | Yes | **Platform-native UUID object** | Represents same 128-bit value as decoded bytes |
| `:typeid` | String | Yes | Original input string | Exact copy of input |

**Postconditions**:
1. All input validation passed (guaranteed, else exception thrown)
2. `:uuid` represents same 128-bit value as base32-decoded suffix
3. `:prefix` + `_` + `:suffix` = `:typeid` (or `:suffix` = `:typeid` if no prefix)
4. Round-trip property: `(= uuid (-> (create prefix uuid) (parse) :uuid))`

---

#### Behavioral Changes

**JVM (Clojure)**:
- Returns `java.util.UUID` instance
- Equality via `=` compares 128-bit value
- Compatible with JDBC, next.jdbc, PostgreSQL UUID columns
- Serialization: Standard Java UUID serialization

**ClojureScript**:
- Returns `cljs.core/UUID` instance
- Equality via `=` compares string representation
- Compatible with JSON serialization (toString)
- Interop: Can be converted to string for API calls

**Cross-Platform**:
- `=` works consistently across platforms
- `str` produces standard hyphenated UUID string
- Database drivers handle platform UUID types natively

---

### `typeid.impl.uuid/bytes->uuid` (NEW)

**Function Signature**:
```clojure
(bytes->uuid uuid-bytes) → UUID
```

**Purpose**: Convert 16-byte array to platform-native UUID object.

---

#### Input Contract

**Parameters**:
- `uuid-bytes` (byte[] or Uint8Array): Exactly 16 bytes representing UUID

**Preconditions**:
- Must be byte array type (platform-specific)
- Must be exactly 16 bytes long
- All byte values (0-255) are valid

**Validation**:
- Throws `ExceptionInfo` if not 16 bytes
- Error includes `:type :typeid/invalid-uuid`, `:message`, `:input`, `:expected`, `:actual`

---

#### Output Contract

**Returns**: Platform-native UUID object
- JVM: `java.util.UUID`
- ClojureScript: `cljs.core/UUID`

**Postconditions**:
1. Represents same 128-bit value as input bytes
2. Inverse of `uuid->bytes`: `(= uuid (-> uuid uuid->bytes bytes->uuid))`
3. Deterministic: Same bytes → equal UUID

---

#### Examples

```clojure
;; JVM
(bytes->uuid (byte-array [0x01 0x8c 0x3f 0x9e 0x9e 0x4e 0x7a 0x8a
                           0x8b 0x2a 0x7e 0x8e 0x9e 0x4e 0x7a 0x8a]))
;; => #uuid "018c3f9e-9e4e-7a8a-8b2a-7e8e9e4e7a8a"

;; ClojureScript
(bytes->uuid (js/Uint8Array. [0x01 0x8c 0x3f 0x9e 0x9e 0x4e 0x7a 0x8a
                               0x8b 0x2a 0x7e 0x8e 0x9e 0x4e 0x7a 0x8a]))
;; => #uuid "018c3f9e-9e4e-7a8a-8b2a-7e8e9e4e7a8a"

;; Edge case: Zero UUID
(bytes->uuid (byte-array 16))  ; All zeros
;; => #uuid "00000000-0000-0000-0000-000000000000"

;; Edge case: Max UUID
(bytes->uuid (byte-array (repeat 16 -1)))  ; All 0xFF
;; => #uuid "ffffffff-ffff-ffff-ffff-ffffffffffff"
```

---

## Compatibility Matrix

### Backward Compatibility

| Use Case | v0.1.x | v0.2.0 | Compatible? |
|----------|--------|--------|-------------|
| Parse and extract UUID | Byte array | UUID object | ❌ Breaking |
| Direct byte manipulation | Supported | Use `codec/decode` | ⚠️ Migration required |
| Database storage | Manual conversion | Direct use | ✅ Improved |
| Round-trip conversion | Works with bytes | Works with UUID | ✅ Improved |
| UUID comparison | Byte comparison | UUID equality | ✅ Improved |
| Low-level codec ops | Unchanged | Unchanged | ✅ Compatible |

### Migration Path

**Breaking Change Impact**:
- **High impact**: Code directly manipulating `:uuid` as byte array
- **Medium impact**: Code validating UUID bytes explicitly
- **Low impact**: Code treating UUID opaquely (most common case)
- **No impact**: Low-level codec usage (`encode`, `decode`, `uuid->hex`, `hex->uuid`)

**Migration Steps**:
1. **Identify usage**: Search codebase for `(:uuid (parse ...))` or `(:uuid parsed-result)`
2. **Update expectations**: Change byte array assumptions to UUID objects
3. **Use codec for bytes**: If bytes needed, use `codec/decode` instead of `parse`
4. **Test round-trips**: Verify UUID equality works as expected

**Example Migrations**:

```clojure
;; Before: Manual byte-to-hex conversion
(let [uuid-bytes (:uuid (parse typeid-str))
      uuid-hex (codec/uuid->hex uuid-bytes)]
  (jdbc/execute! ds ["SELECT * FROM users WHERE id = ?::uuid" uuid-hex]))

;; After: Direct UUID usage
(let [uuid (:uuid (parse typeid-str))]
  (jdbc/execute! ds ["SELECT * FROM users WHERE id = ?" uuid]))


;; Before: Byte array validation
(let [result (parse typeid-str)]
  (when (valid-uuid-bytes? (:uuid result))
    ...))

;; After: UUID objects are pre-validated
(let [result (parse typeid-str)]
  ;; No validation needed - platform UUID is always valid
  ...)


;; Before: Need bytes for low-level operation
(let [uuid-bytes (:uuid (parse typeid-str))]
  (custom-byte-operation uuid-bytes))

;; After: Use codec directly
(let [uuid-bytes (codec/decode typeid-str)]
  (custom-byte-operation uuid-bytes))
```

---

## Error Contracts

### No Changes to Existing Errors

All existing `parse` error types remain unchanged:
- `:typeid/invalid-input-type` - Non-string input
- `:typeid/invalid-length` - Wrong length
- `:typeid/invalid-format` - Uppercase or starts with underscore
- `:typeid/invalid-prefix` - Prefix validation failure
- `:typeid/invalid-suffix` - Suffix validation failure

### New Error (bytes->uuid)

**Type**: `:typeid/invalid-uuid`
**When**: Input is not exactly 16 bytes
**Data**:
```clojure
{:type :typeid/invalid-uuid
 :message "UUID must be exactly 16 bytes"
 :input <invalid-input>
 :expected "16-byte array"
 :actual "X bytes"}  ; or "not a byte array"
```

---

## Performance Contract

### Performance Budgets

| Operation | Before | After | Budget | Status |
|-----------|--------|-------|--------|--------|
| `parse` | ~2μs | ~2.5μs | < 3μs | ✅ Within budget |
| `bytes->uuid` | N/A | ~500ns | < 1μs | ✅ Within budget |
| `codec/decode` | ~1μs | ~1μs | < 1μs | ✅ No change |

**Guarantee**: No operation will exceed its performance budget on typical hardware (modern x86_64 or ARM64).

**Benchmark Verification**:
- Criterium benchmarks for all modified operations
- CI performance regression tests
- Fail build if > 10% regression

---

## Testing Contract

### Required Test Coverage

1. **Unit Tests**:
   - `bytes->uuid` with various inputs (zeros, max, random)
   - `parse` returns UUID objects
   - UUID equality and comparison
   - Platform-specific behavior (JVM vs CLJS)

2. **Property-Based Tests**:
   - Round-trip: UUID → TypeID → UUID
   - Byte equivalence: bytes → UUID → bytes
   - Determinism: Same input → equal output

3. **Integration Tests**:
   - Existing compliance tests pass (valid.yml, invalid.yml)
   - Cross-platform tests (JVM + CLJS)

4. **Performance Tests**:
   - Benchmark `parse` function
   - Benchmark `bytes->uuid` function
   - Verify no regression

### Test Requirements

- ✅ All existing tests updated for UUID objects
- ✅ New tests for `bytes->uuid` function
- ✅ Property-based tests for round-trip
- ✅ Edge case tests (zero, max, various versions)
- ✅ Platform-specific tests (JVM + CLJS)
- ✅ Performance benchmarks

---

## Documentation Contract

### Required Updates

1. **README.md**:
   - Update all `parse` examples to show UUID objects
   - Add migration guide section
   - Update "Parsing TypeIDs" section
   - Update "Common Patterns" examples

2. **Docstrings**:
   - Update `typeid.core/parse` docstring
   - Add `typeid.impl.uuid/bytes->uuid` docstring
   - Include examples showing UUID objects

3. **CHANGELOG.md**:
   - Add breaking change notice under v0.2.0
   - Document migration path
   - Reference this spec for details

4. **API Documentation** (cljdoc):
   - Auto-generated from docstrings
   - Verify examples render correctly

---

## Version Contract

**Version Bump**: 0.1.x → 0.2.0 (MINOR)

**Rationale**:
- Breaking change in experimental (0.x) library
- Minor bump signals incompatibility
- Follows semantic versioning for pre-1.0

**Release Notes**:
```markdown
## [0.2.0] - 2025-XX-XX

### Breaking Changes
- **parse function**: `:uuid` field now returns platform-native UUID objects
  (java.util.UUID on JVM, cljs.core/UUID in ClojureScript) instead of byte arrays
  - **Migration**: Code expecting byte arrays should use `codec/decode` directly
  - **Benefit**: Natural database integration, no manual conversion needed
  - See migration guide: [link to quickstart.md]

### Added
- `typeid.impl.uuid/bytes->uuid`: Convert 16-byte arrays to UUID objects

### Changed
- Performance: `parse` function adds ~500ns overhead for UUID conversion (well within budget)

### Fixed
- API consistency: create accepts UUIDs, parse now returns UUIDs
```

---

## Summary

**Breaking Changes**: 1 (parse return type)
**New Functions**: 1 (bytes->uuid)
**Performance Impact**: Minimal (+500ns)
**Migration Complexity**: Low (most code unaffected)
**Documentation Updates**: Required (README, docstrings, changelog)
**Testing**: Comprehensive (unit, property-based, integration, performance)
