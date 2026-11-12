# Run and process API

## Policy builders

`NonoRunOptions.Builder` follows the current development baseline's option families:

- profile/config: `profile`, `extendProfile`, `config`
- filesystem: `allow`, `read`, `write`, file and Unix-socket variants,
  `allowCurrentDirectory`, `workdir`, `bypassProtection`,
  `suppressSavePrompt`, `strictBrokerPath`
- network: `blockNetwork`, `allowNetwork`, profile/domain/port/proxy/CA methods
- credentials and commands: credential, endpoint, environment credential,
  `allowCommand`, `blockCommand`
- platform/resources: launch services, GPU, HTTP/2, Linux sandbox policy,
  Linux namespace isolation, memory and process limits (Linux cgroup v2)
- lifecycle: dry-run, detach, timeout, rollback, silent mode, diagnostics,
  audit, session, capability elevation, verbosity

The builder rejects known conflicting combinations before launching nono.
Policy still belongs to the caller; the library does not infer permissions.

Use `strictBrokerPath()` to reject a run when the sandbox can write to a
directory on the host `PATH`.

On Linux, `isolateLinuxNamespaces(LinuxNamespace.NET, LinuxNamespace.IPC)`
adds namespaces to the profile's isolation policy. Network isolation requires
`blockNetwork()` or a proxied network policy. `LinuxNamespace.LOOPBACK`
requires network isolation and enables TCP on the namespace's own loopback.
nono checks the combined policy and the host's namespace support at launch.

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
