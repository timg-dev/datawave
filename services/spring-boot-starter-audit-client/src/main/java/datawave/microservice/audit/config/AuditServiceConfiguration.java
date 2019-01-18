package datawave.microservice.audit.config;

import datawave.microservice.audit.AuditServiceProvider;
import datawave.microservice.audit.config.discovery.AuditDiscoveryConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(name = "audit.enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(AuditServiceProperties.class)
@AutoConfigureAfter({AuditDiscoveryConfiguration.class})
public class AuditServiceConfiguration {
    @Bean
    @ConditionalOnMissingBean
    public AuditServiceProvider auditServiceInstanceProvider(AuditServiceProperties serviceProperties) {
        return new AuditServiceProvider(serviceProperties);
    }
}
