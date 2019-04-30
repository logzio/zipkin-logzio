import org.json.simple.JSONArray;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.junit.*;
import org.mockserver.client.MockServerClient;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.HttpRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import zipkin2.Endpoint;
import zipkin2.Span;
import zipkin2.codec.SpanBytesDecoder;
import zipkin2.storage.logzio.ConsumerParams;
import zipkin2.storage.logzio.LogzioSpanConsumer;
import zipkin2.storage.logzio.LogzioStorage;
import zipkin2.storage.logzio.LogzioStorageParams;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.mockserver.integration.ClientAndServer.startClientAndServer;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

public class LogzioSpanConsumerTest {

   private static final Logger logger = LoggerFactory.getLogger(LogzioSpanConsumerTest.class);
   private static final Endpoint LOCAL_ENDPOINT = Endpoint.newBuilder().serviceName("local").build();
   private MockServerClient mockServerClient = null;
   private HttpRequest[] recordedRequests;
   private ClientAndServer mockServer;

   private List<Span> getSampleSpans() {
      List<Span> resultSpans = new ArrayList<Span>();
      JSONParser parser = new JSONParser();
      try {
         String spanSampleFile = LogzioSpanConsumerTest.class.getClassLoader().getResource("spansSample3.json").getFile();
         JSONArray spansJson = (JSONArray) parser.parse(new FileReader(spanSampleFile));
         for (Object span : spansJson) {
            resultSpans.add(SpanBytesDecoder.JSON_V2.decodeOne(span.toString().getBytes()));
         }
      } catch (ParseException e) {
         logger.error("can't parse file to json error: {}", e.getMessage());
      } catch (FileNotFoundException e) {
         logger.error("file not found: {}", e.getMessage());
      } catch (IOException e) {
         logger.error("error: {}", e.getMessage());
      }
      return resultSpans;
   }

   @Before
   public void startMockServer() {
      logger.debug("starting mock server");
      mockServer = startClientAndServer(8070);

      mockServerClient = new MockServerClient("localhost", 8070);
      mockServerClient
              .when(request().withMethod("POST"))
              .respond(response().withStatusCode(200));
   }

   @After
   public void stopMockServer() {
      logger.info("stoping mock server...");
      mockServer.stop();
   }

   @Test
   public void testConsumerAccept() {
      String traceId = "1234567890abcdef";
      Span sampleSpan = Span.newBuilder().traceId(traceId).id("2").timestamp(1L).localEndpoint(LOCAL_ENDPOINT).kind(Span.Kind.CLIENT).build();
      ConsumerParams consumerParams = new ConsumerParams();
      consumerParams.setToken("notARealToken");
      consumerParams.setUrl("http://127.0.0.1:8070");
      LogzioStorageParams storageParams = new LogzioStorageParams();
      storageParams.setConsumerParams(consumerParams);

      LogzioStorage logzioStorage = LogzioStorage.newBuilder().config(storageParams).build();
      LogzioSpanConsumer consumer = (LogzioSpanConsumer) logzioStorage.spanConsumer();
      try {
      consumer.accept(Arrays.asList(sampleSpan)).execute();
      } catch (IOException e) {
         Assert.fail(e.getMessage());
      }


      try {
         Thread.sleep(5000);
      } catch (InterruptedException e) {
         e.printStackTrace();
      }
      recordedRequests = mockServerClient.retrieveRecordedRequests(request().withMethod("POST"));
      Assert.assertEquals(recordedRequests.length,1);
      String body = recordedRequests[0].getBodyAsString();
      Assert.assertTrue(body.contains("\"" + "traceId" + "\":\"" + traceId + "\""));
      Assert.assertTrue(body.contains("\"" + "kind" + "\":\"" + Span.Kind.CLIENT + "\""));
      Assert.assertTrue(body.contains("\"" + "timestamp" + "\":" + 1));
   }

}
