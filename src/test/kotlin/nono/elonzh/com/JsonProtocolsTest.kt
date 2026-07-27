package nono.elonzh.com

import nono.elonzh.com.internal.JsonProtocols
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class JsonProtocolsTest {
    @Test
    fun `extracts merged diagnostics from stderr tail`() {
        val json =
            """
            {
              "session": {
                "exit_code": 1,
                "denials": [],
                "ipc_denials": [],
                "violations": [],
                "diagnostics": [
                  {
                    "code": "network_blocked",
                    "severity": "error",
                    "message": "blocked",
                    "future_field": true
                  }
                ],
                "future_session_field": "kept"
              },
              "proxy": [
                {
                  "code": "credential_not_found",
                  "severity": "warning",
                  "message": "missing"
                }
              ],
              "future_root_field": 1
            }
            """.trimIndent()

        val parsed = JsonProtocols.parseDiagnostics("child error\n$json\n")
        val report = assertNotNull(parsed.report)

        assertEquals("child error\n", parsed.stderrWithoutDiagnostics)
        assertEquals(1, report.exitCode)
        assertEquals("network_blocked", report.diagnostics.single().code)
        assertEquals("error", report.diagnostics.single().severity)
        assertEquals("blocked", report.diagnostics.single().message)
        assertEquals("credential_not_found", report.proxyDiagnostics.single().code)
        assertEquals(json, report.rawJson)
        assertNull(parsed.protocolError)
    }

    @Test
    fun `diagnostic scanner handles braces inside strings`() {
        val json =
            """
            {
              "session": {
                "exit_code": 2,
                "denials": [],
                "ipc_denials": [],
                "violations": [],
                "diagnostics": [
                  {"code":"application_failure","severity":"info","message":"value {nested} stays text"}
                ]
              }
            }
            """.trimIndent()

        val parsed = JsonProtocols.parseDiagnostics("prefix\n$json")

        assertEquals(2, assertNotNull(parsed.report).exitCode)
        assertEquals("prefix\n", parsed.stderrWithoutDiagnostics)
    }

    @Test
    fun `preserves stderr when no diagnostic object exists`() {
        val stderr = "child emitted {not diagnostics}\n"

        val parsed = JsonProtocols.parseDiagnostics(stderr)

        assertNull(parsed.report)
        assertEquals(stderr, parsed.stderrWithoutDiagnostics)
    }

    @Test
    fun `reports malformed trailing diagnostic candidate`() {
        val stderr = "child error\n{\"session\":{\"exit_code\":1,}}\n"

        val parsed = JsonProtocols.parseDiagnostics(stderr)

        assertNull(parsed.report)
        assertEquals(stderr, parsed.stderrWithoutDiagnostics)
        assertNotNull(parsed.protocolError)
    }

    @Test
    fun `does not mistake child JSON for diagnostics`() {
        val stderr = "{\"status\":\"failed\",\"code\":1}\n"

        val parsed = JsonProtocols.parseDiagnostics(stderr)

        assertNull(parsed.report)
        assertNull(parsed.protocolError)
        assertEquals(stderr, parsed.stderrWithoutDiagnostics)
    }

    @Test
    fun `parses profile list and keeps entry JSON`() {
        val json =
            """[{"name":"default","source":"built-in","pack":null,"description":"Base","extends":[],"future":1}]"""

        val profiles = JsonProtocols.parseProfileList(json)

        assertEquals(1, profiles.size)
        assertEquals("default", profiles.single().name)
        assertEquals("built-in", profiles.single().source)
        assertNull(profiles.single().pack)
        assertEquals("Base", profiles.single().description)
        assertEquals(emptyList(), profiles.single().extends)
        assertTrue(profiles.single().rawJson.contains("\"future\":1"))
    }

    @Test
    fun `parses resolved profile while retaining unknown fields`() {
        val json =
            """{"name":"locked","description":"Locked","extends":["default"],"filesystem":{"read":["/src"]},"future":true}"""

        val profile = JsonProtocols.parseProfile(json)

        assertEquals("locked", profile.name)
        assertEquals("Locked", profile.description)
        assertEquals(listOf("default"), profile.extends)
        assertEquals(json, profile.rawJson)
    }

    @Test
    fun `parses invalid profile validation as data`() {
        val json =
            """{"file":"/tmp/profile.json","valid":false,"errors":["bad"],"warnings":["old"],"deprecated_keys":2}"""

        val validation = JsonProtocols.parseValidation(json, exitCode = 1)

        assertFalse(validation.valid)
        assertEquals(listOf("bad"), validation.errors)
        assertEquals(listOf("old"), validation.warnings)
        assertEquals(2, validation.deprecatedKeys)
        assertFalse(validation.strictDeprecationFailure)
        assertEquals(json, validation.rawJson)
    }

    @Test
    fun `represents strict deprecation exit two without a launch error`() {
        val json =
            """{"file":"/tmp/profile.json","valid":true,"errors":[],"warnings":[],"deprecated_keys":1}"""

        val validation = JsonProtocols.parseValidation(json, exitCode = 2)

        assertTrue(validation.valid)
        assertTrue(validation.strictDeprecationFailure)
    }
}
