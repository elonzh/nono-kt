# nono-kt Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build, verify, document, and publish a Java 17 Kotlin/Java library that invokes the nono CLI safely without shell parsing.

**Architecture:** A single JVM artifact exposes an immutable `NonoClient`, typed `nono run` builders, profile JSON operations, version probing, and a generic argv escape hatch. An internal process engine uses `ProcessBuilder`, bounded stream pumps, and process-tree termination; protocol decoders preserve raw CLI output and parse compatible JSON additively.

**Tech Stack:** Kotlin 2.2.21, Java 17, Gradle Kotlin DSL, kotlinx.serialization 1.9.0, kotlin-test/JUnit 5, ktlint 14.0.1, Vanniktech Maven Publish 0.35.0, Go Task, GitHub Actions.

## Global Constraints

- Public packages begin exactly with `nono.elonzh.com`.
- Production bytecode targets Java 17.
- Supported hosts are macOS and Linux.
- Minimum supported nono CLI version is 0.69.0.
- The library never invokes a shell and never widens caller-provided sandbox policy.
- The nono executable is external and is never bundled.
- One artifact is published as `com.elonzh.nono:nono-kt`.
- Every behavior change follows a witnessed red-green-refactor test cycle.
- English and Simplified Chinese documentation stay synchronized.
- Secrets, Central Portal credentials, and signing keys remain outside the repository.

---

## File Map

### Build and repository

- `settings.gradle.kts`: repository name and dependency repositories.
- `build.gradle.kts`: JVM compilation, tests, ktlint, source/Javadoc jars, Maven Central metadata, and publication.
- `gradle/libs.versions.toml`: all plugin and library versions.
- `gradle/wrapper/*`, `gradlew`, `gradlew.bat`: reproducible Gradle runtime.
- `Taskfile.yml`: `format`, `check`, `integration`, `docs`, and publication validation entry points.
- `.github/workflows/ci.yml`: Linux/macOS validation.
- `.github/workflows/release.yml`: tag-driven Maven Central and GitHub release.

### Public API

- `src/main/kotlin/nono/elonzh/com/NonoClient.kt`: client construction, sync/async execution, profiles, and lifecycle.
- `src/main/kotlin/nono/elonzh/com/NonoVersion.kt`: semantic version parsing and compatibility comparison.
- `src/main/kotlin/nono/elonzh/com/NonoExecutable.kt`: resolved executable metadata.
- `src/main/kotlin/nono/elonzh/com/NonoRunOptions.kt`: typed nono 0.69.0 run flags and validation.
- `src/main/kotlin/nono/elonzh/com/NonoRequests.kt`: run and generic command requests plus builders.
- `src/main/kotlin/nono/elonzh/com/NonoExecution.kt`: cancellable asynchronous execution handle.
- `src/main/kotlin/nono/elonzh/com/NonoExecutionOptions.kt`: cwd, environment, input, output, timeout, and capture bounds.
- `src/main/kotlin/nono/elonzh/com/NonoExecutionResult.kt`: exit status, output, duration, truncation, timeout, cancellation, and diagnostics.
- `src/main/kotlin/nono/elonzh/com/NonoProfiles.kt`: profile summary, resolved profile, and validation models.
- `src/main/kotlin/nono/elonzh/com/NonoDiagnostics.kt`: stable diagnostic report projection and raw JSON.
- `src/main/kotlin/nono/elonzh/com/NonoException.kt`: executable, compatibility, launch, I/O, and protocol exceptions.
- `src/main/kotlin/nono/elonzh/com/NonoDsl.kt`: Kotlin convenience builders.

### Internals

- `src/main/kotlin/nono/elonzh/com/internal/ExecutableResolver.kt`: explicit-path/PATH discovery and `--version` probe.
- `src/main/kotlin/nono/elonzh/com/internal/RunArgumentRenderer.kt`: canonical argv emission.
- `src/main/kotlin/nono/elonzh/com/internal/ProcessEngine.kt`: process launch, stream pumping, timeout, and cancellation.
- `src/main/kotlin/nono/elonzh/com/internal/BoundedByteAccumulator.kt`: thread-safe bounded capture.
- `src/main/kotlin/nono/elonzh/com/internal/JsonProtocols.kt`: diagnostics and profile JSON decoding.

### Tests and examples

- `src/test/kotlin/nono/elonzh/com/NonoVersionTest.kt`: version parser and ordering.
- `src/test/kotlin/nono/elonzh/com/RunArgumentRendererTest.kt`: every typed flag and conflict.
- `src/test/kotlin/nono/elonzh/com/ProcessEngineTest.kt`: I/O, timeout, cancellation, truncation, and exit propagation.
- `src/test/kotlin/nono/elonzh/com/JsonProtocolsTest.kt`: diagnostics and profile compatibility.
- `src/test/kotlin/nono/elonzh/com/NonoClientTest.kt`: public API behavior through a fake executable.
- `src/test/java/nono/elonzh/com/JavaApiTest.java`: Java source compatibility.
- `src/test/kotlin/nono/elonzh/com/test/FakeNono.kt`: deterministic executable fixture.
- `src/integrationTest/kotlin/nono/elonzh/com/NonoIntegrationTest.kt`: installed nono smoke tests.
- `examples/kotlin/src/main/kotlin/example/Main.kt`: Kotlin CLI-wrapper example.
- `examples/java/src/main/java/example/Main.java`: Java CLI-wrapper example.

### Documentation

- `README.md`, `README.zh-Hans.md`: synchronized project overview and quick starts.
- `docs/getting-started.md`, `docs/getting-started.zh-Hans.md`: installation and executable discovery.
- `docs/run-api.md`, `docs/run-api.zh-Hans.md`: typed run API.
- `docs/profiles-and-diagnostics.md`, `docs/profiles-and-diagnostics.zh-Hans.md`: JSON operations and result semantics.
- `docs/compatibility.md`, `docs/compatibility.zh-Hans.md`: Java/nono/platform compatibility policy.
- `CONTRIBUTING.md`, `SECURITY.md`, `CHANGELOG.md`, `LICENSE`: project governance.
- `docs/releasing.md`: local and CI publication procedure.

---

### Task 1: Reproducible JVM Project and Version Compatibility

**Files:**
- Create: `settings.gradle.kts`
- Create: `build.gradle.kts`
- Create: `gradle/libs.versions.toml`
- Create: `Taskfile.yml`
- Create: `.gitignore`
- Create: `src/test/kotlin/nono/elonzh/com/NonoVersionTest.kt`
- Create: `src/main/kotlin/nono/elonzh/com/NonoVersion.kt`
- Create: `src/main/kotlin/nono/elonzh/com/NonoException.kt`

**Interfaces:**
- Produces: `NonoVersion.parse(String): NonoVersion`
- Produces: `NonoVersion.requireAtLeast(NonoVersion): Unit`
- Produces: `NonoCompatibilityException(required, actual)`

- [x] **Step 1: Add build configuration**

Use Kotlin 2.2.21, Java toolchain 17, explicit API mode, kotlinx-serialization-json 1.9.0, JUnit Platform, ktlint 14.0.1, and Maven Publish 0.35.0. Configure `VERSION_NAME` with fallback `0.1.0-SNAPSHOT`, group `com.elonzh.nono`, source/Javadoc jars, Apache-2.0 POM metadata, and `mavenCentral`/`signAllPublications`.

```kotlin
kotlin {
    explicitApi()
    compilerOptions.jvmTarget.set(JvmTarget.JVM_17)
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

tasks.test {
    useJUnitPlatform()
}
```

- [x] **Step 2: Generate and verify the Gradle Wrapper**

Run:

```bash
gradle wrapper --gradle-version 8.14.3
./gradlew help --no-daemon
```

Expected: both commands exit 0 and the wrapper reports Gradle 8.14.3.

- [x] **Step 3: Write failing version tests**

```kotlin
class NonoVersionTest {
    @Test
    fun `parses current nono output`() {
        assertEquals(NonoVersion(0, 69, 0), NonoVersion.parse("nono 0.69.0\n"))
    }

    @Test
    fun `orders prerelease before final release`() {
        assertTrue(NonoVersion.parse("0.69.0-rc.1") < NonoVersion.parse("0.69.0"))
    }

    @Test
    fun `rejects unrelated output`() {
        assertFailsWith<NonoProtocolException> { NonoVersion.parse("not-nono") }
    }
}
```

- [x] **Step 4: Run the tests and verify RED**

Run:

```bash
./gradlew test --tests nono.elonzh.com.NonoVersionTest --no-daemon
```

Expected: compilation fails because `NonoVersion` and the protocol exception do not exist.

- [x] **Step 5: Implement semantic version parsing**

```kotlin
public data class NonoVersion(
    public val major: Int,
    public val minor: Int,
    public val patch: Int,
    public val preRelease: String? = null,
) : Comparable<NonoVersion> {
    override fun compareTo(other: NonoVersion): Int =
        compareValuesBy(this, other, NonoVersion::major, NonoVersion::minor, NonoVersion::patch)
            .takeIf { it != 0 }
            ?: comparePreRelease(preRelease, other.preRelease)

    public companion object {
        @JvmStatic
        public fun parse(output: String): NonoVersion
    }
}
```

Parse an optional `nono ` prefix, three numeric components, and an optional SemVer prerelease. Reject trailing non-version text. Compare numeric prerelease identifiers numerically and other identifiers lexically.

- [x] **Step 6: Run checks and verify GREEN**

Run:

```bash
./gradlew test ktlintCheck --no-daemon
```

Expected: all tests pass and ktlint reports no violations.

- [ ] **Step 7: Commit**

```bash
git add settings.gradle.kts build.gradle.kts gradle Taskfile.yml .gitignore src
git commit -m "build: initialize nono-kt JVM library" \
  -m "Configure the Java 17 Kotlin build and publication metadata.

Add tested nono semantic-version parsing and compatibility errors."
```

---

### Task 2: Executable Discovery and Typed Run Arguments

**Files:**
- Create: `src/test/kotlin/nono/elonzh/com/ExecutableResolverTest.kt`
- Create: `src/test/kotlin/nono/elonzh/com/RunArgumentRendererTest.kt`
- Create: `src/main/kotlin/nono/elonzh/com/NonoExecutable.kt`
- Create: `src/main/kotlin/nono/elonzh/com/NonoRunOptions.kt`
- Create: `src/main/kotlin/nono/elonzh/com/NonoRequests.kt`
- Create: `src/main/kotlin/nono/elonzh/com/internal/ExecutableResolver.kt`
- Create: `src/main/kotlin/nono/elonzh/com/internal/RunArgumentRenderer.kt`

**Interfaces:**
- Consumes: `NonoVersion.parse`
- Produces: `NonoExecutable(path: Path, version: NonoVersion)`
- Produces: `NonoRunOptions.builder(): NonoRunOptions.Builder`
- Produces: `NonoRunRequest.builder(String, String...): NonoRunRequest.Builder`
- Produces: `NonoCommandRequest.builder(String...): NonoCommandRequest.Builder`
- Produces: `RunArgumentRenderer.render(executable, request): List<String>`

- [ ] **Step 1: Write failing executable-resolution tests**

Create temporary executable files and pass a controlled environment map. Assert:

```kotlin
assertEquals(explicit.toAbsolutePath(), resolver.resolve(explicit, mapOf("PATH" to pathDir.toString())))
assertEquals(pathDir.resolve("nono"), resolver.resolve(null, mapOf("PATH" to pathDir.toString())))
assertFailsWith<NonoExecutableNotFoundException> {
    resolver.resolve(null, mapOf("PATH" to emptyDir.toString()))
}
```

- [ ] **Step 2: Run resolver tests and verify RED**

Run:

```bash
./gradlew test --tests nono.elonzh.com.ExecutableResolverTest --no-daemon
```

Expected: compilation fails because `ExecutableResolver` is absent.

- [ ] **Step 3: Implement explicit-path and PATH discovery**

Resolve an explicit path before `PATH`, require a regular executable file, split `PATH` using `File.pathSeparator`, and return an absolute normalized path. Do not invoke a shell or consult aliases.

```kotlin
internal class ExecutableResolver(
    private val environment: Map<String, String> = System.getenv(),
) {
    fun resolve(explicit: Path?): Path
    fun probe(path: Path): NonoExecutable
}
```

Probe with `ProcessBuilder(path.toString(), "--version")`, require exit code 0, parse stdout using `NonoVersion.parse`, and preserve stderr in probe exceptions.

- [ ] **Step 4: Write failing argv mapping and conflict tests**

Use hand-derived expected arrays. The canonical path mapping table is:

```text
allow -> --allow
read -> --read
write -> --write
allowFile -> --allow-file
readFile -> --read-file
writeFile -> --write-file
allowUnixSocket -> --allow-unix-socket
allowUnixSocketBind -> --allow-unix-socket-bind
allowUnixSocketDir -> --allow-unix-socket-dir
allowUnixSocketDirBind -> --allow-unix-socket-dir-bind
allowUnixSocketSubtree -> --allow-unix-socket-subtree
allowUnixSocketSubtreeBind -> --allow-unix-socket-subtree-bind
bypassProtection -> --bypass-protection
suppressSavePrompt -> --suppress-save-prompt
```

The canonical network, credential, and resource mapping table is:

```text
blockNetwork -> --block-net
networkProfile -> --network-profile
allowDomain -> --allow-domain
denyDomain -> --deny-domain
listenPort -> --listen-port
openPort -> --open-port
allowConnectPort -> --allow-connect-port
upstreamProxy -> --upstream-proxy
upstreamBypass -> --upstream-bypass
proxyPort -> --proxy-port
trustProxyCa -> --trust-proxy-ca
proxyCaValidityDays -> --proxy-ca-validity
credential -> --credential
allowEndpoint -> --allow-endpoint
environmentCredential -> --env-credential
environmentCredentialMap -> --env-credential-map <ref> <name>
memory -> --memory
maxProcesses -> --max-processes
```

Assert a representative full request exactly:

```kotlin
assertEquals(
    listOf(
        "/opt/bin/nono", "run",
        "--profile", "locked",
        "--extends", "ci",
        "--allow", "/work",
        "--read-file", "/work/input.txt",
        "--block-net",
        "--memory", "512M",
        "--max-processes", "32",
        "--diagnostics-json",
        "--",
        "java", "-jar", "app.jar",
    ),
    rendered,
)
```

Add separate tests that reject: empty child argv, NUL-containing values, config plus sandbox flags, extends without profile, detach timeout without detached, rollback plus no-rollback, rollback-all plus rollback-include, rollback destination without rollback, no-audit plus audit/rollback, and audit signing with disabled audit integrity.

- [ ] **Step 5: Run renderer tests and verify RED**

Run:

```bash
./gradlew test --tests nono.elonzh.com.RunArgumentRendererTest --no-daemon
```

Expected: compilation fails because run builders and renderer are absent.

- [ ] **Step 6: Implement typed builders and canonical rendering**

`NonoRunOptions.Builder` exposes one method per canonical nono 0.69.0 run flag:

```kotlin
public fun profile(value: String): Builder
public fun extendProfile(value: String): Builder
public fun config(value: Path): Builder
public fun allow(value: Path): Builder
public fun read(value: Path): Builder
public fun write(value: Path): Builder
public fun allowFile(value: Path): Builder
public fun readFile(value: Path): Builder
public fun writeFile(value: Path): Builder
public fun allowUnixSocket(value: Path): Builder
public fun allowUnixSocketBind(value: Path): Builder
public fun allowUnixSocketDir(value: Path): Builder
public fun allowUnixSocketDirBind(value: Path): Builder
public fun allowUnixSocketSubtree(value: Path): Builder
public fun allowUnixSocketSubtreeBind(value: Path): Builder
public fun bypassProtection(value: Path): Builder
public fun suppressSavePrompt(value: Path): Builder
public fun allowCurrentDirectory(): Builder
public fun workdir(value: Path): Builder
public fun blockNetwork(): Builder
public fun networkProfile(value: String): Builder
public fun allowDomain(value: String): Builder
public fun denyDomain(value: String): Builder
public fun listenPort(value: Int): Builder
public fun openPort(value: Int): Builder
public fun allowConnectPort(value: Int): Builder
public fun upstreamProxy(value: String): Builder
public fun upstreamBypass(value: String): Builder
public fun proxyPort(value: Int): Builder
public fun trustProxyCa(): Builder
public fun proxyCaValidityDays(value: Int): Builder
public fun credential(value: String): Builder
public fun allowEndpoint(value: String): Builder
public fun environmentCredential(value: String): Builder
public fun environmentCredentialMap(reference: String, environmentVariable: String): Builder
public fun allowLaunchServices(): Builder
public fun allowGpu(): Builder
public fun allowHttp2(): Builder
public fun linuxSandboxPolicy(value: LinuxSandboxPolicy): Builder
public fun memory(value: String): Builder
public fun maxProcesses(value: Long): Builder
public fun dryRun(): Builder
public fun detached(): Builder
public fun detachTimeout(value: Duration): Builder
public fun rollback(): Builder
public fun noRollbackPrompt(): Builder
public fun noRollback(): Builder
public fun rollbackExclude(value: String): Builder
public fun rollbackInclude(value: String): Builder
public fun rollbackAll(): Builder
public fun skipDirectory(value: String): Builder
public fun rollbackDestination(value: Path): Builder
public fun noDiagnostics(): Builder
public fun diagnosticsJson(): Builder
public fun startupTimeout(value: Duration): Builder
public fun noAudit(): Builder
public fun noAuditIntegrity(): Builder
public fun auditIntegrity(): Builder
public fun auditSignKey(value: String): Builder
public fun trustOverride(): Builder
public fun sessionName(value: String): Builder
public fun capabilityElevation(): Builder
public fun verbose(level: Int): Builder
```

Also expose deprecated CLI flags only with `@Deprecated` Java-visible methods: `allowNetwork`, `allowCommand`, and `blockCommand`. Render only canonical flag spellings and preserve insertion order within each repeated option.

- [ ] **Step 7: Run targeted and full tests**

Run:

```bash
./gradlew test ktlintCheck --no-daemon
```

Expected: executable and renderer tests pass with no formatting violations.

- [ ] **Step 8: Commit**

```bash
git add src
git commit -m "feat: add executable and run command models" \
  -m "Resolve and probe external nono executables without shell parsing.

Cover the nono 0.69.0 run flags with validated Kotlin and Java builders."
```

---

### Task 3: Process I/O, Timeout, and Cancellation

**Files:**
- Create: `src/test/kotlin/nono/elonzh/com/ProcessEngineTest.kt`
- Create: `src/test/kotlin/nono/elonzh/com/test/FakeNono.kt`
- Create: `src/main/kotlin/nono/elonzh/com/NonoExecution.kt`
- Create: `src/main/kotlin/nono/elonzh/com/NonoExecutionOptions.kt`
- Create: `src/main/kotlin/nono/elonzh/com/NonoExecutionResult.kt`
- Create: `src/main/kotlin/nono/elonzh/com/internal/BoundedByteAccumulator.kt`
- Create: `src/main/kotlin/nono/elonzh/com/internal/ProcessEngine.kt`

**Interfaces:**
- Produces: `NonoExecution.result(): CompletionStage<NonoExecutionResult>`
- Produces: `NonoExecution.cancel(): Boolean`
- Produces: `NonoExecutionOptions.builder()`
- Produces: `ProcessEngine.start(argv, options, diagnosticsRequested): NonoExecution`

`NonoExecutionOptions.Builder` provides:

```kotlin
public fun workingDirectory(value: Path): Builder
public fun environment(name: String, value: String): Builder
public fun clearEnvironment(): Builder
public fun input(value: NonoInput): Builder
public fun stdout(value: NonoOutput): Builder
public fun stderr(value: NonoOutput): Builder
public fun timeout(value: Duration): Builder
public fun build(): NonoExecutionOptions
```

- [ ] **Step 1: Create the fake nono fixture**

`FakeNono` writes an executable POSIX script into a temporary directory. It implements:

```text
--version                       prints "nono 0.69.0"
run ... -- emit OUT ERR CODE    writes OUT/ERR and exits CODE
run ... -- stdin                copies stdin to stdout
run ... -- sleep SECONDS        sleeps, then prints "finished"
profile list --json             prints a fixed profile array
profile show NAME --json        prints a fixed resolved profile object
profile validate FILE --json    prints a fixed validation object
```

The fixture records each argv element on its own line in a file passed through `FAKE_NONO_ARGS_FILE`.

- [ ] **Step 2: Write failing process tests**

Cover observable behavior:

```kotlin
assertEquals("hello", result.stdoutText())
assertEquals("problem", result.stderrText())
assertEquals(7, result.exitCode)
assertFalse(result.timedOut)
assertFalse(result.cancelled)
```

Add independent tests for byte stdin, inherited/discarded output, callback streaming, per-stream truncation, working directory, environment replacement/addition, timeout returning partial output, explicit cancellation, and descendant termination.

- [ ] **Step 3: Run process tests and verify RED**

Run:

```bash
./gradlew test --tests nono.elonzh.com.ProcessEngineTest --no-daemon
```

Expected: compilation fails because execution types and engine are absent.

- [ ] **Step 4: Implement bounded capture**

```kotlin
internal class BoundedByteAccumulator(private val limit: Int) {
    @Synchronized fun append(bytes: ByteArray, offset: Int, length: Int)
    @Synchronized fun snapshot(): ByteArray
    @Synchronized fun isTruncated(): Boolean
}
```

Reject negative limits. Retain the first `limit` bytes and set `truncated` as soon as additional bytes arrive.

- [ ] **Step 5: Implement execution options and result**

Use public factories:

```kotlin
NonoInput.closed()
NonoInput.inherit()
NonoInput.bytes(value)
NonoOutput.capture(maxBytes)
NonoOutput.inherit()
NonoOutput.discard()
NonoOutput.stream(listener, maxRetainedBytes)
```

`NonoExecutionResult` exposes immutable byte-array copies, UTF-8 text helpers, `exitCode`, `duration`, `timedOut`, `cancelled`, `stdoutTruncated`, `stderrTruncated`, and nullable diagnostics/protocol-error fields.

- [ ] **Step 6: Implement process launch and termination**

Launch with:

```kotlin
val process = ProcessBuilder(argv)
    .directory(options.workingDirectory?.toFile())
    .apply {
        if (!options.inheritEnvironment) {
            environment().clear()
        }
        environment().putAll(options.environment)
    }
    .start()
```

Never pass a single joined command string. Pump piped streams on daemon executor threads. Join pumps after process exit. On timeout or cancellation, destroy descendants deepest-first, call `destroy()`, wait up to 500 milliseconds, then call `destroyForcibly()` for survivors. Complete the result exactly once.

- [ ] **Step 7: Run process tests and verify GREEN**

Run:

```bash
./gradlew test --tests nono.elonzh.com.ProcessEngineTest --no-daemon
./gradlew test ktlintCheck --no-daemon
```

Expected: all process and existing tests pass.

- [ ] **Step 8: Commit**

```bash
git add src
git commit -m "feat: execute nono processes safely" \
  -m "Add bounded I/O capture, streaming, stdin modes, timeouts, and cancellable process-tree termination.

Preserve real child exit codes and partial output without shell invocation."
```

---

### Task 4: Diagnostics and Profile JSON Protocols

**Files:**
- Create: `src/test/kotlin/nono/elonzh/com/JsonProtocolsTest.kt`
- Create: `src/main/kotlin/nono/elonzh/com/NonoDiagnostics.kt`
- Create: `src/main/kotlin/nono/elonzh/com/NonoProfiles.kt`
- Create: `src/main/kotlin/nono/elonzh/com/internal/JsonProtocols.kt`

**Interfaces:**
- Produces: `JsonProtocols.parseDiagnostics(stderr): ParsedDiagnostics`
- Produces: `JsonProtocols.parseProfileList(stdout): List<NonoProfileSummary>`
- Produces: `JsonProtocols.parseProfile(stdout): NonoProfile`
- Produces: `JsonProtocols.parseValidation(stdout, exitCode): NonoProfileValidation`

- [ ] **Step 1: Write failing diagnostics tests**

Use a literal merged report appended to child stderr:

```json
{
  "session": {
    "exit_code": 1,
    "denials": [],
    "ipc_denials": [],
    "violations": [],
    "diagnostics": [{"code":"network_blocked","severity":"error","message":"blocked"}]
  },
  "proxy": []
}
```

Assert extraction leaves preceding child stderr unchanged, returns exit code 1, projects code/severity/message, preserves `rawJson`, and ignores unknown fields. Add tests for no JSON, malformed trailing JSON, nested braces in strings, and child JSON that lacks `session.exit_code`.

- [ ] **Step 2: Write failing profile tests**

Use hand-written fixtures matching nono 0.69.0:

```json
[{"name":"default","source":"built-in","pack":null,"description":"Base","extends":[]}]
```

```json
{"name":"locked","description":"Locked","extends":["default"],"filesystem":{"read":["/src"]}}
```

```json
{"file":"/tmp/profile.json","valid":false,"errors":["bad"],"warnings":[],"deprecated_keys":2}
```

Assert stable fields and exact raw JSON retention. Validation exit 2 with `valid=true` and deprecated keys must be represented as a strict deprecation failure, not a launch exception.

- [ ] **Step 3: Run JSON tests and verify RED**

Run:

```bash
./gradlew test --tests nono.elonzh.com.JsonProtocolsTest --no-daemon
```

Expected: compilation fails because protocol models and parsers are absent.

- [ ] **Step 4: Implement resilient JSON decoding**

Configure:

```kotlin
private val json = Json {
    ignoreUnknownKeys = true
    isLenient = false
    explicitNulls = false
}
```

Diagnostics scanning walks backward over candidate `{` positions, attempts strict JSON parsing, and accepts only an object whose `session.exit_code` is numeric. Profile models project documented stable fields and retain the original JSON string for future nono fields.

- [ ] **Step 5: Run JSON and full tests**

Run:

```bash
./gradlew test ktlintCheck --no-daemon
```

Expected: all JSON compatibility tests pass.

- [ ] **Step 6: Commit**

```bash
git add src
git commit -m "feat: parse nono JSON protocols" \
  -m "Decode profile and validation JSON while retaining raw payloads.

Extract trailing diagnostics additively without hiding child stderr or changing exit status."
```

---

### Task 5: Public Kotlin and Java Client

**Files:**
- Create: `src/test/kotlin/nono/elonzh/com/NonoClientTest.kt`
- Create: `src/test/java/nono/elonzh/com/JavaApiTest.java`
- Create: `src/main/kotlin/nono/elonzh/com/NonoClient.kt`
- Create: `src/main/kotlin/nono/elonzh/com/NonoDsl.kt`
- Modify: `src/main/kotlin/nono/elonzh/com/NonoRequests.kt`
- Modify: `src/main/kotlin/nono/elonzh/com/internal/ProcessEngine.kt`

**Interfaces:**
- Consumes: executable resolver, argument renderer, process engine, and JSON protocols.
- Produces: `NonoClient.builder()`
- Produces: `NonoClient.run(NonoRunRequest): NonoExecutionResult`
- Produces: `NonoClient.start(NonoRunRequest): NonoExecution`
- Produces: `NonoClient.execute(NonoCommandRequest): NonoExecutionResult`
- Produces: `NonoClient.listProfiles(): List<NonoProfileSummary>`
- Produces: `NonoClient.showProfile(String): NonoProfile`
- Produces: `NonoClient.validateProfile(String, Boolean): NonoProfileValidation`

- [ ] **Step 1: Write failing client tests**

Using `FakeNono`, assert strict construction probes `--version`, rejects 0.68.0, can defer probing until first call, renders child argv after `--`, returns child non-zero exit codes, parses requested diagnostics, and routes profile methods through their JSON flags.

Assert generic execution preserves arguments exactly:

```kotlin
client.execute(
    NonoCommandRequest.builder("why", "--json", "--path", "a path with spaces").build(),
)
assertEquals(listOf("why", "--json", "--path", "a path with spaces"), recordedArguments)
```

- [ ] **Step 2: Write failing Java source-compatibility test**

```java
try (NonoClient client = NonoClient.builder()
        .executable(fakeNono)
        .build()) {
    NonoRunRequest request = NonoRunRequest.builder("printf", "hello")
        .options(NonoRunOptions.builder().allowCurrentDirectory().build())
        .build();
    assertEquals(0, client.run(request).getExitCode());
}
```

Also compile and exercise builders, `CompletionStage`, profile methods, byte output, and checked model getters from Java.

- [ ] **Step 3: Run client tests and verify RED**

Run:

```bash
./gradlew test --tests nono.elonzh.com.NonoClientTest --tests nono.elonzh.com.JavaApiTest --no-daemon
```

Expected: compilation fails because `NonoClient` and DSL entry points are absent.

- [ ] **Step 4: Implement the immutable client**

```kotlin
public class NonoClient private constructor(
    public val executable: NonoExecutable,
    private val processEngine: ProcessEngine,
) : AutoCloseable {
    public fun run(request: NonoRunRequest): NonoExecutionResult
    public fun start(request: NonoRunRequest): NonoExecution
    public fun execute(request: NonoCommandRequest): NonoExecutionResult
    public fun listProfiles(): List<NonoProfileSummary>
    public fun showProfile(nameOrPath: String): NonoProfile
    public fun validateProfile(nameOrPath: String, strict: Boolean = false): NonoProfileValidation

    public companion object {
        @JvmStatic public fun builder(): Builder
    }
}
```

Client-owned executors use named daemon threads and shut down in `close()`. Caller-supplied executors remain caller-owned. Synchronous methods unwrap completion failures into the declared `NonoException` hierarchy and preserve interruption.

- [ ] **Step 5: Add Kotlin DSL entry points**

```kotlin
public inline fun nonoRun(
    command: List<String>,
    block: NonoRunOptions.Builder.() -> Unit = {},
): NonoRunRequest

public inline fun nonoClient(block: NonoClient.Builder.() -> Unit): NonoClient
```

The DSL delegates to the same builders used by Java; it does not create a second behavior path.

- [ ] **Step 6: Run all tests and API checks**

Run:

```bash
./gradlew test ktlintCheck --no-daemon
```

Expected: Kotlin and Java tests pass and public API compilation is warning-free.

- [ ] **Step 7: Commit**

```bash
git add src api
git commit -m "feat: expose Kotlin and Java nono client" \
  -m "Compose version probing, typed run execution, generic argv commands, profiles, and diagnostics behind one thread-safe client.

Add Kotlin DSL helpers and verified Java builders and getters."
```

---

### Task 6: Examples and Bilingual Documentation

**Files:**
- Create: `README.md`
- Create: `README.zh-Hans.md`
- Create: `docs/getting-started.md`
- Create: `docs/getting-started.zh-Hans.md`
- Create: `docs/run-api.md`
- Create: `docs/run-api.zh-Hans.md`
- Create: `docs/profiles-and-diagnostics.md`
- Create: `docs/profiles-and-diagnostics.zh-Hans.md`
- Create: `docs/compatibility.md`
- Create: `docs/compatibility.zh-Hans.md`
- Create: `CONTRIBUTING.md`
- Create: `SECURITY.md`
- Create: `CHANGELOG.md`
- Create: `LICENSE`
- Create: `examples/kotlin/build.gradle.kts`
- Create: `examples/kotlin/src/main/kotlin/example/Main.kt`
- Create: `examples/java/build.gradle.kts`
- Create: `examples/java/src/main/java/example/Main.java`
- Modify: `settings.gradle.kts`
- Modify: `Taskfile.yml`

**Interfaces:**
- Consumes: the final public API from Task 5.
- Produces: independently compilable Kotlin and Java examples.
- Produces: paired English/Chinese user documentation.

- [ ] **Step 1: Add example compilation tests**

Include both examples as Gradle subprojects and make root `check` depend on their `compileKotlin`/`compileJava` tasks. Each example depends on `project(":")` and uses only public API.

- [ ] **Step 2: Run example compilation and verify RED**

Run:

```bash
./gradlew :examples:kotlin:compileKotlin :examples:java:compileJava --no-daemon
```

Expected: Gradle reports missing example build/source files.

- [ ] **Step 3: Implement minimal examples**

Kotlin:

```kotlin
NonoClient.builder().build().use { client ->
    val result = client.run(
        nonoRun(listOf("printf", "hello from nono\\n")) {
            allowCurrentDirectory()
            blockNetwork()
        },
    )
    print(result.stdoutText())
}
```

Java uses `NonoClient.builder()`, `NonoRunOptions.builder()`, and `NonoRunRequest.builder()` without Kotlin-only helpers. Both examples pass the `printf` argv directly and never add shell parsing.

- [ ] **Step 4: Write synchronized documentation**

Both README files include: project status, installation coordinates, nono 0.69.0 prerequisite, Kotlin and Java quick starts, explicit policy ownership, result semantics, compatibility table, documentation index, and language switch.

The paired guides cover every public builder family and use matching code behavior. `docs/compatibility*` state Java 17+, macOS/Linux, nono 0.69.0+, forward-compatible generic commands, and semantic versioning. `SECURITY.md` directs vulnerability reports privately and states that the wrapper is not itself a sandbox.

- [ ] **Step 5: Validate docs and examples**

Run:

```bash
task docs
./gradlew check --no-daemon
```

`task docs` checks reciprocal links, paired English/Chinese files, absent broken local Markdown links, and example compilation. Expected: both commands exit 0.

- [ ] **Step 6: Commit**

```bash
git add README.md README.zh-Hans.md docs examples settings.gradle.kts Taskfile.yml CONTRIBUTING.md SECURITY.md CHANGELOG.md LICENSE
git commit -m "docs: add bilingual usage and examples" \
  -m "Document installation, run policies, profiles, diagnostics, compatibility, and security in English and Chinese.

Compile standalone Kotlin and Java consumer examples as part of repository checks."
```

---

### Task 7: CI, Publication, and Release Automation

**Files:**
- Create: `.github/workflows/ci.yml`
- Create: `.github/workflows/release.yml`
- Create: `docs/releasing.md`
- Modify: `build.gradle.kts`
- Modify: `Taskfile.yml`

**Interfaces:**
- Consumes: root `task check`, `task integration`, and Gradle publication tasks.
- Produces: Linux/macOS CI and tag-driven Central/GitHub releases.

- [ ] **Step 1: Add local publication validation**

Configure the POM with:

```text
name: nono-kt
description: Kotlin and Java client for running commands through the nono CLI.
url: https://github.com/elonzh/nono-kt
license: Apache-2.0
developer: elonzh
scm: https://github.com/elonzh/nono-kt
```

Add `task publish:local` to run `publishToMavenLocal`, then resolve the exact generated version from an isolated Gradle consumer fixture.

- [ ] **Step 2: Run local publication validation**

Run:

```bash
task publish:local
./gradlew publishToMavenLocal --no-daemon
```

Expected: signed publication metadata is skipped only for local snapshot validation; POM, module metadata, main jar, sources jar, and Javadoc jar exist and the consumer resolves them.

- [ ] **Step 3: Add CI workflow**

`ci.yml` triggers on pushes and pull requests. A Linux job runs `task check`; a matrix job runs the real integration test on `ubuntu-latest` and `macos-latest` after installing nono 0.69.0 or the selected newer compatibility version. Upload test reports on failure.

- [ ] **Step 4: Add release workflow**

`release.yml` triggers on `v*` tags, verifies that tag `vX.Y.Z` equals `VERSION_NAME=X.Y.Z`, runs `task check` and both host integrations, publishes with:

```text
ORG_GRADLE_PROJECT_mavenCentralUsername
ORG_GRADLE_PROJECT_mavenCentralPassword
ORG_GRADLE_PROJECT_signingInMemoryKey
ORG_GRADLE_PROJECT_signingInMemoryKeyPassword
```

Then create a GitHub Release with generated release notes only after Maven publication succeeds.

- [ ] **Step 5: Validate workflow syntax and publication**

Run:

```bash
task check
task publish:local
```

Parse both workflow files with a YAML parser in `task check`. Expected: all repository and publication validation passes.

- [ ] **Step 6: Commit**

```bash
git add .github build.gradle.kts Taskfile.yml docs/releasing.md
git commit -m "ci: automate validation and releases" \
  -m "Run the Kotlin/Java suite and real nono integration on Linux and macOS.

Validate Maven artifacts locally and publish signed tag releases to Central and GitHub."
```

---

### Task 8: Real nono Integration, Repository Creation, and First Release

**Files:**
- Create: `src/integrationTest/kotlin/nono/elonzh/com/NonoIntegrationTest.kt`
- Modify: `build.gradle.kts`
- Modify: `Taskfile.yml`
- Modify: `CHANGELOG.md`

**Interfaces:**
- Consumes: installed nono 0.69.0+ and all public APIs.
- Produces: verified `elonzh/nono-kt` remote and release `v0.1.0`.

- [ ] **Step 1: Write integration tests before enabling the source set**

```kotlin
class NonoIntegrationTest {
    @Test
    fun `probes installed nono and runs an isolated command`() {
        NonoClient.builder().build().use { client ->
            assertTrue(client.executable.version >= NonoVersion(0, 69, 0))
            val result = client.run(
                nonoRun(listOf("/usr/bin/printf", "integration")) {
                    blockNetwork()
                },
            )
            assertEquals(0, result.exitCode)
            assertEquals("integration", result.stdoutText())
        }
    }
}
```

Add tests for profile list JSON, child exit 23 propagation, separate stdout/stderr, diagnostics request, and timeout termination.

- [ ] **Step 2: Run the integration target and verify RED**

Run:

```bash
./gradlew integrationTest --no-daemon
```

Expected: Gradle reports that `integrationTest` is not defined.

- [ ] **Step 3: Configure the integration source set**

Create an `integrationTest` source set extending main/test dependencies, register a JUnit Platform `integrationTest` task, require `NONO_INTEGRATION=1`, and make `task integration` set that environment. Keep unit `check` deterministic without an installed nono.

- [ ] **Step 4: Install and verify the supported nono CLI**

Use the official release asset or documented installer, then run:

```bash
nono --version
NONO_INTEGRATION=1 ./gradlew integrationTest --no-daemon
```

Expected: version is 0.69.0 or newer and every integration test passes.

- [ ] **Step 5: Run the complete local acceptance suite**

Run:

```bash
task format
task check
task integration
task publish:local
git diff --check
git status --short
```

Expected: all tasks exit 0, diff check is clean, and only intended release changes remain.

- [ ] **Step 6: Commit the integration and release state**

```bash
git add src/integrationTest build.gradle.kts Taskfile.yml CHANGELOG.md
git commit -m "test: verify nono CLI integration" \
  -m "Exercise version probing, sandboxed execution, profiles, diagnostics, output, exit codes, and termination against a real supported nono CLI.

Record the validated 0.1.0 release scope."
```

- [ ] **Step 7: Materialize the requested local repository**

Copy the verified repository, including `.git`, from the isolated temporary workspace to the previously absent `~/workspace/nono-kt`. Verify:

```bash
git -C ~/workspace/nono-kt status --short --branch
git -C ~/workspace/nono-kt log --oneline --decorate -8
```

Expected: branch `codex/initial-implementation` is clean and contains the design, plan, implementation, documentation, automation, and integration commits.

- [ ] **Step 8: Complete the local implementation branch**

After the finishing-branch verification gate, fast-forward `main` locally:

```bash
git switch main
git merge --ff-only codex/initial-implementation
```

Expected: `main` points at the fully verified implementation commit.

- [ ] **Step 9: Create and push the GitHub repository**

Run:

```bash
gh repo create elonzh/nono-kt --public --source ~/workspace/nono-kt --remote origin --push
gh repo view elonzh/nono-kt --json nameWithOwner,isPrivate,url,defaultBranchRef
```

Expected: `nameWithOwner` is `elonzh/nono-kt`, `isPrivate` is false, and default branch is `main`.

- [ ] **Step 10: Create and push the signed tag**

Create signed tag `v0.1.0` and push it:

```bash
git tag -s v0.1.0 -m "nono-kt 0.1.0"
git push origin v0.1.0
```

- [ ] **Step 11: Verify external publication**

Watch the release workflow to completion and verify:

```bash
gh run list --repo elonzh/nono-kt --workflow release.yml --limit 1
gh release view v0.1.0 --repo elonzh/nono-kt
```

Resolve `com.elonzh.nono:nono-kt:0.1.0` from Maven Central in a clean Gradle consumer. Completion requires the GitHub Release, Central artifact, source/Javadoc jars, matching commit/tag, and green CI to be externally observable.
