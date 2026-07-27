# Releasing

Releases are published from signed `vX.Y.Z` tags after CI succeeds on Linux and
macOS with the pinned nono version.

## Repository secrets

Configure Central Portal credentials and an ASCII-armored OpenPGP signing key:

- `MAVEN_CENTRAL_USERNAME`
- `MAVEN_CENTRAL_PASSWORD`
- `SIGNING_IN_MEMORY_KEY`
- `SIGNING_IN_MEMORY_KEY_PASSWORD`

The Central Portal namespace `com.elonzh.nono` must be verified before the first
release.

## Local validation

```bash
task format
task check
task publish:local
task integration
```

`task publish:local` publishes into `build/local-maven`, verifies the POM,
module metadata, main, sources, and Javadoc jars, then compiles an isolated Java
consumer from those artifacts.

## Release

1. Set `VERSION_NAME` in `gradle.properties` and update `CHANGELOG.md`.
2. Commit the release state after the complete local validation.
3. Create a signed annotated tag: `git tag -s vX.Y.Z`.
4. Push the branch and tag.
5. The release workflow verifies the tag/version match and both host
   integrations, publishes signed artifacts to Maven Central, then creates the
   GitHub Release.

Do not reuse or move a published version tag.
