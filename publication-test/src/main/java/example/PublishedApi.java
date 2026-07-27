package example;

import nono.elonzh.com.NonoClient;
import nono.elonzh.com.NonoRunOptions;
import nono.elonzh.com.NonoRunRequest;

public final class PublishedApi {
    private PublishedApi() {}

    public static NonoClient createClient() {
        NonoRunOptions options = NonoRunOptions.builder().blockNetwork().build();
        NonoRunRequest.builder("printf", "published").options(options).build();
        return NonoClient.builder().verifyOnBuild(false).build();
    }
}
