package nono.elonzh.com.test

import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.createDirectories

internal class FakeNono private constructor(
    val executable: Path,
    val argumentsFile: Path,
) {
    fun environment(): Map<String, String> = mapOf("FAKE_NONO_ARGS_FILE" to argumentsFile.toString())

    fun recordedArguments(): List<String> =
        if (Files.exists(argumentsFile)) {
            Files.readAllLines(argumentsFile)
        } else {
            emptyList()
        }

    companion object {
        fun create(
            directory: Path,
            version: String = "0.72.0",
        ): FakeNono {
            directory.createDirectories()
            val executable = directory.resolve("nono")
            val argumentsFile = directory.resolve("arguments.txt")
            Files.writeString(executable, script(version))
            check(executable.toFile().setExecutable(true)) {
                "failed to make fake nono executable"
            }
            return FakeNono(executable = executable, argumentsFile = argumentsFile)
        }

        private fun script(version: String): String =
            """
            #!/bin/sh
            if [ -n "${'$'}{FAKE_NONO_ARGS_FILE:-}" ]; then
              : > "${'$'}FAKE_NONO_ARGS_FILE"
              for argument in "${'$'}@"; do
                printf '%s\n' "${'$'}argument" >> "${'$'}FAKE_NONO_ARGS_FILE"
              done
            fi

            if [ "${'$'}{1:-}" = "--version" ]; then
              printf '%s\n' 'nono $version'
              exit 0
            fi

            if [ "${'$'}{1:-}" = "profile" ]; then
              case "${'$'}{2:-}" in
                list)
                  printf '%s\n' '[{"name":"default","source":"built-in","pack":null,"description":"Base","extends":[]}]'
                  exit 0
                  ;;
                show)
                  printf '%s\n' '{"name":"locked","description":"Locked","extends":["default"],"filesystem":{"read":["/src"]}}'
                  exit 0
                  ;;
                validate)
                  printf '%s\n' '{"file":"/tmp/profile.json","valid":true,"errors":[],"warnings":[],"deprecated_keys":0}'
                  exit 0
                  ;;
              esac
            fi

            if [ "${'$'}{1:-}" != "run" ]; then
              printf 'unsupported fake nono command\n' >&2
              exit 64
            fi

            shift
            while [ "${'$'}#" -gt 0 ] && [ "${'$'}1" != "--" ]; do
              shift
            done
            if [ "${'$'}#" -gt 0 ]; then
              shift
            fi

            case "${'$'}{1:-}" in
              emit)
                printf '%s' "${'$'}{2:-}"
                printf '%s' "${'$'}{3:-}" >&2
                exit "${'$'}{4:-0}"
                ;;
              stdin)
                cat
                ;;
              pwd)
                pwd
                ;;
              environment)
                printf '%s' "${'$'}{FAKE_VALUE:-}"
                ;;
              sleep)
                printf 'started'
                sleep "${'$'}{2:-30}"
                printf 'finished'
                ;;
              spawn)
                sleep "${'$'}{2:-30}" &
                child_pid="${'$'}!"
                printf '%s\n' "${'$'}child_pid"
                wait "${'$'}child_pid"
                ;;
              diagnostics)
                printf 'child error\n' >&2
                printf '%s\n' '{"session":{"exit_code":1,"denials":[],"ipc_denials":[],"violations":[],"diagnostics":[{"code":"network_blocked","severity":"error","message":"blocked"}]},"proxy":[]}' >&2
                exit 1
                ;;
              *)
                printf 'unsupported fake child\n' >&2
                exit 65
                ;;
            esac
            """.trimIndent()
    }
}
