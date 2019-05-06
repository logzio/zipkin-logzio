package zipkin2.storage.logzio;

import io.logz.sender.SenderStatusReporter;
import org.slf4j.Logger;


public class StatusReporterFactory {
    public static SenderStatusReporter newSenderStatusReporter(final Logger logger) {
        return new SenderStatusReporter() {

            public void error(String s) {
                logger.error(LogzioStorage.ZIPKIN_LOGZIO_STORAGE_MSG + s);
            }

            public void error(String s, Throwable throwable) {
                logger.error(LogzioStorage.ZIPKIN_LOGZIO_STORAGE_MSG + s + " " + throwable.getMessage());
            }

            public void warning(String s) {
                logger.warn(LogzioStorage.ZIPKIN_LOGZIO_STORAGE_MSG + s);
            }

            public void warning(String s, Throwable throwable) {
                logger.warn(LogzioStorage.ZIPKIN_LOGZIO_STORAGE_MSG + s + " " + throwable.getMessage());
            }

            @Override
            public void info(String s) {
                    logger.debug(LogzioStorage.ZIPKIN_LOGZIO_STORAGE_MSG + s);
            }

            @Override
            public void info(String s, Throwable throwable) {
                    logger.debug(LogzioStorage.ZIPKIN_LOGZIO_STORAGE_MSG + s + " " + throwable.getMessage());
            }
        };
    }
}
