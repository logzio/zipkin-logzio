/*
 * Copyright 2016-2018 The OpenZipkin Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package zipkin.autoconfigure.storage.logzio;

import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import zipkin2.storage.logzio.ConsumerParams;
import zipkin2.storage.logzio.LogzioStorageParams;
import zipkin2.storage.logzio.LogzioStorage;

import java.io.Serializable;

import ch.qos.logback.classic.Logger;

@ConfigurationProperties("zipkin.storage.logzio")
class ZipkinLogzioStorageProperties implements Serializable { // for Spark jobs
    private static final long serialVersionUID = 0L;
    private static final Logger logger = (Logger) LoggerFactory.getLogger(ZipkinLogzioStorageProperties.class.getName());

    private String logzioToken;
    private String logzioAPIToken;
    private String logzioListenerAddress;

    public void setLogzioToken(String logzioToken) {
        this.logzioToken = logzioToken;
    }

    public String getLogzioToken() {
        return this.logzioToken;
    }

    public void setLogzioAPIToken(String logzioAPIToken) {
        this.logzioAPIToken = logzioAPIToken;
    }

    public String getLogzioAPIToken() {
        return this.logzioAPIToken;
    }

    public String getLogzioListenerAddress() {
        return logzioListenerAddress;
    }

    public void setLogzioListenerAddress(String logzioListenerAddress) {
        this.logzioListenerAddress = logzioListenerAddress;
    }

    public LogzioStorage.Builder toBuilder() {
        LogzioStorageParams config = new LogzioStorageParams();
        ConsumerParams consumerParams = new ConsumerParams();
        consumerParams.setUrl(logzioListenerAddress);
        consumerParams.setToken(logzioToken);
        config.setConsumerParams(consumerParams);
        config.setApiToken(logzioAPIToken);
        logger.info("[zipkin-logzio-storage] config " + config.toString());
        LogzioStorage.Builder builder = LogzioStorage.newBuilder();
        builder.config(config);
        return builder;
    }

}