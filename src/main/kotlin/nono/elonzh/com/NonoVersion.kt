package nono.elonzh.com

public data class NonoVersion(
    public val major: Int,
    public val minor: Int,
    public val patch: Int,
    public val preRelease: String? = null,
) : Comparable<NonoVersion> {
    init {
        require(major >= 0) { "major must not be negative" }
        require(minor >= 0) { "minor must not be negative" }
        require(patch >= 0) { "patch must not be negative" }
    }

    override fun compareTo(other: NonoVersion): Int {
        compareValues(major, other.major).takeIf { it != 0 }?.let { return it }
        compareValues(minor, other.minor).takeIf { it != 0 }?.let { return it }
        compareValues(patch, other.patch).takeIf { it != 0 }?.let { return it }
        return comparePreRelease(preRelease, other.preRelease)
    }

    public fun requireAtLeast(required: NonoVersion) {
        if (this < required) {
            throw NonoCompatibilityException(required = required, actual = this)
        }
    }

    override fun toString(): String =
        buildString {
            append(major)
            append('.')
            append(minor)
            append('.')
            append(patch)
            preRelease?.let {
                append('-')
                append(it)
            }
        }

    public companion object {
        private val versionPattern =
            Regex(
                """^(?:nono\s+)?(0|[1-9]\d*)\.(0|[1-9]\d*)\.(0|[1-9]\d*)(?:-([0-9A-Za-z-]+(?:\.[0-9A-Za-z-]+)*))?(?:\+[0-9A-Za-z-]+(?:\.[0-9A-Za-z-]+)*)?\s*$""",
            )

        @JvmStatic
        public fun parse(output: String): NonoVersion {
            val match =
                versionPattern.matchEntire(output)
                    ?: throw NonoProtocolException("Invalid nono version output: ${output.trim()}")
            val preRelease = match.groupValues[4].ifEmpty { null }
            validatePreRelease(preRelease, output)
            return NonoVersion(
                major = match.groupValues[1].toInt(),
                minor = match.groupValues[2].toInt(),
                patch = match.groupValues[3].toInt(),
                preRelease = preRelease,
            )
        }

        private fun validatePreRelease(
            preRelease: String?,
            output: String,
        ) {
            if (preRelease == null) {
                return
            }
            val invalidNumericIdentifier =
                preRelease
                    .split('.')
                    .any { identifier ->
                        identifier.length > 1 &&
                            identifier.startsWith('0') &&
                            identifier.all(Char::isDigit)
                    }
            if (invalidNumericIdentifier) {
                throw NonoProtocolException("Invalid nono version output: ${output.trim()}")
            }
        }

        private fun comparePreRelease(
            left: String?,
            right: String?,
        ): Int {
            if (left == null && right == null) {
                return 0
            }
            if (left == null) {
                return 1
            }
            if (right == null) {
                return -1
            }

            val leftParts = left.split('.')
            val rightParts = right.split('.')
            for (index in 0 until maxOf(leftParts.size, rightParts.size)) {
                val leftPart = leftParts.getOrNull(index) ?: return -1
                val rightPart = rightParts.getOrNull(index) ?: return 1
                compareIdentifier(leftPart, rightPart).takeIf { it != 0 }?.let { return it }
            }
            return 0
        }

        private fun compareIdentifier(
            left: String,
            right: String,
        ): Int {
            val leftNumeric = left.all(Char::isDigit)
            val rightNumeric = right.all(Char::isDigit)
            return when {
                leftNumeric && rightNumeric ->
                    compareValues(left.length, right.length)
                        .takeIf { it != 0 }
                        ?: left.compareTo(right)
                leftNumeric -> -1
                rightNumeric -> 1
                else -> left.compareTo(right)
            }
        }
    }
}
