# Research: UUID API Promotion

**Feature**: 004-uuid-api-promotion
**Date**: 2025-11-13
**Status**: Complete

## Overview

This feature involves promoting existing UUID utility functions from the internal `typeid.impl.uuid` namespace to a public `typeid.uuid` namespace. Since the implementation already exists and is well-tested, this research focuses on migration strategy, documentation updates, and breaking change communication.

## Technical Decisions

### Decision 1: Namespace Structure

**Decision**: Create `typeid.uuid` as a peer to `typeid.core` and `typeid.codec`

**Rationale**:
- Follows existing naming convention (single-word, descriptive namespace)
- Places UUID utilities at same level as other public APIs
- Makes import path intuitive: `(require '[typeid.uuid :as uuid])`
- Aligns with Clojure community conventions for utility namespaces

**Alternatives Considered**:
1. **`typeid.utils.uuid`** - Rejected because:
   - Adds unnecessary nesting for just three functions
   - "utils" is often overloaded with miscellaneous helpers
   - Doesn't match existing flat namespace structure (`core`, `codec`)

2. **Keep in `typeid.core`** - Rejected because:
   - Would mix TypeID-specific operations with UUID utilities
   - Users who only need UUID operations would be forced to import `core`
   - Violates single-responsibility principle for namespaces

3. **`typeid.uuidv7`** - Rejected because:
   - Two functions (`uuid->bytes`, `bytes->uuid`) work with any UUID version
   - Name would be misleading for general UUID utilities
   - Would require rename if supporting other UUID versions later

**Implementation Notes**:
- Remove `^:no-doc` metadata from namespace declaration
- Add comprehensive namespace docstring explaining purpose and usage
- Ensure all three functions have updated docstrings referencing public namespace

### Decision 2: Breaking Change Strategy

**Decision**: Remove `typeid.impl.uuid` entirely, document in CHANGELOG, no migration period

**Rationale**:
- Library is in 0.x phase (pre-1.0.0), breaking changes are acceptable
- The `impl` namespace designation signals "internal/private" API
- Few users likely depend on it given private namespace convention
- Simple changelog notice is sufficient for 0.x library
- Clean break prevents maintaining dual namespaces

**Alternatives Considered**:
1. **Deprecation period with alias** - Rejected because:
   - Adds maintenance burden for minimal benefit
   - 0.x phase allows breaking changes without deprecation
   - "impl" namespace already signals instability
   - Users accessing impl namespace understand risks

2. **Keep both namespaces** - Rejected because:
   - Creates confusion about which to use
   - Violates "one obvious way" principle
   - Increases maintenance surface

**Implementation Notes**:
- Update CHANGELOG.md with clear "BREAKING CHANGE" notice
- Include migration example in changelog (replace `typeid.impl.uuid` with `typeid.uuid`)
- Version bump as MINOR in 0.x (would be MAJOR in 1.x+)

### Decision 3: Documentation Updates

**Decision**: Update README to replace all `typeid.impl.uuid` references with `typeid.uuid`

**Rationale**:
- README already documents UUID utilities in FAQ section (line 616-623)
- Examples should reference public API, not internal implementation
- Improves discoverability for new users
- Aligns documentation with API stability promise

**Locations to Update**:
1. **README.md**:
   - Line 616-623: FAQ section "How do I get UUID bytes if I need them?"
   - Update example from `typeid.impl.uuid` to `typeid.uuid`
   - Add note about breaking change in v0.3.0 (or next version)

2. **Docstrings**:
   - Remove `^:no-doc` from `typeid.uuid` namespace
   - Add namespace-level docstring with usage examples
   - Ensure function docstrings mention they're part of public API

3. **CHANGELOG.md**:
   - Add entry under next version (e.g., 0.3.0)
   - Format: "BREAKING: Moved UUID utilities from `typeid.impl.uuid` to `typeid.uuid`"
   - Include migration note: "Replace `typeid.impl.uuid` with `typeid.uuid` in requires"

**Implementation Notes**:
- Search codebase for all references to `typeid.impl.uuid`
- Update internal imports in `typeid.core` and `typeid.codec`
- Update test imports
- Verify no lingering references in documentation

### Decision 4: Testing Strategy

**Decision**: Reuse existing test suite without modification, only update imports

**Rationale**:
- No behavioral changes to functions
- Existing tests already cover all edge cases
- Round-trip property tests already verify correctness
- Test coverage is adequate (constitutional requirement met)

**Verification Steps**:
1. Update test namespace imports from `typeid.impl.uuid` to `typeid.uuid`
2. Run full test suite on both JVM and ClojureScript
3. Verify zero test failures
4. Confirm coverage remains unchanged

**Implementation Notes**:
- Primary test file: `test/typeid/uuid_test.cljc` (or similar)
- Update require statement: `[typeid.uuid :as uuid]`
- No test logic changes needed

### Decision 5: Performance Validation

**Decision**: No performance changes expected; validate with existing benchmarks

**Rationale**:
- Namespace rename doesn't affect compiled bytecode
- JVM and JavaScript runtimes optimize based on function implementation, not namespace
- No algorithm changes, no new allocations
- Existing performance budgets apply (< 1μs for byte conversions)

**Verification Steps**:
1. Run existing criterium benchmarks for UUID operations
2. Compare results before and after namespace change
3. Ensure < 5% variance (within noise tolerance)
4. Document results in plan or task notes

**Implementation Notes**:
- Benchmark file likely in `dev/benchmarks/` or similar
- Functions to benchmark: `uuid->bytes`, `bytes->uuid`, `generate-uuidv7`
- Use consistent hardware/environment for before/after comparison

## Research Findings Summary

### Key Insights

1. **Low-Risk Change**: This is primarily a namespace rename with no implementation changes, minimizing risk of introducing bugs.

2. **User Impact**: Minimal user impact expected given:
   - `impl` namespace signals private API
   - README already documents these functions
   - 0.x version allows breaking changes

3. **Documentation is Key**: Success depends more on clear communication (changelog, README) than code changes.

4. **No Technical Unknowns**: All implementation details already resolved in existing code. The only work is moving files and updating references.

### Risks and Mitigations

| Risk | Likelihood | Impact | Mitigation |
|------|-----------|--------|------------|
| Users depending on `impl` namespace | Low | Medium | Clear changelog notice with migration path |
| Performance regression | Very Low | Medium | Run benchmarks before/after to verify |
| Test failures after rename | Very Low | High | Comprehensive test run on both platforms |
| Missed references in docs | Low | Low | Systematic search for all `impl.uuid` references |

### Open Questions

None. All technical decisions resolved.

## Conclusion

This is a straightforward namespace promotion with well-understood requirements. The existing implementation is solid, well-tested, and performant. The primary work is mechanical (moving files, updating imports) with careful attention to documentation and communication of the breaking change.

**Ready to proceed to Phase 1: Design & Contracts**
