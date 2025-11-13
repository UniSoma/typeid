# Feature Specification: UUID API Promotion

**Feature Branch**: `004-uuid-api-promotion`
**Created**: 2025-11-13
**Status**: Draft
**Input**: User description: "Promote typeid.impl.uuid to typeid.uuid as official public API"

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Convert UUID Objects to Bytes (Priority: P1)

Library users need to convert platform-native UUID objects (returned by `typeid/parse`) to byte arrays for specific use cases such as:
- Binary protocols requiring raw bytes
- Legacy database drivers that don't accept UUID objects
- Custom serialization formats
- Low-level networking code

**Why this priority**: This is already documented in the README FAQ (line 616-623) as a necessary workaround. Users are currently forced to access the `impl` namespace, which signals private/unstable API.

**Independent Test**: Can be fully tested by importing the public UUID namespace, passing a UUID object to `uuid->bytes`, and verifying it returns a 16-byte array with correct content.

**Acceptance Scenarios**:

1. **Given** a JVM UUID object, **When** user calls `uuid->bytes`, **Then** returns a 16-byte array with correct byte representation
2. **Given** a ClojureScript UUID object, **When** user calls `uuid->bytes`, **Then** returns a Uint8Array with correct byte representation
3. **Given** an invalid input (non-UUID), **When** user calls `uuid->bytes`, **Then** throws clear exception with error details

---

### User Story 2 - Convert Bytes to UUID Objects (Priority: P1)

Library users need to convert byte arrays to platform-native UUID objects for round-trip operations:
- Deserializing data from binary formats
- Reconstructing UUIDs from database byte columns
- Converting between byte and object representations
- Interoperability with systems that store UUIDs as bytes

**Why this priority**: Natural inverse of `uuid->bytes`. Required for complete round-trip operations. Without this, users have incomplete tooling for UUID manipulation.

**Independent Test**: Can be fully tested by providing a 16-byte array, calling `bytes->uuid`, and verifying it returns a valid platform-native UUID object that matches the input bytes.

**Acceptance Scenarios**:

1. **Given** a 16-byte array, **When** user calls `bytes->uuid`, **Then** returns a platform-native UUID object
2. **Given** a byte array that round-trips through `uuid->bytes`, **When** user calls `bytes->uuid`, **Then** returns UUID equal to original
3. **Given** an invalid byte array (wrong size), **When** user calls `bytes->uuid`, **Then** throws clear exception with validation error

---

### User Story 3 - Generate UUIDv7 Directly (Priority: P2)

Power users may want to generate UUIDv7 objects directly without TypeID string encoding:
- Use UUIDv7 for chronological sorting without TypeID prefixes
- Generate UUIDs for internal system identifiers
- Integrate with existing UUID-based systems while adopting UUIDv7
- Benchmark or test UUID generation independently

**Why this priority**: Lower priority because users can achieve this via `(typeid/parse (typeid/create nil))`, but direct access provides cleaner API for UUID-focused use cases and reduces overhead for users who don't need TypeID encoding.

**Independent Test**: Can be fully tested by calling `generate-uuidv7`, verifying it returns a platform-native UUID object with UUIDv7 structure (timestamp-based, correct version/variant bits).

**Acceptance Scenarios**:

1. **Given** no arguments, **When** user calls `generate-uuidv7`, **Then** returns a platform-native UUID object with UUIDv7 format
2. **Given** multiple sequential calls, **When** user calls `generate-uuidv7`, **Then** each UUID is chronologically sortable
3. **Given** UUIDv7 object, **When** inspecting version bits, **Then** confirms version 7 (0111) and proper variant (10)

---

### Edge Cases

- What happens when `uuid->bytes` receives nil or non-UUID types?
- How does `bytes->uuid` handle byte arrays with incorrect length (< 16 or > 16 bytes)?
- What happens when users pass ClojureScript UUID to JVM function or vice versa?
- How does the API handle corrupted byte data that doesn't form valid UUID structure?
- What happens with platform-specific byte array types (byte[] vs Uint8Array)?

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST provide a public namespace `typeid.uuid` for UUID utility functions
- **FR-002**: System MUST expose `uuid->bytes` function that converts platform-native UUID objects to byte arrays
- **FR-003**: System MUST expose `bytes->uuid` function that converts byte arrays to platform-native UUID objects
- **FR-004**: System MUST expose `generate-uuidv7` function that creates UUIDv7-compliant UUID objects
- **FR-005**: System MUST maintain cross-platform compatibility (JVM and ClojureScript) for all exposed functions
- **FR-006**: System MUST validate input types and throw informative exceptions for invalid inputs
- **FR-007**: System MUST ensure `uuid->bytes` and `bytes->uuid` are inverse operations (round-trip property)
- **FR-008**: System MUST preserve all functionality from `typeid.impl.uuid` in the new public namespace
- **FR-009**: System MUST update README documentation to reference `typeid.uuid` instead of `typeid.impl.uuid`
- **FR-010**: System MUST document the breaking change in CHANGELOG.md

### Key Entities

- **UUID Object**: Platform-native UUID representation (java.util.UUID on JVM, cljs.core/UUID on ClojureScript)
- **Byte Array**: Raw 16-byte representation of UUID (byte[] on JVM, Uint8Array on ClojureScript)
- **UUIDv7**: RFC 9562-compliant UUID with timestamp-based chronological ordering

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Users can access UUID utility functions through public `typeid.uuid` namespace without compiler warnings
- **SC-002**: All three functions (`uuid->bytes`, `bytes->uuid`, `generate-uuidv7`) pass existing test suite without modifications
- **SC-003**: Round-trip operations (UUID → bytes → UUID) preserve equality for 100% of test cases
- **SC-004**: Documentation includes at least 2 practical examples of using `typeid.uuid` namespace
- **SC-005**: Zero breaking changes to existing public API (`typeid.core`, `typeid.codec`)
- **SC-006**: Performance remains within 5% of current implementation (no overhead from namespace change)
- **SC-007**: Library users can complete byte conversion operations without accessing `impl` namespace

## Scope *(mandatory)*

### In Scope

- Creating public `typeid.uuid` namespace with three functions
- Moving function implementations from `typeid.impl.uuid` to `typeid.uuid`
- Removing `typeid.impl.uuid` namespace entirely (breaking change acceptable in 0.x)
- Updating README to document public UUID API
- Updating CHANGELOG with breaking change notice
- Ensuring all tests pass with new namespace structure
- Cross-platform compatibility (JVM and ClojureScript)

### Out of Scope

- Adding new UUID utility functions beyond existing three
- Changing function signatures or behavior
- Supporting additional UUID versions (v1, v4, v6, etc.)
- Adding UUID validation functions
- Creating UUID parsing from strings (already handled by platform)
- Modifying core TypeID functionality (`typeid.core`, `typeid.codec`)
- Detailed migration guide (simple changelog notice is sufficient)

## Dependencies & Assumptions *(mandatory)*

### Dependencies

- No external dependencies required (maintains zero-dependency promise)
- Depends on existing `typeid.impl.uuid` implementation
- Requires updates to namespace imports in internal code

### Assumptions

- Breaking changes are acceptable in 0.x versions (before 1.0.0)
- Simple changelog notice is sufficient for migration (no detailed guide needed)
- Few users currently depend on `typeid.impl.uuid` given its `impl` namespace designation
- Existing test coverage is sufficient to validate namespace change
- No performance regressions expected from namespace reorganization
- README FAQ section already demonstrates user need for these functions

## Open Questions *(include if applicable)*

None. Feature scope is clear and well-defined based on existing implementation.
