# Profiles and diagnostics

## Profiles

The client maps the JSON forms of nono's profile commands:

```kotlin
val profiles: List<NonoProfileSummary> = client.listProfiles()
val profile: NonoProfile = client.showProfile("default")
val validation: NonoProfileValidation =
    client.validateProfile("/work/profile.json", strict = true)
```

For package profiles, use the fully qualified name (for example,
`nolabs-ai/claude`) from `NonoProfileSummary.name` with `showProfile` or
`NonoRunOptions.Builder.profile`. The summary's `pack` identifies the owning
package.

Profile models retain `rawJson` so callers can inspect
new nono fields before nono-kt adds typed accessors. Validation exit code 2 is
represented by `strictDeprecationFailure`; other unexpected command failures
throw `NonoCommandFailedException`.

## Diagnostics

Request diagnostics on a run:

```kotlin
val request = NonoRunRequest.builder("curl", "https://example.com")
    .options(
        NonoRunOptions.builder()
            .blockNetwork()
            .silent()
            .noDiagnostics()
            .diagnosticsJson()
            .build(),
    )
    .build()

val result = client.run(request)
result.diagnostics?.diagnostics?.forEach { diagnostic ->
    println("${diagnostic.code}: ${diagnostic.message}")
}
```

When `diagnosticsJson()` is requested, nono-kt extracts nono's report from
stderr even if the CLI appends guidance after it. `stderr` contains all
remaining stderr without the report, while
`rawStderr` preserves the original captured bytes. Unknown JSON fields are
retained in the report's `rawJson`. If a candidate report is malformed,
or stderr capture is truncated, `diagnosticsProtocolError` is set and the
retained stderr remains intact.

Diagnostics never change the child exit code.
