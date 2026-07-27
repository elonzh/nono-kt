package example;

import nono.elonzh.com.NonoClient;
import nono.elonzh.com.NonoExecutionResult;
import nono.elonzh.com.NonoRunOptions;
import nono.elonzh.com.NonoRunRequest;

public final class Main {
    private Main() {}

    public static void main(String[] args) {
        NonoRunOptions options =
                NonoRunOptions.builder().allowCurrentDirectory().blockNetwork().build();
        NonoRunRequest request =
                NonoRunRequest.builder("printf", "hello from nono\n").options(options).build();

        try (NonoClient client = NonoClient.builder().build()) {
            NonoExecutionResult result = client.run(request);
            System.out.print(result.stdoutText());
        }
    }
}
