package datawave.microservice.metadata.config;

import com.github.benmanes.caffeine.cache.CaffeineSpec;
import datawave.query.composite.CompositeMetadataHelper;
import datawave.query.util.AllFieldMetadataHelper;
import datawave.query.util.MetadataHelper;
import datawave.query.util.MetadataHelperFactory;
import datawave.query.util.TypeMetadataHelper;
import org.apache.accumulo.core.security.Authorizations;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Scope;

import java.util.Map;
import java.util.Set;

@Configuration
@ComponentScan(basePackages = {"datawave.query.util", "datawave.query.composite"},
                includeFilters = {
                        @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, value = {MetadataHelperFactory.class, TypeMetadataHelper.Factory.class})},
                useDefaultFilters = false)
public class MetadataHelperConfiguration {
    @Bean
    @ConditionalOnMissingBean
    public MetadataHelperProperties metadataHelperProperties() {
        return new MetadataHelperProperties();
    }
    
    @Bean
    @Scope("prototype")
    @ConditionalOnMissingBean
    public MetadataHelper metadataHelper(AllFieldMetadataHelper allFieldMetadataHelper, @Qualifier("allMetadataAuths") Set<Authorizations> allMetadataAuths) {
        return new MetadataHelper(allFieldMetadataHelper, allMetadataAuths);
    }
    
    @Bean
    @Scope("prototype")
    @ConditionalOnMissingBean
    public AllFieldMetadataHelper allFieldMetadataHelper(TypeMetadataHelper typeMetadataHelper, CompositeMetadataHelper compositeMetadataHelper) {
        return new AllFieldMetadataHelper(typeMetadataHelper, compositeMetadataHelper);
    }
    
    @Bean
    @Scope("prototype")
    @ConditionalOnMissingBean
    public CompositeMetadataHelper compositeMetadataHelper() {
        return new CompositeMetadataHelper();
    }
    
    @Bean
    @Scope("prototype")
    @ConditionalOnMissingBean
    public TypeMetadataHelper typeMetadataHelper(@Qualifier("typeSubstitutions") Map<String,String> typeSubstitutions,
                    @Qualifier("allMetadataAuths") Set<Authorizations> allMetadataAuths) {
        return new TypeMetadataHelper(typeSubstitutions, allMetadataAuths);
    }
    
    @Bean(name = "typeSubstitutions")
    @ConditionalOnMissingBean(name = "typeSubstitutions")
    public Map<String,String> typeSubstitutions(MetadataHelperProperties metadataHelperProperties) {
        return metadataHelperProperties.getTypeSubstitutions();
    }
    
    @Bean(name = "allMetadataAuths")
    @ConditionalOnMissingBean(name = "allMetadataAuths")
    public Set<Authorizations> allMetadataAuths(MetadataHelperProperties metadataHelperProperties) {
        return metadataHelperProperties.getAllMetadataAuths();
    }
    
    @Bean(name = "metadataHelperCacheManager")
    @ConditionalOnMissingBean
    public CacheManager metadataHelperCacheManager(@Qualifier("metadataHelperCacheSpec") CaffeineSpec caffeineSpec) {
        CaffeineCacheManager caffeineCacheManager = new CaffeineCacheManager();
        caffeineCacheManager.setCaffeineSpec(caffeineSpec);
        return caffeineCacheManager;
    }
    
    @Bean(name = "metadataHelperCacheSpec")
    @ConditionalOnMissingBean
    public CaffeineSpec cacheManagerSpec() {
        return CaffeineSpec.parse("maximumSize=100, expireAfterAccess=24h, expireAfterWrite=24h");
    }
    
}
