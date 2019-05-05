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
        logger.info("Setting up test environment");
        try {
            mockWebServer.start(8123);
        } catch (IOException e) {
            e.printStackTrace();
        }
        params.setApiToken(apiToken);
        params.setSearchApiUrl("http://localhost:8123");
    }

    @AfterClass
    public static void close() throws Exception {
        mockWebServer.close();
        storage.close();
    }

    @Test
    public void doesntTruncateTraceIdByDefault() throws Exception {
        logger.info("Testing get trace");
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
        logger.info("Testing non strict trace ID search");
        params.setStrictTraceId(false);
        storage = LogzioStorage.newBuilder().config(params).build();
        spanStore = new LogzioSpanStore(storage,apiToken);
        mockWebServer.enqueue(new MockResponse());
        spanStore.getTrace("48fec942f3e78b893041d36dc43227fd").execute();

        Assert.assertTrue(mockWebServer.takeRequest().getBody().readUtf8().contains("\"traceId\":\"3041d36dc43227fd\""));
    }

//    @Test
//    public void getServiceNamesTest() {
//        Call<List<String>> reqeust = spanStore.getServiceNames();
//        try {
//            List<String> serviceNames = reqeust.execute();
//            for (String name : serviceNames) {
//                System.out.println(name);
//            }
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//
//    }
//
//    @Test
//    public void getSpanNamesTest() {
//        Call<List<String>> reqeust = spanStore.getSpanNames("frontend");
//        try {
//            List<String> serviceNames = reqeust.execute();
//            for (String name : serviceNames) {
//                System.out.println(name);
//            }
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }
//
//
//    @Test
//    public void test() {
//        List<String> sd = new ArrayList<>();
//        sd.add("sadgads");
//        sd.add("sadgaasdgadsfg");
//        Gson builder = new Gson();
//        System.out.println(builder.toJson(sd));
//    }
//
//    @Test
//    public void getTracesByRequestTest() {
//      QueryRequest request = QueryRequest.newBuilder().endTs(1556461811000L).lookback(12*60*60*1000L).limit(1000).build();
//        try {
//            int traceCount = 0;
//            List<List<Span>> response = spanStore.getTraces(request).execute();
//            for (List<Span> list : response) {
////                for (Span span : list) {
////                    System.out.println(span.toString());
////                }
//            traceCount += list.size();
//            }
//            System.out.println(traceCount);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }

    @Test
    public void newHttpCallHeaderTest() {
        logger.info("Testing token in search call header");
        storage = LogzioStorage.newBuilder().config(params).build();
        spanStore = new LogzioSpanStore(storage,apiToken);
        SearchCallFactory searchCallFactory = new SearchCallFactory(storage.http(),apiToken);
        HttpCall<List<Span>> call = searchCallFactory.newCall(SearchRequest.create(), BodyConverters.SPANS);
        Assert.assertEquals(call.call.request().header(SearchCallFactory.API_TOKEN_HEADER),apiToken);
    }

}


