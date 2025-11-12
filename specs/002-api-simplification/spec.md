# Feature Specification: TypeID API Simplification

**Feature Branch**: `002-api-simplification`
**Created**: 2025-11-11
**Status**: Draft
**Input**: User description: "I changed my mind and want some adjustments to the core api: - Let's drop data as error paradigm
- typeid.core/validate should be named typeid.core/explain. It should return nil if the typeid is valid or a map with error details (this map can be similar to the current one)
- typeid.core/validate should be droped
- typeid.core/typeid->map should be droped. Keep typeid.core/parse instead, with this semantics: if valid returns a map of decomposed components. If invalid, throws an exception.
- Add a second arity to the typeid.core/create function, accepting a prefix and a Clojure/Clojurescript uuid.
- It is not clear the usefullnes of havind decode, encode, hex->uuid and uuid->hex in typeid.core. Move them to a new namespace typeid.codec"

## Clarifications

### Session 2025-11-11

- Q: What should `explain` return when given non-string input (e.g., nil, numbers, maps)? → A: Return an error map indicating invalid input type (graceful handling)
- Q: How should edge-case UUIDs (all zeros, all ones, non-UUIDv7 formats) be handled by the `create` function? → A: Accept any valid UUID (all formats: v1, v4, v7, etc.) and encode without validation of UUID version or content
- Q: What specific exception type(s) should functions throw, and what should the exception data structure contain? → A: Use platform-standard exceptions (ExceptionInfo/ex-info) with consistent data map structure containing `:type`, `:message`, and context

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Simple TypeID Validation (Priority: P1)

As a developer using the TypeID library, I want to check if a TypeID string is valid and understand why it's invalid if it fails, so I can provide appropriate feedback to users or handle errors gracefully.

**Why this priority**: Validation is the most fundamental operation - developers need to verify TypeIDs before processing them. This is essential for any application using TypeIDs.

**Independent Test**: Can be fully tested by calling the explain function with various valid and invalid TypeID strings and verifying it returns nil for valid inputs and error details for invalid inputs.

**Acceptance Scenarios**:

1. **Given** a valid TypeID string, **When** I check its validity, **Then** the system returns nil indicating it's valid
2. **Given** an invalid TypeID string with malformed suffix, **When** I check its validity, **Then** the system returns a map with error type, message, and relevant details
3. **Given** an invalid TypeID string with bad prefix, **When** I check its validity, **Then** the system returns a map explaining the prefix validation failure

---

### User Story 2 - Parse TypeID Components (Priority: P1)

As a developer, I want to extract the components (prefix, suffix, UUID) from a valid TypeID string so I can work with the individual parts programmatically.

**Why this priority**: Parsing is equally fundamental - once validated, developers need to decompose TypeIDs to access their constituent parts (prefix for routing/categorization, UUID for database operations).

**Independent Test**: Can be fully tested by parsing valid TypeID strings and verifying the returned map contains correct prefix, suffix, UUID, and full typeid values. Invalid inputs should throw exceptions.

**Acceptance Scenarios**:

1. **Given** a valid TypeID with prefix, **When** I parse it, **Then** I receive a map with prefix, suffix, uuid bytes, and full typeid string
2. **Given** a valid TypeID without prefix, **When** I parse it, **Then** I receive a map with empty prefix, suffix, uuid bytes, and full typeid string
3. **Given** an invalid TypeID string, **When** I attempt to parse it, **Then** an exception is thrown with error details

---

### User Story 3 - Create TypeIDs with Flexible Options (Priority: P2)

As a developer, I want to create TypeIDs with a single unified function that supports generating fresh TypeIDs (with or without prefix) and creating TypeIDs from existing UUIDs, so I have a consistent, flexible API for all creation scenarios.

**Why this priority**: This consolidates creation logic into one function (`create`), replacing the old `generate` function while adding the ability to create TypeIDs from existing UUIDs for migration and interoperability scenarios.

**Independent Test**: Can be fully tested by calling `create` with different arities (0-arg for new TypeID, 1-arg for new TypeID with prefix, 2-arg for TypeID from existing UUID) and verifying correct behavior for each case.

**Acceptance Scenarios**:

1. **Given** no arguments, **When** I call create, **Then** I receive a properly formatted TypeID string with fresh UUIDv7 and no prefix
2. **Given** a prefix string, **When** I call create with one argument, **Then** I receive a properly formatted TypeID with the given prefix and fresh UUIDv7
3. **Given** a keyword prefix, **When** I call create with one argument, **Then** I receive a properly formatted TypeID using the keyword's name as prefix and fresh UUIDv7
4. **Given** nil as prefix, **When** I call create with one argument, **Then** I receive a prefix-less TypeID with fresh UUIDv7
5. **Given** a prefix string and a valid UUID, **When** I call create with two arguments, **Then** I receive a properly formatted TypeID encoding the provided UUID with the given prefix
6. **Given** a keyword prefix and a valid UUID, **When** I call create with two arguments, **Then** I receive a properly formatted TypeID using the keyword's name as prefix
7. **Given** nil prefix and a valid UUID, **When** I call create with two arguments, **Then** I receive a prefix-less TypeID encoding the provided UUID
8. **Given** an invalid UUID or invalid prefix, **When** I attempt to create a TypeID, **Then** an exception is thrown with error details

---

### User Story 4 - Encode and Decode Operations (Priority: P3)

As a developer working with low-level UUID operations, I want to encode UUID bytes to TypeID format and decode TypeID suffixes back to UUID bytes, as well as convert between UUID bytes and hex string representations.

**Why this priority**: These are specialized codec operations needed for advanced use cases, testing, or integration with systems that work with raw UUID bytes or hex strings. Not required for typical TypeID usage.

**Independent Test**: Can be fully tested by encoding UUID bytes with various prefixes and decoding the results back to verify round-trip integrity. Hex conversions can be tested similarly.

**Acceptance Scenarios**:

1. **Given** UUID bytes and a prefix, **When** I encode them, **Then** I receive a properly formatted TypeID string
2. **Given** a valid TypeID string, **When** I decode it, **Then** I receive the original UUID bytes
3. **Given** UUID bytes, **When** I convert to hex string, **Then** I receive a 32-character lowercase hex representation
4. **Given** a hex string, **When** I convert to UUID bytes, **Then** I receive 16-byte UUID representation
5. **Given** invalid inputs to codec operations, **When** I attempt conversion, **Then** an exception is thrown with error details

---

### Edge Cases

- The `explain` function gracefully handles non-string inputs by returning an error map with invalid input type details
- The `parse` function throws ExceptionInfo (via `ex-info`) for TypeID-like strings with validation errors, with data containing `:type`, `:message`, and context
- The `create` function accepts any valid UUID regardless of version (v1, v4, v7) or content (all-zeros, all-ones) without validation
- Codec functions throw ExceptionInfo for malformed hex strings or wrong-length byte arrays, with structured error data
- All exceptions use ExceptionInfo with consistent data map structure containing `:type`, `:message`, and relevant context fields

## Requirements *(mandatory)*

### Functional Requirements

#### Core API Functions

- **FR-001**: The library MUST provide an `explain` function that accepts any input and returns nil if given a valid TypeID string or an error map for invalid/non-string inputs
- **FR-002**: The `explain` function error map MUST include error type, human-readable message, and relevant debugging data
- **FR-003**: The library MUST provide a `parse` function that returns a map of components (prefix, suffix, uuid, typeid) for valid TypeIDs
- **FR-004**: The `parse` function MUST throw an exception with error details when given invalid TypeID strings

#### TypeID Creation

- **FR-005**: The library MUST provide a `create` function with zero-arity that generates a new TypeID with fresh UUIDv7 (replaces `generate` with no arguments)
- **FR-006**: The library MUST provide a `create` function with one-arity accepting a prefix (nil, string, or keyword) that generates a new TypeID with the given prefix and fresh UUIDv7 (replaces `generate` with prefix)
- **FR-007**: The library MUST provide a `create` function with two-arity accepting prefix and an existing UUID that creates a TypeID from the provided UUID
- **FR-008**: The two-arity `create` function MUST accept Clojure/ClojureScript UUID objects of any version (v1, v4, v7, etc.) without validation of UUID version or content
- **FR-009**: The two-arity `create` function MUST encode the provided UUID into TypeID format with the given prefix, accepting edge cases like all-zeros or all-ones UUIDs
- **FR-010**: All `create` arities MUST handle nil prefix (generates prefix-less TypeID)
- **FR-011**: All `create` arities MUST handle keyword prefix by extracting its name
- **FR-012**: All `create` arities MUST validate prefix format and throw exceptions for invalid prefixes

#### Codec Namespace Organization

- **FR-013**: The library MUST provide a separate `typeid.codec` namespace for low-level conversion functions
- **FR-014**: The `typeid.codec` namespace MUST include an `encode` function that converts UUID bytes and prefix to TypeID string
- **FR-015**: The `typeid.codec` namespace MUST include a `decode` function that extracts UUID bytes from TypeID string
- **FR-016**: The `typeid.codec` namespace MUST include a `uuid->hex` function that converts UUID bytes to hex string
- **FR-017**: The `typeid.codec` namespace MUST include a `hex->uuid` function that converts hex string to UUID bytes

#### Deprecated Functions Removal

- **FR-018**: The `typeid.core/validate` function MUST be removed (replaced by `explain`)
- **FR-019**: The `typeid.core/typeid->map` function MUST be removed (functionality merged into `parse`)
- **FR-020**: The `typeid.core/generate` function MUST be removed (replaced by `create` with zero-arity and one-arity)
- **FR-021**: The `typeid.core/decode` function MUST be moved to `typeid.codec/decode`
- **FR-022**: The `typeid.core/encode` function MUST be moved to `typeid.codec/encode`
- **FR-023**: The `typeid.core/uuid->hex` function MUST be moved to `typeid.codec/uuid->hex`
- **FR-024**: The `typeid.core/hex->uuid` function MUST be moved to `typeid.codec/hex->uuid`

#### Error Handling

- **FR-025**: All functions MUST throw exceptions for invalid inputs (no `{:ok result}` or `{:error map}` pattern)
- **FR-026**: All functions MUST use platform-standard ExceptionInfo (via `ex-info` in Clojure/ClojureScript) for all error conditions
- **FR-027**: Exception data maps MUST contain at minimum: `:type` (keyword identifying error category), `:message` (human-readable string), and relevant context fields (e.g., `:input`, `:prefix`, `:expected`)
- **FR-028**: Exception messages MUST be clear and actionable, describing what went wrong and why

### Key Entities

- **TypeID String**: A string representation combining optional prefix, separator, and base32-encoded UUID suffix (e.g., "user_01h5fskfsk4fpeqwnsyz5hj55t")
- **TypeID Components Map**: A map containing decomposed parts: prefix (string), suffix (string), uuid (bytes), typeid (full string)
- **Error Map**: A map describing validation failures: type (keyword), message (string), data (map with context)
- **UUID**: A universally unique identifier (16 bytes) used as the core identifier within TypeIDs
- **Prefix**: An optional type indicator (0-63 lowercase characters) prepended to TypeIDs

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Developers can determine TypeID validity in a single function call without catching exceptions
- **SC-002**: All core TypeID operations (explain, parse, create) are available in the main `typeid.core` namespace with clear, consistent naming
- **SC-003**: Advanced codec operations are organized in a dedicated `typeid.codec` namespace, separate from core functionality
- **SC-004**: Developers can create TypeIDs from existing UUIDs without needing to work with raw bytes
- **SC-005**: API surface is reduced and unified by consolidating creation into a single `create` function and eliminating redundant functions (validate, typeid->map, generate) while maintaining all capabilities
- **SC-006**: Error handling is consistent across all functions using exceptions with structured data
- **SC-007**: All existing test cases continue to pass with updated function names and error handling patterns
- **SC-008**: Documentation clearly distinguishes between core operations (typeid.core) and codec utilities (typeid.codec)

## Assumptions

1. **UUID Format**: The two-arity `create` function accepts platform-native UUID objects (java.util.UUID on JVM, UUID from cljs.core on ClojureScript)
2. **Backward Compatibility**: This is a breaking change - existing code using `validate`, `typeid->map`, `generate`, or direct access to `encode`/`decode` in typeid.core will need updates
3. **Error Handling Philosophy**: Exceptions are preferred over result maps for consistency with Clojure/ClojureScript conventions and cleaner code (no need to destructure results)
4. **Migration Path**: Users migrating from the old API can: replace `validate` with `explain`, replace `typeid->map` with `parse`, replace `generate` with `create`, and require `typeid.codec` for low-level operations
5. **Function Naming**: `create` is chosen to unify all creation scenarios (with/without UUID, with/without prefix) in a single, multi-arity function
6. **Codec Namespace Purpose**: The `typeid.codec` namespace is for users who need low-level access to encoding/decoding operations, typically for testing, debugging, or integration with other systems

## Dependencies

None - this is a refactoring of existing functionality within the TypeID library.

## Out of Scope

- Changes to the underlying UUID generation or base32 encoding algorithms
- Performance optimizations beyond what's needed for the refactoring
- Additional validation predicates or helper functions
- Changes to the `typeid.validation` namespace (remains as-is for internal use)
- Migration tooling or automated code transformation utilities
- Changes to TypeID specification compliance (still follows v0.3.0 spec)
