package nono.elonzh.com.internal

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import nono.elonzh.com.NonoDiagnostic
import nono.elonzh.com.NonoDiagnosticReport
import nono.elonzh.com.NonoProfile
import nono.elonzh.com.NonoProfileSummary
import nono.elonzh.com.NonoProfileValidation
import nono.elonzh.com.NonoProtocolException

internal data class ParsedDiagnostics(
    val report: NonoDiagnosticReport?,
    val stderrWithoutDiagnostics: String,
    val protocolError: String?,
)

internal object JsonProtocols {
    private val json =
        Json {
            isLenient = false
            ignoreUnknownKeys = true
        }

    fun parseDiagnostics(stderr: String): ParsedDiagnostics {
        var protocolError: String? = null
        for (index in stderr.indices.reversed()) {
            if (stderr[index] != '{') {
                continue
            }
            val endExclusive = findJsonObjectEnd(stderr, index)
            val candidate =
                if (endExclusive == null) {
                    stderr.substring(index).trimEnd()
                } else {
                    stderr.substring(index, endExclusive)
                }
            val root =
                try {
                    json.parseToJsonElement(candidate) as? JsonObject
                } catch (exception: Exception) {
                    if ("\"session\"" in candidate) {
                        protocolError = "Malformed nono diagnostics JSON: ${exception.message}"
                    }
                    null
                } ?: continue

            val session = root["session"] as? JsonObject ?: continue
            val exitCode = session.int("exit_code") ?: continue
            return ParsedDiagnostics(
                report =
                    NonoDiagnosticReport(
                        exitCode = exitCode,
                        diagnostics = session.diagnostics("diagnostics"),
                        proxyDiagnostics = root.diagnostics("proxy"),
                        denialCount = session.arraySize("denials"),
                        ipcDenialCount = session.arraySize("ipc_denials"),
                        violationCount = session.arraySize("violations"),
                        rawJson = candidate,
                    ),
                stderrWithoutDiagnostics =
                    stderr.removeRange(
                        index,
                        consumeLineEnding(stderr, requireNotNull(endExclusive)),
                    ),
                protocolError = null,
            )
        }
        return ParsedDiagnostics(
            report = null,
            stderrWithoutDiagnostics = stderr,
            protocolError = protocolError,
        )
    }

    private fun findJsonObjectEnd(
        text: String,
        start: Int,
    ): Int? {
        var depth = 0
        var inString = false
        var escaped = false
        for (index in start until text.length) {
            val character = text[index]
            if (inString) {
                when {
                    escaped -> escaped = false
                    character == '\\' -> escaped = true
                    character == '"' -> inString = false
                }
                continue
            }
            when (character) {
                '"' -> inString = true
                '{' -> depth++
                '}' -> {
                    depth--
                    if (depth == 0) {
                        return index + 1
                    }
                }
            }
        }
        return null
    }

    private fun consumeLineEnding(
        text: String,
        index: Int,
    ): Int =
        when {
            text.startsWith("\r\n", index) -> index + 2
            text.startsWith("\n", index) -> index + 1
            else -> index
        }

    fun parseProfileList(stdout: String): List<NonoProfileSummary> {
        val array =
            parse(stdout, "profile list") as? JsonArray
                ?: throw NonoProtocolException("nono profile list did not return a JSON array")
        return array.map { element ->
            val value =
                element as? JsonObject
                    ?: throw NonoProtocolException("nono profile list entry was not an object")
            NonoProfileSummary(
                name = value.requiredString("name", "profile list"),
                source = value.requiredString("source", "profile list"),
                pack = value.string("pack"),
                description = value.string("description").orEmpty(),
                extends = value.stringList("extends"),
                error = value.string("error"),
                rawJson = element.toString(),
            )
        }
    }

    fun parseProfile(stdout: String): NonoProfile {
        val value =
            parse(stdout, "profile show") as? JsonObject
                ?: throw NonoProtocolException("nono profile show did not return a JSON object")
        return NonoProfile(
            name = value.requiredString("name", "profile show"),
            description = value.string("description").orEmpty(),
            extends = value.stringList("extends"),
            rawJson = stdout.trim(),
        )
    }

    fun parseValidation(
        stdout: String,
        exitCode: Int,
    ): NonoProfileValidation {
        val value =
            parse(stdout, "profile validate") as? JsonObject
                ?: throw NonoProtocolException("nono profile validate did not return a JSON object")
        val valid =
            (value["valid"] as? JsonPrimitive)?.booleanOrNull
                ?: throw NonoProtocolException("nono profile validate omitted boolean 'valid'")
        val deprecatedKeys = value.int("deprecated_keys") ?: 0
        return NonoProfileValidation(
            file = value.requiredString("file", "profile validate"),
            valid = valid,
            errors = value.stringList("errors"),
            warnings = value.stringList("warnings"),
            deprecatedKeys = deprecatedKeys,
            strictDeprecationFailure = exitCode == STRICT_DEPRECATION_EXIT && valid && deprecatedKeys > 0,
            rawJson = stdout.trim(),
        )
    }

    private fun parse(
        text: String,
        operation: String,
    ): JsonElement =
        try {
            json.parseToJsonElement(text)
        } catch (exception: Exception) {
            throw NonoProtocolException("Invalid JSON from nono $operation", exception)
        }

    private fun JsonObject.requiredString(
        key: String,
        operation: String,
    ): String =
        string(key)
            ?: throw NonoProtocolException("nono $operation omitted string '$key'")

    private fun JsonObject.string(key: String): String? = (get(key) as? JsonPrimitive)?.contentOrNull

    private fun JsonObject.int(key: String): Int? = (get(key) as? JsonPrimitive)?.intOrNull

    private fun JsonObject.stringList(key: String): List<String> =
        (get(key) as? JsonArray)
            ?.map { element ->
                (element as? JsonPrimitive)?.contentOrNull
                    ?: throw NonoProtocolException("nono JSON '$key' contained a non-string value")
            }.orEmpty()

    private fun JsonObject.arraySize(key: String): Int = (get(key) as? JsonArray)?.size ?: 0

    private fun JsonObject.diagnostics(key: String): List<NonoDiagnostic> =
        (get(key) as? JsonArray)
            ?.mapNotNull { element ->
                val value = element as? JsonObject ?: return@mapNotNull null
                val code = value.string("code") ?: return@mapNotNull null
                NonoDiagnostic(
                    code = code,
                    severity = value.string("severity").orEmpty(),
                    message = value.string("message").orEmpty(),
                    rawJson = element.toString(),
                )
            }.orEmpty()

    private const val STRICT_DEPRECATION_EXIT = 2
}
