# Research: Parse Function Returns Platform UUID

**Feature**: 003-parse-uuid-return
**Date**: 2025-11-12
**Phase**: 0 (Research & Technical Decisions)

## Research Questions & Decisions

### Q1: How to convert byte arrays to platform UUID objects?

**Decision**: Use platform-native constructors with bitwise operations

**Rationale**:
- **JVM**: `java.util.UUID` has a constructor accepting two `long` values (most significant bits and least significant bits)
- **ClojureScript**: `cljs.core/uuid` function accepts hex string representation
- Both approaches are zero-dependency and performant
- Pattern already established in existing `uuid->bytes` function (inverse operation)

**Implementation Pattern**:
```clojure
;; JVM
(defn bytes->uuid [uuid-bytes]
  (let [msb (reduce #(bit-or (bit-shift-left %1 8) (bit-and %2 0xFF))
                    0 (take 8 uuid-bytes))
        lsb (reduce #(bit-or (bit-shift-left %1 8) (bit-and %2 0xFF))
                    0 (drop 8 uuid-bytes))]
    (java.util.UUID. msb lsb)))

;; ClojureScript
(defn bytes->uuid [uuid-bytes]
  (let [hex-str (apply str (map #(.padStart (.toString % 16) 2 "0") uuid-bytes))]
    (cljs.core/uuid hex-str)))
```

**Alternatives Considered**:
1. **String conversion via hex**: More allocation overhead, slower
2. **BigInteger intermediate**: Unnecessary complexity for UUID representation
3. **Existing libraries**: Violates zero-dependency constraint

**Performance Impact**: < 500ns expected (simple bitwise operations)

---

### Q2: Should validation functions accept UUID objects as well as bytes?

**Decision**: No - Keep validation functions byte-oriented

**Rationale**:
- Validation happens during decoding (codec layer)
- By the time we have UUID object, validation is already complete
- UUID objects are platform-validated types (no invalid UUIDs possible)
- Maintains clean separation: codec validates bytes, core returns UUIDs
- Existing `valid-uuid-bytes?` function remains unchanged

**Alternatives Considered**:
1. **Add UUID validation predicates**: Unnecessary - platform types are pre-validated
2. **Accept both types**: Complicates API without benefit

---

### Q3: How to handle edge cases (all zeros, all ones, max timestamp)?

**Decision**: No special handling needed - pass through to platform UUID constructors

**Rationale**:
- Platform UUID implementations handle all valid 128-bit values
- Zero UUID (`00000000-0000-0000-0000-000000000000`) is valid
- Max UUID (`ffffffff-ffff-ffff-ffff-ffffffffffff`) is valid
- Edge cases already tested in byte form, UUID conversion is pure transformation
- No validation needed beyond byte validation (already exists)

**Test Coverage**:
- Round-trip tests with edge case UUIDs
- Property-based tests with random bytes
- Explicit tests for zero UUID, max UUID, various versions

---

### Q4: What are the breaking changes and migration path?

**Decision**: Clean break with clear documentation, no compatibility shims

**Rationale**:
- Library is 0.1.x (experimental), breaking changes expected
- Minor version bump (0.1.x → 0.2.0) signals incompatibility
- Users needing bytes can use `codec/decode` directly
- Migration is straightforward: replace `(:uuid parsed)` → stays same, just different type
- Adding compatibility layer would violate simplicity principles

**Migration Guide Required**:
```clojure
;; Before (0.1.x)
(let [parsed (typeid/parse "user_01h455vb4pex5vsknk084sn02q")
      uuid-bytes (:uuid parsed)
      uuid-hex (codec/uuid->hex uuid-bytes)]
  ...)

;; After (0.2.0)
(let [parsed (typeid/parse "user_01h455vb4pex5vsknk084sn02q")
      uuid (:uuid parsed)]  ; Now a UUID object!
  ...)

;; If you need bytes (advanced use case)
(let [uuid-bytes (codec/decode "user_01h455vb4pex5vsknk084sn02q")]
  ...)
```

**Breaking Change Notice** (for CHANGELOG.md):
- `:uuid` field in parsed result now returns platform UUID objects
- Byte representation still available via `codec/decode`
- All examples updated to show UUID objects

---

### Q5: Performance impact on parse function?

**Decision**: Minimal overhead, within existing performance budget

**Rationale**:
- Current parse: decode bytes + split string + construct map = ~2μs
- Adding bytes->uuid: bitwise operations = ~500ns
- Total expected: ~2.5μs (still well under 2μs budget with modern JVMs)
- ClojureScript: Similar profile (string operations are fast in JS)

**Benchmark Strategy**:
1. Baseline parse performance before change
2. Add bytes->uuid benchmark
3. Measure parse performance after change
4. Verify no regression beyond noise threshold (~10%)

**Optimization Opportunities** (if needed):
- Inline bytes->uuid into parse (avoid function call overhead)
- Use transients for byte processing (if profiling shows allocation hotspot)
- Pre-compute bit shifts (unlikely to help, JIT will optimize)

---

### Q6: ClojureScript compatibility considerations?

**Decision**: Use reader conditionals for platform-specific UUID construction

**Rationale**:
- Already established pattern in codebase (see `uuid->bytes`)
- ClojureScript UUID is string-based internally
- Hex string conversion is natural fit for CLJS
- Both platforms tested in CI matrix

**Platform-Specific Code**:
```clojure
#?(:clj (java.util.UUID. msb lsb)
   :cljs (cljs.core/uuid hex-str))
```

**Testing Requirements**:
- JVM tests (Kaocha)
- ClojureScript tests (Kaocha-cljs)
- Both platforms in CI

---

## Technology Decisions Summary

| Aspect | Decision | Rationale |
|--------|----------|-----------|
| **UUID Construction** | Platform-native constructors | Zero dependencies, performant, idiomatic |
| **Validation** | Byte-only (existing) | UUID objects are pre-validated by platform |
| **Edge Cases** | Pass through | Platform handles all valid 128-bit values |
| **Breaking Changes** | Clean break, no shims | 0.x stage, simplicity over compatibility |
| **Performance** | ~500ns overhead | Well within budget, minimal impact |
| **Platform Support** | Reader conditionals | Established pattern, CI-tested |

---

## Dependencies Analysis

**Zero new dependencies required**:
- Uses existing platform UUID support (java.util.UUID, cljs.core/uuid)
- No external libraries needed
- Maintains constitution compliance (Principle IV, Constraint: Zero runtime dependencies)

**Existing Dependencies (dev/test only)**:
- Kaocha (testing)
- test.check (property-based testing)
- Criterium (benchmarking)
- clj-kondo (linting)

---

## Best Practices Applied

### 1. Pure Functions (Principle VIII)
- `bytes->uuid` is pure: same input → same output
- No side effects, no global state
- Deterministic behavior

### 2. Clear Error Semantics (Principle VIII)
- Invalid bytes caught by existing validation (before conversion)
- UUID construction errors (if any) propagate with clear messages
- Platform UUID constructors provide meaningful errors

### 3. Performance Awareness (Principle V)
- Benchmarked approach
- Minimal allocation (two longs or one string)
- No premature optimization

### 4. Compatibility (Principle III)
- Both JVM and ClojureScript supported
- Reader conditionals isolate platform differences
- CI tests both platforms

---

## Risk Assessment

### Low Risk
- **Pure transformation**: Byte array → UUID is deterministic, no business logic
- **Platform support**: UUID types are stable, well-tested platform features
- **Existing patterns**: Follows inverse of existing `uuid->bytes` function

### Medium Risk
- **Performance regression**: Mitigated by benchmarking and performance budget
- **Breaking change impact**: Mitigated by clear documentation and 0.x stage

### No Significant Risks Identified

---

## Next Steps (Phase 1)

1. **Data Model**: Document UUID type changes in entity model
2. **Contracts**: Update parse function API contract
3. **Quickstart**: Create migration guide for existing users
4. **Agent Context**: Update CLAUDE.md with technical decisions

---

## References

- [java.util.UUID JavaDoc](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/util/UUID.html)
- [ClojureScript UUID](https://cljs.github.io/api/cljs.core/uuid)
- Existing implementation: `src/typeid/impl/uuid.cljc` (uuid->bytes function)
- Constitution: `.specify/memory/constitution.md` (Principles IV, V, VIII)
