import io.logz.sender.com.google.gson.Gson;
import org.junit.Before;
import org.junit.Test;
import zipkin2.Call;
import zipkin2.DependencyLink;
import zipkin2.Span;
import zipkin2.storage.QueryRequest;
import zipkin2.storage.logzio.LogzioSpanStore;
import zipkin2.storage.logzio.LogzioStorage;
import zipkin2.storage.logzio.LogzioStorageParams;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class LogzioSpanStoreTest {
    List<String> hosts = java.util.Arrays.asList("https://api.logz.io/v1/search");
//    List<String> hosts = java.util.Arrays.asList("http://localhost:9200");
    String apiToken = "c9b842c7-8527-486f-82de-5bbd8fcb805a";
    LogzioStorageParams params = new LogzioStorageParams();
    LogzioStorage storage ;
    LogzioSpanStore spanStore ;


    @Before
    public void setup() {
        params.setApiToken(apiToken);
        storage = LogzioStorage.newBuilder().config(params).build();
        spanStore = new LogzioSpanStore(storage,apiToken);
    }

    @Test
    public void getTraceByIdTest() {
        try {
            Call<List<Span>> request = spanStore.getTrace("a190c8577f1b59fa");
            List<Span> result = request.execute();

            System.out.println(result.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    @Test
    public void getServiceNamesTest() {
        Call<List<String>> reqeust = spanStore.getServiceNames();
        try {
            List<String> serviceNames = reqeust.execute();
            for (String name : serviceNames) {
                System.out.println(name);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    @Test
    public void getSpanNamesTest() {
        Call<List<String>> reqeust = spanStore.getSpanNames("frontend");
        try {
            List<String> serviceNames = reqeust.execute();
            for (String name : serviceNames) {
                System.out.println(name);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    @Test
    public void test() {
        List<String> sd = new ArrayList<>();
        sd.add("sadgads");
        sd.add("sadgaasdgadsfg");
        Gson builder = new Gson();
        System.out.println(builder.toJson(sd));
    }

    @Test
    public void getTracesByRequestTest() {
      QueryRequest request = QueryRequest.newBuilder().endTs(1556023701000L).lookback(20*60*1000L).limit(1000).build();
        try {
            int traceCount = 0;
            List<List<Span>> response = spanStore.getTraces(request).execute();
            for (List<Span> list : response) {
//                for (Span span : list) {
//                    System.out.println(span.toString());
//                }
            traceCount += list.size();
            }
            System.out.println(traceCount);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}

