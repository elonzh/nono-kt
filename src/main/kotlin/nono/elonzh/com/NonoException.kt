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
