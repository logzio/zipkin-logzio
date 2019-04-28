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
                if (s.contains("DEBUG:"))  {
                    logger.debug(s);
                } else {
                    logger.info(s);
                }
            }

            @Override
            public void info(String s, Throwable throwable) {
                if (s.contains("DEBUG:"))  {
                    logger.debug(s + " " + throwable.getMessage());
                } else {
                    logger.info(s + " " + throwable.getMessage());
                }
            }
        };
    }
}
