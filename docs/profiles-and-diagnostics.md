# Profiles and diagnostics

## Profiles

The client maps the JSON forms of nono's profile commands:

```kotlin
val profiles: List<NonoProfileSummary> = client.listProfiles()
val profile: NonoProfile = client.showProfile("locked")
val validation: NonoProfileValidation =
    client.validateProfile("/work/profile.json", strict = true)
```

Profile models expose stable fields and retain `rawJson` so callers can inspect
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
            .diagnosticsJson()
            .build(),
    )
    .build()

val result = client.run(request)
result.diagnostics?.diagnostics?.forEach { diagnostic ->
    println("${diagnostic.code}: ${diagnostic.message}")
}
```

When `diagnosticsJson()` is requested, nono-kt extracts nono's trailing report
from stderr. `stderr` contains child stderr without the report, while
`rawStderr` preserves the original captured bytes. Unknown JSON fields are
retained in the report's `rawJson`. If a candidate report is malformed,
`diagnosticsProtocolError` is set and the child result remains available.

Diagnostics never change the child exit code.
