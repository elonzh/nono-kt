package nono.elonzh.com

import nono.elonzh.com.internal.ExecutableResolver
import nono.elonzh.com.internal.JsonProtocols
import nono.elonzh.com.internal.ProcessEngine
import nono.elonzh.com.internal.RunArgumentRenderer
import java.nio.charset.StandardCharsets
import java.nio.file.Path
import java.util.concurrent.CompletionException
import java.util.concurrent.CompletionStage
import java.util.concurrent.ExecutionException
import java.util.concurrent.ExecutorService
import java.util.concurrent.ScheduledExecutorService

public class NonoClient private constructor(
    private val executablePath: Path,
    private val minimumVersion: NonoVersion,
    private val resolver: ExecutableResolver,
    private val processEngine: ProcessEngine,
    private val defaultExecutionOptions: NonoExecutionOptions,
    private val environmentOverrides: Map<String, String>,
    verifiedExecutable: NonoExecutable?,
) : AutoCloseable {
    @Volatile
    private var verifiedExecutable: NonoExecutable? = verifiedExecutable
    private val verificationLock = Any()

    public val executable: NonoExecutable
        get() = ensureExecutable()

    public fun run(request: NonoRunRequest): NonoExecutionResult = await(start(request))

    public fun start(request: NonoRunRequest): NonoExecution {
        val execution =
            processEngine.start(
                argv = RunArgumentRenderer.render(ensureExecutable().path, request),
                options = effectiveOptions(request.executionOptions),
            )
        return if ("--diagnostics-json" in request.options.arguments) {
            MappingExecution(execution, ::parseDiagnostics)
        } else {
            execution
        }
    }

    public fun execute(request: NonoCommandRequest): NonoExecutionResult = await(start(request))

    public fun start(request: NonoCommandRequest): NonoExecution {
        val execution =
            processEngine.start(
                argv = RunArgumentRenderer.render(ensureExecutable().path, request),
                options = effectiveOptions(request.executionOptions),
            )
        return if ("--diagnostics-json" in request.arguments) {
            MappingExecution(execution, ::parseDiagnostics)
        } else {
            execution
        }
    }

    public fun listProfiles(): List<NonoProfileSummary> {
        val result = execute(NonoCommandRequest.builder("profile", "list", "--json").build())
        requireSuccess("profile list", result)
        return JsonProtocols.parseProfileList(result.stdoutText())
    }

    public fun showProfile(nameOrPath: String): NonoProfile {
        val result =
            execute(
                NonoCommandRequest.builder("profile", "show", nameOrPath, "--json").build(),
            )
        requireSuccess("profile show", result)
        return JsonProtocols.parseProfile(result.stdoutText())
    }

    @JvmOverloads
    public fun validateProfile(
        nameOrPath: String,
        strict: Boolean = false,
    ): NonoProfileValidation {
        val arguments =
            buildList {
                add("profile")
                add("validate")
                add(nameOrPath)
                add("--json")
                if (strict) {
                    add("--strict")
                }
            }
        val result = execute(NonoCommandRequest.builder(arguments).build())
        if (result.exitCode !in setOf(0, 1, STRICT_DEPRECATION_EXIT)) {
            throw NonoCommandFailedException(
                operation = "profile validate",
                exitCode = result.exitCode,
                stderr = result.stderrText(),
            )
        }
        return JsonProtocols.parseValidation(result.stdoutText(), result.exitCode)
    }

    override fun close() {
        processEngine.close()
    }

    private fun ensureExecutable(): NonoExecutable {
        verifiedExecutable?.let { return it }
        synchronized(verificationLock) {
            verifiedExecutable?.let { return it }
            val probed = resolver.probe(executablePath)
            probed.version.requireAtLeast(minimumVersion)
            verifiedExecutable = probed
            return probed
        }
    }

    private fun effectiveOptions(requestOptions: NonoExecutionOptions?): NonoExecutionOptions =
        (requestOptions ?: defaultExecutionOptions).withEnvironment(environmentOverrides)

    private fun parseDiagnostics(result: NonoExecutionResult): NonoExecutionResult {
        val parsed =
            JsonProtocols.parseDiagnostics(
                stderr = result.rawStderrText(),
                expectedExitCode = result.exitCode,
                stderrTruncated = result.stderrTruncated,
            )
        return result.withDiagnostics(
            childStderr = parsed.stderrWithoutDiagnostics.toByteArray(StandardCharsets.UTF_8),
            report = parsed.report,
            protocolError = parsed.protocolError,
        )
    }

    private fun await(execution: NonoExecution): NonoExecutionResult =
        try {
            execution.result().toCompletableFuture().get()
        } catch (exception: InterruptedException) {
            execution.cancel()
            Thread.currentThread().interrupt()
            throw NonoProcessException("Interrupted while waiting for nono", exception)
        } catch (exception: ExecutionException) {
            throw rethrow(exception.cause ?: exception)
        }

    private fun requireSuccess(
        operation: String,
        result: NonoExecutionResult,
    ) {
        if (result.exitCode != 0) {
            throw NonoCommandFailedException(
                operation = operation,
                exitCode = result.exitCode,
                stderr = result.stderrText(),
            )
        }
    }

    public class Builder internal constructor() {
        private var executable: Path? = null
        private var minimumVersion: NonoVersion = MINIMUM_NONO_VERSION
        private var verifyOnBuild: Boolean = true
        private val environment = System.getenv().toMutableMap()
        private val environmentOverrides = linkedMapOf<String, String>()
        private var defaultExecutionOptions = NonoExecutionOptions.builder().build()
        private var ioExecutor: ExecutorService? = null
        private var scheduler: ScheduledExecutorService? = null

        public fun executable(value: Path): Builder =
            apply {
                executable = value
            }

        public fun minimumVersion(value: NonoVersion): Builder =
            apply {
                minimumVersion = value
            }

        public fun verifyOnBuild(value: Boolean): Builder =
            apply {
                verifyOnBuild = value
            }

        public fun environment(
            name: String,
            value: String,
        ): Builder =
            apply {
                require(name.isNotEmpty()) { "environment variable name must not be empty" }
                require('=' !in name) { "environment variable name must not contain '='" }
                require('\u0000' !in name && '\u0000' !in value) {
                    "environment variables must not contain NUL"
                }
                environment[name] = value
                environmentOverrides[name] = value
            }

        public fun defaultExecutionOptions(value: NonoExecutionOptions): Builder =
            apply {
                defaultExecutionOptions = value
            }

        public fun executors(
            ioExecutor: ExecutorService,
            scheduler: ScheduledExecutorService,
        ): Builder =
            apply {
                this.ioExecutor = ioExecutor
                this.scheduler = scheduler
            }

        public fun build(): NonoClient {
            require((ioExecutor == null) == (scheduler == null)) {
                "I/O and scheduler executors must be supplied together"
            }
            val resolver = ExecutableResolver(environment = environment.toMap())
            val executablePath = resolver.resolve(executable)
            val verified =
                if (verifyOnBuild) {
                    resolver.probe(executablePath).also {
                        it.version.requireAtLeast(minimumVersion)
                    }
                } else {
                    null
                }
            val engine =
                if (ioExecutor != null && scheduler != null) {
                    ProcessEngine(
                        ioExecutor = checkNotNull(ioExecutor),
                        scheduler = checkNotNull(scheduler),
                        ownsExecutors = false,
                    )
                } else {
                    ProcessEngine()
                }
            return NonoClient(
                executablePath = executablePath,
                minimumVersion = minimumVersion,
                resolver = resolver,
                processEngine = engine,
                defaultExecutionOptions = defaultExecutionOptions,
                environmentOverrides = environmentOverrides.toMap(),
                verifiedExecutable = verified,
            )
        }
    }

    public companion object {
        @JvmField
        public val MINIMUM_NONO_VERSION: NonoVersion = NonoVersion(0, 72, 0)

        @JvmStatic
        public fun builder(): Builder = Builder()

        private const val STRICT_DEPRECATION_EXIT = 2

        private fun rethrow(cause: Throwable): NonoException =
            when (cause) {
                is NonoException -> cause
                is CompletionException ->
                    cause.cause?.let(::rethrow)
                        ?: NonoProcessException("nono execution failed", cause)
                else -> NonoProcessException("nono execution failed", cause)
            }
    }
}

private class MappingExecution(
    private val delegate: NonoExecution,
    mapper: (NonoExecutionResult) -> NonoExecutionResult,
) : NonoExecution {
    private val mapped = delegate.result().thenApply(mapper)

    override val pid: Long
        get() = delegate.pid

    override fun result(): CompletionStage<NonoExecutionResult> = mapped

    override fun cancel(): Boolean = delegate.cancel()
}
