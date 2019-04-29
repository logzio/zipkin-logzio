package zipkin2.storage.logzio;

import io.logz.sender.SenderStatusReporter;
import org.slf4j.Logger;


public class StatusReporterFactory {
    public static SenderStatusReporter newSenderStatusReporter(final Logger logger) {
        return new SenderStatusReporter() {

            public void error(String s) {
                logger.error(s);
            }

            public void error(String s, Throwable throwable) {
                logger.error(s + " " + throwable.getMessage());
            }

            public void warning(String s) {
                logger.warn(s);
            }

            public void warning(String s, Throwable throwable) {
                logger.warn(s + " " + throwable.getMessage());
            }

            @Override
            public void info(String s) {
                    logger.debug(s);
            }

            @Override
            public void info(String s, Throwable throwable) {
                    logger.debug(s + " " + throwable.getMessage());
            }
        };
    }
}
