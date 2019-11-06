package zipkin2.storage.logzio;

import io.logz.sender.HttpsRequestConfiguration;
import io.logz.sender.LogzioSender;
import io.logz.sender.SenderStatusReporter;
import io.logz.sender.com.google.common.hash.Hashing;
import io.logz.sender.exceptions.LogzioParameterErrorException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class ConsumerParams {

    private static final Logger logger = LoggerFactory.getLogger(ConsumerParams.class);

    private String url;
    public static final String TYPE = "zipkinSpan";
    private String accountToken;
    private final int threadPoolSize = 3;
    private final boolean compressRequests = true;
    private ScheduledExecutorService senderExecutors;

    // Disk queue parameters
    private File queueDir;
    private final int fileSystemFullPercentThreshold = 98;
    private final int diskSpaceCheckInterval = 1000;
    private int senderDrainInterval;
    private int cleanSentTracesInterval;

    public void setQueueDir() {
        String tokenTypeSha = Hashing.sha256()
                .hashString(TYPE + accountToken, StandardCharsets.UTF_8)
                .toString();
        String queuePath = System.getProperty("user.dir");
        queuePath += queuePath.endsWith("/") ? "" : "/";
        queuePath += "logzio-storage" + tokenTypeSha;
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
        setQueueDir();
    }

    public ScheduledExecutorService getSenderExecutors() {
        return senderExecutors;
    }

    public LogzioSender getLogzioSender() {
        HttpsRequestConfiguration requestConf;
        try {
            requestConf = HttpsRequestConfiguration
                    .builder()
                    .setLogzioListenerUrl(getUrl())
                    .setLogzioType(this.TYPE)
                    .setLogzioToken(getAccountToken())
                    .setCompressRequests(this.compressRequests)
                    .build();
        } catch (LogzioParameterErrorException e) {
            logger.error(LogzioStorage.ZIPKIN_LOGZIO_STORAGE_MSG + "problem in one or more parameters with error {}", e.getMessage());
            return null;
        }
        senderExecutors = Executors.newScheduledThreadPool(this.threadPoolSize);
        SenderStatusReporter statusReporter = StatusReporterFactory.newSenderStatusReporter(LoggerFactory.getLogger(LogzioSender.class));
        LogzioSender.Builder senderBuilder = LogzioSender
                .builder()
                .setTasksExecutor(senderExecutors)
                .setReporter(statusReporter)
                .setHttpsRequestConfiguration(requestConf)
                .setDebug(true)
                .setDrainTimeoutSec(this.senderDrainInterval)
                .withDiskQueue()
                    .setQueueDir(this.queueDir)
                    .setCheckDiskSpaceInterval(this.diskSpaceCheckInterval)
                    .setFsPercentThreshold(this.fileSystemFullPercentThreshold)
                    .setGcPersistedQueueFilesIntervalSeconds(this.cleanSentTracesInterval)
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
                " account_token=" + (accountToken.isEmpty() ? "" : "********" + accountToken.substring(accountToken.length() - 4)) +
                " cleanSentTracesInterval=" + cleanSentTracesInterval +
                " senderDrainInterval=" + senderDrainInterval +
                '}';
    }

    public void setSenderDrainInterval(int senderDrainInterval) {
        this.senderDrainInterval = senderDrainInterval;
    }

    public void setCleanSentTracesInterval(int cleanSentTracesInterval) {
        this.cleanSentTracesInterval = cleanSentTracesInterval;
    }
}
