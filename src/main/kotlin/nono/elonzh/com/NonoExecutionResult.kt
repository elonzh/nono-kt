package nono.elonzh.com

import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.time.Duration

public class NonoExecutionResult internal constructor(
    public val exitCode: Int,
    stdout: ByteArray,
    stderr: ByteArray,
    rawStderr: ByteArray = stderr,
    public val duration: Duration,
    public val timedOut: Boolean,
    public val cancelled: Boolean,
    public val stdoutTruncated: Boolean,
    public val stderrTruncated: Boolean,
    public val diagnostics: NonoDiagnosticReport? = null,
    public val diagnosticsProtocolError: String? = null,
) {
    private val stdoutBytes = stdout.copyOf()
    private val stderrBytes = stderr.copyOf()
    private val rawStderrBytes = rawStderr.copyOf()

    public val stdout: ByteArray
        get() = stdoutBytes.copyOf()

    public val stderr: ByteArray
        get() = stderrBytes.copyOf()

    public val rawStderr: ByteArray
        get() = rawStderrBytes.copyOf()

    @JvmOverloads
    public fun stdoutText(charset: Charset = StandardCharsets.UTF_8): String = stdoutBytes.toString(charset)

    @JvmOverloads
    public fun stderrText(charset: Charset = StandardCharsets.UTF_8): String = stderrBytes.toString(charset)

    @JvmOverloads
    public fun rawStderrText(charset: Charset = StandardCharsets.UTF_8): String = rawStderrBytes.toString(charset)

    internal fun withDiagnostics(
        childStderr: ByteArray,
        report: NonoDiagnosticReport?,
        protocolError: String?,
    ): NonoExecutionResult =
        NonoExecutionResult(
            exitCode = exitCode,
            stdout = stdoutBytes,
            stderr = childStderr,
            rawStderr = rawStderrBytes,
            duration = duration,
            timedOut = timedOut,
            cancelled = cancelled,
            stdoutTruncated = stdoutTruncated,
            stderrTruncated = stderrTruncated,
            diagnostics = report,
            diagnosticsProtocolError = protocolError,
        )
}
