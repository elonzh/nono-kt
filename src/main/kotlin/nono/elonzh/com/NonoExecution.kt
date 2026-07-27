package nono.elonzh.com

import java.util.concurrent.CompletionStage

public interface NonoExecution {
    public val pid: Long

    public fun result(): CompletionStage<NonoExecutionResult>

    public fun cancel(): Boolean
}
