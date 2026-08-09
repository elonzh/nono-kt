# Getting started

## Requirements

- Java 17 or later
- Kotlin 2.2.x when consuming from Kotlin
- the nono CLI used by the current development baseline on Linux or macOS

Add the Maven Central dependency:

```kotlin
implementation("com.elonzh.nono:nono-kt:0.1.0")
```

## Create a client

`NonoClient.builder().build()` resolves `nono` from `PATH`, runs
`nono --version`, and rejects unsupported versions. An explicit executable and
environment overrides are available when needed:

```kotlin
val client = NonoClient.builder()
    .executable(Path.of("/opt/nono/bin/nono"))
    .environment("NONO_HOME", "/opt/nono")
    .build()
```

Use `verifyOnBuild(false)` only to delay the same verification until first use.
Close the client to release its worker threads. Executors passed with
`executors(io, scheduler)` remain caller-owned.

## Build a run request

The child command is always a list of arguments:

```kotlin
val request = NonoRunRequest.builder("java", "-jar", "app.jar")
    .options(
        NonoRunOptions.builder()
            .profile("locked")
            .read(Path.of("input"))
            .write(Path.of("output"))
            .blockNetwork()
            .memory("512M")
            .maxProcesses(32)
            .build(),
    )
    .build()
```

nono-kt produces `nono run ... -- java -jar app.jar` directly. Do not join
untrusted arguments into a command string.

## Read the result

```kotlin
client.use {
    val result = it.run(request)
    println("exit=${result.exitCode}")
    print(result.stdoutText())
    System.err.print(result.stderrText())
}
```

A child exit such as 1 or 127 is a normal `NonoExecutionResult`. Executable
discovery, version, launch, and JSON protocol failures use the
`NonoException` hierarchy.

Continue with [Run and process API](run-api.md) or
[Profiles and diagnostics](profiles-and-diagnostics.md).
