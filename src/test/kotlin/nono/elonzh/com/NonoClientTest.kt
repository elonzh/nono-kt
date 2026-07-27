package nono.elonzh.com

import nono.elonzh.com.test.FakeNono
import java.nio.file.Files
import java.nio.file.Path
import java.time.Duration
import java.util.concurrent.TimeUnit
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class NonoClientTest {
    private val temporaryDirectories = mutableListOf<Path>()

    @AfterTest
    fun cleanUp() {
        temporaryDirectories.forEach { directory ->
            Files.walk(directory).use { paths ->
                paths.sorted(Comparator.reverseOrder()).forEach(Files::deleteIfExists)
            }
        }
    }

    @Test
    fun `strict client probes executable on build`() {
        val fake = fakeNono()

        NonoClient
            .builder()
            .executable(fake.executable)
            .environment("FAKE_NONO_ARGS_FILE", fake.argumentsFile.toString())
            .build()
            .use { client ->
                assertEquals(NonoVersion(0, 69, 0), client.executable.version)
            }

        assertEquals(listOf("--version"), fake.recordedArguments())
    }

    @Test
    fun `strict client rejects old nono`() {
        val fake = fakeNono(version = "0.68.0")

        assertFailsWith<NonoCompatibilityException> {
            NonoClient.builder().executable(fake.executable).build()
        }
    }

    @Test
    fun `deferred verification waits for first executable access`() {
        val fake = fakeNono()
        val client =
            NonoClient
                .builder()
                .executable(fake.executable)
                .environment("FAKE_NONO_ARGS_FILE", fake.argumentsFile.toString())
                .verifyOnBuild(false)
                .build()

        client.use {
            assertFalse(Files.exists(fake.argumentsFile))
            assertEquals(NonoVersion(0, 69, 0), client.executable.version)
            assertEquals(listOf("--version"), fake.recordedArguments())
        }
    }

    @Test
    fun `runs child and preserves nonzero exit`() {
        val fake = fakeNono()
        client(fake).use { client ->
            val result =
                client.run(
                    NonoRunRequest
                        .builder("emit", "hello", "problem", "23")
                        .executionOptions(recordingOptions(fake))
                        .build(),
                )

            assertEquals(23, result.exitCode)
            assertEquals("hello", result.stdoutText())
            assertEquals("problem", result.stderrText())
            assertEquals(
                listOf("run", "--", "emit", "hello", "problem", "23"),
                fake.recordedArguments(),
            )
        }
    }

    @Test
    fun `parses requested diagnostics and preserves raw stderr`() {
        val fake = fakeNono()
        client(fake).use { client ->
            val result =
                client.run(
                    NonoRunRequest
                        .builder("diagnostics")
                        .options(NonoRunOptions.builder().diagnosticsJson().build())
                        .build(),
                )

            assertEquals(1, result.exitCode)
            assertEquals("child error\n", result.stderrText())
            assertTrue(result.rawStderrText().contains("\"session\""))
            assertEquals("network_blocked", assertNotNull(result.diagnostics).diagnostics.single().code)
            assertEquals(null, result.diagnosticsProtocolError)
        }
    }

    @Test
    fun `executes generic command with exact arguments`() {
        val fake = fakeNono()
        client(fake).use { client ->
            val result =
                client.execute(
                    NonoCommandRequest
                        .builder("why", "--json", "--path", "a path with spaces", "")
                        .executionOptions(recordingOptions(fake))
                        .build(),
                )

            assertEquals(64, result.exitCode)
            assertEquals(
                listOf("why", "--json", "--path", "a path with spaces", ""),
                fake.recordedArguments(),
            )
        }
    }

    @Test
    fun `exposes profile JSON operations`() {
        val fake = fakeNono()
        client(fake).use { client ->
            assertEquals("default", client.listProfiles().single().name)
            assertEquals("locked", client.showProfile("locked").name)
            assertTrue(client.validateProfile("/tmp/profile.json").valid)
        }
    }

    @Test
    fun `starts cancellable asynchronous execution`() {
        val fake = fakeNono()
        client(fake).use { client ->
            val execution =
                client.start(
                    NonoRunRequest
                        .builder("sleep", "30")
                        .executionOptions(
                            NonoExecutionOptions
                                .builder()
                                .timeout(Duration.ofSeconds(10))
                                .build(),
                        ).build(),
                )

            assertTrue(execution.cancel())
            assertTrue(
                execution
                    .result()
                    .toCompletableFuture()
                    .get(5, TimeUnit.SECONDS)
                    .cancelled,
            )
        }
    }

    @Test
    fun `Kotlin DSL delegates to public builders`() {
        val request =
            nonoRun(listOf("printf", "hello")) {
                allowCurrentDirectory()
                blockNetwork()
            }

        assertEquals(listOf("printf", "hello"), request.command)
        assertEquals(
            listOf("--allow-cwd", "--block-net"),
            request.options.arguments,
        )
    }

    private fun client(fake: FakeNono): NonoClient =
        NonoClient
            .builder()
            .executable(fake.executable)
            .environment("FAKE_NONO_ARGS_FILE", fake.argumentsFile.toString())
            .build()

    private fun recordingOptions(fake: FakeNono): NonoExecutionOptions =
        NonoExecutionOptions
            .builder()
            .environment("FAKE_NONO_ARGS_FILE", fake.argumentsFile.toString())
            .build()

    private fun fakeNono(version: String = "0.69.0"): FakeNono {
        val directory = createTempDirectory("nono-kt-client-")
        temporaryDirectories.add(directory)
        return FakeNono.create(directory.resolve("bin"), version)
    }
}
