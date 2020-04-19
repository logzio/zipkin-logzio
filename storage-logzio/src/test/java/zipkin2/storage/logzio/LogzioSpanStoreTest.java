package zipkin2.storage.logzio;

import okhttp3.Headers;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import zipkin2.Span;
import zipkin2.storage.logzio.client.HttpCall;
import zipkin2.storage.logzio.client.SearchCallFactory;
import zipkin2.storage.logzio.client.SearchRequest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class LogzioSpanStoreTest {
    @Rule public MockWebServer mockWebServer = new MockWebServer();

    private static String apiToken = "not-a-real-api-token";
    private static LogzioStorageParams params = new LogzioStorageParams();
    private static LogzioStorage storage;
    private static LogzioSpanStore spanStore;

    @Before
    public void setup() {
        params.setApiToken(apiToken);
        params.setSearchApiUrl("http://127.0.0.1:" + mockWebServer.getPort());
        params.getConsumerParams().setAccountToken("");
    }

    @After
    public void close() {
        storage.close();
    }

    @Test
    public void doesntTruncateTraceIdByDefault() throws Exception {
        params.setStrictTraceId(true);
        storage = LogzioStorage.newBuilder().config(params).build();
        spanStore = new LogzioSpanStore(storage, apiToken);
        mockWebServer.enqueue(new MockResponse());
        spanStore.getTrace("48fec942f3e78b893041d36dc43227fd").execute();

        assertThat(mockWebServer.takeRequest().getBody().readUtf8())
            .contains("\"traceId\":\"48fec942f3e78b893041d36dc43227fd\"");
    }

    @Test
    public void truncatesTraceIdTo16CharsWhenNtStrict() throws Exception {
        params.setStrictTraceId(false);
        storage = LogzioStorage.newBuilder().config(params).build();
        spanStore = new LogzioSpanStore(storage, apiToken);
        mockWebServer.enqueue(new MockResponse());
        spanStore.getTrace("48fec942f3e78b893041d36dc43227fd").execute();

        assertThat(mockWebServer.takeRequest().getBody().readUtf8())
            .contains("\"traceId\":\"3041d36dc43227fd\"");
    }

    @Test
    public void newHttpCallHeaderTest() {
        storage = LogzioStorage.newBuilder().config(params).build();
        spanStore = new LogzioSpanStore(storage, apiToken);
        SearchCallFactory searchCallFactory = new SearchCallFactory(storage.http(), apiToken);
        HttpCall<List<Span>> call = searchCallFactory.newCall(SearchRequest.create(), BodyConverters.SPANS);

        assertThat(call.call.request().headers()).isEqualTo(Headers.of(
            "Content-Type", "application/json",
            SearchCallFactory.API_TOKEN_HEADER, apiToken
        ));
    }

}


