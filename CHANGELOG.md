# Changelog

All notable changes are documented here. The project follows
[Semantic Versioning](https://semver.org/).

## 0.3.0 - 2026-11-12

- Preserve package identity for profiles listed by their fully qualified names.
- Add typed options for Linux namespace isolation and strict checks for writable
  directories on the host PATH.

## 0.2.0 - 2026-08-09

- Track the current pre-1.0 nono development baseline and install the latest
  official release in CI so upstream incompatibilities surface early.
- Add typed `silent()` support for `nono run --silent`.
- Extract diagnostics JSON even when nono appends human-readable guidance after
  the report.
- Reject ambiguous diagnostic candidates when stderr capture is truncated, and
  validate report shape and exit code before removing JSON from child stderr.

## 0.1.0 - 2026-07-27

- Add Kotlin and Java APIs for `nono run`.
- Add exact-argv generic command execution.
- Add synchronous, asynchronous, cancellable, timed, and bounded-I/O process
  handling.
- Add profile and diagnostics JSON models.
- Add Linux and macOS compatibility checks, examples, and bilingual docs.
- Verify the public API against the official nono 0.69.0 CLI release.
