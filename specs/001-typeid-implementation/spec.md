# Feature Specification: TypeID Clojure/ClojureScript Library

**Feature Branch**: `001-typeid-implementation`
**Created**: 2025-11-10
**Status**: Draft
**Input**: User description: "Build typeid, a Clojure and ClojureScript implementation of the TypeId specification (described in typeid.md)"

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Generate and Parse TypeIDs (Priority: P1)

As a developer, I need to generate new TypeIDs with type prefixes and parse existing TypeIDs back into their UUID components so that I can use type-safe identifiers in my application.

**Why this priority**: This is the core functionality - without the ability to create and parse TypeIDs, the library has no value. This represents the minimum viable product.

**Independent Test**: Can be fully tested by generating TypeIDs with various prefixes (including empty prefix), parsing them back, and verifying the UUID matches. Delivers immediate value for applications needing type-safe identifiers.

**Acceptance Scenarios**:

1. **Given** I provide a type prefix "user", **When** I generate a new TypeID, **Then** I receive a string like "user_01h5fskfsk4fpeqwnsyz5hj55t" containing the prefix, separator, and base32-encoded UUIDv7
2. **Given** I provide an empty prefix, **When** I generate a new TypeID, **Then** I receive a 26-character base32-encoded UUIDv7 without prefix or separator
3. **Given** I have a valid TypeID string "user_01h5fskfsk4fpeqwnsyz5hj55t", **When** I parse it, **Then** I receive the prefix "user" and the original UUID bytes
4. **Given** I generate a TypeID, **When** I parse it immediately, **Then** the parsed UUID matches the generated UUID exactly

---

### User Story 2 - Validate TypeIDs (Priority: P2)

As a developer, I need to validate TypeID strings to ensure they conform to the specification before processing them, so that I can reject malformed identifiers early and provide clear error messages.

**Why this priority**: Validation is critical for production use but the library can function without explicit validation (parsing will fail on invalid input). This is the next most important feature after basic generation/parsing.

**Independent Test**: Can be fully tested by providing valid and invalid TypeID strings and verifying correct validation results with appropriate error messages. Works independently of generation functionality.

**Acceptance Scenarios**:

1. **Given** a valid TypeID string "user_01h5fskfsk4fpeqwnsyz5hj55t", **When** I validate it, **Then** it passes validation
2. **Given** an invalid prefix "User_01h5fskfsk4fpeqwnsyz5hj55t" (uppercase), **When** I validate it, **Then** it fails with a clear message about invalid prefix format
3. **Given** an invalid suffix "user_8zzzzzzzzzzzzzzzzzzzzzzzzz" (first char > 7), **When** I validate it, **Then** it fails with a clear message about suffix overflow
4. **Given** a prefix ending with underscore "_prefix_01h5fskfsk4fpeqwnsyz5hj55t", **When** I validate it, **Then** it fails with a clear message about invalid prefix format
5. **Given** a TypeID shorter than 26 characters, **When** I validate it, **Then** it fails with a clear message about invalid length

---

### User Story 3 - Convert Between TypeID and UUID Formats (Priority: P3)

As a developer, I need to convert existing UUIDs (in hex or byte format) to TypeIDs and extract UUIDs from TypeIDs in various formats, so that I can integrate TypeIDs with existing systems that use standard UUIDs.

**Why this priority**: Conversion enables integration with existing systems but is not required for basic TypeID usage. Applications can work with TypeIDs exclusively without needing to convert to/from standard UUID formats.

**Independent Test**: Can be fully tested by providing UUIDs in various formats, converting to TypeIDs, and verifying round-trip conversion works correctly. Independent of generation and validation.

**Acceptance Scenarios**:

1. **Given** a UUIDv7 in hex format, **When** I encode it with prefix "user", **Then** I receive a valid TypeID string
2. **Given** a valid TypeID, **When** I decode it to hex format, **Then** I receive the original UUID as a hex string
3. **Given** a valid TypeID, **When** I decode it to bytes, **Then** I receive the original 16-byte UUID representation
4. **Given** a UUIDv4 (non-v7), **When** I encode it with a prefix, **Then** the encoding succeeds (supporting custom UUID variants)
5. **Given** a TypeID, **When** I extract just the suffix, **Then** I receive the 26-character base32-encoded UUID

---

### Edge Cases

- What happens when a prefix contains 63 characters (maximum allowed)?
- What happens when a prefix contains consecutive underscores (e.g., "my__type")?
- How does the system handle a TypeID with exactly the character '7' as the first suffix character (boundary of valid range)?
- What happens when parsing a TypeID with the wrong number of characters in the suffix?
- How does the system handle encoding/decoding at the exact moment of a timestamp boundary in UUIDv7?
- What happens when a prefix contains only a single character (discouraged but technically valid)?
- How does the system handle base32 characters that are not in the valid alphabet?
- What happens with TypeIDs that have the maximum length (90 characters)?

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: Library MUST generate new TypeIDs with UUIDv7 timestamps and specified type prefixes
- **FR-002**: Library MUST support empty prefixes (generating prefix-less TypeIDs)
- **FR-003**: Library MUST encode UUIDs using Crockford's base32 alphabet (0-9, a-z excluding i, l, o, u)
- **FR-004**: Library MUST parse TypeID strings into prefix and UUID components
- **FR-005**: Library MUST validate type prefixes match regex: `^([a-z]([a-z_]{0,61}[a-z])?)?$`
- **FR-006**: Library MUST reject prefixes longer than 63 characters
- **FR-007**: Library MUST reject prefixes containing uppercase letters, digits, or starting/ending with underscores
- **FR-008**: Library MUST accept prefixes with consecutive underscores (e.g., "my__type")
- **FR-009**: Library MUST encode UUID suffixes as exactly 26 base32 characters
- **FR-010**: Library MUST reject suffixes where the first character exceeds '7' (preventing 128-bit overflow)
- **FR-011**: Library MUST use lowercase base32 encoding exclusively (no uppercase)
- **FR-012**: Library MUST generate UUIDs with version bits 48-51 set to `0111` (UUIDv7)
- **FR-013**: Library MUST generate UUIDs with variant bits 64-65 set to `10` (RFC 4122 variant)
- **FR-014**: Library MUST support encoding user-provided UUID bytes (for custom UUID variants)
- **FR-015**: Library MUST validate TypeID total length is between 26 (no prefix) and 90 characters (max prefix + separator + suffix)
- **FR-016**: Library MUST provide clear error messages for all validation failures
- **FR-017**: Library MUST support round-trip conversion: generate → parse → UUID comparison must succeed
- **FR-018**: Library MUST handle base32 encoding using big-endian bit order with 2 zero bits prepended
- **FR-019**: Library MUST work identically on both JVM Clojure and ClojureScript platforms
- **FR-020**: Library MUST pass all test cases from the reference valid.yml and invalid.yml files

### Key Entities

- **TypeID**: A string composed of an optional type prefix, separator (if prefix present), and 26-character base32-encoded UUID suffix. Represents a type-safe identifier with embedded timestamp (UUIDv7).

- **Type Prefix**: A 0-63 character lowercase string matching `[a-z]([a-z_]{0,61}[a-z])?` or empty string. Denotes the type/category of the identifier (e.g., "user", "order", "session").

- **UUID Suffix**: A 128-bit UUID encoded as 26 base32 characters using Crockford's alphabet. When generated, contains a UUIDv7 with timestamp in the first 48 bits.

- **Base32 Encoding**: Custom encoding of 130 bits (2 zero bits + 128 UUID bits) into 26 characters using alphabet `0123456789abcdefghjkmnpqrstvwxyz`, processed in 5-bit groups.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Developers can generate a new TypeID and parse it back in under 2 microseconds on typical hardware
- **SC-002**: Library correctly encodes and decodes 100% of test cases from the reference valid.yml file (bidirectional conversion)
- **SC-003**: Library correctly rejects 100% of invalid TypeID strings from the reference invalid.yml file with appropriate error messages
- **SC-004**: All library functions produce identical results on JVM Clojure 1.11+ and ClojureScript
- **SC-005**: Documentation includes working examples for all three priority user stories (generate, validate, convert)
- **SC-006**: Every public library function has a docstring with parameter descriptions and at least one usage example
- **SC-007**: Library passes 100% of property-based tests verifying round-trip encoding (generate → parse → compare succeeds)
- **SC-008**: All validation failures include error messages that specify what rule was violated (prefix format, suffix overflow, length, etc.)
- **SC-009**: Library has zero warnings or errors from clj-kondo static analysis
- **SC-010**: Benchmarks confirm encoding performance is under 1 microsecond per operation and decoding is under 1 microsecond per operation

## Assumptions

- **A-001**: The library will be used primarily for generating new identifiers, with parsing/validation as secondary use cases
- **A-002**: Applications will use TypeIDs as opaque strings in most cases, only parsing when needed
- **A-003**: Performance targets are based on typical development hardware (modern multi-core CPU)
- **A-004**: The reference test files (valid.yml, invalid.yml) are available for validation during implementation
- **A-005**: Users of the library have basic familiarity with UUIDs and understand the concept of identifier prefixes
- **A-006**: The library will be distributed via Clojars following standard Clojure library conventions
- **A-007**: ClojureScript support assumes a JavaScript runtime with standard date/time capabilities for timestamp generation
- **A-008**: Error messages can be in English only (no internationalization required initially)

## Constraints

- **C-001**: Must comply with TypeID specification version 0.3.0 exactly
- **C-002**: Base32 encoding must match reference implementation bit-for-bit
- **C-003**: Cannot use standard Java base32 libraries (wrong encoding scheme)
- **C-004**: Must maintain compatibility across JVM and JavaScript platforms (no platform-specific APIs in public interface)
- **C-005**: Must use only pure functions for encoding/decoding (no mutable state)
- **C-006**: UUIDv7 timestamp generation must use monotonic time when available to prevent duplicate IDs in rapid succession

## Dependencies & Integration

- **D-001**: Library depends on standard UUID generation capabilities of the platform (java.util.UUID on JVM, crypto APIs in JavaScript)
- **D-002**: Testing requires access to valid.yml and invalid.yml reference test files
- **D-003**: Property-based testing requires test.check library (dev dependency only)
- **D-004**: Validation may optionally integrate with Malli or clojure.spec for schema validation at public API boundaries
- **D-005**: No runtime dependencies beyond core platform capabilities (JVM or JavaScript)
