package nono.elonzh.com

import nono.elonzh.com.internal.ExecutableResolver
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ExecutableResolverTest {
    private val temporaryDirectories = mutableListOf<Path>()

    @AfterTest
    fun cleanUp() {
        temporaryDirectories.forEach { directory ->
            Files
                .walk(directory)
                .sorted(Comparator.reverseOrder())
                .forEach(Files::deleteIfExists)
        }
    }

    @Test
    fun `explicit executable wins over PATH`() {
        val explicit = executable("explicit/nono", "nono 0.69.0")
        val pathExecutable = executable("path/nono", "nono 0.70.0")

        val resolved =
            ExecutableResolver(environment = mapOf("PATH" to pathExecutable.parent.toString()))
                .resolve(explicit)

        assertEquals(explicit.toAbsolutePath().normalize(), resolved)
    }

    @Test
    fun `finds executable on PATH`() {
        val executable = executable("bin/nono", "nono 0.69.0")

        val resolved =
            ExecutableResolver(environment = mapOf("PATH" to executable.parent.toString()))
                .resolve(explicit = null)

        assertEquals(executable.toAbsolutePath().normalize(), resolved)
    }

    @Test
    fun `rejects missing executable`() {
        val emptyDirectory = temporaryDirectory().resolve("empty").createDirectories()

        assertFailsWith<NonoExecutableNotFoundException> {
            ExecutableResolver(environment = mapOf("PATH" to emptyDirectory.toString()))
                .resolve(explicit = null)
        }
    }

    @Test
    fun `probes executable version`() {
        val executable = executable("bin/nono", "nono 0.69.0")

        val result = ExecutableResolver().probe(executable)

        assertEquals(executable.toAbsolutePath().normalize(), result.path)
        assertEquals(NonoVersion(0, 69, 0), result.version)
    }

    private fun executable(
        relativePath: String,
        versionOutput: String,
    ): Path {
        val path = temporaryDirectory().resolve(relativePath)
        path.parent.createDirectories()
        Files.writeString(
            path,
            """
            #!/bin/sh
            if [ "${'$'}1" = "--version" ]; then
              printf '%s\n' '$versionOutput'
              exit 0
            fi
            exit 64
            """.trimIndent(),
        )
        check(path.toFile().setExecutable(true)) { "failed to make $path executable" }
        return path
    }

    private fun temporaryDirectory(): Path = createTempDirectory("nono-kt-resolver-").also(temporaryDirectories::add)
}
