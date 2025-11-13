---
description: Create a new release (update versions, changelog, tag, and optionally deploy)
---

When the user invokes this command, guide them through creating a new release by following these steps:

## Step 1: Determine Current Version

1. Read `CHANGELOG.md` to find the current released version (look for the most recent `## [X.Y.Z]` heading)
2. Display the current version to the user
3. Check if there are unreleased changes in the `## [Unreleased]` section
4. If no unreleased changes, inform the user and ask if they still want to proceed

## Step 2: Ask for New Version

Based on semantic versioning, suggest the next version:
- **Patch** (X.Y.Z → X.Y.Z+1): Bug fixes, documentation, minor changes
- **Minor** (X.Y.Z → X.Y+1.0): New features, backward compatible
- **Major** (X.Y.Z → X+1.0.0): Breaking changes

Use the AskUserQuestion tool to ask which version type:
- Question: "What type of release is this?"
- Options:
  - Patch (e.g., 0.1.0 → 0.1.1) - Bug fixes, docs, minor changes
  - Minor (e.g., 0.1.0 → 0.2.0) - New features, backward compatible
  - Major (e.g., 0.1.0 → 1.0.0) - Breaking changes
  - Custom - Let me specify the version manually

If Custom is selected, ask for the specific version number.

Calculate the new version based on the selection.

## Step 3: Update README.md

Update all version references in `README.md`:

1. Read the file
2. Find installation examples under "Installation" or "Quick Start":
   - Clojure CLI (deps.edn): `{:deps {io.github.unisoma/typeid {:mvn/version "X.Y.Z"}}}`
   - Leiningen: `[io.github.unisoma/typeid "X.Y.Z"]`
3. Replace the old version with the new version
4. Confirm the changes with the user before proceeding

## Step 4: Update bb.edn

Update the version display in the `info` task:

1. Read `bb.edn`
2. Find the `info` task
3. Look for the version line (e.g., `(println "Version:      X.Y.Z")`)
4. Replace with the new version
5. Confirm the changes with the user before proceeding

## Step 5: Update CHANGELOG.md

Update the changelog:

1. Read `CHANGELOG.md`
2. Replace the `## [Unreleased]` section heading with the new version and today's date:
   - Format: `## [X.Y.Z] - YYYY-MM-DD`
3. Add a new empty `## [Unreleased]` section at the top (after the header)
4. Show the user what will be changed
5. Confirm before proceeding

## Step 6: Commit Version Bump

Create a commit with the version changes:

1. Stage the modified files: `git add README.md bb.edn CHANGELOG.md`
2. Create commit message:
   ```
   chore(release): Bump version to X.Y.Z

   - Update version in README.md installation examples
   - Update version in bb.edn info task
   - Update CHANGELOG.md with release date
   ```
3. Show the commit message to the user
4. Commit the changes

## Step 7: Create Git Tag

Create a git tag for the release:

1. Create an annotated tag: `git tag -a vX.Y.Z -m "Release vX.Y.Z"`
2. Show the tag to the user
3. Inform them they can push with: `git push && git push --tags`

## Step 8: Final Summary

Display a summary:

```
✅ Release vX.Y.Z created successfully!

Updated files:
  - README.md
  - bb.edn
  - CHANGELOG.md

Git:
  - Committed version bump
  - Created tag vX.Y.Z

Next steps:
  - Push to GitHub: git push && git push --tags
```

## Important Notes

- **Always confirm with the user** before making irreversible changes (commits, tags)
- **Check git status** before starting to ensure working directory is clean
- **Do not push automatically** - let the user decide when to push
- **Build uses RELEASE_VERSION env var** - the version in build.clj is just a default
- **For pre-1.0 versions**: Remind user that breaking changes can happen in minor versions

## Error Handling

- If working directory is not clean, warn the user and ask if they want to proceed
- If git tag already exists, warn and ask if they want to delete and recreate
- If build or deploy fails, show the error and stop the process
- If files don't contain expected version patterns, inform the user and ask for manual verification
