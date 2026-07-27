package nono.elonzh.com

import nono.elonzh.com.internal.ProcessEngine
import nono.elonzh.com.test.FakeNono
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ProcessEngineTest {
    private lateinit var temporaryDirectory: Path
    private lateinit var fakeNono: FakeNono
    private lateinit var engine: ProcessEngine

    @BeforeTest
    fun setUp() {
        temporaryDirectory = createTempDirectory("nono-kt-process-")
        fakeNono = FakeNono.create(temporaryDirectory.resolve("bin"))
        engine = ProcessEngine()
    }

    @AfterTest
    fun cleanUp() {
        engine.close()
        Files.walk(temporaryDirectory).use { paths ->
            paths.sorted(Comparator.reverseOrder()).forEach(Files::deleteIfExists)
        }
    }

    @Test
    fun `captures stdout stderr and child exit code`() {
        val result = execute("emit", "hello", "problem", "7")

        assertEquals("hello", result.stdoutText())
        assertEquals("problem", result.stderrText())
        assertEquals(7, result.exitCode)
        assertFalse(result.timedOut)
        assertFalse(result.cancelled)
    }

    @Test
    fun `writes byte input and closes stdin`() {
        val options =
            NonoExecutionOptions
                .builder()
                .input(NonoInput.bytes("from stdin".toByteArray()))
                .build()

        val result = execute("stdin", options = options)

        assertEquals("from stdin", result.stdoutText())
    }

    @Test
    fun `streams output and retains a bounded copy`() {
        val streamed = ByteArrayOutputStream()
        val options =
            NonoExecutionOptions
                .builder()
                .stdout(
                    NonoOutput.stream(
                        listener = NonoOutputListener { bytes -> streamed.write(bytes) },
                        maxRetainedBytes = 64,
                    ),
                ).build()

        val result = execute("emit", "streamed", "", "0", options = options)

        assertEquals("streamed", streamed.toString(StandardCharsets.UTF_8))
        assertEquals("streamed", result.stdoutText())
        assertFalse(result.stdoutTruncated)
    }

    @Test
    fun `bounds captured output and reports truncation`() {
        val options =
            NonoExecutionOptions
                .builder()
                .stdout(NonoOutput.capture(maxBytes = 4))
                .stderr(NonoOutput.capture(maxBytes = 3))
                .build()

        val result = execute("emit", "abcdefgh", "123456", "0", options = options)

        assertEquals("abcd", result.stdoutText())
        assertEquals("123", result.stderrText())
        assertTrue(result.stdoutTruncated)
        assertTrue(result.stderrTruncated)
    }

    @Test
    fun `uses requested working directory and environment`() {
        val workingDirectory = temporaryDirectory.resolve("working")
        Files.createDirectories(workingDirectory)
        val cwdOptions =
            NonoExecutionOptions
                .builder()
                .workingDirectory(workingDirectory)
                .build()
        val environmentOptions =
            NonoExecutionOptions
                .builder()
                .clearEnvironment()
                .environment("FAKE_VALUE", "configured")
                .build()

        val actualWorkingDirectory =
            Path.of(execute("pwd", options = cwdOptions).stdoutText().trim()).toRealPath()
        assertEquals(workingDirectory.toRealPath(), actualWorkingDirectory)
        assertEquals("configured", execute("environment", options = environmentOptions).stdoutText())
    }

    @Test
    fun `discards output without changing exit status`() {
        val options =
            NonoExecutionOptions
                .builder()
                .stdout(NonoOutput.discard())
                .stderr(NonoOutput.discard())
                .build()

        val result = execute("emit", "hidden", "also hidden", "9", options = options)

        assertEquals(9, result.exitCode)
        assertEquals("", result.stdoutText())
        assertEquals("", result.stderrText())
    }

    @Test
    fun `inherits output without retaining a copy`() {
        val options =
            NonoExecutionOptions
                .builder()
                .stdout(NonoOutput.inherit())
                .stderr(NonoOutput.inherit())
                .build()

        val result = execute("emit", "", "", "0", options = options)

        assertEquals("", result.stdoutText())
        assertEquals("", result.stderrText())
    }

    @Test
    fun `times out and returns partial output`() {
        val options =
            NonoExecutionOptions
                .builder()
                .timeout(java.time.Duration.ofMillis(150))
                .build()

        val result = execute("sleep", "30", options = options)

        assertTrue(result.timedOut)
        assertFalse(result.cancelled)
        assertEquals("started", result.stdoutText())
    }

    @Test
    fun `cancels a running process`() {
        val execution =
            engine.start(
                argv("sleep", "30"),
                NonoExecutionOptions.builder().build(),
            )

        assertTrue(execution.cancel())
        val result = execution.result().toCompletableFuture().get(5, TimeUnit.SECONDS)

        assertTrue(result.cancelled)
        assertFalse(result.timedOut)
    }

    @Test
    fun `does not cancel a completed process`() {
        val execution =
            engine.start(
                argv("emit", "done", "", "0"),
                NonoExecutionOptions.builder().build(),
            )
        execution.result().toCompletableFuture().get(5, TimeUnit.SECONDS)

        assertFalse(execution.cancel())
    }

    @Test
    fun `rejects execution after close before launching a process`() {
        engine.close()

        assertFailsWith<NonoProcessException> {
            engine.start(
                argv("sleep", "30"),
                NonoExecutionOptions.builder().build(),
            )
        }
    }

    @Test
    fun `terminates process when caller executor rejects scheduling`() {
        val ioExecutor = Executors.newSingleThreadExecutor()
        val scheduler = Executors.newSingleThreadScheduledExecutor()
        ioExecutor.shutdownNow()
        scheduler.shutdownNow()
        val rejectedEngine =
            ProcessEngine(
                ioExecutor = ioExecutor,
                scheduler = scheduler,
                ownsExecutors = false,
            )

        assertFailsWith<NonoProcessException> {
            rejectedEngine.start(
                argv("sleep", "30"),
                NonoExecutionOptions.builder().build(),
            )
        }
    }

    @Test
    fun `terminates descendants on timeout`() {
        val options =
            NonoExecutionOptions
                .builder()
                .timeout(java.time.Duration.ofMillis(200))
                .build()

        val result = execute("spawn", "30", options = options)
        val descendantPid = result.stdoutText().trim().toLong()

        assertTrue(result.timedOut)
        assertFalse(
            ProcessHandle
                .of(descendantPid)
                .map(ProcessHandle::isAlive)
                .orElse(false),
        )
    }

    private fun execute(
        vararg childArguments: String,
        options: NonoExecutionOptions = NonoExecutionOptions.builder().build(),
    ): NonoExecutionResult =
        engine
            .start(argv(*childArguments), options)
            .result()
            .toCompletableFuture()
            .get(5, TimeUnit.SECONDS)

    private fun argv(vararg childArguments: String): List<String> = listOf(fakeNono.executable.toString(), "run", "--", *childArguments)
}
