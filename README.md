# nono-kt

[简体中文](README.zh-Hans.md)

Kotlin and Java client for running commands through the
[`nono`](https://github.com/nolabs-ai/nono) CLI without shell parsing.

> Status: early preview. See the [compatibility policy](docs/compatibility.md)
> before choosing a nono release.

## Installation

```kotlin
dependencies {
    implementation("com.elonzh.nono:nono-kt:0.1.0")
}
```

Install `nono` separately and make it available on `PATH`, or pass its path to
`NonoClient.builder().executable(path)`.

## Kotlin

```kotlin
NonoClient.builder().build().use { client ->
    val result = client.run(
        nonoRun(listOf("printf", "hello\n")) {
            allowCurrentDirectory()
            blockNetwork()
        },
    )
    print(result.stdoutText())
}
```

## Java

```java
NonoRunOptions options =
        NonoRunOptions.builder().allowCurrentDirectory().blockNetwork().build();
NonoRunRequest request =
        NonoRunRequest.builder("printf", "hello\n").options(options).build();

try (NonoClient client = NonoClient.builder().build()) {
    NonoExecutionResult result = client.run(request);
    System.out.print(result.stdoutText());
}
```

`run` returns the child exit code as data, including non-zero exits. Launch,
compatibility, and protocol failures throw a `NonoException`. Use `start` for a
cancellable asynchronous execution.

The caller owns the sandbox policy. nono-kt maps typed options to `nono run`
arguments and executes argv directly; it does not add permissions or weaken a
requested policy.

## Documentation

- [Documentation website](https://elonzh.github.io/nono-kt/)
- [Getting started](docs/getting-started.md)
- [Run and process API](docs/run-api.md)
- [Profiles and diagnostics](docs/profiles-and-diagnostics.md)
- [Compatibility](docs/compatibility.md)
- [Kotlin example](examples/kotlin/src/main/kotlin/example/Main.kt)
- [Java example](examples/java/src/main/java/example/Main.java)

## License

Apache License 2.0.
