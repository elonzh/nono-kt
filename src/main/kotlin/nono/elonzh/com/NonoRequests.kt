package nono.elonzh.com

public class NonoRunRequest private constructor(
    public val command: List<String>,
    public val options: NonoRunOptions,
) {
    public class Builder internal constructor(
        command: List<String>,
    ) {
        private val command = command.toList()
        private var options: NonoRunOptions = NonoRunOptions.builder().build()

        public fun options(value: NonoRunOptions): Builder =
            apply {
                options = value
            }

        public fun build(): NonoRunRequest {
            validateCommand(command)
            return NonoRunRequest(command = command, options = options)
        }
    }

    public companion object {
        @JvmStatic
        public fun builder(
            program: String,
            vararg arguments: String,
        ): Builder = Builder(listOf(program, *arguments))

        @JvmStatic
        public fun builder(command: List<String>): Builder = Builder(command)
    }
}

public class NonoCommandRequest private constructor(
    public val arguments: List<String>,
) {
    public class Builder internal constructor(
        arguments: List<String>,
    ) {
        private val arguments = arguments.toList()

        public fun build(): NonoCommandRequest {
            require(arguments.isNotEmpty()) { "nono arguments must not be empty" }
            arguments.forEach(::validateArgument)
            require(arguments.first().isNotBlank()) { "nono command must not be blank" }
            return NonoCommandRequest(arguments)
        }
    }

    public companion object {
        @JvmStatic
        public fun builder(vararg arguments: String): Builder = Builder(arguments.toList())

        @JvmStatic
        public fun builder(arguments: List<String>): Builder = Builder(arguments)
    }
}

private fun validateCommand(command: List<String>) {
    require(command.isNotEmpty()) { "child command must not be empty" }
    command.forEach(::validateArgument)
    require(command.first().isNotBlank()) { "child program must not be blank" }
}

private fun validateArgument(argument: String) {
    require('\u0000' !in argument) { "arguments must not contain NUL" }
}
