# Tasks: UUID API Promotion

**Input**: Design documents from `/specs/004-uuid-api-promotion/`
**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/api.md

**Tests**: This feature does not require new tests. Existing tests will be updated to use the new namespace.

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

## Path Conventions

Single library project structure:
- Source: `src/typeid/`
- Tests: `test/typeid/`

---

## Phase 1: Setup (Namespace Preparation)

**Purpose**: Verify current state and prepare for namespace migration

- [x] T001 Verify existing implementation in src/typeid/impl/uuid.cljc has all three functions
- [x] T002 [P] Find all internal references to typeid.impl.uuid in src/ directory
- [x] T003 [P] Find all test references to typeid.impl.uuid in test/ directory

---

## Phase 2: Foundational (Breaking Prerequisites)

**Purpose**: Core preparation that MUST be complete before ANY user story implementation

**⚠️ CRITICAL**: No user story work can begin until this phase is complete

- [x] T004 Create new public namespace file at src/typeid/uuid.cljc
- [x] T005 Copy implementation from src/typeid/impl/uuid.cljc to src/typeid/uuid.cljc
- [x] T006 Remove ^:no-doc metadata from namespace declaration in src/typeid/uuid.cljc
- [x] T007 Add comprehensive namespace docstring with examples to src/typeid/uuid.cljc

**Checkpoint**: Foundation ready - user story implementation can now begin

---

## Phase 3: User Story 1 - Convert UUID Objects to Bytes (Priority: P1) 🎯 MVP

**Goal**: Expose `uuid->bytes` function in public typeid.uuid namespace for converting UUID objects to byte arrays

**Independent Test**: Import typeid.uuid, call uuid->bytes with a UUID object, verify it returns a 16-byte array

### Implementation for User Story 1

- [x] T008 [US1] Ensure uuid->bytes function in src/typeid/uuid.cljc has comprehensive docstring with JVM and ClojureScript examples
- [x] T009 [US1] Verify uuid->bytes validates UUID input and throws ex-info for invalid types
- [x] T010 [US1] Update test/typeid/uuid_test.cljc to import from typeid.uuid instead of typeid.impl.uuid
- [x] T011 [US1] Run JVM tests for uuid->bytes to verify functionality unchanged
- [x] T012 [US1] Run ClojureScript tests for uuid->bytes to verify functionality unchanged

**Checkpoint**: uuid->bytes is publicly accessible and fully tested

---

## Phase 4: User Story 2 - Convert Bytes to UUID Objects (Priority: P1)

**Goal**: Expose `bytes->uuid` function in public typeid.uuid namespace for converting byte arrays to UUID objects

**Independent Test**: Import typeid.uuid, call bytes->uuid with a 16-byte array, verify it returns a valid UUID object that round-trips correctly

### Implementation for User Story 2

- [x] T013 [US2] Ensure bytes->uuid function in src/typeid/uuid.cljc has comprehensive docstring with JVM and ClojureScript examples
- [x] T014 [US2] Verify bytes->uuid validates byte array length (must be exactly 16 bytes)
- [x] T015 [US2] Verify bytes->uuid throws ex-info for invalid input (wrong size, wrong type)
- [x] T016 [US2] Add test case verifying round-trip property: uuid == bytes->uuid(uuid->bytes(uuid))
- [x] T017 [US2] Run JVM tests for bytes->uuid to verify functionality unchanged
- [x] T018 [US2] Run ClojureScript tests for bytes->uuid to verify functionality unchanged

**Checkpoint**: bytes->uuid is publicly accessible, round-trip operations verified

---

## Phase 5: User Story 3 - Generate UUIDv7 Directly (Priority: P2)

**Goal**: Expose `generate-uuidv7` function in public typeid.uuid namespace for creating timestamp-ordered UUIDs

**Independent Test**: Import typeid.uuid, call generate-uuidv7 multiple times, verify each returns a valid UUIDv7 with correct version bits and chronological ordering

### Implementation for User Story 3

- [x] T019 [US3] Ensure generate-uuidv7 function in src/typeid/uuid.cljc has comprehensive docstring explaining UUIDv7 structure
- [x] T020 [US3] Verify generate-uuidv7 produces UUIDs with version bits = 7 (0111) in byte 6
- [x] T021 [US3] Verify generate-uuidv7 produces chronologically sortable UUIDs
- [x] T022 [US3] Run JVM tests for generate-uuidv7 to verify functionality unchanged
- [x] T023 [US3] Run ClojureScript tests for generate-uuidv7 to verify functionality unchanged

**Checkpoint**: All three UUID utility functions are publicly accessible and tested

---

## Phase 6: Internal References Update

**Purpose**: Update all internal code that references the old impl namespace

- [x] T024 [P] Update typeid.core namespace in src/typeid/core.cljc to require typeid.uuid instead of typeid.impl.uuid
- [x] T025 [P] Update typeid.codec namespace in src/typeid/codec.cljc to require typeid.uuid instead of typeid.impl.uuid (N/A: codec doesn't import uuid)
- [x] T026 [P] Search for any other internal references to typeid.impl.uuid and update them (Updated: dev/benchmarks/core_bench.clj, dev/user.clj, test/typeid/compliance_test.clj)
- [x] T027 Run full test suite (JVM and ClojureScript) to verify all internal references work correctly

**Checkpoint**: All internal code uses new public namespace

---

## Phase 7: Breaking Change - Remove Old Namespace

**Purpose**: Complete the migration by removing the old impl namespace

- [x] T028 Delete old namespace file at src/typeid/impl/uuid.cljc
- [x] T029 Run full test suite (JVM) to verify no regressions after deletion
- [x] T030 Run full test suite (ClojureScript) to verify no regressions after deletion
- [x] T031 Run clj-kondo to ensure zero warnings or errors

**Checkpoint**: Old namespace removed, all tests passing

---

## Phase 8: Documentation & Release Preparation

**Purpose**: Update all user-facing documentation and prepare for release

- [x] T032 [P] Update README.md FAQ section (line 616-623) to use typeid.uuid instead of typeid.impl.uuid
- [x] T033 [P] Add CHANGELOG.md entry for breaking change: "BREAKING: Moved UUID utilities from typeid.impl.uuid to typeid.uuid"
- [x] T034 [P] Add migration note to CHANGELOG.md: "Replace requires of typeid.impl.uuid with typeid.uuid"
- [x] T035 [P] Add at least 2 usage examples of typeid.uuid namespace to README.md
- [x] T036 Verify cljdoc will generate documentation for new public namespace (check namespace is not marked :no-doc)
- [x] T037 [P] Update version number in appropriate files for 0.3.0 release (or next version)

**Checkpoint**: Documentation complete, ready for release

---

## Phase 9: Performance & Final Validation

**Purpose**: Verify performance and run final validation checks

- [x] T038 Run existing benchmarks for uuid->bytes (verify < 1μs on JVM, < 1μs on ClojureScript)
- [x] T039 Run existing benchmarks for bytes->uuid (verify < 1μs on JVM, < 1μs on ClojureScript)
- [x] T040 Run existing benchmarks for generate-uuidv7 (verify < 2μs on JVM, < 2μs on ClojureScript)
- [x] T041 Compare benchmark results to baseline: verify performance within 5% of previous implementation
- [x] T042 Run complete test suite on both platforms one final time
- [x] T043 Verify all success criteria from spec.md are met

**Checkpoint**: All success criteria met, ready for PR/release

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies - can start immediately
- **Foundational (Phase 2)**: Depends on Setup (Phase 1) completion
- **User Story 1 (Phase 3)**: Depends on Foundational (Phase 2) completion
- **User Story 2 (Phase 4)**: Depends on Foundational (Phase 2) completion - CAN run in parallel with US1
- **User Story 3 (Phase 5)**: Depends on Foundational (Phase 2) completion - CAN run in parallel with US1 and US2
- **Internal References (Phase 6)**: Depends on ALL user stories (Phases 3-5) completion
- **Remove Old Namespace (Phase 7)**: Depends on Internal References (Phase 6) completion
- **Documentation (Phase 8)**: Depends on Remove Old Namespace (Phase 7) - Can partially overlap
- **Performance (Phase 9)**: Depends on all previous phases

### User Story Dependencies

All three user stories are independent and work on the same file (src/typeid/uuid.cljc), but:

- **User Story 1**: `uuid->bytes` function - Independent
- **User Story 2**: `bytes->uuid` function - Independent (though conceptually paired with US1)
- **User Story 3**: `generate-uuidv7` function - Independent

Since all three stories edit the same file, they CANNOT be done in parallel by different developers. However, they are conceptually independent functions, so they can be completed sequentially in any order.

### Within Each User Story

- Docstring updates before tests
- Validation checks before running tests
- JVM tests can run in parallel with ClojureScript tests (different platforms)

### Parallel Opportunities

**Phase 1 (Setup)**:
- T002 (find internal refs) and T003 (find test refs) can run in parallel

**Phase 2 (Foundational)**:
- All tasks are sequential (same file operations)

**Phases 3-5 (User Stories)**:
- Since all work on same file, must be sequential
- Within each story, JVM and ClojureScript tests can run in parallel

**Phase 6 (Internal References)**:
- T024, T025, T026 can run in parallel (different files)

**Phase 8 (Documentation)**:
- T032, T033, T034, T035, T037 can all run in parallel (different files or different sections)

---

## Parallel Example: Phase 6 (Internal References Update)

```bash
# Launch all internal reference updates together:
Task: "Update typeid.core in src/typeid/core.cljc"
Task: "Update typeid.codec in src/typeid/codec.cljc"
Task: "Search and update any other references"
```

---

## Parallel Example: Phase 8 (Documentation)

```bash
# Launch all documentation updates together:
Task: "Update README.md FAQ section"
Task: "Add CHANGELOG.md breaking change entry"
Task: "Add CHANGELOG.md migration note"
Task: "Add usage examples to README.md"
Task: "Update version number"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only - Absolute Minimum)

1. Complete Phase 1: Setup
2. Complete Phase 2: Foundational
3. Complete Phase 3: User Story 1 (uuid->bytes only)
4. **STOP and VALIDATE**: Test uuid->bytes independently
5. Optionally deploy/demo with just one function public

**Note**: This gives users access to uuid->bytes, but without bytes->uuid, they cannot do round-trips. Consider MVP as User Stories 1 + 2 together for practical use.

### Recommended MVP (User Stories 1 + 2 - Practical Minimum)

1. Complete Phase 1: Setup
2. Complete Phase 2: Foundational
3. Complete Phase 3: User Story 1 (uuid->bytes)
4. Complete Phase 4: User Story 2 (bytes->uuid)
5. **STOP and VALIDATE**: Test round-trip operations
6. Optionally deploy/demo with bidirectional conversion

**Rationale**: uuid->bytes and bytes->uuid are paired operations. Having both enables the most common use case (round-trip serialization).

### Full Feature (All User Stories)

1. Complete Setup + Foundational
2. Complete User Story 1 (uuid->bytes)
3. Complete User Story 2 (bytes->uuid)
4. Complete User Story 3 (generate-uuidv7)
5. Complete Internal References update
6. Complete Breaking Change (remove old namespace)
7. Complete Documentation
8. Complete Performance validation
9. **Ready for PR/release**

### Incremental Delivery

Not applicable for this feature - all changes must be released together as a single breaking change (namespace rename). Cannot partially deploy.

However, you can pause at checkpoints for validation:

1. After Phase 2: New namespace exists but nothing uses it yet
2. After Phase 5: All functions public, old namespace still exists (no breaking change yet)
3. After Phase 7: Breaking change complete (old namespace gone)
4. After Phase 9: Release-ready

---

## Notes

- [P] tasks = different files, no dependencies, can run in parallel
- [Story] label maps task to specific user story (US1, US2, US3)
- All three user stories edit the same file (src/typeid/uuid.cljc), so they must be done sequentially
- Tests are updates to existing tests, not new test creation
- Existing test coverage is sufficient per constitution
- Performance budgets: uuid->bytes/bytes->uuid < 1μs, generate-uuidv7 < 2μs
- Breaking change is acceptable in 0.x version
- Verify clj-kondo stays clean throughout
- Success criteria from spec.md must all be verified in Phase 9

---

## Task Count Summary

- **Total Tasks**: 43
- **Phase 1 (Setup)**: 3 tasks
- **Phase 2 (Foundational)**: 4 tasks
- **Phase 3 (US1 - uuid->bytes)**: 5 tasks
- **Phase 4 (US2 - bytes->uuid)**: 6 tasks
- **Phase 5 (US3 - generate-uuidv7)**: 5 tasks
- **Phase 6 (Internal References)**: 4 tasks
- **Phase 7 (Remove Old Namespace)**: 4 tasks
- **Phase 8 (Documentation)**: 6 tasks
- **Phase 9 (Performance & Validation)**: 6 tasks

**Parallel Opportunities**: 11 tasks marked [P] across different phases

**MVP Scope** (Recommended): Phases 1-4 (User Stories 1 + 2) = 18 tasks

**Independent Test Criteria**:
- **US1**: Call uuid->bytes with UUID, verify 16-byte array returned
- **US2**: Call bytes->uuid with bytes, verify UUID returned and round-trip equality holds
- **US3**: Call generate-uuidv7, verify UUIDv7 structure and chronological sorting
