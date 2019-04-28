import org.json.simple.JSONArray;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import zipkin2.Call;
import zipkin2.Span;
import zipkin2.codec.SpanBytesDecoder;
import zipkin2.storage.SpanConsumer;
import zipkin2.storage.logzio.ConsumerParams;
import zipkin2.storage.logzio.LogzioSpanConsumer;
import zipkin2.storage.logzio.LogzioStorage;
import zipkin2.storage.logzio.LogzioStorageParams;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class LogzioSpanConsumerTest {

   private static final Logger logger = LoggerFactory.getLogger(LogzioSpanConsumerTest.class);

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
//
   @Test
   public void testConsumer() {
      List<Span> spans = getSampleSpans();
      for (Span span : spans) {
         System.out.println(span.toString());
      }

      ConsumerParams consumerParams = new ConsumerParams();
      consumerParams.setToken("oCwtQDtWjDOMcHXHGGNrnRgkEMxCDuiO");
      consumerParams.setUrl("https://listener.logz.io:8071");
      LogzioStorageParams storageParams = new LogzioStorageParams();
      storageParams.setConsumerParams(consumerParams);

      LogzioStorage logzioStorage = LogzioStorage.newBuilder().config(storageParams).build();
      LogzioSpanConsumer consumer = (LogzioSpanConsumer) logzioStorage.spanConsumer();
      for (int i = 1; i < 1 ; i++) {
         Call<Void> callback = consumer.accept(spans);
         try {
            callback.execute();
         } catch (IOException e) {
            Assert.fail(e.getMessage());
         }
      }

      try {
         Thread.sleep(5000);
      } catch (InterruptedException e) {
         e.printStackTrace();
      }
   }
}
