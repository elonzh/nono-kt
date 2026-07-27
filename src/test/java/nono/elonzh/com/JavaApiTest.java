package nono.elonzh.com;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class JavaApiTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void JavaBuildersExecuteThroughNonoClient() throws Exception {
        Path fakeNono = temporaryDirectory.resolve("nono");
        Files.writeString(
                fakeNono,
                """
                #!/bin/sh
                if [ "$1" = "--version" ]; then
                  printf 'nono 0.69.0\\n'
                  exit 0
                fi
                if [ "$1" = "run" ]; then
                  printf 'java output'
                  exit 0
                fi
                if [ "$1 $2 $3" = "profile list --json" ]; then
                  printf '[{"name":"default","source":"builtin","pack":null,"description":"Default","extends":[],"error":null}]'
                  exit 0
                fi
                exit 64
                """);
        assertTrue(fakeNono.toFile().setExecutable(true));

        try (NonoClient client = NonoClient.builder().executable(fakeNono).build()) {
            NonoRunOptions options =
                    NonoRunOptions.builder().allowCurrentDirectory().blockNetwork().build();
            NonoRunRequest request =
                    NonoRunRequest.builder("printf", "hello").options(options).build();

            NonoExecutionResult result = client.run(request);
            NonoExecutionResult asyncResult =
                    client.start(request).result().toCompletableFuture().get();
            NonoProfileSummary profile = client.listProfiles().get(0);

            assertEquals(0, result.getExitCode());
            assertEquals("java output", result.stdoutText());
            assertEquals("java output", new String(result.getStdout()));
            assertEquals(0, asyncResult.getExitCode());
            assertEquals("default", profile.getName());
            assertEquals(new NonoVersion(0, 69, 0, null), client.getExecutable().getVersion());
        }
    }
}
