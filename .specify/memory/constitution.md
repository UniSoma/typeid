<!--
Sync Impact Report:
Version: 1.1.0 (Amendment - Zero Runtime Dependencies)
Ratification Date: 2025-11-10
Last Amended: 2025-11-10

Changes:
- MINOR version bump (principle materially expanded)
- Amended Principle IV (Types & Contracts): Removed Malli as runtime dependency option
- Validation must now use manual predicates (no external validation libraries at runtime)
- Updated Technology Stack: Removed Malli from validation stack
- Updated Constraints: Clarified zero external runtime dependencies (was ambiguous before)
- Malli remains acceptable as dev/test dependency only

Templates Status:
✅ plan-template.md - Reviewed, Constitution Check section aligns with principles
✅ spec-template.md - Reviewed, requirements structure supports library design
✅ tasks-template.md - Reviewed, task categorization supports principle-driven development
✅ Command files - All speckit commands reviewed for consistency

Follow-up Items:
- Update plan.md to remove Malli from runtime dependencies
- Update research.md validation strategy
- Update data-model.md schemas (use manual predicates)
- Update contracts/api.md (remove Malli schema references)
-->

# TypeID Clojure Library Constitution

## Core Principles

### I. API Design

All public APIs MUST follow these non-negotiable rules:

- **Small, composable functions**: Each function does one thing well, accepts data, returns data
- **Stable public API surface**: Breaking changes require MAJOR version bump; deprecation warnings MUST precede removal by at least one MINOR version
- **Explicit namespace hygiene**: Public API in dedicated namespace(s), internal implementation in separate namespace(s) with clear `-internal` or `.impl` naming
- **Data-in/data-out**: Pure functions by default, no side effects in core library functions unless explicitly documented

**Rationale**: Clojure's strength lies in composable data transformation. A stable, predictable API enables library users to upgrade confidently and compose functionality freely.

### II. Code Quality

Code quality gates are NON-NEGOTIABLE:

- **100% clj-kondo clean**: Zero warnings or errors in CI; violations block merge
- **Docstrings on all public vars**: Every `defn`, `def`, `defmacro` in public namespaces MUST have a docstring explaining purpose, parameters, return value, and at least one example
- **Exhaustive examples**: README MUST include examples for all major use cases; Codox documentation MUST be generated and complete

**Rationale**: High-quality documentation and static analysis prevent bugs and reduce support burden. Docstrings are executable documentation that IDEs and tooling can leverage.

### III. Compatibility

Target environments MUST be explicitly stated and tested:

- **JVM Clojure 1.11+ by default**: All code MUST run on Clojure 1.11 or later on the JVM
- **ClojureScript support**: If applicable, MUST be explicitly stated in README with supported versions and tested in CI
- **Babashka support**: If applicable, MUST be explicitly stated in README and tested in CI; incompatibilities (e.g., Java interop) MUST be documented
- **Compatibility matrix**: CI MUST test against all supported Clojure versions (minimum: oldest + latest)

**Rationale**: Clojure's ecosystem spans JVM, JS, and native environments. Explicit compatibility statements prevent surprises and enable confident adoption.

### IV. Types & Contracts

Data shape validation and testing are MANDATORY:

- **Manual validation predicates**: All public entry points MUST validate input data shapes using hand-written predicate functions (no external validation libraries at runtime); validation failures MUST return clear error messages
- **Property-based tests with test.check**: Critical functions MUST have generative tests that verify properties hold across random inputs
- **Schema documentation**: Data schemas (as predicate specs) MUST be documented in README and/or Codox
- **No silent failures**: Invalid data MUST fail fast with actionable error messages
- **Optional dev/test validation libraries**: Malli, clojure.spec, or similar MAY be used in dev/test for generative testing or schema-based test generation, but MUST NOT be runtime dependencies

**Rationale**: Runtime validation catches errors early. Manual predicates keep library dependency-free. Property-based testing reveals edge cases unit tests miss. Clear schemas (even as predicates) are executable specifications.

### V. Performance

Performance characteristics MUST be measured and documented:

- **Avoid unnecessary allocation in hot paths**: Use transients, avoid intermediate collections where appropriate
- **Benchmark with criterium**: Critical functions (called in loops, processing large data) MUST have criterium benchmarks in `dev/benchmarks/`
- **Publish performance budgets**: Where relevant (e.g., encoding/decoding functions), document expected performance (e.g., "< 1μs per encoding on typical hardware")
- **No premature optimization**: Optimize only after profiling shows need; document trade-offs

**Rationale**: Performance matters for library adoption. Benchmarks prevent regressions and guide optimization efforts.

### VI. Build & Release

Build and release process MUST be reproducible and automated:

- **Clojure CLI (deps.edn) + tools.build**: Use standard Clojure tooling for builds; avoid Leiningen for new projects
- **Semantic versioning**: MAJOR.MINOR.PATCH strictly enforced; breaking changes = MAJOR, new features = MINOR, bug fixes = PATCH
- **Reproducible builds**: Builds MUST be deterministic given the same input; pin all dependency versions
- **Signed and automated deploys to Clojars**: CI MUST handle releases; artifacts MUST be GPG-signed
- **Changelog**: CHANGELOG.md MUST document all changes with version, date, and category (Added, Changed, Fixed, Removed)

**Rationale**: Reproducible builds enable trust. Semantic versioning enables confident upgrades. Automation prevents human error.

### VII. Testing

Testing strategy MUST ensure correctness:

- **Kaocha or clojure.test with test.check**: Use standard testing tools; Kaocha preferred for advanced features (watch mode, coverage)
- **Deterministic tests**: Tests MUST NOT depend on external services, network, or filesystem unless explicitly marked as integration tests
- **CI matrix for supported Clojure versions**: All supported versions MUST pass tests in CI
- **Coverage goals**: Aim for >80% code coverage; critical paths (encoding, decoding, validation) MUST have 100% coverage
- **Test organization**: Unit tests in `test/`, integration tests (if any) in `test/integration/`

**Rationale**: Comprehensive testing enables confident refactoring. Deterministic tests prevent flaky CI. Version matrices ensure compatibility claims are verified.

### VIII. Observability

Code MUST be debuggable and maintainable:

- **Pure functions by default**: Side effects (I/O, state) MUST be isolated and clearly documented
- **No implicit global state**: Avoid `def` with mutable state (atoms, refs, agents) in library code; if unavoidable, document rationale
- **Clear error semantics**: Exceptions MUST include context (what operation failed, what data caused failure); avoid leaking implementation types in error messages
- **Logging**: Use standard logging (e.g., `clojure.tools.logging`) for library events; MUST be optional (no forced logging dependency)

**Rationale**: Pure functions are easy to test and reason about. Clear errors reduce debugging time. Isolated state prevents action-at-a-distance bugs.

## Project-Specific Constraints

### TypeID Library Scope

This library implements the TypeID specification (version 0.3.0) for Clojure:

- **Encoding/Decoding**: UUIDv7 ↔ TypeID string representation
- **Validation**: Prefix validation, suffix validation, length constraints
- **Generation**: Generate new TypeIDs with valid UUIDv7 timestamps
- **Compatibility**: Support for custom UUID variants (user-provided bytes)

### Technology Stack

- **Language**: Clojure 1.11+ (JVM)
- **Optional**: ClojureScript and/or Babashka support (TBD)
- **Validation**: Manual predicate functions (no runtime dependencies)
- **Testing**: Kaocha + test.check (dev dependencies only)
- **Build**: deps.edn + tools.build
- **CI**: GitHub Actions (or equivalent)

### Performance Goals

- **Encoding**: < 1μs per TypeID encoding (typical hardware)
- **Decoding**: < 1μs per TypeID decoding (typical hardware)
- **Validation**: < 500ns per prefix validation (typical hardware)

### Constraints

- **Zero external runtime dependencies**: Library MUST have zero runtime dependencies beyond org.clojure/clojure and org.clojure/clojurescript (no Malli, no clojure.spec, no external validation libraries)
- **Base32 implementation**: Must match reference implementation bit-for-bit
- **Spec compliance**: Must pass all test cases in valid.yml and invalid.yml from the reference spec

## Development Workflow

### Quality Gates

All pull requests MUST pass:

1. **clj-kondo**: Zero warnings/errors
2. **Tests**: All tests pass on all supported Clojure versions
3. **Coverage**: No decrease in coverage; critical paths maintain 100%
4. **Documentation**: All public vars have docstrings with examples
5. **Benchmarks**: If performance-sensitive code changed, benchmarks MUST NOT regress beyond acceptable threshold (documented per-function)

### Code Review Requirements

- **Two approvals** for breaking changes (MAJOR version bumps)
- **One approval** for all other changes
- **Self-merge** allowed for documentation-only changes (after CI passes)

### Complexity Justification

Any violation of simplicity principles (e.g., introducing abstractions, dependencies, or non-standard patterns) MUST be justified in PR description:

- **What problem** does this solve?
- **Why** is the added complexity necessary?
- **What simpler alternatives** were considered and why rejected?

## Governance

### Constitution Authority

This constitution supersedes all other development practices. In case of conflict between this document and other guidance (README, PR templates, etc.), this document wins.

### Amendment Process

1. **Proposal**: Open an issue describing the proposed amendment and rationale
2. **Discussion**: Minimum 7 days for community feedback
3. **Approval**: Requires maintainer consensus (all active maintainers must approve or abstain)
4. **Migration Plan**: For changes affecting existing code, provide migration steps
5. **Version Bump**: Update CONSTITUTION_VERSION according to semantic versioning:
   - **MAJOR**: Principle removed or redefined in breaking way
   - **MINOR**: New principle added or existing principle materially expanded
   - **PATCH**: Clarification, typo fix, non-semantic refinement
6. **Update Date**: Update LAST_AMENDED_DATE to amendment date

### Versioning Policy

- **Constitution Version**: Separate from library version; tracks governance changes
- **Library Version**: Follows semantic versioning per Principle VI
- **Compatibility**: Constitution amendments MUST NOT retroactively invalidate released library versions

### Compliance Review

- **Every PR**: Reviewer MUST verify compliance with all applicable principles
- **Quarterly**: Maintainers review codebase for drift from constitution
- **Annual**: Full constitution review to ensure principles remain relevant

### Guidance Documents

For runtime development assistance, agents (AI or human) should consult:

- **This constitution** for non-negotiable rules and principles
- **README.md** for quick-start and usage examples
- **typeid.md** for TypeID specification details
- **Template files** in `.specify/templates/` for feature planning and task structure

**Version**: 1.1.0 | **Ratified**: 2025-11-10 | **Last Amended**: 2025-11-10
