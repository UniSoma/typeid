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

3. **Prepare commit message**:
   - Review the changes one more time
   - Draft a clear, concise commit message
   - **STOP - Do not commit yet**

4. **Request user approval**:
   - Present the proposed commit message to the user
   - Ask if they want to proceed with the commit
   - Wait for explicit confirmation before running `git commit`

## Important Notes

- **NEVER commit automatically** - Always ask the user for explicit approval before running `git commit`
- **Do not push automatically** - Let the user decide when to push
- **Use your judgment** to determine if documentation updates are needed. When in doubt, ask the user.
- **Confirmation required**: Even if the command says "commit", you must ask first
