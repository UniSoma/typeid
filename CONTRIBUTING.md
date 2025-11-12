# Contributing to TypeID Clojure

Thank you for your interest in contributing to TypeID Clojure! This document provides guidelines and instructions for contributing to the project.

## Table of Contents

- [Code of Conduct](#code-of-conduct)
- [Getting Started](#getting-started)
- [Development Setup](#development-setup)
- [Project Structure](#project-structure)
- [Development Workflow](#development-workflow)
- [Code Quality Standards](#code-quality-standards)
- [Testing Requirements](#testing-requirements)
- [Pull Request Process](#pull-request-process)
- [Release Process](#release-process)

## Code of Conduct

This project adheres to a code of conduct that promotes respect, inclusivity, and constructive collaboration. By participating, you agree to uphold these standards.

## Getting Started

### Prerequisites

- **JDK 11+** (recommended: JDK 17 or 21)
- **Clojure CLI** (version 1.11+)
- **Node.js 15+** (for ClojureScript tests)
- **Git**
- **Babashka** (optional but recommended) - [Install instructions](https://babashka.org/)

### Fork and Clone

1. Fork the repository on GitHub
2. Clone your fork locally:
   ```bash
   git clone https://github.com/YOUR_USERNAME/typeid.git
   cd typeid
   ```
3. Add the upstream repository:
   ```bash
   git remote add upstream https://github.com/UniSoma/typeid.git
   ```

## Development Setup

### Install Dependencies

```bash
# Quick setup with Babashka (recommended)
bb setup

# Or manually:
npm install                              # Install Node.js dependencies
clojure -M:test -m kaocha.runner         # Verify setup by running tests
```

### Start a REPL

```bash
# Start nREPL server
clojure -M:nrepl

# Or start with dev utilities loaded
clojure -M:dev
```

The dev REPL automatically loads the `user` namespace with helpful utilities:
- `(examples)` - Show usage examples
- `(test-all)` - Run all tests
- `(bench)` - Run benchmarks
- `(reload)` - Reload changed namespaces

## Project Structure

```
typeid/
├── src/typeid/           # Source code (cross-platform .cljc files)
│   ├── core.cljc         # Public API
│   ├── validation.cljc   # Validation predicates
│   └── impl/             # Internal implementation
│       ├── base32.cljc   # Base32 encoding/decoding
│       ├── uuid.cljc     # UUIDv7 generation
│       └── util.cljc     # Utilities
├── test/typeid/          # Test files
│   ├── *_test.cljc       # Unit tests
│   └── compliance_test.cljc  # Spec compliance tests
├── dev/                  # Development utilities
│   ├── user.clj          # REPL utilities
│   └── benchmarks/       # Performance benchmarks
├── specs/                # Design documents
└── .github/workflows/    # CI/CD configuration
```

## Development Workflow

### 1. Create a Feature Branch

```bash
git checkout -b feature/your-feature-name
```

Use descriptive branch names:
- `feature/` - New features
- `fix/` - Bug fixes
- `docs/` - Documentation changes
- `refactor/` - Code refactoring
- `perf/` - Performance improvements

### 2. Make Changes

- Write code following the [Code Quality Standards](#code-quality-standards)
- Add tests for new functionality
- Update documentation as needed
- Run tests frequently: `bb test` (or `clojure -M:test -m kaocha.runner`)

### 3. Commit Changes

Write clear, descriptive commit messages:

```bash
git commit -m "feat: Add support for custom prefix validation"
```

Commit message format:
- `feat:` - New feature
- `fix:` - Bug fix
- `docs:` - Documentation changes
- `test:` - Test additions or modifications
- `refactor:` - Code refactoring
- `perf:` - Performance improvements
- `chore:` - Build process or tooling changes

### 4. Keep Your Branch Updated

```bash
git fetch upstream
git rebase upstream/main
```

### 5. Push and Create Pull Request

```bash
git push origin feature/your-feature-name
```

Then create a pull request on GitHub.

## Code Quality Standards

### Clojure Style

- Follow standard Clojure idioms and style conventions
- Use `cljfmt` for consistent formatting: `clojure -M:dev -m cljfmt.main fix src test`
- Keep functions small and focused (single responsibility)
- Use descriptive names for functions and variables
- Add docstrings to all public functions
- Include usage examples in docstrings

### Code Organization

- **Public API**: All public functions in `typeid.core` namespace
- **Internal implementation**: All helper functions in `typeid.impl` namespace
- **Validation**: All validation predicates in `typeid.validation` namespace
- **Cross-platform**: Use `.cljc` files with reader conditionals for platform-specific code

### Performance

- Use type hints to eliminate reflection warnings
- Add `(set! *warn-on-reflection* true)` at the top of performance-critical namespaces
- Run benchmarks to verify performance targets are met
- Avoid unnecessary allocations in hot paths

### Documentation

- Add docstrings to all public functions
- Include usage examples in docstrings
- Update README.md for user-facing changes
- Add CHANGELOG.md entry for all changes

## Testing Requirements

### Test Coverage

- **Minimum overall coverage**: 80%
- **Critical path coverage**: 100% (base32, UUID generation, validation)
- All public API functions must have tests
- All error cases must be tested

### Running Tests

**With Babashka (recommended):**

```bash
bb test              # Run all JVM tests
bb test:cljs         # Run ClojureScript tests
bb test:coverage     # Run with coverage report
bb test:watch        # Watch mode (run tests on file changes)
bb test:all          # Run both JVM and ClojureScript tests
```

**Without Babashka:**

```bash
clojure -M:test -m kaocha.runner                                      # JVM tests
clojure -M:test:cljs -m kaocha.runner --config-file tests.cljs.edn    # ClojureScript tests
clojure -M:test:coverage                                              # With coverage
clojure -M:test -m kaocha.runner --watch                              # Watch mode
```

### Writing Tests

- Use descriptive test names: `(deftest generate-with-valid-prefix-test ...)`
- Test both success and failure cases
- Include edge cases and boundary conditions
- Use property-based testing (test.check) for round-trip properties
- Follow the existing test structure and conventions

Example test:

```clojure
(deftest generate-with-valid-prefix-test
  (testing "Generate TypeID with valid prefix"
    (let [result (typeid/generate "user")]
      (is (contains? result :ok))
      (is (string? (:ok result)))
      (is (str/starts-with? (:ok result) "user_")))))
```

### Compliance Tests

All changes must pass the official TypeID specification compliance tests:
- `test/resources/valid.yml` - Valid TypeID test cases
- `test/resources/invalid.yml` - Invalid TypeID test cases

## Linting and Formatting

### clj-kondo

Run linting before committing:

```bash
bb lint              # With Babashka
clojure -M:lint      # Without Babashka
```

**Zero-tolerance policy**: No warnings or errors allowed. Fix all issues before submitting.

### cljfmt

Format code before committing:

```bash
bb format            # With Babashka
clojure -M:dev -m cljfmt.main fix src test dev    # Without Babashka
```

### All Quality Checks

```bash
bb quality           # Run lint + format check
bb ci:check          # Full CI suite (tests + quality)
```

## Pull Request Process

### Before Submitting

1. ✅ All tests pass (JVM and ClojureScript)
2. ✅ Code coverage meets requirements (80% overall)
3. ✅ clj-kondo shows zero warnings/errors
4. ✅ Code is formatted with cljfmt
5. ✅ Documentation is updated
6. ✅ CHANGELOG.md entry added

### PR Checklist

```markdown
- [ ] Tests pass on JVM (Clojure 1.11 and 1.12)
- [ ] Tests pass on ClojureScript (Node.js)
- [ ] Code coverage ≥80%
- [ ] clj-kondo clean (zero warnings/errors)
- [ ] Code formatted with cljfmt
- [ ] All public functions have docstrings
- [ ] README.md updated (if user-facing changes)
- [ ] CHANGELOG.md entry added
- [ ] Compliance tests pass (valid.yml and invalid.yml)
```

### Review Process

1. Create pull request with clear description of changes
2. Link related issues (if any)
3. Wait for CI checks to pass
4. Address review feedback
5. Maintainer will merge when approved

## Benchmarks

### Running Benchmarks

```bash
bb bench                                  # With Babashka
clojure -M:dev -m benchmarks.core-bench   # Without Babashka
```

### Performance Targets

- `generate`: < 2μs total
- `base32/encode`: < 1μs
- `base32/decode`: < 1μs
- `prefix validation`: < 500ns

If your changes affect performance, include benchmark results in your PR description.

## Release Process

Releases are managed by project maintainers. The process is:

1. Update version in `build.clj`
2. Update `CHANGELOG.md` with release notes
3. Create git tag: `git tag v0.1.0`
4. Push tag: `git push upstream v0.1.0`
5. GitHub Actions automatically deploys to Clojars

## Getting Help

- **Issues**: Report bugs or request features via [GitHub Issues](https://github.com/UniSoma/typeid/issues)
- **Discussions**: Ask questions in [GitHub Discussions](https://github.com/UniSoma/typeid/discussions)
- **Documentation**: See [README.md](README.md) and [API docs](https://unisoma.github.io/typeid/)

## Recognition

Contributors will be:
- Listed in the project README
- Mentioned in release notes
- Credited in relevant documentation

Thank you for contributing to TypeID Clojure!
