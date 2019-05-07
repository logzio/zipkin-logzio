import org.junit.*;
import org.mockserver.client.MockServerClient;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.HttpRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import zipkin2.Endpoint;
import zipkin2.Span;
import zipkin2.storage.logzio.ConsumerParams;
import zipkin2.storage.logzio.LogzioSpanConsumer;
import zipkin2.storage.logzio.LogzioStorage;
import zipkin2.storage.logzio.LogzioStorageParams;

import java.io.IOException;
import java.util.Collections;

import static org.mockserver.integration.ClientAndServer.startClientAndServer;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

public class LogzioSpanConsumerTest {

   private static final Logger logger = LoggerFactory.getLogger(LogzioSpanConsumerTest.class);
   private static final Endpoint LOCAL_ENDPOINT = Endpoint.newBuilder().serviceName("local").build();
   private static MockServerClient mockServerClient = null;
   private static ClientAndServer mockServer;
   private static LogzioStorageParams storageParams = new LogzioStorageParams();


   @BeforeClass
   public static void startMockServer() {
      logger.debug(LogzioStorage.ZIPKIN_LOGZIO_STORAGE_MSG + "starting mock server");
      mockServer = startClientAndServer(8070);

      mockServerClient = new MockServerClient("localhost", 8070);
      mockServerClient
              .when(request().withMethod("POST"))
              .respond(response().withStatusCode(200));
   }

   @BeforeClass
   public static void setup() {

      ConsumerParams consumerParams = new ConsumerParams();
      consumerParams.setAccountToken("notARealToken");
      consumerParams.setListenerUrl("http://127.0.0.1:8070");
      storageParams.setConsumerParams(consumerParams);
      storageParams.setApiToken("");

   }

   @AfterClass
   public static void stopMockServer() {
      logger.info(LogzioStorage.ZIPKIN_LOGZIO_STORAGE_MSG + "stoping mock server...");
      mockServer.stop();
   }

   @Test
   public void testConsumerAccept() {
      logger.info(LogzioStorage.ZIPKIN_LOGZIO_STORAGE_MSG + "Testing consumer accept..");
      Span sampleSpan = Span.newBuilder().traceId("1234567890abcdef").id("2").timestamp(1L).localEndpoint(LOCAL_ENDPOINT).kind(Span.Kind.CLIENT).build();
      LogzioStorage logzioStorage = LogzioStorage.newBuilder().config(storageParams).build();
      LogzioSpanConsumer consumer = (LogzioSpanConsumer) logzioStorage.spanConsumer();
      try {
      consumer.accept(Collections.singletonList(sampleSpan)).execute();
      } catch (IOException e) {
         Assert.fail(e.getMessage());
      }

      HttpRequest[] recordedRequests = mockServerClient.retrieveRecordedRequests(request().withMethod("POST"));
      Assert.assertEquals(recordedRequests.length,1);
      String body = recordedRequests[0].getBodyAsString();
      Assert.assertTrue(body.contains("\"" + "traceId" + "\":\"" + sampleSpan.traceId() + "\""));
      Assert.assertTrue(body.contains("\"" + "kind" + "\":\"" + Span.Kind.CLIENT + "\""));
      Assert.assertTrue(body.contains("\"" + "timestamp" + "\":" + 1));
   }

   @Test
   public void closeStorageTest() {
      logger.info(LogzioStorage.ZIPKIN_LOGZIO_STORAGE_MSG + "Testing storage close..");
      Span sampleSpan = Span.newBuilder().traceId("1234567890abcdef").id("2").timestamp(1L).localEndpoint(LOCAL_ENDPOINT).kind(Span.Kind.CLIENT).build();
      LogzioStorage logzioStorage = LogzioStorage.newBuilder().config(storageParams).build();
      LogzioSpanConsumer consumer = (LogzioSpanConsumer) logzioStorage.spanConsumer();
      logzioStorage.close();
      try {
         consumer.accept(Collections.singletonList(sampleSpan)).execute();
      } catch (IOException e) {
         Assert.fail(e.getMessage());
      } catch (IllegalStateException ex) {
         return;
      }
      Assert.fail("Send traces succeeded but storage was closed");
   }

   @Test
   public void interruptSenderCloseTest() {

      logger.info(LogzioStorage.ZIPKIN_LOGZIO_STORAGE_MSG + "Testing storage close..");
      Span sampleSpan = Span.newBuilder().traceId("1234567890abcdef").id("2").timestamp(1L).localEndpoint(LOCAL_ENDPOINT).kind(Span.Kind.CLIENT).build();
      LogzioStorage logzioStorage = LogzioStorage.newBuilder().config(storageParams).build();
      LogzioSpanConsumer consumer = (LogzioSpanConsumer) logzioStorage.spanConsumer();

      Thread storageThread = new Thread(() -> {
         try {
            for (int i = 0 ; i < 1000 ; i++) {
               consumer.accept(Collections.singletonList(sampleSpan));
            }
         } catch (IllegalStateException ex) {
            logger.info("storage is closed");
            return;
         }
         Assert.fail("sent msgs but storage was closed");
      });

      Thread closingThread = new Thread(logzioStorage::close);

      storageThread.start();
      closingThread.start();
      closingThread.interrupt();
   }

}
