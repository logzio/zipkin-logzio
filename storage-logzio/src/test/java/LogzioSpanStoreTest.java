import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import zipkin2.Span;
import zipkin2.elasticsearch.internal.client.HttpCall;
import zipkin2.storage.logzio.BodyConverters;
import zipkin2.storage.logzio.LogzioSpanStore;
import zipkin2.storage.logzio.LogzioStorage;
import zipkin2.storage.logzio.LogzioStorageParams;
import zipkin2.storage.logzio.client.SearchCallFactory;
import zipkin2.storage.logzio.client.SearchRequest;

import java.io.IOException;
import java.util.List;


public class LogzioSpanStoreTest {
    private static MockWebServer mockWebServer = new MockWebServer();

    private static final Logger logger = LoggerFactory.getLogger(LogzioSpanStoreTest.class);

    private static String apiToken = "not-a-real-api-token";
    private static LogzioStorageParams params = new LogzioStorageParams();
    private static LogzioStorage storage ;
    private static LogzioSpanStore spanStore ;

    @BeforeClass
    public static void setup() {
        logger.info(LogzioStorage.ZIPKIN_LOGZIO_STORAGE_MSG + "Setting up test environment");
        try {
            mockWebServer.start(8123);
        } catch (IOException e) {
            e.printStackTrace();
        }
        params.setApiToken(apiToken);
        params.setSearchApiUrl("http://localhost:8123");
        params.getConsumerParams().setAccountToken("");
    }

    @AfterClass
    public static void close() throws Exception {
        mockWebServer.close();
        storage.close();
    }

    @Test
    public void doesntTruncateTraceIdByDefault() throws Exception {
        logger.info(LogzioStorage.ZIPKIN_LOGZIO_STORAGE_MSG + "Testing get trace");
        params.setStrictTraceId(true);
        storage = LogzioStorage.newBuilder().config(params).build();
        spanStore = new LogzioSpanStore(storage,apiToken);
        mockWebServer.enqueue(new MockResponse());
        spanStore.getTrace("48fec942f3e78b893041d36dc43227fd").execute();
        RecordedRequest request = mockWebServer.takeRequest();
        String body = request.getBody().readUtf8();
        Assert.assertTrue(body.contains("\"traceId\":\"48fec942f3e78b893041d36dc43227fd\""));
    }

    @Test
    public void truncatesTraceIdTo16CharsWhenNtStrict() throws Exception {
        logger.info(LogzioStorage.ZIPKIN_LOGZIO_STORAGE_MSG + "Testing non strict trace ID search");
        params.setStrictTraceId(false);
        storage = LogzioStorage.newBuilder().config(params).build();
        spanStore = new LogzioSpanStore(storage,apiToken);
        mockWebServer.enqueue(new MockResponse());
        spanStore.getTrace("48fec942f3e78b893041d36dc43227fd").execute();

        Assert.assertTrue(mockWebServer.takeRequest().getBody().readUtf8().contains("\"traceId\":\"3041d36dc43227fd\""));
    }

    @Test
    public void newHttpCallHeaderTest() {
        logger.info(LogzioStorage.ZIPKIN_LOGZIO_STORAGE_MSG + "Testing api token in search call header");
        storage = LogzioStorage.newBuilder().config(params).build();
        spanStore = new LogzioSpanStore(storage,apiToken);
        SearchCallFactory searchCallFactory = new SearchCallFactory(storage.http(),apiToken);
        HttpCall<List<Span>> call = searchCallFactory.newCall(SearchRequest.create(), BodyConverters.SPANS);
        Assert.assertEquals(call.call.request().header(SearchCallFactory.API_TOKEN_HEADER),apiToken);
    }

}


