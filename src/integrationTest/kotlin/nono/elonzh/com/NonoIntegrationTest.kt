package nono.elonzh.com

import java.nio.file.Path
import java.time.Duration
import java.util.concurrent.TimeUnit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class NonoIntegrationTest {
    @Test
    fun `probes installed nono and runs an isolated command`() {
        client().use { client ->
            assertTrue(client.executable.version >= NonoVersion(0, 72, 0))

            val result =
                client.run(
                    nonoRun(listOf("/usr/bin/printf", "integration")) {
                        blockNetwork()
                    },
                )

            assertEquals(0, result.exitCode)
            assertEquals("integration", result.stdoutText())
        }
    }

    @Test
    fun `reads installed profiles as JSON`() {
        client().use { client ->
            val profiles = client.listProfiles()

            assertTrue(profiles.isNotEmpty())
            assertTrue(profiles.all { it.name.isNotBlank() })
            assertTrue(profiles.all { it.rawJson.startsWith("{") })
        }
    }

    @Test
    fun `preserves child output and exit code`() {
        client().use { client ->
            val result =
                client.run(
                    NonoRunRequest
                        .builder(
                            "/bin/sh",
                            "-c",
                            "printf stdout-value; printf stderr-value >&2; exit 23",
                        ).build(),
                )

            assertEquals(23, result.exitCode)
            assertEquals("stdout-value", result.stdoutText())
            assertFalse(result.stdoutText().contains("stderr-value"))
            assertTrue(result.stderrText().contains("stderr-value"))
        }
    }

    @Test
    fun `extracts requested diagnostics`() {
        client().use { client ->
            val result =
                client.run(
                    NonoRunRequest
                        .builder("/usr/bin/printf", "diagnostic")
                        .options(NonoRunOptions.builder().diagnosticsJson().build())
                        .build(),
                )

            assertEquals(0, result.exitCode)
            assertEquals("diagnostic", result.stdoutText())
            assertNotNull(result.diagnostics)
            assertEquals(null, result.diagnosticsProtocolError)
            assertTrue(result.rawStderrText().contains("\"session\""))
        }
    }

    @Test
    fun `extracts diagnostics before appended failure guidance`() {
        client().use { client ->
            val result =
                client.run(
                    NonoRunRequest
                        .builder("/bin/sh", "-c", "printf child-error >&2; exit 7")
                        .options(NonoRunOptions.builder().diagnosticsJson().build())
                        .build(),
                )

            assertEquals(7, result.exitCode)
            assertEquals(7, assertNotNull(result.diagnostics).exitCode)
            assertEquals(null, result.diagnosticsProtocolError)
            assertTrue(result.stderrText().contains("child-error"))
            assertTrue(result.stderrText().contains("Command exited with code 7"))
            assertFalse(result.stderrText().contains("\"session\""))
            assertTrue(result.rawStderrText().contains("\"session\""))
        }
    }

    @Test
    fun `terminates timed out command`() {
        client().use { client ->
            val startedAt = System.nanoTime()
            val result =
                client.run(
                    NonoRunRequest
                        .builder("/bin/sh", "-c", "sleep 30")
                        .executionOptions(
                            NonoExecutionOptions
                                .builder()
                                .timeout(Duration.ofMillis(500))
                                .build(),
                        ).build(),
                )
            val elapsedSeconds = TimeUnit.NANOSECONDS.toSeconds(System.nanoTime() - startedAt)

            assertTrue(result.timedOut)
            assertFalse(result.cancelled)
            assertTrue(elapsedSeconds < 10)
        }
    }

    private fun client(): NonoClient {
        val builder = NonoClient.builder()
        System.getenv("NONO_EXECUTABLE")?.let { builder.executable(Path.of(it)) }
        return builder.build()
    }
}
