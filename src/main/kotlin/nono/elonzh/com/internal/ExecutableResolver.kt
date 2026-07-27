package nono.elonzh.com.internal

import nono.elonzh.com.NonoExecutable
import nono.elonzh.com.NonoExecutableNotFoundException
import nono.elonzh.com.NonoExecutableProbeException
import nono.elonzh.com.NonoLaunchException
import nono.elonzh.com.NonoVersion
import java.io.File
import java.nio.file.Files
import java.nio.file.Path

internal class ExecutableResolver(
    private val environment: Map<String, String> = System.getenv(),
) {
    fun resolve(explicit: Path?): Path {
        if (explicit != null) {
            return explicit
                .toAbsolutePath()
                .normalize()
                .takeIf(::isUsable)
                ?: throw NonoExecutableNotFoundException(explicit.toString())
        }

        val executable =
            environment["PATH"]
                .orEmpty()
                .split(File.pathSeparatorChar)
                .asSequence()
                .filter(String::isNotEmpty)
                .map {
                    Path
                        .of(it)
                        .resolve(EXECUTABLE_NAME)
                        .toAbsolutePath()
                        .normalize()
                }.firstOrNull(::isUsable)
        return executable ?: throw NonoExecutableNotFoundException(requestedPath = null)
    }

    fun probe(path: Path): NonoExecutable {
        val resolved = resolve(path)
        val processBuilder = ProcessBuilder(resolved.toString(), "--version")
        processBuilder.environment().putAll(environment)
        val process =
            try {
                processBuilder.start()
            } catch (exception: Exception) {
                throw NonoLaunchException(
                    argv = listOf(resolved.toString(), "--version"),
                    cause = exception,
                )
            }
        val stdout = process.inputStream.bufferedReader().use { it.readText() }
        val stderr = process.errorStream.bufferedReader().use { it.readText() }
        val exitCode = process.waitFor()
        if (exitCode != 0) {
            throw NonoExecutableProbeException(
                executable = resolved.toString(),
                exitCode = exitCode,
                stderr = stderr,
            )
        }
        return NonoExecutable(path = resolved, version = NonoVersion.parse(stdout))
    }

    private fun isUsable(path: Path): Boolean = Files.isRegularFile(path) && Files.isExecutable(path)

    private companion object {
        private const val EXECUTABLE_NAME = "nono"
    }
}
