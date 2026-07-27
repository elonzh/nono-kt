package nono.elonzh.com.internal

import nono.elonzh.com.NonoExecution
import nono.elonzh.com.NonoExecutionOptions
import nono.elonzh.com.NonoExecutionResult
import nono.elonzh.com.NonoInput
import nono.elonzh.com.NonoLaunchException
import nono.elonzh.com.NonoOutput
import nono.elonzh.com.NonoProcessException
import java.io.InputStream
import java.time.Duration
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionStage
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.ThreadFactory
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

internal class ProcessEngine(
    private val ioExecutor: ExecutorService =
        Executors.newCachedThreadPool(DaemonThreadFactory("nono-kt-io")),
    private val scheduler: ScheduledExecutorService =
        Executors.newSingleThreadScheduledExecutor(DaemonThreadFactory("nono-kt-timeout")),
    private val ownsExecutors: Boolean = true,
) : AutoCloseable {
    private val lifecycleLock = Any()
    private var closed = false

    fun start(
        argv: List<String>,
        options: NonoExecutionOptions,
    ): NonoExecution =
        synchronized(lifecycleLock) {
            if (closed) {
                throw NonoProcessException(
                    "nono process engine is closed",
                    IllegalStateException("process engine is closed"),
                )
            }
            require(argv.isNotEmpty()) { "argv must not be empty" }
            argv.forEach { argument ->
                require('\u0000' !in argument) { "argv must not contain NUL" }
            }

            val processBuilder = ProcessBuilder(argv)
            options.workingDirectory?.let { processBuilder.directory(it.toFile()) }
            if (!options.inheritEnvironment) {
                processBuilder.environment().clear()
            }
            processBuilder.environment().putAll(options.environment)
            configureRedirects(processBuilder, options)

            val process =
                try {
                    processBuilder.start()
                } catch (exception: Exception) {
                    throw NonoLaunchException(argv = argv, cause = exception)
                }

            var timeoutTask: ScheduledFuture<*>? = null

            try {
                val stdoutCapture = accumulator(options.stdout)
                val stderrCapture = accumulator(options.stderr)
                val stdoutPump = pump(process.inputStream, options.stdout, stdoutCapture)
                val stderrPump = pump(process.errorStream, options.stderr, stderrCapture)
                val inputWriter = writeInput(process, options.input)
                val completion = CompletableFuture<NonoExecutionResult>()
                val terminationReason = AtomicReference(TerminationReason.NONE)
                val startedAt = System.nanoTime()

                val execution =
                    ProcessExecution(
                        process = process,
                        completion = completion,
                        terminationReason = terminationReason,
                    )

                options.timeout?.let { timeout ->
                    timeoutTask =
                        scheduler.schedule(
                            {
                                if (process.isAlive &&
                                    terminationReason.compareAndSet(TerminationReason.NONE, TerminationReason.TIMEOUT)
                                ) {
                                    terminateProcessTree(process)
                                }
                            },
                            timeout.toNanos(),
                            TimeUnit.NANOSECONDS,
                        )
                }

                ioExecutor.execute {
                    try {
                        val exitCode = process.waitFor()
                        inputWriter.join()
                        stdoutPump.join()
                        stderrPump.join()
                        completion.complete(
                            NonoExecutionResult(
                                exitCode = exitCode,
                                stdout = stdoutCapture?.snapshot() ?: ByteArray(0),
                                stderr = stderrCapture?.snapshot() ?: ByteArray(0),
                                duration = Duration.ofNanos(System.nanoTime() - startedAt),
                                timedOut = terminationReason.get() == TerminationReason.TIMEOUT,
                                cancelled = terminationReason.get() == TerminationReason.CANCELLED,
                                stdoutTruncated = stdoutCapture?.isTruncated() == true,
                                stderrTruncated = stderrCapture?.isTruncated() == true,
                            ),
                        )
                    } catch (exception: InterruptedException) {
                        Thread.currentThread().interrupt()
                        terminateProcessTree(process)
                        completion.completeExceptionally(
                            NonoProcessException("Interrupted while waiting for nono", exception),
                        )
                    } catch (exception: Exception) {
                        terminateProcessTree(process)
                        completion.completeExceptionally(
                            NonoProcessException("Failed while executing nono", unwrap(exception)),
                        )
                    } finally {
                        timeoutTask?.cancel(false)
                    }
                }

                execution
            } catch (exception: Exception) {
                timeoutTask?.cancel(false)
                terminateProcessTree(process)
                throw NonoProcessException("Failed to schedule nono execution", exception)
            }
        }

    override fun close() {
        synchronized(lifecycleLock) {
            if (closed) {
                return
            }
            closed = true
            if (ownsExecutors) {
                scheduler.shutdownNow()
                ioExecutor.shutdownNow()
            }
        }
    }

    private fun configureRedirects(
        processBuilder: ProcessBuilder,
        options: NonoExecutionOptions,
    ) {
        processBuilder.redirectInput(
            when (options.input.mode) {
                NonoInput.Mode.INHERIT -> ProcessBuilder.Redirect.INHERIT
                NonoInput.Mode.CLOSED,
                NonoInput.Mode.BYTES,
                -> ProcessBuilder.Redirect.PIPE
            },
        )
        processBuilder.redirectOutput(redirect(options.stdout))
        processBuilder.redirectError(redirect(options.stderr))
    }

    private fun redirect(output: NonoOutput): ProcessBuilder.Redirect =
        when (output.mode) {
            NonoOutput.Mode.INHERIT -> ProcessBuilder.Redirect.INHERIT
            NonoOutput.Mode.DISCARD -> ProcessBuilder.Redirect.DISCARD
            NonoOutput.Mode.CAPTURE,
            NonoOutput.Mode.STREAM,
            -> ProcessBuilder.Redirect.PIPE
        }

    private fun accumulator(output: NonoOutput): BoundedByteAccumulator? =
        when (output.mode) {
            NonoOutput.Mode.CAPTURE,
            NonoOutput.Mode.STREAM,
            -> BoundedByteAccumulator(output.maxRetainedBytes)
            NonoOutput.Mode.INHERIT,
            NonoOutput.Mode.DISCARD,
            -> null
        }

    private fun pump(
        stream: InputStream,
        output: NonoOutput,
        accumulator: BoundedByteAccumulator?,
    ): CompletableFuture<Void> {
        if (output.mode == NonoOutput.Mode.INHERIT || output.mode == NonoOutput.Mode.DISCARD) {
            return CompletableFuture.completedFuture(null)
        }
        return CompletableFuture.runAsync(
            {
                stream.use {
                    val buffer = ByteArray(STREAM_BUFFER_SIZE)
                    while (true) {
                        val read = it.read(buffer)
                        if (read < 0) {
                            break
                        }
                        accumulator?.append(buffer, 0, read)
                        output.listener?.onOutput(buffer.copyOf(read))
                    }
                }
            },
            ioExecutor,
        )
    }

    private fun writeInput(
        process: Process,
        input: NonoInput,
    ): CompletableFuture<Void> =
        when (input.mode) {
            NonoInput.Mode.INHERIT -> CompletableFuture.completedFuture(null)
            NonoInput.Mode.CLOSED -> {
                process.outputStream.close()
                CompletableFuture.completedFuture(null)
            }
            NonoInput.Mode.BYTES ->
                CompletableFuture.runAsync(
                    {
                        process.outputStream.use { stream ->
                            stream.write(checkNotNull(input.bytes))
                        }
                    },
                    ioExecutor,
                )
        }

    private class ProcessExecution(
        private val process: Process,
        private val completion: CompletableFuture<NonoExecutionResult>,
        private val terminationReason: AtomicReference<TerminationReason>,
    ) : NonoExecution {
        override val pid: Long
            get() = process.pid()

        override fun result(): CompletionStage<NonoExecutionResult> = completion

        override fun cancel(): Boolean {
            if (completion.isDone || !process.isAlive) {
                return false
            }
            if (!terminationReason.compareAndSet(TerminationReason.NONE, TerminationReason.CANCELLED)) {
                return false
            }
            terminateProcessTree(process)
            return true
        }
    }

    private enum class TerminationReason {
        NONE,
        TIMEOUT,
        CANCELLED,
    }

    private companion object {
        private const val STREAM_BUFFER_SIZE = 8192
        private const val GRACEFUL_TERMINATION_MILLIS = 500L

        private fun terminateProcessTree(process: Process) {
            val root = process.toHandle()
            val descendants =
                root
                    .descendants()
                    .use { stream -> stream.toList() }
                    .sortedByDescending(::processDepth)
            descendants.forEach(ProcessHandle::destroy)
            root.destroy()

            val deadline =
                System.nanoTime() +
                    TimeUnit.MILLISECONDS.toNanos(GRACEFUL_TERMINATION_MILLIS)
            while ((root.isAlive || descendants.any(ProcessHandle::isAlive)) && System.nanoTime() < deadline) {
                Thread.sleep(10)
            }

            descendants
                .filter(ProcessHandle::isAlive)
                .forEach(ProcessHandle::destroyForcibly)
            if (root.isAlive) {
                root.destroyForcibly()
            }
        }

        private fun processDepth(handle: ProcessHandle): Int {
            var depth = 0
            var parent = handle.parent()
            while (parent.isPresent) {
                depth += 1
                parent = parent.get().parent()
            }
            return depth
        }

        private fun unwrap(exception: Exception): Throwable = exception.cause ?: exception
    }
}

private class DaemonThreadFactory(
    private val prefix: String,
) : ThreadFactory {
    private val sequence = AtomicInteger()

    override fun newThread(runnable: Runnable): Thread =
        Thread(runnable, "$prefix-${sequence.incrementAndGet()}").apply {
            isDaemon = true
        }
}
