# Changelog

All notable changes are documented here. The project follows
[Semantic Versioning](https://semver.org/).

## Unreleased

- Raise the default minimum and typed CLI baseline to nono 0.72.0 for the
  upcoming 0.2.x line.
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
