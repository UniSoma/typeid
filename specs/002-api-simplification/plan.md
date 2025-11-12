# Implementation Plan: TypeID API Simplification

**Branch**: `002-api-simplification` | **Date**: 2025-11-11 | **Spec**: [spec.md](./spec.md)
**Input**: Feature specification from `/specs/002-api-simplification/spec.md`

**Note**: This template is filled in by the `/speckit.plan` command. See `.specify/templates/commands/plan.md` for the execution workflow.

## Summary

Refactor the TypeID library API to simplify error handling and organize functionality by removing the data-as-error paradigm, renaming/reorganizing functions for clarity, and separating low-level codec operations into a dedicated namespace. The core changes include: replacing `validate` with `explain` (returns nil for valid, error map for invalid), renaming `typeid->map` to `parse` (throws exceptions on invalid input), adding two-arity `create` function to accept existing UUIDs, and moving codec functions (`encode`, `decode`, `uuid->hex`, `hex->uuid`) to a new `typeid.codec` namespace.

## Technical Context

**Language/Version**: Clojure 1.11+, ClojureScript (targeting JVM and JS platforms)
**Primary Dependencies**: Zero runtime dependencies (library only); Kaocha + test.check for testing
**Storage**: N/A (library does not handle storage)
**Testing**: Kaocha with test.check for property-based testing; CI matrix tests Clojure 1.11/1.12 on JDK 17/21
**Target Platform**: JVM (Java 17+) and JavaScript (via ClojureScript)
**Project Type**: Single library project with dual platform support
**Performance Goals**: < 1μs encoding/decoding per TypeID on typical hardware
**Constraints**: Zero external runtime dependencies; spec compliance (must pass all TypeID spec v0.3.0 test cases)
**Scale/Scope**: Small, focused library (~500 LOC); public API surface of ~8 functions across 2 namespaces

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

### API Design (Principle I)
✅ **PASS**: This refactoring improves API composability by:
- Removing redundant functions (`validate` → `explain`, removing `typeid->map`)
- Separating concerns (codec operations → `typeid.codec` namespace)
- Maintaining data-in/data-out pattern with explicit error handling via exceptions
- No breaking of namespace hygiene (codec functions moved to clearly separated namespace)

### Code Quality (Principle II)
✅ **PASS**: Existing code is 100% clj-kondo clean; refactoring will maintain this standard. All public functions have docstrings. This change requires updating:
- Docstrings for renamed/new functions (`explain`, `parse`, two-arity `create`)
- README examples for new API
- Codox documentation

### Compatibility (Principle III)
✅ **PASS**: Refactoring maintains existing JVM Clojure 1.11+ and ClojureScript support. No changes to platform compatibility. CI matrix testing remains unchanged.

### Types & Contracts (Principle IV)
✅ **PASS**:
- Manual validation predicates used (no external libraries)
- `explain` function provides clear validation with error maps
- `parse` and `create` use exceptions with structured data (`:type`, `:message`, context)
- All validation remains manual predicate-based (zero runtime dependencies maintained)

### Performance (Principle V)
✅ **PASS**: Performance goals clearly defined (<1μs encoding/decoding). No unnecessary allocation in hot paths. Benchmarking strategy specified using criterium.

### Build & Release (Principle VI)
✅ **PASS**: Initial release (v1.0.0):
- Clean API design with core and codec namespaces
- Exception-based error handling
- Two-arity `create` for UUID encoding
- Zero runtime dependencies

### Testing (Principle VII)
✅ **PASS**: Comprehensive test suite will be implemented for all API functions. Coverage goals defined (>80% overall, 100% for critical paths). Test.check property-based tests included for key invariants.

### Observability (Principle VIII)
✅ **PASS**: All functions remain pure. Exception-based error handling provides clearer error semantics than data-as-error pattern. No global state introduced.

### Project-Specific Constraints
✅ **PASS**:
- Zero external runtime dependencies enforced
- TypeID spec v0.3.0 compliance required
- Performance goals defined and achievable

**GATE STATUS**: ✅ **APPROVED** - No violations. Clean initial API design with clear separation of concerns.

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
src/
└── typeid/
    ├── core.clj           # Main public API (generate, create, explain, parse)
    ├── codec.clj          # NEW: Low-level codec operations (encode, decode, uuid->hex, hex->uuid)
    └── validation.clj     # Internal validation (unchanged)

test/
└── typeid/
    ├── core_test.clj      # Tests for main API functions
    ├── codec_test.clj     # NEW: Tests for codec namespace
    └── validation_test.clj # Validation tests (unchanged)
```

**Structure Decision**: Single project structure with standard Clojure layout. The refactoring introduces one new namespace (`typeid.codec`) to separate low-level codec operations from high-level API functions in `typeid.core`. The `validation.clj` internal namespace remains unchanged. All modified and new namespaces will have corresponding test files.

## Complexity Tracking

N/A - No constitution violations. All changes align with project principles.

---

## Phase 0: Research (Complete)

**Output**: `research.md`

**Key Decisions**:
1. **Error handling**: Mixed approach - `explain` returns data, other functions throw exceptions
2. **Function naming**: `explain` chosen for semantic clarity
3. **Codec organization**: Separate `typeid.codec` namespace for low-level operations
4. **UUID acceptance**: Platform-native UUID objects, version-agnostic
5. **Exception structure**: Consistent `:type`, `:message`, context map format

**All NEEDS CLARIFICATION items resolved**: ✅

---

## Phase 1: Design & Contracts (Complete)

### Data Model

**Output**: `data-model.md`

**Key Entities**:
1. **TypeID String**: External representation (`[prefix_]suffix`)
2. **Components Map**: Decomposed parts (`:prefix`, `:suffix`, `:uuid`, `:typeid`)
3. **Error Map**: Structured validation/exception data (`:type`, `:message`, context)
4. **UUID**: Platform-native 128-bit identifier (version-agnostic)
5. **Prefix**: Optional type identifier (string/keyword/nil)

**Validation Strategy**: Manual predicates (no external libraries, per constitution)

### API Contracts

**Output**: `contracts/api.md`

**Public API Surface**:

**typeid.core** (3 functions):
- `create` - Create TypeID with 3 arities:
  - 0-arity: Generate new TypeID with fresh UUIDv7 (replaces `generate` with no args)
  - 1-arity: Generate new TypeID with prefix + fresh UUIDv7 (replaces `generate` with prefix)
  - 2-arity: Create TypeID from existing UUID + prefix
- `explain` - Validate and explain errors (returns nil or error map)
- `parse` - Parse TypeID to components (throws exception on invalid)

**typeid.codec** (4 functions):
- `encode` - UUID bytes + prefix → TypeID string
- `decode` - TypeID string → UUID bytes
- `uuid->hex` - UUID bytes → hex string
- `hex->uuid` - Hex string → UUID bytes

**Error Handling**: Exception-based with structured `ex-info` data

**Performance Budgets**: All operations <1-5μs (maintained from original)

### Quickstart Guide

**Output**: `quickstart.md`

**Coverage**:
- All major use cases with examples
- Common error handling patterns
- Platform-specific notes (JVM/ClojureScript)
- Performance characteristics

### Agent Context Update

**Status**: ✅ Updated `CLAUDE.md` with new technologies

---

## Constitution Check (Post-Design Re-evaluation)

*Re-evaluating all principles after Phase 1 design completion*

### API Design (Principle I)
✅ **PASS** (confirmed):
- **Small, composable functions**: 8 total functions across 2 namespaces, each with single clear purpose
- **Stable public API**: Initial release with clean, well-documented API surface
- **Namespace hygiene**: Core operations in `typeid.core`, low-level operations in `typeid.codec`, internal in `typeid.validation`
- **Data-in/data-out**: All functions pure, no side effects (error handling via exceptions per Clojure conventions)

**Design validation**:
- API surface: 7 functions total (3 core + 4 codec)
- Clear separation: user-facing operations vs advanced codec utilities
- No redundancy: Single validation function (`explain`), single parse function (`parse`), single creation function (`create` with 3 arities)

### Code Quality (Principle II)
✅ **PASS** (confirmed):
- **clj-kondo clean**: Design maintains existing standards, no new patterns that would violate
- **Docstrings**: All 8 public functions will have comprehensive docstrings (examples provided in contracts)
- **Examples**: Quickstart covers all use cases; API contract includes examples for each function
- **Documentation plan**: README, Codox, quickstart, API contracts all specified

**Design validation**:
- Quickstart.md provides 15+ usage examples covering all functions
- API contract includes pre/post-conditions and error cases for all functions
- Clear documentation for all use cases

### Compatibility (Principle III)
✅ **PASS** (confirmed):
- **JVM Clojure 1.11+**: No changes to platform support
- **ClojureScript**: Maintained via reader literals and platform-native UUID handling
- **CI matrix**: No changes required (still tests 1.11/1.12 on JDK 17/21)
- **Platform-native UUID**: Design explicitly handles both `java.util.UUID` and `cljs.core/UUID`

**Design validation**:
- Data model documents platform-specific UUID handling
- Quickstart includes platform-specific examples for JVM and ClojureScript
- No new dependencies that would break platform compatibility

### Types & Contracts (Principle IV)
✅ **PASS** (confirmed):
- **Manual validation**: All predicates hand-written (documented in data-model.md)
- **Property-based tests**: API contract specifies 5 key properties to test with test.check
- **Schema documentation**: Data model documents all schemas as predicate specifications
- **Fast failures**: Error maps and exceptions provide clear, actionable messages
- **Zero runtime deps**: Design maintains this constraint (only dev/test deps)

**Design validation**:
- Data-model.md includes complete predicate definitions for all validations
- Error map structure standardized across all functions
- API contract includes property-based test specifications
- No external validation libraries required (manual predicates only)

### Performance (Principle V)
✅ **PASS** (confirmed):
- **No unnecessary allocation**: Efficient encoding/decoding implementation planned
- **Performance budgets**: All operations <1-5μs (documented in API contract)
- **Benchmarks**: Benchmark suite specified for all operations
- **No premature optimization**: Focus on correct implementation first, optimize as needed

**Design validation**:
- API contract documents performance budgets for all operations
- Data model notes allocation patterns for all operations
- Quickstart includes performance notes for user reference

### Build & Release (Principle VI)
✅ **PASS** (confirmed):
- **Clojure CLI + tools.build**: Standard Clojure tooling used
- **Semantic versioning**: Initial release v1.0.0
- **Reproducible builds**: Deterministic library-only code
- **Signed deploys**: Standard Clojars deployment process
- **Changelog**: Initial release documented in quickstart and API contract

**Design validation**:
- Clean initial API design
- Version 1.0.0 correctly specified in all documentation
- Clear API organization documented

### Testing (Principle VII)
✅ **PASS** (confirmed):
- **Kaocha + test.check**: Standard Clojure testing tools specified
- **Deterministic tests**: Design includes property-based tests that are deterministic when seeded
- **CI matrix**: Tests run on Clojure 1.11/1.12, JDK 17/21, and ClojureScript
- **Coverage goals**: API contract specifies >80% overall, 100% for critical paths
- **Test organization**: Standard structure (unit tests in test/, property-based tests integrated)

**Design validation**:
- API contract includes contract tests with test.check properties
- Data model documents validation predicates suitable for testing
- Quickstart includes testing examples

### Observability (Principle VIII)
✅ **PASS** (confirmed):
- **Pure functions**: All 8 public functions are pure transformations
- **No global state**: No atoms, refs, or agents introduced
- **Clear error semantics**: Error maps and exceptions include context (`:input`, `:expected`, `:actual`)
- **No logging**: Library functions remain side-effect free

**Design validation**:
- Data model confirms all transformations are pure
- Error map structure includes debugging context
- No I/O or state introduced in design

### Project-Specific Constraints
✅ **PASS** (confirmed):
- **Zero external runtime dependencies**: Design enforces this (manual predicates only)
- **Base32 implementation**: Must implement per TypeID spec v0.3.0
- **TypeID spec v0.3.0 compliance**: All validations match spec requirements
- **Performance goals**: <1μs encoding/decoding target

**Design validation**:
- No runtime dependencies specified
- Base32 implementation required per spec
- All validation rules from spec documented in data-model.md

---

## Post-Design Gate Status

**FINAL VERDICT**: ✅ **APPROVED FOR IMPLEMENTATION**

**Summary**:
- All 8 constitution principles satisfied
- No violations or complexity justifications needed
- Clean initial API design with unified `create` function
- Version 1.0.0 (initial release)
- Zero runtime dependencies
- Performance goals defined and achievable
- Documentation complete and thorough

**Ready for Phase 2**: Task generation (`/speckit.tasks`) can proceed

---

## Implementation Notes

### Critical Success Factors

1. **Maintain TypeID spec compliance**: All validation predicates must match spec v0.3.0 exactly
2. **Cross-platform consistency**: Behavior must be identical on JVM and ClojureScript
3. **Error message quality**: All error messages must be clear, actionable, and consistent
4. **Test coverage**: Critical paths (encode, decode, parse) must maintain 100% coverage
5. **Performance**: No regressions in benchmark measurements

### Risk Mitigation

| Risk | Mitigation |
|------|------------|
| Performance issues | Benchmark all operations, verify <1μs goals |
| Platform inconsistency | Test matrix covers both JVM and ClojureScript |
| Poor error messages | Review all error messages for clarity and actionability |
| Incomplete test coverage | Aim for >80% overall, 100% for critical paths |

### Testing Strategy

1. **Unit tests**: Cover all 8 public functions with valid/invalid inputs
2. **Property-based tests**: 5 key properties (round-trip, validation consistency, etc.)
3. **Edge cases**: All-zeros UUID, max-length prefix, empty prefix, non-string inputs
4. **Error tests**: Verify exception structure and error map format
5. **Cross-platform**: Run full suite on JVM (Clojure 1.11, 1.12) and ClojureScript

### Documentation Checklist

- [ ] Write README with usage examples
- [ ] Write all docstrings with examples and exceptions
- [ ] Generate Codox documentation
- [ ] Create CHANGELOG for initial release
- [ ] Prepare release announcement

---

## Next Steps

1. **Run `/speckit.tasks`**: Generate actionable, dependency-ordered tasks.md
2. **Run `/speckit.implement`**: Execute implementation plan
3. **Verify**: Run tests, benchmarks, linting
4. **Document**: Write README, generate Codox, create CHANGELOG
5. **Release**: Tag v1.0.0, deploy to Clojars, announce initial release
