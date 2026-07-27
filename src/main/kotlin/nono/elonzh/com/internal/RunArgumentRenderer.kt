package nono.elonzh.com.internal

import nono.elonzh.com.NonoCommandRequest
import nono.elonzh.com.NonoRunRequest
import java.nio.file.Path

internal object RunArgumentRenderer {
    fun render(
        executable: Path,
        request: NonoRunRequest,
    ): List<String> =
        buildList {
            add(executable.toString())
            add("run")
            addAll(request.options.arguments)
            add("--")
            addAll(request.command)
        }

    fun render(
        executable: Path,
        request: NonoCommandRequest,
    ): List<String> =
        buildList {
            add(executable.toString())
            addAll(request.arguments)
        }
}
