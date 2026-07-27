package nono.elonzh.com

public sealed class NonoException(
    message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause)

public class NonoProtocolException(
    message: String,
    cause: Throwable? = null,
) : NonoException(message, cause)

public class NonoCompatibilityException(
    public val required: NonoVersion,
    public val actual: NonoVersion,
) : NonoException("nono $required or newer is required, but $actual is installed")

public class NonoExecutableNotFoundException(
    public val requestedPath: String?,
) : NonoException(
        requestedPath?.let { "nono executable is not usable: $it" }
            ?: "nono executable was not found on PATH",
    )

public class NonoExecutableProbeException(
    public val executable: String,
    public val exitCode: Int,
    public val stderr: String,
) : NonoException(
        "Failed to probe nono executable $executable (exit $exitCode): ${stderr.trim()}",
    )

public class NonoLaunchException(
    public val argv: List<String>,
    cause: Throwable,
) : NonoException("Failed to launch nono executable ${argv.firstOrNull().orEmpty()}", cause)

public class NonoProcessException(
    message: String,
    cause: Throwable,
) : NonoException(message, cause)

public class NonoCommandFailedException(
    public val operation: String,
    public val exitCode: Int,
    public val stderr: String,
) : NonoException("nono $operation failed with exit code $exitCode: ${stderr.trim()}")
