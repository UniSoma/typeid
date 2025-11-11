# Tasks: TypeID Clojure/ClojureScript Library

**Input**: Design documents from `/specs/001-typeid-implementation/`
**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/api.md

**Tests**: This implementation follows a test-driven approach with comprehensive test coverage (>80% overall, 100% for critical paths) as specified in the plan.

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `- [ ] [ID] [P?] [Story?] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (US1, US2, US3)
- Include exact file paths in descriptions

## Path Conventions

Single library project structure:
- Source: `src/typeid/`
- Tests: `test/typeid/`
- Dev tools: `dev/`
- Config: Repository root

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Project initialization and basic structure

- [X] T001 Create directory structure per plan.md (src/typeid/, test/typeid/, dev/, .github/workflows/)
- [X] T002 Create deps.edn with :dev, :test, :coverage, :build, :cljs, :codox aliases per research.md
- [X] T003 [P] Create build.clj with clean, pom, jar, install, deploy tasks per research.md:469-535
- [X] T004 [P] Create .clj-kondo/config.edn with zero-tolerance configuration
- [X] T005 [P] Create .cljfmt.edn with Clojure formatting rules
- [X] T006 [P] Create .github/workflows/ci.yml with test-clj and test-cljs jobs per research.md:538-576
- [X] T007 [P] Create .github/workflows/release.yml for Clojars deployment per research.md:579-598
- [X] T008 [P] Create tests.cljs.edn for Kaocha ClojureScript configuration per research.md:602-615
- [X] T009 [P] Create package.json with Node.js dependencies per research.md:617-628
- [X] T010 [P] Create CHANGELOG.md with version 0.1.0-SNAPSHOT entry
- [X] T011 [P] Create LICENSE file (MIT License per plan.md)
- [X] T012 [P] Download reference test files valid.yml and invalid.yml to test/resources/

**Checkpoint**: Project structure complete, ready for foundational code

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Core utilities and validation that ALL user stories depend on

**⚠️ CRITICAL**: No user story work can begin until this phase is complete

- [X] T013 [P] Implement base32 encode alphabet in src/typeid/impl/base32.cljc per data-model.md:186-209
- [X] T014 [P] Implement base32 decode map in src/typeid/impl/base32.cljc per data-model.md:186-209
- [X] T015 Implement base32/encode function (UUID bytes → 26-char string) in src/typeid/impl/base32.cljc with type hints per research.md:125-268
- [X] T016 Implement base32/decode function (26-char string → UUID bytes) in src/typeid/impl/base32.cljc with type hints per research.md:125-268
- [X] T017 [P] Implement prefix validation predicates in src/typeid/validation.cljc per research.md:284-366
- [X] T018 [P] Implement TypeID string validation predicates in src/typeid/validation.cljc per data-model.md:52-59
- [X] T019 [P] Implement UUID bytes validation predicates in src/typeid/validation.cljc per data-model.md:166-180
- [X] T020 Implement UUIDv7 generation in src/typeid/impl/uuid.cljc with reader conditionals for JVM/JS per research.md:49-122
- [X] T021 [P] Create shared utility functions in src/typeid/impl/util.cljc (string splitting, bit manipulation helpers)
- [X] T022 Add (set! *warn-on-reflection* true) to all src namespaces per research.md:125-268

**Checkpoint**: Foundation ready - base32 encoding, UUID generation, validation all working

---

## Phase 3: User Story 1 - Generate and Parse TypeIDs (Priority: P1) 🎯 MVP

**Goal**: Generate new TypeIDs with type prefixes and parse existing TypeIDs back into UUID components

**Independent Test**: Generate TypeIDs with various prefixes (including empty), parse them back, verify UUID matches

### Tests for User Story 1

> **NOTE: Write these tests FIRST, ensure they FAIL before implementation**

- [X] T023 [P] [US1] Create unit test for generate function in test/typeid/core_test.cljc with valid/invalid prefix cases
- [X] T024 [P] [US1] Create unit test for parse function in test/typeid/core_test.cljc with valid/invalid TypeID strings
- [ ] T025 [P] [US1] Create property-based tests for generate→parse round-trip in test/typeid/properties_test.cljc per research.md:410-508
- [X] T026 [P] [US1] Create base32 encoding unit tests in test/typeid/impl/base32_test.cljc
- [X] T027 [P] [US1] Create UUIDv7 generation tests in test/typeid/impl/uuid_test.cljc
- [X] T028 [P] [US1] Create validation unit tests in test/typeid/validation_test.cljc

### Implementation for User Story 1

- [X] T029 [US1] Implement typeid.core/generate function in src/typeid/core.cljc per contracts/api.md:16-75
- [X] T030 [US1] Implement typeid.core/parse function in src/typeid/core.cljc per contracts/api.md:78-146
- [X] T031 [US1] Add comprehensive docstrings with examples to generate and parse functions per research.md:608-679
- [X] T032 [US1] Implement error-as-data pattern {:ok result} or {:error error-map} for both functions per data-model.md:296-323
- [X] T033 [US1] Add namespace-level docstring to src/typeid/core.cljc explaining library purpose and usage

**Checkpoint**: At this point, User Story 1 should be fully functional - can generate and parse TypeIDs independently

---

## Phase 4: User Story 2 - Validate TypeIDs (Priority: P2)

**Goal**: Validate TypeID strings to ensure they conform to specification before processing

**Independent Test**: Provide valid and invalid TypeID strings, verify correct validation results with appropriate error messages

### Tests for User Story 2

- [X] T034 [P] [US2] Create validation unit tests in test/typeid/core_test.cljc covering all error types per contracts/api.md:148-199
- [X] T035 [P] [US2] Create error semantics test suite in test/typeid/error_semantics_test.cljc per research.md:416-486
- [X] T036 [P] [US2] Add edge case tests (63-char prefix, consecutive underscores, boundary chars) in test/typeid/core_test.cljc

### Implementation for User Story 2

- [X] T037 [US2] Implement typeid.core/validate function in src/typeid/core.cljc per contracts/api.md:148-199
- [X] T038 [US2] Ensure all validation errors include :type, :message, :data keys per data-model.md:296-323
- [X] T039 [US2] Add docstring with validation examples to validate function per contracts/api.md:180-199
- [X] T040 [US2] Implement predicate functions in typeid.validation namespace (valid-prefix?, valid-typeid-string?, etc.)

**Checkpoint**: At this point, User Stories 1 AND 2 should both work independently - can generate, parse, and validate

---

## Phase 5: User Story 3 - Convert Between TypeID and UUID Formats (Priority: P3)

**Goal**: Convert existing UUIDs to TypeIDs and extract UUIDs from TypeIDs in various formats

**Independent Test**: Provide UUIDs in various formats, convert to TypeIDs, verify round-trip conversion works

### Tests for User Story 3

- [ ] T041 [P] [US3] Create unit tests for from-uuid function in test/typeid/core_test.cljc with UUIDv4/v7 variants
- [ ] T042 [P] [US3] Create unit tests for to-uuid function in test/typeid/core_test.cljc
- [ ] T043 [P] [US3] Create unit tests for to-uuid-string function in test/typeid/core_test.cljc
- [ ] T044 [P] [US3] Create unit tests for suffix extraction in test/typeid/core_test.cljc
- [ ] T045 [P] [US3] Add property-based tests for UUID↔TypeID round-trip in test/typeid/properties_test.cljc

### Implementation for User Story 3

- [ ] T046 [P] [US3] Implement typeid.core/from-uuid function in src/typeid/core.cljc per contracts/api.md:201-252
- [ ] T047 [P] [US3] Implement typeid.core/to-uuid function in src/typeid/core.cljc per contracts/api.md:254-297
- [ ] T048 [P] [US3] Implement typeid.core/to-uuid-string function in src/typeid/core.cljc per contracts/api.md:299-345
- [ ] T049 [P] [US3] Implement typeid.core/get-prefix function in src/typeid/core.cljc per contracts/api.md:347-382
- [ ] T050 [P] [US3] Implement typeid.core/get-suffix function in src/typeid/core.cljc per contracts/api.md:384-419
- [ ] T051 [US3] Add docstrings with examples to all conversion functions per contracts/api.md

**Checkpoint**: All user stories should now be independently functional - full TypeID lifecycle supported

---

## Phase 6: Compliance & Quality Assurance

**Purpose**: Ensure 100% specification compliance and cross-platform compatibility

- [ ] T052 [P] Create compliance test loader in test/typeid/compliance_test.cljc to parse valid.yml and invalid.yml
- [ ] T053 [US1] Implement valid.yml test cases (encode UUID → expected TypeID) in test/typeid/compliance_test.cljc
- [ ] T054 [US1] Implement valid.yml test cases (decode TypeID → expected UUID) in test/typeid/compliance_test.cljc
- [ ] T055 [US2] Implement invalid.yml test cases (reject with appropriate errors) in test/typeid/compliance_test.cljc
- [ ] T056 [P] Run clj-kondo on all source and test files, fix all warnings
- [ ] T057 [P] Run cljfmt on all source and test files, ensure consistent formatting
- [ ] T058 [P] Verify all public functions have docstrings with examples
- [ ] T059 Run test suite on JVM (Clojure 1.11 and 1.12, JDK 17 and 21)
- [ ] T060 Run test suite on ClojureScript (Node.js 18 and 20)
- [ ] T061 [P] Verify test coverage ≥80% overall using cloverage per research.md:510-532
- [ ] T062 [P] Verify 100% coverage for base32/encode, base32/decode, uuid/generate-uuidv7, validation predicates

**Checkpoint**: Library passes all compliance tests and quality gates

---

## Phase 7: Performance & Benchmarking

**Purpose**: Verify sub-microsecond performance targets

- [ ] T063 [P] Create benchmark suite in dev/benchmarks/core_bench.clj using criterium per research.md:243-268
- [ ] T064 [P] Benchmark typeid.core/generate (target: <2μs total)
- [ ] T065 [P] Benchmark base32/encode (target: <1μs)
- [ ] T066 [P] Benchmark base32/decode (target: <1μs)
- [ ] T067 [P] Benchmark prefix validation (target: <500ns)
- [ ] T068 Verify zero reflection warnings in benchmark output per research.md:224-240
- [ ] T069 Add benchmark results to README performance section per research.md:826-834

**Checkpoint**: Performance targets met and documented

---

## Phase 8: Documentation & Polish

**Purpose**: Complete user-facing documentation and final quality improvements

- [ ] T070 [P] Create README.md following outline in research.md:719-888
- [ ] T071 [P] Add installation instructions for deps.edn and Leiningen to README.md
- [ ] T072 [P] Add quick start examples for all three user stories to README.md
- [ ] T073 [P] Add troubleshooting section with common errors to README.md per research.md:847-871
- [ ] T074 [P] Add comparison table (TypeID vs UUID vs ULID vs Nano ID) to README.md per research.md:836-845
- [ ] T075 [P] Generate Codox documentation with clojure -X:codox
- [ ] T076 [P] Create dev/user.clj with REPL utilities for development
- [ ] T077 [P] Create CONTRIBUTING.md with development setup instructions
- [ ] T078 [P] Verify quickstart.md examples are executable and correct
- [ ] T079 [P] Add code coverage badge to README.md
- [ ] T080 [P] Add CI status badge to README.md
- [ ] T081 Update CHANGELOG.md with release notes for version 0.1.0

**Checkpoint**: Library ready for initial release

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies - can start immediately
- **Foundational (Phase 2)**: Depends on Setup completion - BLOCKS all user stories
- **User Story 1 (Phase 3)**: Depends on Foundational (Phase 2) completion
- **User Story 2 (Phase 4)**: Depends on Foundational (Phase 2) completion - can run parallel with US1
- **User Story 3 (Phase 5)**: Depends on Foundational (Phase 2) completion - can run parallel with US1/US2
- **Compliance (Phase 6)**: Depends on User Stories 1, 2, 3 completion
- **Performance (Phase 7)**: Depends on User Stories 1, 2, 3 completion
- **Documentation (Phase 8)**: Depends on all implementation phases completion

### User Story Dependencies

- **User Story 1 (P1)**: Can start after Foundational (Phase 2) - No dependencies on other stories
- **User Story 2 (P2)**: Can start after Foundational (Phase 2) - Independent of US1 (validation works on any string)
- **User Story 3 (P3)**: Can start after Foundational (Phase 2) - Independent of US1/US2 (conversion is standalone)

**All three user stories can be developed in parallel after Phase 2 completes**

### Within Each User Story

1. Tests FIRST (write tests that FAIL)
2. Implementation (make tests PASS)
3. Documentation (add docstrings and examples)
4. Integration (ensure story works independently)

### Parallel Opportunities

**Setup Phase (Phase 1)**:
- T003, T004, T005, T006, T007, T008, T009, T010, T011, T012 can all run in parallel

**Foundational Phase (Phase 2)**:
- T013, T014 (base32 alphabet/decode map) can run in parallel
- T017, T018, T019 (validation predicates) can run in parallel after T013/T014
- T021, T022 (utilities) can run in parallel with validation

**User Story 1 Tests**:
- T023, T024, T025, T026, T027, T028 can all run in parallel

**User Story 2 Tests**:
- T034, T035, T036 can all run in parallel

**User Story 3 Tests**:
- T041, T042, T043, T044, T045 can all run in parallel

**User Story 3 Implementation**:
- T046, T047, T048, T049, T050 can all run in parallel (different functions)

**Compliance Phase**:
- T052, T056, T057, T058, T061, T062 can run in parallel

**Performance Phase**:
- T063, T064, T065, T066, T067 can run in parallel

**Documentation Phase**:
- T070, T071, T072, T073, T074, T075, T076, T077, T078, T079, T080 can all run in parallel

---

## Parallel Example: User Story 1

```bash
# Launch all tests for User Story 1 together:
Task: "Create unit test for generate function in test/typeid/core_test.cljc"
Task: "Create unit test for parse function in test/typeid/core_test.cljc"
Task: "Create property-based tests for generate→parse round-trip in test/typeid/properties_test.cljc"
Task: "Create base32 encoding unit tests in test/typeid/impl/base32_test.cljc"
Task: "Create UUIDv7 generation tests in test/typeid/impl/uuid_test.cljc"
Task: "Create prefix validation unit tests in test/typeid/impl/prefix_test.cljc"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Setup (T001-T012)
2. Complete Phase 2: Foundational (T013-T022) - CRITICAL
3. Complete Phase 3: User Story 1 (T023-T033)
4. **STOP and VALIDATE**:
   - Run tests: `clojure -M:test -m kaocha.runner`
   - Test generate and parse independently
   - Verify round-trip works
5. Deploy/demo if ready

**This gives you a minimal but functional TypeID library that can generate and parse TypeIDs**

### Incremental Delivery

1. Complete Setup + Foundational (T001-T022) → Foundation ready
2. Add User Story 1 (T023-T033) → Test independently → Deploy/Demo (MVP!)
   - **Value**: Can now generate and parse TypeIDs
3. Add User Story 2 (T034-T040) → Test independently → Deploy/Demo
   - **Value**: Can now validate TypeIDs with clear error messages
4. Add User Story 3 (T041-T051) → Test independently → Deploy/Demo
   - **Value**: Can now convert between UUID and TypeID formats
5. Add Compliance (T052-T062) → Test independently
   - **Value**: 100% spec compliance verified
6. Add Performance (T063-T069) → Benchmark
   - **Value**: Performance targets verified
7. Add Documentation (T070-T081) → Publish
   - **Value**: Library ready for public release

### Parallel Team Strategy

With multiple developers:

1. Team completes Setup + Foundational together (T001-T022)
2. Once Foundational is done:
   - **Developer A**: User Story 1 (T023-T033) - Generate & Parse
   - **Developer B**: User Story 2 (T034-T040) - Validation
   - **Developer C**: User Story 3 (T041-T051) - Conversion
3. Stories complete and integrate independently
4. Team collaborates on Compliance, Performance, Documentation

---

## Critical Path Analysis

**Longest dependency chain** (Setup → Foundational → US1 → Compliance → Performance → Docs):
- T001-T012 (Setup): ~2-4 hours
- T013-T022 (Foundational): ~6-8 hours
- T023-T033 (US1): ~4-6 hours
- T052-T062 (Compliance): ~3-4 hours
- T063-T069 (Performance): ~2-3 hours
- T070-T081 (Documentation): ~3-4 hours

**Total critical path**: ~20-29 hours for complete library

**MVP critical path** (Setup → Foundational → US1): ~12-18 hours

---

## Notes

- **[P] tasks** = different files, no dependencies within phase
- **[Story] label** maps task to specific user story for traceability
- **Each user story is independently testable** - can verify US2 validation without needing US1 generation
- **Zero external runtime dependencies** - all validation is manual predicates, no Malli at runtime
- **Cross-platform** - all .cljc files work on both JVM and JavaScript
- **TDD approach** - tests written first, implementation makes them pass
- **Performance-critical** - all hot paths have type hints to eliminate reflection
- **100% spec compliance** - reference test files (valid.yml, invalid.yml) must all pass

**Success Criteria Met**:
- SC-001: Generate + parse in <2μs (verified in Phase 7)
- SC-002: 100% valid.yml test cases pass (verified in Phase 6)
- SC-003: 100% invalid.yml test cases rejected (verified in Phase 6)
- SC-004: Identical JVM/CLJS results (verified in Phase 6)
- SC-005: Documentation examples for all stories (verified in Phase 8)
- SC-006: All public functions have docstrings (verified in Phase 6)
- SC-007: Property-based tests verify round-trip (Phase 3, US1)
- SC-008: All errors include :type, :message, :data (Phase 4, US2)
- SC-009: Zero clj-kondo warnings (verified in Phase 6)
- SC-010: Encode/decode <1μs (verified in Phase 7)
