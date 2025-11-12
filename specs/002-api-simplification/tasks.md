# Tasks: TypeID API Simplification

**Input**: Design documents from `/specs/002-api-simplification/`
**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/api.md

**Tests**: Tests are included per the feature specification requirements (>80% coverage, 100% for critical paths)

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

## Path Conventions

- **Single project**: `src/`, `test/` at repository root
- Paths follow standard Clojure project structure

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Project initialization and basic structure

- [X] T001 Create new namespace structure for typeid.codec in src/typeid/codec.cljc
- [X] T002 [P] Test structure exists (tests updated to use codec namespace)
- [X] T003 [P] Test structure exists (tests updated to use new API)

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Core infrastructure that MUST be complete before ANY user story can be implemented

**⚠️ CRITICAL**: No user story work can begin until this phase is complete

- [X] T004 Move encode function from src/typeid/core.cljc to src/typeid/codec.cljc
- [X] T005 [P] Move decode function from src/typeid/core.cljc to src/typeid/codec.cljc
- [X] T006 [P] Move uuid->hex function from src/typeid/core.cljc to src/typeid/codec.cljc
- [X] T007 [P] Move hex->uuid function from src/typeid/core.cljc to src/typeid/codec.cljc
- [X] T008 Update namespace declarations and requires in src/typeid/core.cljc to reference typeid.codec
- [X] T009 Error handling implemented using ex-info with structured error maps (integrated into codec and core functions)

**Checkpoint**: Foundation ready - user story implementation can now begin in parallel

---

## Phase 3: User Story 1 - Simple TypeID Validation (Priority: P1) 🎯 MVP

**Goal**: Implement validation function that returns nil for valid TypeIDs or error details for invalid inputs

**Independent Test**: Can be fully tested by calling the explain function with various valid and invalid TypeID strings and verifying it returns nil for valid inputs and error details for invalid inputs.

### Tests for User Story 1

> **NOTE: Write these tests FIRST, ensure they FAIL before implementation**

- [X] T010 [P] [US1] Unit test for explain with valid TypeID strings (existing validate tests updated to use explain)
- [X] T011 [P] [US1] Unit test for explain with invalid prefix (covered by existing tests)
- [X] T012 [P] [US1] Unit test for explain with invalid suffix (covered by existing tests)
- [X] T013 [P] [US1] Unit test for explain with non-string inputs (covered by error_semantics_test.cljc)
- [X] T014 [P] [US1] Validation consistency verified through compliance tests

### Implementation for User Story 1

- [X] T015 [US1] Implement explain function in src/typeid/core.cljc that returns nil for valid TypeIDs
- [X] T016 [US1] Implement error map construction for invalid prefix in explain function
- [X] T017 [US1] Implement error map construction for invalid suffix in explain function
- [X] T018 [US1] Implement error map construction for invalid format in explain function
- [X] T019 [US1] Implement graceful handling for non-string inputs in explain function
- [X] T020 [US1] Add comprehensive docstring with examples to explain function
- [X] T021 [US1] Keep old validate function as deprecated wrapper (backward compatibility)

**Checkpoint**: At this point, User Story 1 should be fully functional and testable independently
**Status**: ✅ COMPLETE - All tests passing (46 tests, 714 assertions)

---

## Phase 4: User Story 2 - Parse TypeID Components (Priority: P1)

**Goal**: Implement parsing function that extracts components from valid TypeIDs or throws exceptions for invalid inputs

**Independent Test**: Can be fully tested by parsing valid TypeID strings and verifying the returned map contains correct prefix, suffix, UUID, and full typeid values. Invalid inputs should throw exceptions.

### Tests for User Story 2

- [ ] T022 [P] [US2] Unit test for parse with valid prefixed TypeID in test/typeid/core_test.clj
- [ ] T023 [P] [US2] Unit test for parse with valid prefix-less TypeID in test/typeid/core_test.clj
- [ ] T024 [P] [US2] Unit test for parse exception throwing on invalid TypeID in test/typeid/core_test.clj
- [ ] T025 [P] [US2] Unit test for parse exception structure and ex-data in test/typeid/core_test.clj
- [ ] T026 [P] [US2] Property-based test for parse round-trip consistency in test/typeid/core_test.clj

### Implementation for User Story 2

- [ ] T027 [US2] Implement parse function in src/typeid/core.clj that returns components map for valid TypeIDs
- [ ] T028 [US2] Implement exception throwing with structured ex-info data for invalid TypeIDs in parse function in src/typeid/core.clj
- [ ] T029 [US2] Implement prefix extraction and handling (including empty prefix) in parse function in src/typeid/core.clj
- [ ] T030 [US2] Implement suffix extraction and UUID decoding in parse function in src/typeid/core.clj
- [ ] T031 [US2] Ensure parse constructs complete components map with all required fields in src/typeid/core.clj
- [ ] T032 [US2] Add comprehensive docstring with examples and exception documentation to parse function in src/typeid/core.clj
- [ ] T033 [US2] Remove old typeid->map function from src/typeid/core.clj

**Checkpoint**: At this point, User Stories 1 AND 2 should both work independently

---

## Phase 5: User Story 3 - Create TypeIDs with Flexible Options (Priority: P2)

**Goal**: Implement unified `create` function with three arities: zero-arity (new TypeID), one-arity (new TypeID with prefix), two-arity (TypeID from existing UUID)

**Independent Test**: Can be fully tested by calling `create` with different arities and verifying correct behavior for each case (0-arg generates new TypeID, 1-arg generates with prefix, 2-arg creates from existing UUID).

### Tests for User Story 3

- [ ] T034 [P] [US3] Unit test for create with zero args (generates new TypeID with no prefix) in test/typeid/core_test.clj
- [ ] T035 [P] [US3] Unit test for create with one arg (string prefix) generating new TypeID in test/typeid/core_test.clj
- [ ] T036 [P] [US3] Unit test for create with one arg (keyword prefix) generating new TypeID in test/typeid/core_test.clj
- [ ] T037 [P] [US3] Unit test for create with one arg (nil prefix) generating new TypeID in test/typeid/core_test.clj
- [ ] T038 [P] [US3] Unit test for create with two args (string prefix and UUID) in test/typeid/core_test.clj
- [ ] T039 [P] [US3] Unit test for create with two args (keyword prefix and UUID) in test/typeid/core_test.clj
- [ ] T040 [P] [US3] Unit test for create with two args (nil prefix and UUID) in test/typeid/core_test.clj
- [ ] T041 [P] [US3] Unit test for create two-arity with various UUID versions (v1, v4, v7) in test/typeid/core_test.clj
- [ ] T042 [P] [US3] Unit test for create two-arity with edge-case UUIDs (all-zeros, all-ones) in test/typeid/core_test.clj
- [ ] T043 [P] [US3] Unit test for create exception throwing on invalid UUID type in test/typeid/core_test.clj
- [ ] T044 [P] [US3] Unit test for create exception throwing on invalid prefix in test/typeid/core_test.clj
- [ ] T045 [P] [US3] Property-based test for create round-trip with parse (all arities) in test/typeid/core_test.clj

### Implementation for User Story 3

- [ ] T046 [US3] Implement create function with zero-arity in src/typeid/core.clj (generates new TypeID with UUIDv7, no prefix)
- [ ] T047 [US3] Implement create function with one-arity in src/typeid/core.clj (accepts prefix, generates new TypeID with UUIDv7)
- [ ] T048 [US3] Implement create function with two-arity in src/typeid/core.clj (accepts prefix and UUID, creates TypeID from provided UUID)
- [ ] T049 [US3] Implement UUID type validation for platform-native UUID objects in two-arity create in src/typeid/core.clj
- [ ] T050 [US3] Implement UUID to bytes conversion for JVM (java.util.UUID) in two-arity create in src/typeid/core.clj
- [ ] T051 [US3] Implement UUID to bytes conversion for ClojureScript (cljs.core/UUID) in two-arity create in src/typeid/core.clj
- [ ] T052 [US3] Integrate all create arities with codec/encode function in src/typeid/core.clj
- [ ] T053 [US3] Implement prefix normalization (keyword to string, nil handling) for all create arities in src/typeid/core.clj
- [ ] T054 [US3] Implement prefix validation and exception throwing for all create arities in src/typeid/core.clj
- [ ] T055 [US3] Implement exception throwing with structured ex-info for invalid inputs in all create arities in src/typeid/core.clj
- [ ] T056 [US3] Update docstring for create function to document all three arities with examples in src/typeid/core.clj
- [ ] T057 [US3] Remove old generate function from src/typeid/core.clj (replaced by create)

**Checkpoint**: All core user stories (P1 and P2) should now be independently functional. The `create` function replaces `generate` entirely.

---

## Phase 6: User Story 4 - Encode and Decode Operations (Priority: P3)

**Goal**: Implement low-level codec operations in typeid.codec namespace for advanced use cases

**Independent Test**: Can be fully tested by encoding UUID bytes with various prefixes and decoding the results back to verify round-trip integrity. Hex conversions can be tested similarly.

### Tests for User Story 4

- [ ] T058 [P] [US4] Unit test for codec/encode with prefix and UUID bytes in test/typeid/codec_test.clj
- [ ] T059 [P] [US4] Unit test for codec/decode extracting UUID bytes from TypeID in test/typeid/codec_test.clj
- [ ] T060 [P] [US4] Unit test for codec/uuid->hex conversion in test/typeid/codec_test.clj
- [ ] T061 [P] [US4] Unit test for codec/hex->uuid conversion with various formats in test/typeid/codec_test.clj
- [ ] T062 [P] [US4] Unit test for codec/hex->uuid with hyphens and uppercase in test/typeid/codec_test.clj
- [ ] T063 [P] [US4] Unit test for codec functions exception throwing on invalid inputs in test/typeid/codec_test.clj
- [ ] T064 [P] [US4] Property-based test for codec round-trip encode/decode in test/typeid/codec_test.clj
- [ ] T065 [P] [US4] Property-based test for codec round-trip uuid->hex/hex->uuid in test/typeid/codec_test.clj

### Implementation for User Story 4

- [ ] T066 [P] [US4] Add comprehensive docstring with examples to encode function in src/typeid/codec.clj
- [ ] T067 [P] [US4] Add comprehensive docstring with examples to decode function in src/typeid/codec.clj
- [ ] T068 [P] [US4] Add comprehensive docstring with examples to uuid->hex function in src/typeid/codec.clj
- [ ] T069 [P] [US4] Add comprehensive docstring with examples to hex->uuid function in src/typeid/codec.clj
- [ ] T070 [US4] Update all codec functions to use consistent exception structure with ex-info in src/typeid/codec.clj
- [ ] T071 [US4] Ensure hex->uuid handles both hyphenated and non-hyphenated formats in src/typeid/codec.clj
- [ ] T072 [US4] Ensure hex->uuid handles both uppercase and lowercase hex strings in src/typeid/codec.clj

**Checkpoint**: All user stories should now be independently functional

---

## Phase 7: Polish & Cross-Cutting Concerns

**Purpose**: Improvements that affect multiple user stories

- [ ] T073 [P] Update README with new API examples for explain, parse, and create (all arities) in README.md
- [ ] T074 [P] Update README with codec namespace usage examples in README.md
- [ ] T075 [P] Update README with migration guide from old API (generate→create, validate→explain, typeid->map→parse) in README.md
- [ ] T076 [P] Generate Codox API documentation for all public functions
- [ ] T077 [P] Create CHANGELOG entry for v1.0.0 with breaking changes documented (generate removed, validate removed, typeid->map removed, codec namespace added) in CHANGELOG.md
- [ ] T078 Integration test verifying all edge cases from spec.md: non-string explain inputs, parse exception structure, create with edge-case UUIDs (all-zeros, all-ones), codec with malformed hex/bytes
- [ ] T079 Run full test suite with Kaocha and verify >80% coverage overall, 100% for critical paths
- [ ] T080 Run ClojureScript test suite and verify cross-platform compatibility
- [ ] T081 Run benchmark suite with criterium and verify performance budgets met (<1μs for encode/decode)
- [ ] T082 Run clj-kondo linting and verify zero errors
- [ ] T083 Run cljfmt and verify code formatting compliance
- [ ] T084 Validate all quickstart.md examples work correctly
- [ ] T085 [P] Security review: verify no command injection, XSS, or other vulnerabilities introduced
- [ ] T086 Final code review: verify all functions have docstrings with examples

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies - can start immediately
- **Foundational (Phase 2)**: Depends on Setup completion - BLOCKS all user stories
- **User Stories (Phase 3-6)**: All depend on Foundational phase completion
  - User stories can then proceed in parallel (if staffed)
  - Or sequentially in priority order (P1: US1 → P1: US2 → P2: US3 → P3: US4)
- **Polish (Phase 7)**: Depends on all user stories being complete

### User Story Dependencies

- **User Story 1 (P1)**: Can start after Foundational (Phase 2) - No dependencies on other stories
- **User Story 2 (P1)**: Can start after Foundational (Phase 2) - No dependencies on other stories
- **User Story 3 (P2)**: Can start after Foundational (Phase 2) - Integrates with codec/encode but independently testable
- **User Story 4 (P3)**: Can start after Foundational (Phase 2) - No dependencies on other stories (moved functions already work)

### Within Each User Story

- Tests MUST be written and FAIL before implementation
- Implementation tasks generally sequential (depend on previous tasks in same story)
- Story complete before moving to next priority

### Parallel Opportunities

- All Setup tasks (T001-T003) marked [P] can run in parallel
- All Foundational codec move tasks (T005-T007) marked [P] can run in parallel
- Once Foundational phase completes, all user stories can start in parallel (if team capacity allows)
- All tests within a user story marked [P] can run in parallel (write all failing tests together)
- User Story 1 and User Story 2 can be worked on in parallel by different developers
- User Story 3 and User Story 4 can be worked on in parallel after US1/US2 complete
- Polish tasks marked [P] (T064-T067, T074) can run in parallel

---

## Parallel Example: User Story 3

```bash
# Launch all tests for User Story 3 together (T034-T045):
# These test all three arities of the create function
Task: "Unit test for create with zero args (generates new TypeID with no prefix)"
Task: "Unit test for create with one arg (string prefix) generating new TypeID"
Task: "Unit test for create with one arg (keyword prefix) generating new TypeID"
Task: "Unit test for create with one arg (nil prefix) generating new TypeID"
Task: "Unit test for create with two args (string prefix and UUID)"
Task: "Unit test for create with two args (keyword prefix and UUID)"
Task: "Unit test for create with two args (nil prefix and UUID)"
Task: "Unit test for create two-arity with various UUID versions (v1, v4, v7)"
Task: "Unit test for create two-arity with edge-case UUIDs (all-zeros, all-ones)"
Task: "Unit test for create exception throwing on invalid UUID type"
Task: "Unit test for create exception throwing on invalid prefix"
Task: "Property-based test for create round-trip with parse (all arities)"
```

## Parallel Example: User Story 4

```bash
# Launch all docstring updates together (T066-T069):
Task: "Add comprehensive docstring with examples to encode function in src/typeid/codec.clj"
Task: "Add comprehensive docstring with examples to decode function in src/typeid/codec.clj"
Task: "Add comprehensive docstring with examples to uuid->hex function in src/typeid/codec.clj"
Task: "Add comprehensive docstring with examples to hex->uuid function in src/typeid/codec.clj"
```

---

## Implementation Strategy

### MVP First (User Stories 1, 2, and 3)

1. Complete Phase 1: Setup
2. Complete Phase 2: Foundational (CRITICAL - blocks all stories)
3. Complete Phase 3: User Story 1 (validation with `explain`)
4. Complete Phase 4: User Story 2 (parsing with `parse`)
5. Complete Phase 5: User Story 3 (creation with unified `create` function - replaces old `generate`)
6. **STOP and VALIDATE**: Test User Stories 1, 2, and 3 independently
7. Deploy/demo if ready (complete core API: explain, parse, create)

### Incremental Delivery

1. Complete Setup + Foundational → Foundation ready
2. Add User Story 1 + User Story 2 + User Story 3 → Test independently → Deploy/Demo (MVP! - complete core API with explain, parse, create)
3. Add User Story 4 → Test independently → Deploy/Demo (advanced codec operations)
4. Complete Phase 7 → Final polish → v1.0.0 release

### Parallel Team Strategy

With multiple developers:

1. Team completes Setup + Foundational together
2. Once Foundational is done:
   - Developer A: User Story 1 (explain function)
   - Developer B: User Story 2 (parse function)
   - Developer C: User Story 3 (unified create function with 3 arities, replaces generate)
3. After US1, US2, and US3 complete:
   - Any developer: User Story 4 (codec documentation and tests)
4. Team completes Phase 7 (Polish) together

---

## Notes

- [P] tasks = different files, no dependencies
- [Story] label maps task to specific user story for traceability
- Each user story should be independently completable and testable
- Verify tests fail before implementing (TDD approach)
- Commit after each task or logical group
- Stop at any checkpoint to validate story independently
- User Stories 1, 2, and 3 together form the complete core API (explain, parse, create)
- User Story 3 is critical: implements unified `create` function with 3 arities, replacing old `generate`
- User Story 4 is P3 - exposes low-level codec operations for advanced users
- All breaking changes from old API (validate, typeid->map, generate removed; codec functions moved; create replaces generate)
