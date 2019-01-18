package datawave.microservice.audit.config.discovery;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration props for audit service discovery
 */
@ConfigurationProperties(prefix = "audit.discovery")
public class AuditDiscoveryProperties {
    
    private boolean failFast = false;
    
    public boolean isFailFast() {
        return failFast;
    }
    
    public void setFailFast(boolean failFast) {
        this.failFast = failFast;
    }
    
}
