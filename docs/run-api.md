# Run and process API

[简体中文](run-api.zh-Hans.md) · [Home](../README.md)

## Policy builders

`NonoRunOptions.Builder` follows nono 0.69.0 option families:

- profile/config: `profile`, `extendProfile`, `config`
- filesystem: `allow`, `read`, `write`, file and Unix-socket variants,
  `allowCurrentDirectory`, `workdir`, `bypassProtection`,
  `suppressSavePrompt`
- network: `blockNetwork`, `allowNetwork`, profile/domain/port/proxy/CA methods
- credentials and commands: credential, endpoint, environment credential,
  `allowCommand`, `blockCommand`
- platform/resources: launch services, GPU, HTTP/2, Linux sandbox policy,
  memory and process limits
- lifecycle: dry-run, detach, timeout, rollback, diagnostics, audit, session,
  capability elevation, verbosity

The builder rejects known conflicting combinations before launching nono.
Policy still belongs to the caller; the library does not infer permissions.

## Process I/O

Attach `NonoExecutionOptions` to either request type:

```kotlin
val execution = NonoExecutionOptions.builder()
    .workingDirectory(Path.of("/work"))
    .environment("MODE", "test")
    .input(NonoInput.bytes("input".toByteArray()))
    .stdout(NonoOutput.capture(1_048_576))
    .stderr(NonoOutput.stream({ bytes -> consume(bytes) }, 1_048_576))
    .timeout(Duration.ofSeconds(30))
    .build()
```

Input can be closed, inherited, or supplied as bytes. Output can be captured,
streamed while retaining a bounded tail, inherited, or discarded. Result flags
report timeout, cancellation, and output truncation.

## Async and cancellation

```kotlin
val execution = client.start(request)
execution.result().thenAccept { result -> println(result.exitCode) }

// Later:
execution.cancel()
```

Cancellation terminates the started process tree. `close()` shuts down only
client-owned executors.

## Generic commands

Use `NonoCommandRequest` for forward-compatible nono subcommands not modeled by
the typed run API:

```kotlin
val result = client.execute(
    NonoCommandRequest.builder("why", "--json", "--path", "/work").build(),
)
```

Arguments are passed exactly and never interpreted by a shell.
