# Implementation Plan: Parse Function Returns Platform UUID

**Branch**: `003-parse-uuid-return` | **Date**: 2025-11-12 | **Spec**: [spec.md](./spec.md)
**Input**: Feature specification from `/specs/003-parse-uuid-return/spec.md`

## Summary

Change the `parse` function to return platform-native UUID objects (java.util.UUID on JVM, cljs.core/UUID in ClojureScript) instead of 16-byte arrays. This improves API consistency, enables natural round-trip conversions, and eliminates manual type conversions for database integration.

**Technical Approach**: Implement `bytes->uuid` conversion utility in the `typeid.impl.uuid` namespace, update `parse` function in `typeid.core` to convert bytes to UUID before returning, update all tests to validate UUID objects, and update documentation examples.

## Technical Context

**Language/Version**: Clojure 1.11+ (JVM), ClojureScript 1.12+ (JS)
**Primary Dependencies**: Zero runtime dependencies (only org.clojure/clojure and org.clojure/clojurescript)
**Storage**: N/A (library does not handle storage)
**Testing**: Kaocha + test.check for property-based testing
**Target Platform**: JVM (Clojure 1.11/1.12 on JDK 17/21), JavaScript (ClojureScript via Node.js/browsers)
**Project Type**: Single library project (src/ and test/ structure)
**Performance Goals**:
- `parse` function: < 2μs per operation
- `bytes->uuid` utility: < 500ns per conversion
- No performance regression from current implementation
**Constraints**:
- Zero runtime dependencies beyond Clojure/ClojureScript
- 100% backward compatibility for low-level codec functions
- Breaking change acceptable (0.x experimental stage)
- Must pass all existing compliance tests
**Scale/Scope**: Single library with 6 source namespaces, ~1000 LOC, breaking API change affecting parse return type

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

### Gate 1: API Design (Principle I)

✅ **PASS** - Breaking change with proper versioning
- **Rule**: "Breaking changes require MAJOR version bump; deprecation warnings MUST precede removal by at least one MINOR version"
- **Justification**: Library is in 0.x experimental stage where breaking changes are expected. Minor version bump (0.1.x → 0.2.0) is appropriate. No deprecation period needed for pre-1.0 releases.
- **Compliance**: Changelog will document breaking change clearly

### Gate 2: Code Quality (Principle II)

✅ **PASS** - Standard quality requirements apply
- 100% clj-kondo clean (required)
- Docstrings on all public functions (required)
- Updated examples in README and docstrings

### Gate 3: Compatibility (Principle III)

✅ **PASS** - Existing compatibility maintained
- JVM Clojure 1.11+ support maintained
- ClojureScript support maintained
- CI matrix testing both platforms
- Platform-specific UUID implementations

### Gate 4: Types & Contracts (Principle IV)

✅ **PASS** - No external validation dependencies
- Manual predicate validation for UUID objects
- Property-based tests with test.check
- Clear error messages for invalid data
- No runtime dependencies introduced

### Gate 5: Performance (Principle V)

✅ **PASS** - Performance budget maintained
- Benchmarks exist for parse operation
- New `bytes->uuid` utility will be benchmarked
- Target: < 2μs for parse (existing budget)
- Target: < 500ns for bytes->uuid conversion
- No performance regression expected (minimal overhead)

### Gate 6: Build & Release (Principle VI)

✅ **PASS** - Standard release process
- Semantic versioning: 0.1.x → 0.2.0 (MINOR bump)
- Changelog updated with breaking change notice
- deps.edn + tools.build (existing)

### Gate 7: Testing (Principle VII)

✅ **PASS** - Comprehensive test updates required
- All existing tests updated for UUID objects
- New property-based tests for bytes->uuid
- Round-trip conversion tests
- Edge case tests (all zeros, all ones, max timestamp)
- CI matrix tests on Clojure 1.11/1.12, JDK 17/21

### Gate 8: Observability (Principle VIII)

✅ **PASS** - Pure functions maintained
- `bytes->uuid` is pure function
- No global state introduced
- Clear error messages if UUID conversion fails

**Constitution Compliance**: ✅ ALL GATES PASSED - No violations, feature aligns with all principles.

---

## Post-Design Constitution Re-Check

*GATE: Re-evaluated after Phase 1 design artifacts (research.md, data-model.md, contracts/api.md, quickstart.md)*

### Design Validation

After completing Phase 1 design, re-checking all constitution gates:

✅ **Gate 1 (API Design)**: Breaking change properly documented
- Migration guide created (quickstart.md)
- API contract specifies v0.1.x → v0.2.0 bump
- Clear before/after examples

✅ **Gate 2 (Code Quality)**: Documentation complete
- Docstring updates specified in contracts/api.md
- README update requirements documented
- Examples show UUID objects consistently

✅ **Gate 3 (Compatibility)**: Platform support confirmed
- JVM: `java.util.UUID` constructor with MSB/LSB
- ClojureScript: `cljs.core/uuid` from hex string
- Both tested in CI matrix

✅ **Gate 4 (Types & Contracts)**: Zero dependencies maintained
- `bytes->uuid` uses platform constructors only
- No external validation libraries
- Property-based tests with test.check (dev dependency)

✅ **Gate 5 (Performance)**: Budgets validated
- Research confirms ~500ns for bytes->uuid
- Total parse budget: ~2.5μs (within < 3μs target)
- Benchmark strategy defined

✅ **Gate 6 (Build & Release)**: Release process clear
- Version: 0.1.x → 0.2.0
- Changelog template provided
- Breaking change documented

✅ **Gate 7 (Testing)**: Test strategy comprehensive
- Property-based tests (round-trip, byte equivalence, determinism)
- Edge case tests (zero UUID, max UUID, various versions)
- Platform-specific tests (JVM + CLJS)
- Performance benchmarks

✅ **Gate 8 (Observability)**: Pure functions maintained
- `bytes->uuid` is pure (deterministic, no side effects)
- Clear error messages for invalid input
- No global state introduced

**Post-Design Compliance**: ✅ ALL GATES PASSED - Design fully compliant with constitution.

**Design Quality**: High confidence in implementation approach. All technical decisions validated against constitution principles.

## Project Structure

### Documentation (this feature)

```text
specs/003-parse-uuid-return/
├── plan.md              # This file
├── research.md          # Phase 0 output (technical decisions)
├── data-model.md        # Phase 1 output (entity changes)
├── quickstart.md        # Phase 1 output (migration guide)
├── contracts/           # Phase 1 output (API contract changes)
│   └── api.md          # Parse function contract
└── tasks.md             # Phase 2 output (/speckit.tasks - NOT created yet)
```

### Source Code (repository root)

```text
src/
├── typeid/
│   ├── core.cljc           # MODIFY: parse function to return UUID
│   ├── codec.cljc          # NO CHANGE: low-level codec unchanged
│   ├── validation.cljc     # REVIEW: may need UUID validation
│   └── impl/
│       ├── uuid.cljc       # MODIFY: add bytes->uuid function
│       ├── base32.cljc     # NO CHANGE
│       └── util.cljc       # NO CHANGE

test/
├── typeid/
│   ├── core_test.cljc          # MODIFY: expect UUID objects
│   ├── compliance_test.clj     # MODIFY: UUID comparison
│   ├── error_semantics_test.cljc  # REVIEW: error message validation
│   └── impl/
│       └── uuid_test.cljc      # ADD: bytes->uuid tests

dev/
└── benchmarks/
    └── core_bench.clj      # MODIFY: add bytes->uuid benchmark
```

**Structure Decision**: Single library project using standard Clojure src/ and test/ layout. Implementation changes are isolated to:
1. `typeid.impl.uuid` namespace (new `bytes->uuid` function)
2. `typeid.core` namespace (update `parse` function)
3. Test files (update expectations from byte arrays to UUIDs)
4. Documentation (README examples, docstrings)

## Complexity Tracking

> **No violations to justify** - Feature fully compliant with constitution.

This is a straightforward API improvement that:
- Maintains zero dependencies
- Uses pure functions
- Follows existing patterns
- Requires no new abstractions
- Breaking change is acceptable in 0.x stage
