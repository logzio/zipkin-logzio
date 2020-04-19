package zipkin.module.storage.logzio;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import zipkin2.storage.logzio.ConsumerParams;
import zipkin2.storage.logzio.LogzioStorage;
import zipkin2.storage.logzio.LogzioStorageParams;

import java.io.Serializable;

@ConfigurationProperties("zipkin.storage.logzio")
class ZipkinLogzioStorageProperties implements Serializable { // for Spark jobs
    private static final long serialVersionUID = 0L;
    private static final Logger logger = LoggerFactory.getLogger(ZipkinLogzioStorageProperties.class.getName());
    private static final String HTTPS_PREFIX = "https://";
    private static final String SEARCH_API_SUFFIX = "/v1/search";
    private static final String LISTENER_HTTPS_PORT = ":8071";

    private String logzioAccountToken;
    private String logzioApiToken;
    private String logzioListenerHost;
    private String logzioApiHost;
    private int senderDrainInterval;
    private int cleanSentTracesInterval;

    public void setLogzioAccountToken(String logzioAccountToken) {
        this.logzioAccountToken = logzioAccountToken;
    }

    public String getLogzioAccountToken() {
        return this.logzioAccountToken;
    }

    public void setLogzioApiToken(String logzioApiToken) {
        this.logzioApiToken = logzioApiToken;
    }

    public String getLogzioApiToken() {
        return this.logzioApiToken;
    }

    public String getLogzioListenerHost() {
        return logzioListenerHost;
    }

    public void setLogzioListenerHost(String logzioListenerHost) {
        this.logzioListenerHost = logzioListenerHost;
    }

    public String getLogzioApiHost() {
        return logzioApiHost;
    }

    public int getSenderDrainInterval() {
        return senderDrainInterval;
    }

    public void setSenderDrainInterval(int senderDrainInterval) {
        this.senderDrainInterval = senderDrainInterval;
    }

    public int getCleanSentTracesInterval() {
        return cleanSentTracesInterval;
    }

    public void setCleanSentTracesInterval(int cleanSentTracesInterval) {
        this.cleanSentTracesInterval = cleanSentTracesInterval;
    }

    public void setLogzioApiHost(String logzioApiHost) {
        this.logzioApiHost = logzioApiHost;
    }

    public LogzioStorage.Builder toBuilder() {
        LogzioStorageParams config = new LogzioStorageParams();
        ConsumerParams consumerParams = new ConsumerParams();
        consumerParams.setListenerUrl(HTTPS_PREFIX + logzioListenerHost + LISTENER_HTTPS_PORT);
        consumerParams.setAccountToken(logzioAccountToken);
        consumerParams.setSenderDrainInterval(senderDrainInterval);
        consumerParams.setCleanSentTracesInterval(cleanSentTracesInterval);
        config.setConsumerParams(consumerParams);
        config.setApiToken(logzioApiToken);
        config.setSearchApiUrl(HTTPS_PREFIX + logzioApiHost + SEARCH_API_SUFFIX);
        logger.info(LogzioStorage.ZIPKIN_LOGZIO_STORAGE_MSG + "config " + config.toString());
        return LogzioStorage.newBuilder().config(config);
    }


}

