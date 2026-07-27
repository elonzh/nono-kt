package nono.elonzh.com.internal

import java.io.ByteArrayOutputStream

internal class BoundedByteAccumulator(
    private val limit: Int,
) {
    private val output = ByteArrayOutputStream(minOf(limit, DEFAULT_INITIAL_CAPACITY))
    private var truncated = false

    init {
        require(limit >= 0) { "capture limit must not be negative" }
    }

    @Synchronized
    fun append(
        bytes: ByteArray,
        offset: Int,
        length: Int,
    ) {
        require(offset >= 0 && length >= 0 && offset + length <= bytes.size) {
            "invalid byte range"
        }
        val remaining = limit - output.size()
        val retained = minOf(remaining, length)
        if (retained > 0) {
            output.write(bytes, offset, retained)
        }
        if (retained < length) {
            truncated = true
        }
    }

    @Synchronized
    fun snapshot(): ByteArray = output.toByteArray()

    @Synchronized
    fun isTruncated(): Boolean = truncated

    private companion object {
        private const val DEFAULT_INITIAL_CAPACITY = 8192
    }
}
