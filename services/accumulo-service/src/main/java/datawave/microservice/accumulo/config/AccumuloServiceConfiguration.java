package datawave.microservice.accumulo.config;

import datawave.microservice.accumulo.config.AccumuloServiceConfiguration.MetricsClusterProperties;
import datawave.microservice.accumulo.config.AccumuloServiceConfiguration.WarehouseClusterProperties;

import datawave.accumulo.util.security.UserAuthFunctions;
import datawave.marking.MarkingFunctions;
import datawave.microservice.config.accumulo.AccumuloProperties;
import datawave.microservice.config.cluster.ClusterProperties;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

@Configuration
@EnableConfigurationProperties({WarehouseClusterProperties.class, MetricsClusterProperties.class})
public class AccumuloServiceConfiguration {
    
    @Bean
    @Lazy
    @Qualifier("warehouse")
    @ConditionalOnMissingBean
    public AccumuloProperties warehouseAccumuloProperies(WarehouseClusterProperties warehouseProperties) {
        return warehouseProperties.getAccumulo();
    }
    
    @Bean
    @Lazy
    @Qualifier("metrics")
    @ConditionalOnMissingBean
    public AccumuloProperties metricsAccumuloProperies(MetricsClusterProperties metricsProperties) {
        return metricsProperties.getAccumulo();
    }
    
    @Bean
    @ConditionalOnMissingBean
    public MarkingFunctions markingFunctions() {
        return new MarkingFunctions.NoOp();
    }
    
    @Bean
    @ConditionalOnMissingBean
    public UserAuthFunctions userAuthFunctions() {
        return UserAuthFunctions.getInstance();
    }
    
    @ConfigurationProperties(prefix = "metrics-cluster")
    public static class MetricsClusterProperties extends ClusterProperties {
        
    }
    
    @ConfigurationProperties(prefix = "warehouse-cluster")
    public static class WarehouseClusterProperties extends ClusterProperties {
        
    }
}
