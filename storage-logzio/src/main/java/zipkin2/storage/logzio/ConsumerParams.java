package zipkin2.storage.logzio;

import io.logz.sender.HttpsRequestConfiguration;
import io.logz.sender.LogzioSender;
import io.logz.sender.SenderStatusReporter;
import io.logz.sender.exceptions.LogzioParameterErrorException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.concurrent.Executors;

public class ConsumerParams {

    private static final Logger logger = LoggerFactory.getLogger(ConsumerParams.class);

    private String url;
    private final String type = "zipkinSpan";
    private String accountToken;
    private final int threadPoolSize = 3;
    private final boolean compressRequests = true;
    private final boolean fromDisk = true;

    // Disk queue parameters
    private File queueDir;
    private final int fileSystemFullPercentThreshold = 98;
    private final int gcPersistedQueueFilesIntervalSeconds = 30;
    private final int diskSpaceCheckInterval = 1000;

    public ConsumerParams() {
        String queuePath = System.getProperty("user.dir");
        queuePath += queuePath.endsWith("/") ? "" : "/";
        queuePath += "logzio-storage";
        this.queueDir = new File(queuePath);
    }

    public String getUrl() {
        return url;
    }

    public void setListenerUrl(String url) {
        this.url = url;
    }

    public String getAccountToken() {
        return accountToken;
    }

    public void setAccountToken(String accountToken) {
        this.accountToken = accountToken;
    }

    public LogzioSender getLogzioSender() {
        HttpsRequestConfiguration requestConf;
        try {
            requestConf = HttpsRequestConfiguration
                    .builder()
                    .setLogzioListenerUrl(getUrl())
                    .setLogzioType(this.type)
                    .setLogzioToken(getAccountToken())
                    .setCompressRequests(this.compressRequests)
                    .build();
        } catch (LogzioParameterErrorException e) {
            logger.error(LogzioStorage.ZIPKIN_LOGZIO_STORAGE_MSG + "problem in one or more parameters with error {}", e.getMessage());
            return null;
        }
        SenderStatusReporter statusReporter = StatusReporterFactory.newSenderStatusReporter(LoggerFactory.getLogger(LogzioSender.class));
        LogzioSender.Builder senderBuilder = LogzioSender
                .builder()
                .setTasksExecutor(Executors.newScheduledThreadPool(this.threadPoolSize))
                .setReporter(statusReporter)
                .setHttpsRequestConfiguration(requestConf)
                .setDebug(true)
                .withDiskQueue()
                .setQueueDir(this.queueDir)
                .setCheckDiskSpaceInterval(this.diskSpaceCheckInterval)
                .setFsPercentThreshold(this.fileSystemFullPercentThreshold)
                .setGcPersistedQueueFilesIntervalSeconds(this.gcPersistedQueueFilesIntervalSeconds)
                .endDiskQueue();
        try {
            return senderBuilder.build();
        } catch (LogzioParameterErrorException e) {
            logger.error(LogzioStorage.ZIPKIN_LOGZIO_STORAGE_MSG + "problem in one or more parameters with error {}", e.getMessage());
        }
        return null;
    }

    @Override
    public String toString() {
        return "SpanConsumerConfig{" +
                "listener_url='" + url + '\'' +
                "account_token=" + (accountToken.isEmpty() ? "" : "********" + accountToken.substring(accountToken.length()-4)) +
                '}';
    }
}
