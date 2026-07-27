# nono-kt Design

Status: approved for specification on 2026-07-27

## Goal

Create `elonzh/nono-kt`, a JVM library that lets Kotlin and Java applications
run commands through the nono CLI without shell parsing. The library will live
at `~/workspace/nono-kt`, use the package prefix `nono.elonzh.com`, and be
published to Maven Central and GitHub Releases.

## Scope

The first release will provide:

- a typed API for `nono run`;
- profile listing, inspection, and validation;
- executable discovery and version compatibility checks;
- a generic command API for forward-compatible access to other nono commands;
- Kotlin-first and Java-friendly entry points;
- captured, inherited, and streamed process I/O;
- timeout, cancellation, exit status, and structured diagnostics support.

Native bindings, a bundled nono binary, Kotlin Multiplatform targets, and a
second profile implementation are out of scope. nono remains the policy and
execution authority.

## Compatibility and Build

- Kotlin 2.2.21 and Java 17 bytecode.
- Supported hosts: macOS and Linux, matching nono.
- Minimum supported nono version: 0.69.0.
- Gradle Kotlin DSL with a version catalog and Gradle Wrapper.
- Go Task as the repository command interface.
- One published library artifact: `com.elonzh.nono:nono-kt`.
- Public Kotlin and Java packages begin with `nono.elonzh.com`.

The executable is resolved from an explicit path first and then from `PATH`.
Construction of a strict client verifies `nono --version`; callers may request
deferred verification when startup latency matters.

## Public API

`NonoClient` is the main immutable, thread-safe entry point. Kotlin callers get
factory functions and DSL builders. Java callers get conventional builders,
getters, overloads, and `CompletionStage` asynchronous methods.

The main public models are:

- `NonoClient` and `NonoClient.Builder`;
- `NonoRunRequest`, containing the child argv and typed nono run options;
- `NonoCommandRequest`, the explicit generic-command escape hatch;
- `NonoExecutionResult`, containing the exit code, stdout, stderr, duration,
  timeout state, and optional diagnostics;
- `NonoProfileSummary`, `NonoProfile`, and `NonoProfileValidation`;
- `NonoVersion` and `NonoCompatibility`;
- `NonoException` subtypes for executable, version, launch, I/O, and protocol
  failures.

Child non-zero exit codes are normal results. Failures that prevent nono from
starting or make the requested operation unknowable are exceptions.

## Run Model

The client builds an argv list and launches it with `ProcessBuilder`. It never
constructs or invokes a shell command. The child command always follows nono's
`--` separator.

Typed run options cover the nono 0.69.0 `run` surface: profile/config
selection, filesystem grants, work directory, network restrictions and proxy
rules, credentials, resource limits, rollback, audit, diagnostic, detached,
and startup options. Mutually exclusive combinations are rejected before
launch.

The generic command API accepts nono arguments as a list, not as a command
string. It is deliberately named and documented as an escape hatch so new nono
features are usable before a typed API release.

I/O modes are selected independently for stdout and stderr:

- capture to bounded in-memory buffers;
- inherit the JVM process streams;
- stream chunks to callbacks while optionally retaining a bounded copy.

Input can be inherited, closed, supplied as bytes, or streamed. Timeout and
cancellation terminate the process tree and return the partial captured
output. Backpressure and bounded capture prevent unbounded memory growth.

When `--diagnostics-json` is requested, the library preserves raw stderr and
best-effort parses the trailing nono diagnostic object. A parsing failure does
not hide command output; it is reported as protocol metadata.

## Profiles

Profile operations call the CLI's JSON modes:

- `nono profile list --json`;
- `nono profile show <name-or-path> --json`;
- `nono profile validate <name-or-path> --json`, optionally with `--strict`.

Unknown JSON fields are retained or ignored safely so compatible nono additions
do not break existing clients. The library does not duplicate nono's profile
schema or policy evaluation.

## Safety

- No shell parsing or interpolation.
- No implicit filesystem, network, credential, or current-directory grants.
- The caller supplies the sandbox policy; the wrapper never widens it.
- Argument order and path values are preserved exactly.
- Secrets and child output are never logged by the library.
- Diagnostic parsing is additive and cannot alter the real exit status.
- Compatibility checks fail clearly instead of silently dropping unsupported
  options.

## Testing

Unit tests will exhaustively verify argv generation, validation, version
parsing, JSON decoding, bounded output, timeout, cancellation, and error
mapping. A deterministic fake nono executable will cover process behavior and
Java interoperability.

Integration tests will run against nono 0.69.0 or newer on macOS and Linux and
verify version detection, profile JSON operations, successful sandboxed
execution, child exit-code propagation, stdout/stderr handling, and timeout
termination.

CI will run formatting, static checks, unit tests, Java consumer tests,
documentation generation, publication validation, and host integration tests.

## Documentation and Release

The repository will include synchronized English and Simplified Chinese
README files, API documentation, Kotlin and Java quick starts, profile and
diagnostics examples, compatibility policy, contributing guidance, security
notes, changelog, and release instructions.

Releases use signed semantic-version tags. GitHub Actions builds and verifies
the release, publishes `com.elonzh.nono:nono-kt` to Maven Central, and creates a
GitHub Release from the same tag. Secrets and signing material remain outside
the repository.

## Acceptance Criteria

The work is complete when:

1. `~/workspace/nono-kt` and `github.com/elonzh/nono-kt` exist and match.
2. Kotlin and Java examples run a child command through an installed nono CLI.
3. The typed `run`, profile, version, generic command, I/O, timeout,
   cancellation, and diagnostic APIs are tested.
4. macOS and Linux CI pass against the supported nono baseline.
5. English and Chinese documentation are synchronized and usable.
6. A signed release is published to both Maven Central and GitHub Releases.
