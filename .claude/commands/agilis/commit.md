---
description: Review staged changes, update changelog/readme if needed, and commit
---

# Review and Commit Changes

Follow these steps:

1. **Review staged changes**: Run `git diff --staged` to see what's being committed

2. **Update documentation if relevant**:
   - If changes affect user-facing features or APIs, update the README.md
   - If this is a notable change, add an entry to CHANGELOG.md under the appropriate version
   - Skip this step if changes are internal only (refactoring, tests, benchmarks, etc.)

3. **Create commit**: Follow the standard git commit workflow:
   - Review the changes one more time
   - Write a clear, concise commit message
   - Create the commit

## Important Notes

- **Always confirm with the user** before making irreversible changes (commits, tags)
- **Do not push automatically** - let the user decide when to push
- **Use your judgment** to determine if documentation updates are needed. When in doubt, ask the user.
