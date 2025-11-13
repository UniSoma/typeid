---
description: Check if changes since last release justify a new version
---

# Release Justification Check

Analyze all changes since the last version tag and determine if a new release is justified.

## Instructions

Follow these steps:

1. **Find the latest version tag**:
   ```bash
   git describe --tags --abbrev=0
   ```

2. **List all commits since that tag**:
   ```bash
   git log <tag>..HEAD --format="%h %s%n%b" --no-merges
   ```

3. **Get change statistics**:
   ```bash
   git diff <tag>..HEAD --stat
   ```

4. **Analyze the changes** and categorize them:
   - **API changes**: Breaking changes, new public APIs, signature modifications
   - **Features**: New functionality, enhancements
   - **Bug fixes**: Corrections to existing functionality
   - **Performance**: Measurable improvements to speed/memory
   - **Documentation**: README, guides, docstrings (library code comments count as docs)
   - **Internal/Dev tooling**: Build scripts, CI, dev-only tools, internal refactoring
   - **Dependencies**: Updates to deps.edn or package.json
   - **Tests**: New or updated tests

5. **Make a recommendation** based on semantic versioning:

   ### Justifies a Release:
   - **MAJOR** (x.0.0): Breaking API changes, removed functionality
   - **MINOR** (0.x.0): New features, new public APIs, enhancements (backwards compatible)
   - **PATCH** (0.0.x): Bug fixes, security patches, performance improvements

   ### Does NOT Justify a Release:
   - Documentation-only changes (unless significant and user-requested)
   - Internal refactoring with no user-visible impact
   - Dev tooling, CI, or build script changes
   - Test additions/updates (unless fixing release-blocking bugs)

6. **Provide a clear summary**:
   ```
   ## Release Assessment: Changes Since v<VERSION>

   **Total commits:** X commits

   ### Change Summary
   [List commits with categorization]

   ### Analysis
   [Categorize changes with checkmarks]

   ### Recommendation: [JUSTIFIED / NOT JUSTIFIED]

   [Explanation with reasoning]

   ### Suggested Action
   [What to do next]
   ```

## Important Notes

- Be objective - documentation and tooling improvements are valuable but typically don't justify releases
- Consider user impact - would users benefit from upgrading?
- Think about maintenance overhead - each release requires changelog updates, testing, and announcement
- When in doubt, err on the side of waiting for more substantial changes
- Multiple small improvements may collectively justify a patch release
