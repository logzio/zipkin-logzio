package zipkin2.storage.logzio;

import com.google.auto.value.extension.memoized.Memoized;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import zipkin2.elasticsearch.internal.client.HttpCall;
import zipkin2.storage.SpanConsumer;
import zipkin2.storage.SpanStore;
import zipkin2.storage.StorageComponent;

import java.util.List;

public final class LogzioStorage extends StorageComponent {


    private static final int MAX_HTTP_REQUESTS = 64;
    public static final String JSON_TIMESTAMP_MILLIS_FIELD = "timestamp_millis";
    public static final String JSON_ANNOTATION = "_q";
    public static final String JSON_TIMESTAMP_FIELD = "@timestamp";
    public static final String JSON_SERVICE_NAME_FIELD = "localEndpoint.serviceName";
    public static final String JSON_NAME_FIELD = "name";
    public static final String JSON_DURATION_FIELD = "duration";
    public static final String JSON_TRACE_ID_FIELD = "traceId";
    public static final String JSON_REMOTE_SERVICE_NAME_FIELD = "remoteEndpoint.serviceName";
    private volatile boolean closeCalled;
    private static final Logger logger = LoggerFactory.getLogger(LogzioStorage.class);
    private final LogzioSpanConsumer spanConsumer;
    private final LogzioSpanStore spanStore;
    private boolean strictTraceId;
    private List<String> LOGZIO_HOST_AS_LIST = java.util.Arrays.asList("https://api.logz.io/v1/search");


    public static Builder newBuilder() {
        return new Builder();
    }

    private LogzioStorage(LogzioStorageParams config) {
        this.strictTraceId = config.isStrictTraceId();

        if (!config.getConsumerParams().getToken().isEmpty()) {
            this.spanConsumer = new LogzioSpanConsumer(config.getConsumerParams());
        } else {
            logger.warn("logz.io token was not supplied, couldn't generate span consumer (traces will not be stored)");
            this.spanConsumer = null;
        }
        if (!config.getApiToken().isEmpty()) {
            if (config.getSearchURL() != null) {
                LOGZIO_HOST_AS_LIST = java.util.Arrays.asList(config.getSearchURL());
            }
            this.spanStore = new LogzioSpanStore(this, config.getApiToken());
        } else {
            logger.warn("API token was not supplied, couldn't generate span store (traces will be stored but not shown)");
            this.spanStore = null;
        }
    }

    public SpanStore spanStore() {
        if (this.spanStore == null) {
            throw new IllegalArgumentException("API token was not supplied, couldn't generate span store");
        }
        return this.spanStore;
    }

    public SpanConsumer spanConsumer() {
        return this.spanConsumer;
    }

    public boolean isStrictTraceId() {
        return strictTraceId;
    }

    @Memoized
    public // hosts resolution might imply a network call, and we might make a new okhttp instance
    HttpCall.Factory http() {
        List<String> hosts = LOGZIO_HOST_AS_LIST;
        OkHttpClient ok =
                hosts.size() == 1
                        ? client()
                        : client()
                        .newBuilder()
                        .dns(PseudoAddressRecordSet.create(hosts, client().dns()))
                        .build();
        ok.dispatcher().setMaxRequests(MAX_HTTP_REQUESTS);
        ok.dispatcher().setMaxRequestsPerHost(MAX_HTTP_REQUESTS);
        return new HttpCall.Factory(ok, HttpUrl.parse(hosts.get(0)));
    }

    private OkHttpClient client() {
        return new OkHttpClient();
    }

    @Override
    public synchronized void close() {
        if (closeCalled) return;
        if (spanConsumer != null) {
            spanConsumer.close();
        }
        closeCalled = true;
        http().close();
    }

    public static final class Builder extends StorageComponent.Builder {
        ConsumerParams consumerParams;
        String apiToken = "";
        boolean strictTraceId = true;
        private String searchURL;

        @Override
        public Builder strictTraceId(boolean strictTraceId) {
            this.strictTraceId = strictTraceId;
            return this;
        }

        /**
         * Ignored since is mandatory
         *
         * @param searchEnabled
         */
        @Override
        public Builder searchEnabled(boolean searchEnabled) {
            return this;
        }

        public Builder token(String token) {
            consumerParams.setToken(token);
            return this;
        }

        public Builder listenerURL(String url) {
            consumerParams.setUrl(url);
            return this;
        }

        public Builder config(LogzioStorageParams storageParams) {
            if (storageParams == null) throw new IllegalArgumentException("consumerParams == null");
            this.consumerParams = storageParams.getConsumerParams();
            this.apiToken = storageParams.getApiToken();
            this.searchURL = storageParams.getSearchURL();
            this.strictTraceId = storageParams.isStrictTraceId();
            return this;
        }

        @Override
        public LogzioStorage build() {
            if (consumerParams.getToken().isEmpty() && apiToken.isEmpty()) {
                throw new IllegalArgumentException("At least one of logz.io token or api-token has to be valid");
            }
            LogzioStorageParams storageParams = new LogzioStorageParams();
            storageParams.setApiToken(apiToken);
            storageParams.setSearchURL(searchURL);
            storageParams.setConsumerParams(consumerParams);
            storageParams.setStrictTraceId(strictTraceId);
            return new LogzioStorage(storageParams);
        }

        Builder() {
        }
    }
}
