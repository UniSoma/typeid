# Feature Specification: Parse Function Returns Platform UUID

**Feature Branch**: `003-parse-uuid-return`
**Created**: 2025-11-12
**Status**: Draft
**Input**: User description: "Let's change the API to consider the above considerations"

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Round-trip UUID Conversion (Priority: P1)

Developers need to create a TypeID from an existing UUID and then parse it back, expecting to get the same UUID object for comparison, database operations, or further processing.

**Why this priority**: This is the most fundamental use case and represents a breaking inconsistency in the current API. Without this, developers cannot perform natural round-trip conversions, which is a core expectation when working with ID libraries.

**Independent Test**: Can be fully tested by creating a TypeID from a UUID, parsing it, and verifying the returned UUID equals the original, delivering immediate value for UUID-based workflows.

**Acceptance Scenarios**:

1. **Given** a UUID object created from any version (v1, v4, v7), **When** a developer creates a TypeID with that UUID and then parses it, **Then** the extracted UUID equals the original UUID object
2. **Given** a TypeID string parsed into components, **When** the developer extracts the UUID, **Then** the UUID can be directly compared with other UUID objects using standard equality operators
3. **Given** a parsed TypeID with a UUID, **When** the developer recreates a TypeID using that UUID and the original prefix, **Then** the resulting TypeID string matches the original TypeID string

---

### User Story 2 - Database Integration with Native Types (Priority: P1)

Developers need to parse TypeIDs received from external sources (APIs, user input) and store the UUID portion in a database using native UUID columns without manual byte-to-UUID conversion.

**Why this priority**: Database integration is a primary use case for any ID system. Requiring manual conversion adds friction and increases the likelihood of errors in production code.

**Independent Test**: Can be fully tested by parsing a TypeID and using the UUID directly with database drivers that expect native UUID objects, delivering value for persistence workflows.

**Acceptance Scenarios**:

1. **Given** a TypeID string received from an API, **When** a developer parses it and extracts the UUID, **Then** the UUID can be passed directly to database operations without conversion
2. **Given** a TypeID parsed in a web application, **When** the developer needs to send the UUID to a backend API, **Then** the UUID serializes to standard UUID string format automatically
3. **Given** multiple TypeIDs parsed from a batch operation, **When** the developer queries the database using the extracted UUIDs, **Then** all queries execute without type conversion errors

---

### User Story 3 - API Consistency for Developer Experience (Priority: P2)

Developers expect that functions accepting UUIDs and functions returning UUIDs use the same type, creating a consistent and predictable API surface.

**Why this priority**: While not blocking functionality, API consistency significantly improves developer experience and reduces cognitive load when learning and using the library.

**Independent Test**: Can be fully tested by reviewing the API documentation and verifying that creation and parsing both work with platform UUID objects symmetrically, delivering value through improved API usability.

**Acceptance Scenarios**:

1. **Given** TypeID creation accepts platform UUID objects as input, **When** developers parse TypeIDs and extract UUIDs, **Then** the same UUID type is returned
2. **Given** a developer reading the library documentation, **When** they see examples of both creation and parsing, **Then** the UUID type appears consistent across all examples
3. **Given** a developer using type specs or schemas, **When** they define the expected shape of parsed results, **Then** the UUID field type matches the UUID input type for creation

---

### Edge Cases

- What happens when a TypeID with an edge case UUID (zero UUID, max UUID with all 0xFF bytes, maximum timestamp) is parsed? The UUID object should be returned correctly.
- How does the system handle parsing TypeIDs created from non-v7 UUIDs (v1, v4, random)? The parsed UUID should match the original UUID regardless of version.
- What happens when the same TypeID is parsed multiple times? Each parse operation should return an equivalent UUID (comparing equal but potentially different object instances).
- How does the system handle backward compatibility? This will be a breaking change with a minor version bump (e.g., 0.1.x → 0.2.0) since the library is in experimental stage.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: When parsing TypeIDs, the system MUST return platform-native UUID objects (not byte arrays)
- **FR-002**: UUID objects returned from parsing MUST be equivalent to UUIDs used for creation (enabling round-trip conversions)
- **FR-003**: Parsing MUST support all UUID versions (v1, v4, v7, etc.) and edge cases (zero UUID, max UUID with all 0xFF bytes, maximum timestamp values)
- **FR-004**: The system MUST provide utilities to convert between byte arrays and platform UUID objects
- **FR-005**: User documentation MUST show UUID objects (not byte arrays) in all parsing examples
- **FR-006**: API reference documentation MUST accurately reflect UUID object return types
- **FR-007**: Test suites MUST validate UUID object behavior and equality
- **FR-008**: Users who need byte representation MUST still be able to access it via low-level decoding operations
- **FR-009**: The version number MUST be incremented to indicate breaking change (minor version bump: 0.1.x → 0.2.0)

### Key Entities

- **TypeID**: Represents a type-safe identifier with a prefix and UUIDv7 suffix; parsed representation includes prefix, suffix, UUID object, and original string
- **UUID**: Platform-native universally unique identifier object representing a 128-bit value
- **Parsed Result**: Structured data containing type prefix, encoded suffix, UUID object, and original identifier string

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Developers can perform round-trip ID conversions (create from UUID → parse → extract UUID) with direct equality checking without manual conversions
- **SC-002**: Database integration code using parsed UUIDs requires zero manual type conversions for standard database operations
- **SC-003**: All existing low-level byte manipulation functionality remains accessible through dedicated operations
- **SC-004**: Documentation examples demonstrate consistent UUID type usage across creation and parsing operations
- **SC-005**: Developers familiar with standard UUID handling can use the library without learning custom conversion patterns

## Assumptions

- Developers prefer working with platform-native types over low-level byte arrays for high-level operations
- Most use cases involve database storage or UUID comparison, not direct byte manipulation
- Users requiring byte representation are advanced users who can use lower-level codec functions
- The change will primarily affect test code and examples, not production code that treats UUIDs opaquely
- Platform UUID types provide sufficient performance for typical ID operations

## Dependencies

- Requires conversion utilities between byte arrays and platform UUID objects
- May require updates to validation logic if it specifically checks for byte array types
- Depends on platform-native UUID support (available on both JVM and JavaScript platforms)

## Migration Notes

- This is a **breaking change** that will be released with a minor version bump (0.1.x → 0.2.0)
- Since the library is in experimental stage (0.x), breaking changes are expected and acceptable
- Users will need to update code that directly depends on byte array representation
- Migration path: users requiring bytes should use low-level codec operations instead of parsing operations
- Changelog and release notes must clearly document this breaking change

## Out of Scope

- Changes to TypeID creation from UUIDs (already works correctly with platform UUID objects)
- Changes to base32 suffix encoding/decoding algorithms
- Changes to low-level codec operations for byte array manipulation
- Performance optimization of UUID conversion (focus on correctness and consistency first)
- Support for non-standard UUID representations or third-party UUID libraries
- Backward compatibility layer or dual-mode operation (clean breaking change preferred)
