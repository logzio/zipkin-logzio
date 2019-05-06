package zipkin.autoconfigure.storage.logzio;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import zipkin2.storage.StorageComponent;

@Configuration
@EnableConfigurationProperties(ZipkinLogzioStorageProperties.class)
@ConditionalOnProperty(name = "zipkin.storage.type", havingValue = "logzio")
@ConditionalOnMissingBean(StorageComponent.class)
class ZipkinLogzioStorageAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    StorageComponent storage(ZipkinLogzioStorageProperties properties,
                             @Value("${zipkin.storage.strict-trace-id:true}") boolean strictTraceId) {
        return properties.toBuilder().strictTraceId(strictTraceId).build();
    }
}
