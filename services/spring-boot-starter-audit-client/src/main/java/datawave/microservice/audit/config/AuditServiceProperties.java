package datawave.microservice.audit.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Top-level properties for the audit service
 */
@ConfigurationProperties(prefix = "audit")
public class AuditServiceProperties {
    
    private String serviceId = "audit";
    
    private String uri = "http://localhost/audit";
    
    public String getUri() {
        return uri;
    }
    
    public void setUri(String uri) {
        this.uri = uri;
    }
    
    public String getServiceId() {
        return serviceId;
    }
    
    public void setServiceId(String serviceId) {
        this.serviceId = serviceId;
    }
}
