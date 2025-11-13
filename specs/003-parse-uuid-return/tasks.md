# Tasks: Parse Function Returns Platform UUID

**Input**: Design documents from `/specs/003-parse-uuid-return/`
**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/api.md, quickstart.md

**Tests**: Property-based and unit tests are included as requested in the feature specification for comprehensive validation.

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

## Path Conventions

- Single library project: `src/typeid/`, `test/typeid/` at repository root
- Benchmark code: `dev/benchmarks/`
- Documentation: `README.md`, `CHANGELOG.md`

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Prepare feature branch and verify existing baseline

- [X] T001 Verify current branch is `003-parse-uuid-return` and up to date
- [X] T002 [P] Run existing test suite to establish baseline in test/typeid/
- [X] T003 [P] Run existing benchmarks to establish baseline in dev/benchmarks/core_bench.clj
- [X] T004 Document baseline performance metrics for parse function (~2μs)

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Core UUID conversion utility that ALL user stories depend on

**⚠️ CRITICAL**: No user story work can begin until this phase is complete

- [X] T005 Implement `bytes->uuid` function in src/typeid/impl/uuid.cljc with platform-specific reader conditionals
- [X] T006 Add docstring to `bytes->uuid` with examples for both JVM and ClojureScript platforms
- [X] T007 Add unit tests for `bytes->uuid` in test/typeid/impl/uuid_test.cljc validating 16-byte input requirement
- [X] T008 [P] Add property-based tests for byte-to-UUID determinism in test/typeid/impl/uuid_test.cljc
- [X] T009 [P] Add edge case tests for zero UUID and max UUID in test/typeid/impl/uuid_test.cljc
- [X] T010 Verify `bytes->uuid` round-trips correctly with existing `uuid->bytes` function
- [X] T011 Add performance benchmark for `bytes->uuid` in dev/benchmarks/core_bench.clj targeting < 500ns

**Checkpoint**: Foundation ready - `bytes->uuid` function complete and tested

---

## Phase 3: User Story 1 - Round-trip UUID Conversion (Priority: P1) 🎯 MVP

**Goal**: Enable developers to create TypeID from UUID, parse it, and get the same UUID object back for natural comparison and database operations

**Independent Test**: Create TypeID from any UUID (v1/v4/v7), parse it, verify returned UUID equals original using standard equality operators

### Implementation for User Story 1

- [X] T012 [US1] Modify `parse` function in src/typeid/core.cljc to call `bytes->uuid` before returning result map
- [X] T013 [US1] Update `parse` docstring in src/typeid/core.cljc to document UUID object return type with examples
- [X] T014 [US1] Update all parse tests in test/typeid/core_test.cljc to expect UUID objects instead of byte arrays
- [X] T015 [P] [US1] Add property-based test for round-trip conversion in test/typeid/core_test.cljc: UUID → create → parse → UUID equality
- [X] T016 [P] [US1] Add explicit round-trip tests for UUIDv1, v4, and v7 in test/typeid/core_test.cljc
- [X] T017 [P] [US1] Add round-trip test for edge case UUIDs (zero, max, various timestamps) in test/typeid/core_test.cljc

**Checkpoint**: At this point, User Story 1 should be fully functional - round-trip conversions work with direct UUID equality

---

## Phase 4: User Story 2 - Database Integration with Native Types (Priority: P1)

**Goal**: Enable developers to parse TypeIDs from external sources and use UUID directly in database operations without manual conversion

**Independent Test**: Parse TypeID string, extract UUID, use directly with JDBC or database driver expecting native UUID type

### Implementation for User Story 2

- [X] T018 [US2] Update compliance tests in test/typeid/compliance_test.clj to compare UUID objects instead of byte arrays
- [X] T019 [P] [US2] Add integration test demonstrating database-ready UUID usage in test/typeid/core_test.cljc
- [X] T020 [P] [US2] Add test for UUID serialization to string format in test/typeid/core_test.cljc
- [X] T021 [P] [US2] Update ClojureScript tests to verify UUID serialization to JSON in test/typeid/core_test.cljc
- [X] T022 [US2] Verify parsed UUID objects work with platform equality operators (= function)

**Checkpoint**: At this point, User Stories 1 AND 2 should both work - UUIDs are database-ready without conversion

---

## Phase 5: User Story 3 - API Consistency for Developer Experience (Priority: P2)

**Goal**: Ensure symmetric API where creation accepts UUIDs and parsing returns UUIDs, creating predictable developer experience

**Independent Test**: Review all public API functions - verify UUID type consistency across creation and parsing operations

### Implementation for User Story 3

- [X] T023 [US3] Update README.md introduction section to show UUID objects in all parse examples
- [X] T024 [US3] Update README.md "Parsing TypeIDs" section with UUID object examples and benefits
- [X] T025 [US3] Update README.md "Common Patterns" section showing database integration with UUID objects
- [X] T026 [US3] Add migration guide section to README.md referencing quickstart.md for detailed migration path
- [X] T027 [P] [US3] Update README.md "Round-trip Conversion" examples showing natural UUID equality
- [X] T028 [P] [US3] Add FAQ section to README.md addressing "How do I get bytes?" with codec/decode example
- [X] T029 [US3] Verify all parsing examples in README.md show UUID objects (not byte arrays) per FR-005 requirement

**Checkpoint**: All user stories should now be independently functional - API is consistent and well-documented

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Finalize breaking change, documentation, and validation

- [X] T030 [P] Update CHANGELOG.md with v0.2.0 breaking change section per contracts/api.md version contract
- [X] T031 [P] Add migration notes to CHANGELOG.md referencing quickstart.md for migration guide
- [X] T032 [P] Document performance impact in CHANGELOG.md: parse adds ~500ns for UUID conversion
- [X] T033 Run performance benchmarks and verify no regression beyond 10% threshold
- [X] T034 Verify performance budget: parse < 3μs, bytes->uuid < 1μs per contracts/api.md
- [X] T035 [P] Update error_semantics_test.cljc if needed to handle UUID types in test/typeid/
- [X] T036 [P] Run clj-kondo linter and ensure 100% clean per constitution requirement
- [X] T037 Run full test suite on JVM (Clojure 1.11 and 1.12) verifying all tests pass
- [X] T038 Run full test suite on ClojureScript via Node.js verifying all tests pass
- [X] T039 [P] Verify all public functions have complete docstrings per constitution requirement
- [X] T039b [P] Verify docstrings accurately document UUID return types for FR-006 compliance (cljdoc auto-publishes API docs)
- [X] T040 Review all modified files for code quality and adherence to project standards
- [X] T041 Run quickstart.md validation scenarios manually to verify migration guide accuracy
- [X] T042 Tag version as v0.2.0 in preparation for release

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies - can start immediately
- **Foundational (Phase 2)**: Depends on Setup completion - BLOCKS all user stories
- **User Stories (Phase 3, 4, 5)**: All depend on Foundational phase (T005-T011) completion
  - User stories can then proceed in parallel (if staffed)
  - Or sequentially in priority order (P1 → P1 → P2)
- **Polish (Phase 6)**: Depends on all user stories being complete

### User Story Dependencies

- **User Story 1 (P1)**: Depends on Foundational phase - specifically requires `bytes->uuid` function
- **User Story 2 (P1)**: Depends on User Story 1 - requires parse returning UUID objects
- **User Story 3 (P2)**: Depends on User Stories 1 and 2 - documents final consistent API

**Note**: User Stories 1 and 2 are both P1 but must be sequential since US2 validates database integration of the UUID objects returned by US1.

### Within Each User Story

- User Story 1: Core implementation (T012-T013) before tests (T014-T017)
- User Story 2: Tests can run in parallel once US1 complete
- User Story 3: Documentation tasks mostly parallelizable

### Parallel Opportunities

- **Setup Phase**: T002 and T003 can run in parallel (different operations)
- **Foundational Phase**: T008 and T009 can run in parallel (different test files)
- **User Story 1**: T015, T016, T017 can run in parallel after core implementation (different test cases)
- **User Story 2**: T018, T019, T020, T021 can run in parallel (different test aspects)
- **User Story 3**: T023-T029 have some parallelization: T027, T028 can run independently
- **Polish Phase**: T030, T031, T032 (changelog), T036 (linting), T039 (docstrings) can run in parallel

---

## Parallel Example: Foundational Phase

```bash
# After T005-T007 complete, launch tests in parallel:
Task: "Property-based tests for byte-to-UUID determinism"
Task: "Edge case tests for zero UUID and max UUID"
```

## Parallel Example: User Story 1

```bash
# After T012-T014 complete, launch all round-trip tests together:
Task: "Property-based test for round-trip conversion"
Task: "Explicit round-trip tests for UUIDv1, v4, and v7"
Task: "Round-trip test for edge case UUIDs"
```

## Parallel Example: User Story 2

```bash
# Launch all integration tests for User Story 2 together:
Task: "Update compliance tests to compare UUID objects"
Task: "Integration test demonstrating database-ready UUID usage"
Task: "Test for UUID serialization to string format"
Task: "Update ClojureScript tests to verify UUID serialization"
```

---

## Implementation Strategy

### MVP First (User Stories 1 & 2 Only)

1. Complete Phase 1: Setup (T001-T004)
2. Complete Phase 2: Foundational (T005-T011) - CRITICAL, blocks all stories
3. Complete Phase 3: User Story 1 (T012-T017) - Core round-trip functionality
4. Complete Phase 4: User Story 2 (T018-T022) - Database integration validation
5. **STOP and VALIDATE**: Test round-trip and database usage independently
6. Optionally deploy/demo at this point (basic functionality complete)

### Incremental Delivery

1. Complete Setup + Foundational → Foundation ready (`bytes->uuid` works)
2. Add User Story 1 → Test independently → Round-trip conversions work (MVP!)
3. Add User Story 2 → Test independently → Database integration validated
4. Add User Story 3 → Test independently → Documentation complete, API consistent
5. Polish Phase → Ready for release as v0.2.0

### Parallel Team Strategy

With multiple developers:

1. Team completes Setup + Foundational together (critical path)
2. Once Foundational is done:
   - Developer A: User Story 1 implementation (T012-T017)
   - Developer B: Prepare documentation updates (review migration guide)
3. Once US1 complete:
   - Developer A: User Story 2 tests (T018-T022)
   - Developer B: User Story 3 documentation (T023-T029)
4. Team completes Polish phase together (testing, linting, validation)

---

## Detailed File Changes Summary

### Files to Modify (7 files)

1. **src/typeid/impl/uuid.cljc**: Add `bytes->uuid` function (T005-T006)
2. **src/typeid/core.cljc**: Update `parse` function to return UUID objects (T012-T013)
3. **test/typeid/impl/uuid_test.cljc**: New tests for `bytes->uuid` (T007-T010)
4. **test/typeid/core_test.cljc**: Update all parse tests for UUID objects (T014-T022)
5. **test/typeid/compliance_test.clj**: Update UUID comparisons (T018)
6. **dev/benchmarks/core_bench.clj**: Add `bytes->uuid` benchmark (T011, T033-T034)
7. **README.md**: Update all examples and add migration guide (T023-T029)

### Files to Create (1 file)

1. **CHANGELOG.md entry**: Add v0.2.0 breaking change documentation (T030-T032)

### Files to Review (2 files)

1. **test/typeid/error_semantics_test.cljc**: Verify no UUID type conflicts (T035)
2. **src/typeid/validation.cljc**: Verify no changes needed (validation stays byte-focused)

---

## Success Metrics

Upon completion of all tasks:

- ✅ All existing tests pass with UUID objects instead of byte arrays
- ✅ New property-based tests validate round-trip conversions
- ✅ Performance benchmarks show < 10% regression (target: +500ns for parse)
- ✅ 100% clj-kondo clean (constitution requirement)
- ✅ All public functions have docstrings (constitution requirement)
- ✅ Both JVM and ClojureScript platforms pass CI tests
- ✅ Documentation clearly shows UUID objects in all examples
- ✅ Migration guide provides clear path for existing users
- ✅ Breaking change properly documented in CHANGELOG

---

## Notes

- **[P] tasks**: Different files or test cases, no dependencies on incomplete tasks
- **[Story] label**: Maps task to specific user story for traceability
- **Each user story**: Independently completable and testable
- **Breaking change**: This is acceptable in 0.x stage per constitution compliance
- **Zero dependencies**: No external libraries added, only platform UUID support
- **Performance budget**: parse < 3μs, bytes->uuid < 1μs (per contracts/api.md)
- **Testing**: Property-based tests with test.check for comprehensive validation
- **CI validation**: Must pass on Clojure 1.11/1.12, JDK 17/21, and ClojureScript
