package zipkin2.storage.logzio;

import okhttp3.mockwebserver.MockWebServer;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import zipkin2.CheckResult;

import static org.assertj.core.api.Assertions.assertThat;

public class LogzioStorageTest {
    @Rule public MockWebServer mockWebServer = new MockWebServer();

    private static String apiToken = "not-a-real-api-token";
    private static LogzioStorageParams params = new LogzioStorageParams();
    private static LogzioStorage storage;

    @Before
    public void setup() {
        params.setApiToken(apiToken);
        params.setSearchApiUrl("http://127.0.0.1:" + mockWebServer.getPort());
        params.getConsumerParams().setAccountToken("");
        storage = LogzioStorage.newBuilder().config(params).build();
    }

    @After
    public void close() {
        storage.close();
    }

    @Test
    public void toString_includesUrl() {
        assertThat(storage.toString())
            .startsWith("LogzioStorage{logzioApiHost=http://127.0.0.1:");
    }

    @Test
    @Ignore("check() isn't implemented which makes /health and livenessProbes useless")
    public void check_downWhenDown() {
        assertThat(storage.check())
            .isNotEqualTo(CheckResult.OK);
    }
}


