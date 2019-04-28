package zipkin2.storage.logzio;

import ch.qos.logback.classic.Logger;
import org.slf4j.LoggerFactory;
import zipkin2.Call;
import zipkin2.DependencyLink;
import zipkin2.Span;
import zipkin2.elasticsearch.internal.client.HttpCall;
import zipkin2.storage.GroupByTraceId;
import zipkin2.storage.QueryRequest;
import zipkin2.storage.SpanStore;
import zipkin2.storage.StrictTraceId;
import zipkin2.storage.logzio.client.Aggregation;
import zipkin2.storage.logzio.client.SearchCallFactory;
import zipkin2.storage.logzio.client.SearchRequest;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class LogzioSpanStore implements SpanStore {
    private final long namesLookback;
    private final boolean strictTraceId;
    private final SearchCallFactory search;
    private final Call.Mapper<List<Span>, List<List<Span>>> groupByTraceId;
    private static final Logger logger = (Logger) LoggerFactory.getLogger(LogzioSpanStore.class.getName());


    public LogzioSpanStore(LogzioStorage storage, String apiToken) {
        this.search = new SearchCallFactory(storage.http(), apiToken);
        this.strictTraceId = storage.isStrictTraceId();
        this.groupByTraceId = GroupByTraceId.create(strictTraceId);
        this.namesLookback = 7776000000L; //90 days
        logger.info("StrictTraceId=" + strictTraceId);
    }

    @Override
    public Call<List<List<Span>>> getTraces(QueryRequest request) {
        long endMillis = request.endTs();
        long beginMillis = endMillis - request.lookback();

        SearchRequest.Filters filters = new SearchRequest.Filters();
        filters.addRange(LogzioStorage.JSON_TIMESTAMP_FIELD, beginMillis, endMillis);
        if (request.serviceName() != null) {
            filters.addTerm(LogzioStorage.JSON_SERVICE_NAME_FIELD, request.serviceName());
        }

        if (request.spanName() != null) {
            filters.addTerm(LogzioStorage.JSON_NAME_FIELD, request.spanName());
        }

        for (Map.Entry<String, String> kv : request.annotationQuery().entrySet()) {
            if (kv.getValue().isEmpty()) {
                filters.addTerm(LogzioStorage.JSON_ANNOTATION, kv.getKey());
            } else {
                filters.addTerm(LogzioStorage.JSON_ANNOTATION, kv.getKey() + "=" + kv.getValue());
            }
        }

        if (request.minDuration() != null) {
            filters.addRange(LogzioStorage.JSON_DURATION_FIELD, request.minDuration(), request.maxDuration());
        }

        // We need to filter to traces that contain at least one span that matches the request,
        // but the zipkin API is supposed to order traces by first span, regardless of if it was
        // filtered or not. This is not possible without either multiple, heavyweight queries
        // or complex multiple indexing, defeating much of the elegance of using elasticsearch for this.
        // So we fudge and order on the first span among the filtered spans - in practice, there should
        // be no significant difference in user experience since span start times are usually very
        // close to each other in human time.
        Aggregation traceIdTimestamp =
                Aggregation.terms(LogzioStorage.JSON_TRACE_ID_FIELD, request.limit())
                        .addSubAggregation(Aggregation.min(LogzioStorage.JSON_TIMESTAMP_FIELD))
                        .orderBy(LogzioStorage.JSON_TIMESTAMP_FIELD, "desc");

        SearchRequest esRequest =
                SearchRequest.create().filters(filters).addAggregation(traceIdTimestamp);
        HttpCall<List<String>> traceIdsCall = search.newCall(esRequest, BodyConverters.KEYS);

        Call<List<List<Span>>> result =
                traceIdsCall.flatMap(new GetSpansByTraceId(search, beginMillis, endMillis)).map(groupByTraceId);
        // Elasticsearch lookup by trace ID is by the full 128-bit length, but there's still a chance of
        // clash on lower-64 bit. When strict trace ID is enabled, we only filter client-side on clash.
        return strictTraceId ? result.map(StrictTraceId.filterTraces(request)) : result;
    }

    @Override
    public Call<List<Span>> getTrace(String traceId) {
        traceId = Span.normalizeTraceId(traceId);
        if (!strictTraceId && traceId.length() == 32) traceId = traceId.substring(16);
        SearchRequest request = SearchRequest.create().term(LogzioStorage.JSON_TRACE_ID_FIELD, traceId);
        return search.newCall(request, BodyConverters.SPANS);
    }

    @Override
    public Call<List<String>> getServiceNames() {
        long endMillis = System.currentTimeMillis();
        long beginMillis = endMillis - namesLookback;

        // Service name queries include both local and remote endpoints. This is different than
        // Span name, as a span name can only be on a local endpoint.
        SearchRequest.Filters filters = new SearchRequest.Filters();
        filters.addRange(LogzioStorage.JSON_TIMESTAMP_FIELD, beginMillis, endMillis);
        SearchRequest request =
                SearchRequest.create()
                        .filters(filters)
                        .addAggregation(Aggregation.terms(LogzioStorage.JSON_SERVICE_NAME_FIELD, 1000))
                        .addAggregation(Aggregation.terms(LogzioStorage.JSON_REMOTE_SERVICE_NAME_FIELD, 1000));
        return search.newCall(request, BodyConverters.KEYS);
    }

    @Override
    public Call<List<String>> getSpanNames(String serviceName) {
        if (serviceName.isEmpty()) return Call.emptyList();

        long endMillis = System.currentTimeMillis();
        long beginMillis = endMillis - namesLookback;

        // A span name is only valid on a local endpoint, as a span name is defined locally
        SearchRequest.Filters filters =
                new SearchRequest.Filters()
                        .addRange(LogzioStorage.JSON_TIMESTAMP_FIELD, beginMillis, endMillis)
                        .addTerm(LogzioStorage.JSON_SERVICE_NAME_FIELD, serviceName.toLowerCase(Locale.ROOT));

        SearchRequest request =
                SearchRequest.create()
                        .filters(filters)
                        .addAggregation(Aggregation.terms(LogzioStorage.JSON_NAME_FIELD, 1000));

        return search.newCall(request, BodyConverters.KEYS);
    }

    @Override
    public Call<List<DependencyLink>> getDependencies(long endTs, long lookback) {
        logger.error("Zipkin-logz.io doesn't support dependencies analysis");
        return Call.emptyList();
    }

    static final class GetSpansByTraceId implements Call.FlatMapper<List<String>, List<Span>> {
        final SearchCallFactory search;
        private long beginMillis;
        private long endMillis;

        GetSpansByTraceId(SearchCallFactory search) {
            this.search = search;
        }

        public GetSpansByTraceId(SearchCallFactory search, long beginMillis, long endMillis) {
            this.search = search;
            this.beginMillis = beginMillis;
            this.endMillis = endMillis;
            long complement = TimeUnit.DAYS.toMillis(1) - (endMillis - beginMillis);
            if (complement > 0) { //search trace ID's for longer period (24 hours) to include spans that are out of the time range
                this.endMillis = Math.min(System.currentTimeMillis(), this.endMillis + Math.round(complement / 2));
                this.beginMillis -= TimeUnit.DAYS.toMillis(1) - (this.endMillis - this.beginMillis);
            }
        }


        @Override
        public Call<List<Span>> map(List<String> inputs) {
            if (inputs.isEmpty()) return Call.emptyList();
            SearchRequest.Filters filters = new SearchRequest.Filters();

            if (beginMillis != 0) {
                filters = new SearchRequest.Filters().addRange(LogzioStorage.JSON_TIMESTAMP_FIELD, beginMillis, endMillis);
            }
            filters.addTerms(LogzioStorage.JSON_TRACE_ID_FIELD, inputs);
            SearchRequest getTraces = SearchRequest.create().filters(filters);
            return search.newCall(getTraces, BodyConverters.SPANS);
        }

    }
}
