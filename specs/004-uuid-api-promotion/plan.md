# Implementation Plan: UUID API Promotion

**Branch**: `004-uuid-api-promotion` | **Date**: 2025-11-13 | **Spec**: [spec.md](spec.md)
**Input**: Feature specification from `/specs/004-uuid-api-promotion/spec.md`

**Note**: This template is filled in by the `/speckit.plan` command. See `.specify/templates/commands/plan.md` for the execution workflow.

## Summary

Promote `typeid.impl.uuid` namespace to `typeid.uuid` as part of the official public API. This makes three UUID utility functions publicly available: `uuid->bytes`, `bytes->uuid`, and `generate-uuidv7`. The old `impl` namespace will be removed entirely (breaking change acceptable in 0.x). This addresses documented user needs in the README FAQ and provides a stable API contract for UUID operations.

## Technical Context

**Language/Version**: Clojure 1.11+ (JVM), ClojureScript 1.12+ (JS)
**Primary Dependencies**: Zero runtime dependencies (org.clojure/clojure and org.clojure/clojurescript only)
**Storage**: N/A (library only, no persistence)
**Testing**: Kaocha + test.check for property-based testing
**Target Platform**: JVM (Clojure 1.11+) and JavaScript (ClojureScript)
**Project Type**: Single library project
**Performance Goals**: No change from existing implementation (< 1μs for byte conversion operations)
**Constraints**:
- Zero external runtime dependencies (constitutional requirement)
- Must maintain cross-platform compatibility (JVM and ClojureScript)
- Performance within 5% of current implementation
- Zero breaking changes to existing public API (`typeid.core`, `typeid.codec`)
**Scale/Scope**:
- 3 public functions being promoted
- 1 new public namespace (`typeid.uuid`)
- 1 namespace being removed (`typeid.impl.uuid`)
- Existing test suite must pass without modification

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

### Principle I: API Design ✅

- **Small, composable functions**: All three promoted functions (`uuid->bytes`, `bytes->uuid`, `generate-uuidv7`) are pure, single-purpose functions
- **Stable public API surface**: Moving from `.impl` to public namespace establishes stability contract; breaking change handled via MAJOR/MINOR version bump
- **Explicit namespace hygiene**: `typeid.uuid` clearly designates public API; removing `.impl` namespace aligns with hygiene requirements
- **Data-in/data-out**: All three functions are pure (UUID/bytes → bytes/UUID, no side effects)

**Status**: ✅ PASS - Promotes good API design by making useful utilities officially public

### Principle II: Code Quality ✅

- **100% clj-kondo clean**: Existing code is already kondo-clean; namespace rename won't introduce warnings
- **Docstrings on all public vars**: Existing functions have comprehensive docstrings with examples
- **Exhaustive examples**: README will be updated with public namespace examples (already has examples for impl namespace)

**Status**: ✅ PASS - No code changes needed, only namespace promotion and documentation updates

### Principle III: Compatibility ✅

- **JVM Clojure 1.11+ by default**: Already supported
- **ClojureScript support**: Already supported and tested
- **Babashka support**: N/A for this feature
- **Compatibility matrix**: Existing CI already tests both platforms

**Status**: ✅ PASS - No compatibility changes

### Principle IV: Types & Contracts ✅

- **Manual validation predicates**: Existing functions validate inputs (type checks for UUID objects and byte arrays)
- **Property-based tests**: Existing test suite includes property-based tests for round-trip operations
- **Schema documentation**: Functions already documented; public API will improve discoverability
- **No silent failures**: All functions throw clear exceptions on invalid input

**Status**: ✅ PASS - Existing validation maintained

### Principle V: Performance ✅

- **Avoid unnecessary allocation**: No implementation changes, existing optimizations preserved
- **Benchmark with criterium**: No new functionality; performance must remain within 5% (success criterion)
- **Publish performance budgets**: Existing budgets apply (< 1μs for byte conversions)
- **No premature optimization**: N/A (no new code)

**Status**: ✅ PASS - Performance maintained

### Principle VI: Build & Release ✅

- **Clojure CLI + tools.build**: Already in use
- **Semantic versioning**: Breaking change will trigger MINOR version bump (0.x allows breaking changes)
- **Reproducible builds**: Already ensured
- **Signed and automated deploys**: Already in place
- **Changelog**: CHANGELOG.md will document breaking change (FR-010)

**Status**: ✅ PASS - Standard release process applies

### Principle VII: Testing ✅

- **Kaocha + test.check**: Already in use
- **Deterministic tests**: Existing tests are deterministic
- **CI matrix**: Already tests supported versions
- **Coverage goals**: Existing coverage maintained (no new code)
- **Test organization**: Tests already organized properly

**Status**: ✅ PASS - Existing test suite applies without modification

### Principle VIII: Observability ✅

- **Pure functions by default**: All three functions are pure
- **No implicit global state**: None introduced
- **Clear error semantics**: Existing error handling preserved
- **Logging**: N/A (pure functions, no logging needed)

**Status**: ✅ PASS - No observability concerns

### Overall Constitution Check Result

**✅ ALL GATES PASSED**

No violations detected. This is a straightforward namespace promotion that aligns with all constitutional principles. The change improves API hygiene by promoting useful utilities from private (`impl`) to public namespace.

## Project Structure

### Documentation (this feature)

```text
specs/[###-feature]/
├── plan.md              # This file (/speckit.plan command output)
├── research.md          # Phase 0 output (/speckit.plan command)
├── data-model.md        # Phase 1 output (/speckit.plan command)
├── quickstart.md        # Phase 1 output (/speckit.plan command)
├── contracts/           # Phase 1 output (/speckit.plan command)
└── tasks.md             # Phase 2 output (/speckit.tasks command - NOT created by /speckit.plan)
```

### Source Code (repository root)

```text
src/typeid/
├── core.cljc           # Main TypeID API (unchanged)
├── codec.cljc          # Encoding/decoding (unchanged)
├── uuid.cljc           # NEW: Public UUID utilities (promoted from impl.uuid)
└── impl/
    ├── base32.cljc     # Internal base32 implementation
    ├── validation.cljc # Internal validation
    └── uuid.cljc       # TO BE REMOVED: Old implementation location

test/typeid/
├── core_test.cljc      # Core API tests
├── codec_test.cljc     # Codec tests
├── uuid_test.cljc      # UUID tests (namespace import updated)
└── impl/
    └── base32_test.cljc # Internal tests
```

**Structure Decision**: Single library project. The change involves:
1. Creating new `src/typeid/uuid.cljc` with public API
2. Moving implementation from `src/typeid/impl/uuid.cljc` to new location
3. Updating namespace declarations to remove `^:no-doc` metadata
4. Updating internal references in `typeid.core` and `typeid.codec`
5. Updating test imports to use `typeid.uuid` instead of `typeid.impl.uuid`
6. Removing old `src/typeid/impl/uuid.cljc` file

## Complexity Tracking

**No violations** - All constitution checks passed. This section is not applicable.

---

## Phase 0: Research ✅

**Status**: Complete
**Output**: [research.md](research.md)

**Key Decisions**:
1. Namespace structure: `typeid.uuid` as peer to `typeid.core`
2. Breaking change strategy: Remove old namespace entirely, changelog notice only
3. Documentation updates: README FAQ section and all docstrings
4. Testing strategy: Reuse existing tests, update imports only
5. Performance validation: No changes expected, verify with benchmarks

**Findings**: All technical unknowns resolved. This is a straightforward namespace promotion with minimal risk.

---

## Phase 1: Design & Contracts ✅

**Status**: Complete

**Artifacts Generated**:
- [data-model.md](data-model.md) - UUID data types and transformations
- [contracts/api.md](contracts/api.md) - Public API contract for typeid.uuid namespace
- [quickstart.md](quickstart.md) - User-facing guide for UUID utilities

**Design Summary**:
- **Data Types**: UUID objects (platform-native), byte arrays (16 bytes), UUIDv7 structure
- **Public Functions**: `uuid->bytes`, `bytes->uuid`, `generate-uuidv7`
- **Error Handling**: Structured `ex-info` exceptions with detailed error data
- **Cross-Platform**: JVM (java.util.UUID) and ClojureScript (cljs.core/UUID) support
- **Performance**: All functions meet budget (< 1-2μs)

**Constitution Re-Check**: ✅ All gates still PASS

No design changes that would violate constitutional principles. The public API maintains:
- Pure functions (data in, data out)
- Manual validation predicates (no external dependencies)
- Comprehensive documentation with examples
- Cross-platform compatibility
- Performance within budgets

**Agent Context Updated**: ✓ CLAUDE.md updated with feature technologies

---

## Next Steps

This plan is now complete through Phase 1. To proceed with implementation:

1. Run `/speckit.tasks` to generate actionable task list
2. Execute tasks in dependency order
3. Verify all success criteria from spec.md

**Ready for task generation** ✅
