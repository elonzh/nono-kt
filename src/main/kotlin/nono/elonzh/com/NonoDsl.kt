package nono.elonzh.com

public inline fun nonoRun(
    command: List<String>,
    block: NonoRunOptions.Builder.() -> Unit = {},
): NonoRunRequest {
    val options = NonoRunOptions.builder().apply(block).build()
    return NonoRunRequest.builder(command).options(options).build()
}

public inline fun nonoClient(block: NonoClient.Builder.() -> Unit): NonoClient = NonoClient.builder().apply(block).build()
