# Implementation Plan: TypeID Clojure/ClojureScript Library

**Branch**: `001-typeid-implementation` | **Date**: 2025-11-10 | **Spec**: [spec.md](spec.md)
**Input**: Feature specification from `/specs/001-typeid-implementation/spec.md`

**Note**: This template is filled in by the `/speckit.plan` command. See `.specify/templates/commands/plan.md` for the execution workflow.

## Summary

Implement a Clojure/ClojureScript library that generates, parses, validates, and converts TypeIDs—type-safe extensions of UUIDv7 with base32 encoding and optional type prefixes. The library must comply with TypeID specification v0.3.0, provide cross-platform compatibility (JVM + JS), achieve sub-microsecond performance for encoding/decoding, and pass 100% of reference test cases. The implementation will use deps.edn + tools.build for builds, Kaocha + test.check for testing, Malli for validation, clj-kondo + cljfmt for code quality, and Codox for documentation.

## Technical Context

**Language/Version**: Clojure 1.11+ (JVM) and ClojureScript (JS target); CI matrix testing Clojure 1.11/1.12 on JDK 17/21

**Primary Dependencies**:
- **Runtime**: ZERO external dependencies (only org.clojure/clojure and org.clojure/clojurescript)
- **Build**: tools.build (build automation, dev-only)
- **Testing**: Kaocha (test runner, dev-only), test.check (property-based testing, dev-only)
- **Validation**: Manual predicate functions (no external libraries at runtime)
- **Linting/Formatting**: clj-kondo (static analysis, dev-only), cljfmt (code formatting, dev-only)
- **Documentation**: Codox (API doc generation, dev-only)
- **Benchmarking**: criterium (performance benchmarking, dev-only)

**Storage**: N/A (pure library, no persistence)

**Testing**: Kaocha with test.check for property-based tests; reference test files (valid.yml, invalid.yml) for compliance testing

**Target Platform**: JVM (Clojure 1.11+) and JavaScript (ClojureScript); Node.js and browser environments for ClojureScript

**Project Type**: Single library project (no backend/frontend split)

**Performance Goals**:
- Encoding: < 1μs per TypeID encoding operation
- Decoding: < 1μs per TypeID decoding operation
- Validation: < 500ns per prefix validation
- Generate + parse round-trip: < 2μs total

**Constraints**:
- Must match reference implementation bit-for-bit for base32 encoding
- **Zero external runtime dependencies** (only org.clojure/clojure and org.clojure/clojurescript allowed)
- Pure functions only (no mutable state)
- Cross-platform API (no platform-specific functions in public API)
- Must pass 100% of valid.yml and invalid.yml test cases

**Scale/Scope**:
- Library scope: ~500-1000 LOC of implementation code
- Public API: ~8-12 functions (generate, parse, validate, encode, decode, utilities)
- Test coverage: >80% overall, 100% for critical paths
- Documentation: README + Codox API docs + quickstart guide

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

### Principle I: API Design ✅ PASS

- **Small, composable functions**: Plan includes separate functions for generate, parse, validate, encode, decode - each with single responsibility
- **Stable public API surface**: Semantic versioning enforced; namespace hygiene with public API in `typeid.core`, internal in `typeid.impl`
- **Data-in/data-out**: All functions pure, accepting maps/strings and returning maps/strings
- **Rationale**: Design follows functional composition patterns standard in Clojure libraries

### Principle II: Code Quality ✅ PASS

- **100% clj-kondo clean**: CI includes clj-kondo step with zero-tolerance for warnings/errors
- **Docstrings on all public vars**: Plan requires docstrings with examples on all public functions
- **Exhaustive examples**: README, Codox, and quickstart.md will include examples for all user stories
- **Rationale**: Code quality gates in CI enforce standards before merge

### Principle III: Compatibility ✅ PASS

- **JVM Clojure 1.11+**: Primary target, tested in CI
- **ClojureScript support**: Explicitly included in plan with CLJC files for cross-platform code
- **CI matrix**: GitHub Actions matrix testing Clojure 1.11/1.12 on JDK 17/21
- **Rationale**: Cross-platform support is a primary requirement (FR-019) and will be tested continuously

### Principle IV: Types & Contracts ✅ PASS

- **Manual validation predicates**: Public entry points will use hand-written predicate functions for input validation (no external libraries)
- **Property-based tests**: test.check will verify round-trip properties and edge cases (dev dependency only)
- **Schema documentation**: Validation predicates will be documented in README and Codox
- **Clear error messages**: Validation failures will specify which rule violated (FR-016)
- **Rationale**: Critical for library correctness; manual predicates keep library dependency-free while maintaining validation

### Principle V: Performance ✅ PASS

- **Avoid unnecessary allocation**: Base32 encoding will use efficient bit manipulation; transients for intermediate buffers
- **Criterium benchmarks**: Benchmarks in `dev/benchmarks/` for encode/decode functions
- **Performance budgets published**: README will document sub-microsecond targets
- **No premature optimization**: Initial implementation focuses on correctness, then optimize based on benchmarks
- **Rationale**: Performance is a success criterion (SC-010); benchmarks ensure targets are met

### Principle VI: Build & Release ✅ PASS

- **Clojure CLI + tools.build**: deps.edn with :dev, :test, :build aliases; build.clj with clean/jar/install/deploy tasks
- **Semantic versioning**: MAJOR.MINOR.PATCH enforced; CHANGELOG.md required
- **Reproducible builds**: All dependencies pinned in deps.edn
- **Automated deploys to Clojars**: GitHub Actions release workflow with GPG signing
- **Rationale**: Specified in user input; aligns with constitution requirements

### Principle VII: Testing ✅ PASS

- **Kaocha + test.check**: Specified in user input; Kaocha config for test organization
- **Deterministic tests**: Unit tests self-contained; reference test files (valid.yml, invalid.yml) checked in
- **CI matrix**: Clojure 1.11/1.12 on JDK 17/21
- **Coverage goals**: >80% overall, 100% for encode/decode/validate
- **Test organization**: tests/ directory with unit tests; integration tests for cross-platform behavior
- **Rationale**: Comprehensive testing is critical for spec compliance (FR-020)

### Principle VIII: Observability ✅ PASS

- **Pure functions by default**: All encoding/decoding/validation functions are pure
- **No implicit global state**: No atoms/refs/agents in library code
- **Clear error semantics**: Validation errors include context (prefix format, suffix overflow, length)
- **Optional logging**: No logging dependencies in runtime; errors returned as data
- **Rationale**: Pure functions enable testing and reasoning; error-as-data is idiomatic Clojure

### Constitution Violations

**NONE** - All principles pass. This plan fully complies with the TypeID Clojure Library Constitution v1.0.0.

## Project Structure

### Documentation (this feature)

```text
specs/001-typeid-implementation/
├── plan.md              # This file (/speckit.plan command output)
├── spec.md              # Feature specification (already created)
├── research.md          # Phase 0 output: UUIDv7 generation, base32 encoding, CLJC strategies
├── data-model.md        # Phase 1 output: TypeID components, validation rules
├── quickstart.md        # Phase 1 output: Getting started guide with examples
├── contracts/           # Phase 1 output: Public API function signatures
│   └── api.md           # Public API contract documentation
├── checklists/
│   └── requirements.md  # Specification quality checklist (already created)
└── tasks.md             # Phase 2 output (/speckit.tasks command - NOT created by /speckit.plan)
```

### Source Code (repository root)

```text
typeid/                  # Repository root
├── deps.edn             # Dependencies and aliases (:dev, :test, :build)
├── build.clj            # tools.build tasks (clean, jar, install, deploy)
├── CHANGELOG.md         # Semantic versioning changelog
├── README.md            # Usage examples, installation, quick start
├── LICENSE              # Open source license
├── .clj-kondo/          # clj-kondo configuration
│   └── config.edn
├── .cljfmt.edn          # cljfmt formatting rules
├── .github/
│   └── workflows/
│       ├── ci.yml       # Lint + test + build + docs
│       ├── release.yml  # Tag-based deploy to Clojars
│       └── bench.yml    # On-demand benchmarking (optional)
│
├── src/
│   └── typeid/
│       ├── core.cljc    # Public API (generate, parse, validate, encode, decode)
│       ├── impl/
│       │   ├── base32.cljc    # Base32 encoding/decoding (Crockford alphabet)
│       │   ├── uuid.cljc      # UUIDv7 generation (platform-specific via reader conditionals)
│       │   ├── prefix.cljc    # Prefix validation logic
│       │   └── util.cljc      # Shared utilities
│       └── validation.cljc    # Public validation predicates
│
├── test/
│   ├── typeid/
│   │   ├── core_test.cljc         # Public API unit tests
│   │   ├── impl/
│   │   │   ├── base32_test.cljc   # Base32 encoding unit tests
│   │   │   ├── uuid_test.cljc     # UUIDv7 generation tests
│   │   │   └── prefix_test.cljc   # Prefix validation tests
│   │   ├── properties_test.cljc   # test.check property-based tests
│   │   └── compliance_test.cljc   # valid.yml + invalid.yml test cases
│   └── resources/
│       ├── valid.yml              # Reference valid test cases
│       └── invalid.yml            # Reference invalid test cases
│
├── dev/
│   ├── benchmarks/
│   │   └── core_bench.clj         # Criterium benchmarks for encode/decode
│   └── user.clj                   # REPL utilities for development
│
└── docs/                          # Generated by Codox (gitignored, published to GitHub Pages)
    └── index.html
```

**Structure Decision**: Single library project structure chosen because this is a standalone library with no backend/frontend split. The `src/` directory uses the standard Clojure layout with a top-level namespace (`typeid.core`) for the public API and `typeid.impl` for internal implementation details. Tests mirror the source structure. CLJC files enable cross-platform compatibility via reader conditionals for platform-specific code (UUIDv7 generation). Benchmarks and dev utilities in `dev/` are excluded from the library jar.

## Complexity Tracking

> **Fill ONLY if Constitution Check has violations that must be justified**

No violations detected. All constitution principles pass. No complexity justification required.

---

## Post-Design Constitution Re-Evaluation

*Re-checked after Phase 1 design completion (2025-11-10)*

### Updated Assessment

All 8 constitution principles remain **PASSING** after detailed design:

**✅ I. API Design**:
- Public API defined in [contracts/api.md](contracts/api.md) with 8 composable functions
- Namespace hygiene: `typeid.core` (public), `typeid.impl.*` (internal)
- All functions pure, data-in/data-out confirmed

**✅ II. Code Quality**:
- Docstring format specified in contracts with examples
- README, Codox, and quickstart.md planned in quickstart
- clj-kondo CI step specified in research.md

**✅ III. Compatibility**:
- CLJC strategy documented in research.md
- CI matrix (Clojure 1.11/1.12, JDK 17/21) confirmed
- Cross-platform differences isolated via reader conditionals

**✅ IV. Types & Contracts**:
- Manual validation predicates defined in data-model.md (Prefix, TypeIDString, UUIDBytes, etc.)
- Property-based test strategy with test.check specified in research.md (dev dependency only)
- Validation failures return structured error data (`:type`, `:message`, `:data`)
- Zero external validation libraries at runtime

**✅ V. Performance**:
- Performance targets documented in contracts/api.md (< 1μs encode/decode)
- Criterium benchmarks planned in `dev/benchmarks/`
- Optimization strategies identified: transients, lookup tables, bit manipulation

**✅ VI. Build & Release**:
- deps.edn structure specified with :dev, :test, :build aliases
- build.clj tasks defined (clean, jar, install, deploy)
- GitHub Actions workflows planned (ci.yml, release.yml)
- CHANGELOG.md and semantic versioning required

**✅ VII. Testing**:
- Three-tier testing strategy: unit (Kaocha), property (test.check), compliance (valid.yml/invalid.yml)
- Test organization mirrors source structure
- Coverage goals: >80% overall, 100% for critical paths

**✅ VIII. Observability**:
- Pure functions throughout (confirmed in data-model.md)
- No global state (atoms/refs/agents)
- Error-as-data pattern: `{:error {:type :message :data}}`

### Design Artifacts Summary

| Artifact | Status | Location | Key Decisions |
|----------|--------|----------|---------------|
| Technical Context | ✅ Complete | plan.md | Clojure 1.11+/CLJS, deps.edn, Kaocha, clj-kondo, Codox, **zero runtime deps** |
| Research | ✅ Complete | research.md | Custom UUIDv7, bit-manipulation base32, CLJC + reader conditionals |
| Data Model | ⚠️ Needs Update | data-model.md | TypeID/ParsedTypeID/UUID entities, ~~Malli schemas~~→manual predicates, validation rules |
| API Contracts | ✅ Complete | contracts/api.md | 8 public functions, error-as-data, performance targets |
| Quickstart | ✅ Complete | quickstart.md | 5 usage patterns, integration examples, FAQ |
| Agent Context | ✅ Updated | CLAUDE.md | Language, dependencies, project type added |

### Constitution Compliance Score: 8/8 PASS ✅

**No violations introduced during design phase.** All principles remain satisfied. Ready to proceed to `/speckit.tasks` for implementation task generation.
