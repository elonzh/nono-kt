package nono.elonzh.com

import java.nio.file.Path

public data class NonoExecutable(
    public val path: Path,
    public val version: NonoVersion,
)
