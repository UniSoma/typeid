# Research: TypeID API Simplification

**Feature**: 002-api-simplification
**Date**: 2025-11-11
**Status**: Complete

## Overview

This research phase analyzes the technical decisions for refactoring the TypeID library API to improve clarity, consistency, and separation of concerns.

## Research Questions & Findings

### 1. Error Handling Pattern: Exceptions vs Data-as-Error

**Question**: Should the library use exceptions or data-as-error pattern for invalid inputs?

**Decision**: Mixed approach - `explain` returns data (nil or error map), other functions throw exceptions

**Rationale**:
- **Validation use case** (`explain`): Developers need to check validity without catching exceptions - data return is optimal
- **Parsing/creation use cases** (`parse`, `create`): Invalid input is exceptional - exceptions provide cleaner control flow
- **Clojure conventions**: Exception-based error handling is standard for parsing/creation functions (see `clojure.edn/read-string`, `java.util.UUID/fromString`)
- **User feedback**: Original request specifically asked to drop data-as-error for validation, but `explain` provides validation feedback via data

**Alternatives considered**:
1. **Pure data-as-error**: Would require wrapping all results in `{:ok ...}` or `{:error ...}`, making simple operations verbose
2. **Pure exceptions**: Would require try-catch for validation checks, defeating the purpose of a validation function
3. **clojure.spec integration**: Rejected per constitution (zero runtime dependencies)

**Implementation notes**:
- `explain` returns `nil` for valid input, error map for invalid
- Error maps include: `:type` (keyword), `:message` (string), `:context` (map with details like `:input`, `:expected`)
- `parse` and `create` throw `ex-info` with same structured data map
- Platform-native exceptions: `clojure.lang.ExceptionInfo` on JVM, `js/Error` with `ex-info` data on ClojureScript

---

### 2. Function Naming: `explain` vs `valid?` vs `validate`

**Question**: What should the validation function be named?

**Decision**: `explain`

**Rationale**:
- **Semantic clarity**: "explain" suggests it provides information about the input, not just yes/no
- **Return value alignment**: Returns nil (no explanation needed) or explanation (error details)
- **Differentiation from predicates**: Clojure convention uses `?` suffix for boolean predicates; `explain` clearly signals non-boolean return
- **Spec precedent**: `clojure.spec.alpha/explain` follows similar pattern (though we're not using spec)

**Alternatives considered**:
1. **`valid?`**: Would suggest boolean return, not error details
2. **`validate`**: Was the original function; ambiguous about return value (throws? returns data? returns boolean?)
3. **`check`**: Less descriptive than `explain`

---

### 3. Codec Namespace Organization

**Question**: Should codec functions be in a separate namespace?

**Decision**: Yes - create `typeid.codec` namespace

**Rationale**:
- **Separation of concerns**: High-level API (`generate`, `create`, `explain`, `parse`) vs low-level codec operations
- **API clarity**: Most users won't need direct access to encoding/decoding - separate namespace signals "advanced/internal use"
- **Reduced cognitive load**: Smaller `typeid.core` namespace makes API easier to understand
- **Future flexibility**: Can add more codec utilities without cluttering core API

**Alternatives considered**:
1. **Keep all functions in typeid.core**: Creates large namespace with mixed abstraction levels
2. **Make codec functions private**: Would prevent legitimate advanced use cases (testing, integration)
3. **Create typeid.internal namespace**: "internal" suggests private; codec functions are public but specialized

**Functions to move**:
- `encode`: UUID bytes + prefix → TypeID string
- `decode`: TypeID string → UUID bytes
- `uuid->hex`: UUID bytes → hex string
- `hex->uuid`: Hex string → UUID bytes

---

### 4. Two-Arity `create` Function Implementation

**Question**: How should `create` accept existing UUIDs across Clojure/ClojureScript?

**Decision**: Accept platform-native UUID objects without version validation

**Rationale**:
- **Platform compatibility**:
  - JVM: `java.util.UUID` (standard Java class)
  - ClojureScript: `#uuid` reader literal produces `cljs.core/UUID` type
- **UUID version agnostic**: TypeID encoding works with any UUID bytes, regardless of version (v1, v4, v7, etc.)
- **Edge case handling**: Accept all-zeros, all-ones, or any valid UUID without validation
- **Simplicity**: No need to validate UUID version or content - just encode the bytes

**Alternatives considered**:
1. **Accept only UUIDv7**: Would reject valid use cases (migrating from v4 UUIDs, testing with specific UUIDs)
2. **Accept byte arrays**: Would require users to manually convert UUIDs to bytes, reducing ergonomics
3. **Accept hex strings**: Would require parsing and validation, adding complexity

**Implementation notes**:
- Single type dispatch: `(create prefix uuid)` where `uuid` is platform-native UUID type
- Extract bytes using platform-specific methods:
  - JVM: Convert `java.util.UUID` to byte array via `getMostSignificantBits`/`getLeastSignificantBits`
  - ClojureScript: UUID objects already have byte representation
- Prefix handling: Same as existing `generate` function (nil, string, or keyword)

---

### 5. Exception Data Structure

**Question**: What should exception data maps contain?

**Decision**: Consistent structure with `:type`, `:message`, and context fields

**Rationale**:
- **Structured error handling**: Allows programmatic inspection of errors (useful for testing, logging)
- **Debugging support**: Context fields provide specific details about what went wrong
- **Platform consistency**: Works identically on JVM and ClojureScript via `ex-info`

**Standard structure**:
```clojure
{:type :typeid/invalid-prefix        ; Keyword identifying error category
 :message "Invalid prefix: ..."      ; Human-readable description
 :input "bad_typeid"                  ; What was provided
 :expected "lowercase alphanumeric"   ; What was expected (optional)
 :actual "uppercase characters"}      ; What was found (optional)
```

**Error type taxonomy**:
- `:typeid/invalid-prefix` - Prefix violates TypeID spec constraints
- `:typeid/invalid-suffix` - Suffix is malformed or wrong length
- `:typeid/invalid-format` - Overall format doesn't match TypeID pattern
- `:typeid/invalid-input-type` - Non-string input to functions expecting strings
- `:typeid/invalid-uuid` - UUID format is invalid (for `create` function)

**Alternatives considered**:
1. **Plain string messages**: Not programmatically inspectable
2. **Different structure per function**: Would make error handling inconsistent
3. **Spec-style error maps**: Would require spec dependency (constitution violation)

---

## Technical Decisions Summary

| Decision Area | Choice | Key Constraint |
|--------------|--------|----------------|
| Error handling | Mixed: `explain` returns data, others throw exceptions | Clojure conventions + usability |
| Validation function name | `explain` | Semantic clarity (returns explanation) |
| Codec organization | Separate `typeid.codec` namespace | Separation of concerns |
| UUID acceptance | Platform-native UUID objects, version-agnostic | Cross-platform compatibility |
| Exception structure | Consistent `:type`, `:message`, context map | Structured error handling |

## Dependencies Analysis

**Runtime dependencies**: None (maintained per constitution)

**Dev/test dependencies**: No changes required
- Kaocha: Test runner (existing)
- test.check: Property-based testing (existing)

**Platform requirements**:
- JVM: Clojure 1.11+, Java 17+
- ClojureScript: ClojureScript 1.11+

## Performance Considerations

**No performance impact expected**:
- Function renaming is compile-time change (no runtime cost)
- Exception throwing has same performance as existing error handling
- Two-arity `create` reuses existing encoding logic
- Namespace organization is compile-time change (no runtime cost)

**Benchmark verification**:
- Existing benchmarks remain valid after refactoring
- No changes to hot paths (encoding/decoding algorithms unchanged)

## Implementation Strategy

**Phase order**:
1. Create `typeid.codec` namespace with codec functions
2. Implement `explain` function for validation
3. Implement `parse` function for parsing with exceptions
4. Implement two-arity `create` function for UUID encoding
5. Implement all tests for new API
6. Complete documentation (README, docstrings)

## Open Questions

None - all clarifications resolved via user input in spec.md.

## References

- TypeID Specification v0.3.0: https://github.com/jetpack-io/typeid
- Clojure error handling conventions: https://clojure.org/reference/special_forms#try
- Constitution: `.specify/memory/constitution.md`
