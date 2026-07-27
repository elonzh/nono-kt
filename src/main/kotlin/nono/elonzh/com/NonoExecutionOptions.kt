package nono.elonzh.com

import java.nio.file.Path
import java.time.Duration

public fun interface NonoOutputListener {
    public fun onOutput(bytes: ByteArray)
}

public class NonoInput private constructor(
    internal val mode: Mode,
    bytes: ByteArray?,
) {
    internal val bytes: ByteArray? = bytes?.copyOf()

    internal enum class Mode {
        CLOSED,
        INHERIT,
        BYTES,
    }

    public companion object {
        @JvmStatic
        public fun closed(): NonoInput = NonoInput(mode = Mode.CLOSED, bytes = null)

        @JvmStatic
        public fun inherit(): NonoInput = NonoInput(mode = Mode.INHERIT, bytes = null)

        @JvmStatic
        public fun bytes(value: ByteArray): NonoInput = NonoInput(mode = Mode.BYTES, bytes = value)
    }
}

public class NonoOutput private constructor(
    internal val mode: Mode,
    internal val maxRetainedBytes: Int,
    internal val listener: NonoOutputListener?,
) {
    internal enum class Mode {
        CAPTURE,
        INHERIT,
        DISCARD,
        STREAM,
    }

    public companion object {
        public const val DEFAULT_MAX_CAPTURE_BYTES: Int = 16 * 1024 * 1024

        @JvmStatic
        @JvmOverloads
        public fun capture(maxBytes: Int = DEFAULT_MAX_CAPTURE_BYTES): NonoOutput {
            require(maxBytes >= 0) { "maximum captured bytes must not be negative" }
            return NonoOutput(mode = Mode.CAPTURE, maxRetainedBytes = maxBytes, listener = null)
        }

        @JvmStatic
        public fun inherit(): NonoOutput = NonoOutput(mode = Mode.INHERIT, maxRetainedBytes = 0, listener = null)

        @JvmStatic
        public fun discard(): NonoOutput = NonoOutput(mode = Mode.DISCARD, maxRetainedBytes = 0, listener = null)

        @JvmStatic
        @JvmOverloads
        public fun stream(
            listener: NonoOutputListener,
            maxRetainedBytes: Int = DEFAULT_MAX_CAPTURE_BYTES,
        ): NonoOutput {
            require(maxRetainedBytes >= 0) { "maximum retained bytes must not be negative" }
            return NonoOutput(
                mode = Mode.STREAM,
                maxRetainedBytes = maxRetainedBytes,
                listener = listener,
            )
        }
    }
}

public class NonoExecutionOptions private constructor(
    public val workingDirectory: Path?,
    public val environment: Map<String, String>,
    public val inheritEnvironment: Boolean,
    public val input: NonoInput,
    public val stdout: NonoOutput,
    public val stderr: NonoOutput,
    public val timeout: Duration?,
) {
    public class Builder internal constructor() {
        private var workingDirectory: Path? = null
        private val environment = linkedMapOf<String, String>()
        private var inheritEnvironment = true
        private var input = NonoInput.closed()
        private var stdout = NonoOutput.capture()
        private var stderr = NonoOutput.capture()
        private var timeout: Duration? = null

        public fun workingDirectory(value: Path): Builder =
            apply {
                workingDirectory = value
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
            }

        public fun clearEnvironment(): Builder =
            apply {
                inheritEnvironment = false
            }

        public fun input(value: NonoInput): Builder =
            apply {
                input = value
            }

        public fun stdout(value: NonoOutput): Builder =
            apply {
                stdout = value
            }

        public fun stderr(value: NonoOutput): Builder =
            apply {
                stderr = value
            }

        public fun timeout(value: Duration): Builder =
            apply {
                require(!value.isNegative && !value.isZero) { "timeout must be positive" }
                timeout = value
            }

        public fun build(): NonoExecutionOptions =
            NonoExecutionOptions(
                workingDirectory = workingDirectory,
                environment = environment.toMap(),
                inheritEnvironment = inheritEnvironment,
                input = input,
                stdout = stdout,
                stderr = stderr,
                timeout = timeout,
            )
    }

    public companion object {
        @JvmStatic
        public fun builder(): Builder = Builder()
    }
}
