package nono.elonzh.com

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class NonoVersionTest {
    @Test
    fun `parses current nono version output`() {
        assertEquals(
            NonoVersion(major = 0, minor = 69, patch = 0),
            NonoVersion.parse("nono 0.69.0\n"),
        )
    }

    @Test
    fun `parses plain prerelease version`() {
        assertEquals(
            NonoVersion(major = 1, minor = 2, patch = 3, preRelease = "rc.4"),
            NonoVersion.parse("1.2.3-rc.4"),
        )
    }

    @Test
    fun `orders numeric prerelease identifiers numerically`() {
        assertTrue(NonoVersion.parse("0.69.0-rc.2") < NonoVersion.parse("0.69.0-rc.10"))
    }

    @Test
    fun `orders prerelease before final release`() {
        assertTrue(NonoVersion.parse("0.69.0-rc.1") < NonoVersion.parse("0.69.0"))
    }

    @Test
    fun `rejects unrelated output`() {
        assertFailsWith<NonoProtocolException> {
            NonoVersion.parse("not-nono")
        }
    }

    @Test
    fun `reports incompatible installed version`() {
        val exception =
            assertFailsWith<NonoCompatibilityException> {
                NonoVersion(0, 68, 0).requireAtLeast(NonoVersion(0, 69, 0))
            }

        assertEquals(NonoVersion(0, 69, 0), exception.required)
        assertEquals(NonoVersion(0, 68, 0), exception.actual)
    }
}
