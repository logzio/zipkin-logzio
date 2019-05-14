package zipkin2.storage.logzio;

public class LogzioStorageParams {

    private String apiToken;

    private ConsumerParams consumerParams;
    private boolean strictTraceId = true;
    private String searchApiUrl;

    public LogzioStorageParams() {
        consumerParams = new ConsumerParams();
    }

    public ConsumerParams getConsumerParams() {
        return consumerParams;
    }

    @Override
    public String toString() {
        return "LogzioStorageConfig{" +
                consumerParams.toString() +
                " ApiToken=" + (apiToken.isEmpty() ? "" : "********" + apiToken.substring(apiToken.length()-4)) + "," +
                " StrictTracId=" + strictTraceId + "," +
                " ApiURL=" + searchApiUrl +
                "}";
    }

    public String getApiToken() {
        return apiToken;
    }

    public void setApiToken(String apiToken) {
        this.apiToken = apiToken;
    }

    public void setConsumerParams(ConsumerParams consumerParams) {
        this.consumerParams = consumerParams;
    }

    public void setStrictTraceId(boolean strictTraceId) {
        this.strictTraceId = strictTraceId;
    }

    public boolean isStrictTraceId() {
        return strictTraceId;
    }

    public void setSearchApiUrl(String url) {
        this.searchApiUrl = url;
    }

    public String getSearchApiUrl() {
        return searchApiUrl;
    }
}
