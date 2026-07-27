package nono.elonzh.com

import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.time.Duration

public class NonoExecutionResult internal constructor(
    public val exitCode: Int,
    stdout: ByteArray,
    stderr: ByteArray,
    public val duration: Duration,
    public val timedOut: Boolean,
    public val cancelled: Boolean,
    public val stdoutTruncated: Boolean,
    public val stderrTruncated: Boolean,
) {
    private val stdoutBytes = stdout.copyOf()
    private val stderrBytes = stderr.copyOf()

    public val stdout: ByteArray
        get() = stdoutBytes.copyOf()

    public val stderr: ByteArray
        get() = stderrBytes.copyOf()

    @JvmOverloads
    public fun stdoutText(charset: Charset = StandardCharsets.UTF_8): String = stdoutBytes.toString(charset)

    @JvmOverloads
    public fun stderrText(charset: Charset = StandardCharsets.UTF_8): String = stderrBytes.toString(charset)
}
