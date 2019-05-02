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
    private String token = "";
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
        queuePath += "traces";
        this.queueDir = new File(queuePath);
    }

    public String getUrl() {
        return url;
    }

    public void setListenerUrl(String url) {
        this.url = url;
    }

    public String getType() {
        return type;
    }


    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public int getThreadPoolSize() {
        return threadPoolSize;
    }

    public boolean isCompressRequests() {
        return compressRequests;
    }

    public int getDiskSpaceCheckInterval() {
        return diskSpaceCheckInterval;
    }

    public File getQueueDir() {
        return queueDir;
    }

    public int getFileSystemFullPercentThreshold() {
        return fileSystemFullPercentThreshold;
    }

    public int getGcPersistedQueueFilesIntervalSeconds() {
        return gcPersistedQueueFilesIntervalSeconds;
    }

    public boolean isFromDisk() {
        return fromDisk;
    }

    public LogzioSender getLogzioSender() {
        HttpsRequestConfiguration requestConf;
        try {
            requestConf = HttpsRequestConfiguration
                    .builder()
                    .setLogzioListenerUrl(getUrl())
                    .setLogzioType(getType())
                    .setLogzioToken(getToken())
                    .setCompressRequests(isCompressRequests())
                    .build();
        } catch (LogzioParameterErrorException e) {
            logger.error("problem in one or more parameters with error {}", e.getMessage());
            return null;
        }
        SenderStatusReporter statusReporter = StatusReporterFactory.newSenderStatusReporter(LoggerFactory.getLogger(LogzioSender.class));
        LogzioSender.Builder senderBuilder = LogzioSender
                .builder()
                .setTasksExecutor(Executors.newScheduledThreadPool(getThreadPoolSize()))
                .setReporter(statusReporter)
                .setHttpsRequestConfiguration(requestConf)
                .setDebug(true)
                .withDiskQueue()
                .setQueueDir(getQueueDir())
                .setCheckDiskSpaceInterval(getDiskSpaceCheckInterval())
                .setFsPercentThreshold(getFileSystemFullPercentThreshold())
                .setGcPersistedQueueFilesIntervalSeconds(getGcPersistedQueueFilesIntervalSeconds())
                .endDiskQueue();
        try {
            return senderBuilder.build();
        } catch (LogzioParameterErrorException e) {
            logger.error("problem in one or more parameters with error {}", e.getMessage());
        }
        return null;
    }

    @Override
    public String toString() {
        return "SpanConsumerConfig{" +
                "listenerUrl='" + url + '\'' +
                ", token=" + token +
                ", StorageMode='" + (fromDisk ? "fromDisk" : "InMemory") + '\'' +
                '}';
    }
}
