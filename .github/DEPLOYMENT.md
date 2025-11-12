# Deployment Guide

This document explains how to deploy the library to Clojars using GitHub Actions.

## Prerequisites

Before deploying from CI, you need to set up the following GitHub secrets.

### Required Secrets

Go to your GitHub repository settings → Secrets and variables → Actions → New repository secret

#### 1. CLOJARS_USERNAME
- **Value**: Your Clojars username (e.g., `jxonas`)
- **Description**: Username for authenticating with Clojars

#### 2. CLOJARS_PASSWORD
- **Value**: Your Clojars deploy token
- **How to get it**:
  1. Log in to https://clojars.org
  2. Go to https://clojars.org/tokens
  3. Click "Generate token"
  4. Copy the token (starts with `CLOJARS_`)
  5. Use this token as the password (NOT your account password)

**Note**: GPG signing is disabled in CI deployments to avoid passphrase management issues. Artifacts are still secured via HTTPS and Clojars authentication. If you want to sign releases locally, you can do so manually.

## Deployment Workflows

### 1. SNAPSHOT Deployments (Automatic)

**Trigger**: Push to `main` or `master` branch

**What happens**:
- Runs tests
- Builds JAR with version `0.1.0-SNAPSHOT`
- Deploys to Clojars automatically

**Skip deployment**: Add `[skip ci]` to your commit message

**Manual trigger**: Go to Actions → Deploy SNAPSHOT → Run workflow

### 2. Release Deployments (Tag-based)

**Trigger**: Push a version tag (e.g., `v0.1.0`)

**What happens**:
- Extracts version from tag (removes `v` prefix)
- Builds JAR with that version
- Deploys to Clojars with GPG signature

**How to create a release**:
```bash
# Make sure your working directory is clean
git status

# Create and push a version tag
git tag -a v0.1.0 -m "Release version 0.1.0"
git push origin v0.1.0
```

**Version format**:
- Tag: `v0.1.0` → Deployed as: `0.1.0`
- Tag: `v1.2.3` → Deployed as: `1.2.3`
- Tag: `v0.2.0-beta1` → Deployed as: `0.2.0-beta1`

## Verifying Deployment

After deployment completes:

1. **Check GitHub Actions**:
   - Go to Actions tab in your repository
   - Find the workflow run
   - Verify it completed successfully

2. **Check Clojars**:
   - Visit https://clojars.org/io.github.unisoma/typeid
   - Verify the new version appears

3. **Test the deployed artifact**:
   ```bash
   # Create a test project
   mkdir test-typeid && cd test-typeid

   # Add dependency
   echo '{:deps {io.github.unisoma/typeid {:mvn/version "0.1.0"}}}' > deps.edn

   # Test it loads
   clojure -M -e "(require '[typeid.core :as typeid]) (println (typeid/create \"test\"))"
   ```

## Troubleshooting

### "authentication failed for https://clojars.org/repo"
- Verify `CLOJARS_USERNAME` and `CLOJARS_PASSWORD` secrets are set correctly
- Ensure you're using a deploy token, not your account password
- Check the token hasn't expired

### Build fails in CI
- Check that all tests pass locally: `bb test`
- Verify deps.edn is valid
- Check GitHub Actions logs for specific error messages

### SNAPSHOT not updating
- SNAPSHOT versions can take a few minutes to propagate
- Clear your local Maven cache: `rm -rf ~/.m2/repository/io/github/unisoma/typeid`
- Force update in deps.edn: `clojure -Sforce -M -e "(println \"Updated\")"`

## Local Deployment (for testing)

To deploy from your local machine:

```bash
# Set credentials
export CLOJARS_USERNAME="your-username"
export CLOJARS_PASSWORD="your-deploy-token"

# For SNAPSHOT
clojure -T:build deploy

# For release version
export RELEASE_VERSION="0.1.0"
clojure -T:build deploy
```

## Release Checklist

Before creating a release tag:

- [ ] All tests pass: `bb test`
- [ ] Linting passes: `bb lint`
- [ ] CHANGELOG.md is updated
- [ ] Version in README.md examples is updated
- [ ] Documentation is up to date
- [ ] Build succeeds: `bb build`

Then create the release:
```bash
git tag -a v0.1.0 -m "Release 0.1.0"
git push origin v0.1.0
```

## Security Best Practices

1. **Never commit secrets to the repository**
2. **Rotate deploy tokens periodically** (every 6-12 months)
3. **Use deploy tokens, not account passwords**
4. **Set GPG key expiration dates** and renew before expiry
5. **Review deployment logs** to ensure no secrets are leaked
6. **Limit repository access** to trusted contributors

## Additional Resources

- [Clojars Deploy Tokens](https://github.com/clojars/clojars-web/wiki/Deploy-Tokens)
- [GPG Key Management](https://docs.github.com/en/authentication/managing-commit-signature-verification)
- [GitHub Actions Secrets](https://docs.github.com/en/actions/security-guides/using-secrets-in-github-actions)
