package zipkin2.storage.logzio;

import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import zipkin2.storage.SpanConsumer;
import zipkin2.storage.SpanStore;
import zipkin2.storage.StorageComponent;
import zipkin2.storage.logzio.client.HttpCall;

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
    public static final String ZIPKIN_LOGZIO_STORAGE_MSG = "[zipkin-logzio-storage] ";
    private volatile boolean closeCalled;
    private static final Logger logger = LoggerFactory.getLogger(LogzioStorage.class);
    private final LogzioSpanConsumer spanConsumer;
    private final LogzioSpanStore spanStore;
    private boolean strictTraceId;
    private String logzioApiHost;

    public static Builder newBuilder() {
        return new Builder();
    }

    private LogzioStorage(LogzioStorageParams config) {
        this.strictTraceId = config.isStrictTraceId();

        if (!config.getConsumerParams().getAccountToken().isEmpty()) {
            this.spanConsumer = new LogzioSpanConsumer(config.getConsumerParams());
        } else {
            logger.warn(LogzioStorage.ZIPKIN_LOGZIO_STORAGE_MSG + "logz.io account token was not supplied, couldn't generate span consumer (traces will not be stored)");
            this.spanConsumer = null;
        }
        if (!config.getApiToken().isEmpty()) {
            if (config.getSearchApiUrl() != null) {
                logzioApiHost = config.getSearchApiUrl();
            }
            this.spanStore = new  LogzioSpanStore(this, config.getApiToken());
        } else {
            logger.warn(LogzioStorage.ZIPKIN_LOGZIO_STORAGE_MSG + "logz.io API token was not supplied, couldn't generate span store (traces will be stored but not shown)");
            this.spanStore = null;
        }
    }

    public SpanStore spanStore() {
        if (this.spanStore == null) {
            throw new IllegalArgumentException("logz.io API token was not supplied, couldn't generate span store");
        }
        return this.spanStore;
    }

    public SpanConsumer spanConsumer() {
        return this.spanConsumer;
    }

    public boolean isStrictTraceId() {
        return strictTraceId;
    }

    volatile HttpCall.Factory http;

    // hosts resolution might imply a network call, and we might make a new okhttp instance
    HttpCall.Factory http() {
        if (http == null) {
            synchronized (this) {
                if (http == null) {
                    OkHttpClient ok = new OkHttpClient();
                    ok.dispatcher().setMaxRequests(MAX_HTTP_REQUESTS);
                    ok.dispatcher().setMaxRequestsPerHost(MAX_HTTP_REQUESTS);
                    http = new HttpCall.Factory(ok, HttpUrl.parse(logzioApiHost));
                }
            }
        }
        return http;
    }

    @Override
    public synchronized void close() {
        if (closeCalled) return;
        if (spanConsumer != null) {
            spanConsumer.close();
        }
        if (spanStore != null) {
            http().close();
        }
        closeCalled = true;

    }

    public static final class Builder extends StorageComponent.Builder {
        private LogzioStorageParams storageParams;

        @Override
        public Builder strictTraceId(boolean strictTraceId) {
            this.storageParams.setStrictTraceId(strictTraceId);
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

        public Builder config(LogzioStorageParams storageParams) {
            this.storageParams = storageParams;
            return this;
        }

        @Override
        public LogzioStorage build() {
            if (this.storageParams.getConsumerParams().getAccountToken().isEmpty() && this.storageParams.getApiToken().isEmpty()) {
                throw new IllegalArgumentException("At least one of logz.io account token or api-token has to be valid");
            }
            return new LogzioStorage(this.storageParams);
        }

        Builder() {
        }
    }
}
