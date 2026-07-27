package example

import nono.elonzh.com.NonoClient
import nono.elonzh.com.nonoRun

fun main() {
    NonoClient.builder().build().use { client ->
        val result =
            client.run(
                nonoRun(listOf("printf", "hello from nono\n")) {
                    allowCurrentDirectory()
                    blockNetwork()
                },
            )
        print(result.stdoutText())
    }
}
