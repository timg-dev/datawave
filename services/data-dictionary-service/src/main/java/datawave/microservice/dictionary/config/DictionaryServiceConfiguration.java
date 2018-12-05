package datawave.microservice.dictionary.config;

import datawave.accumulo.util.security.UserAuthFunctions;
import datawave.marking.MarkingFunctions;
import datawave.microservice.config.accumulo.AccumuloProperties;
import datawave.microservice.config.web.DatawaveServerProperties;
import datawave.microservice.dictionary.data.DatawaveDataDictionary;
import datawave.microservice.dictionary.data.DatawaveDataDictionaryImpl;
import datawave.microservice.dictionary.http.converter.DataDictionaryHttpMessageConverter;
import datawave.microservice.metadata.MetadataDescriptionsHelper;
import datawave.microservice.metadata.MetadataDescriptionsHelperFactory;
import datawave.query.util.MetadataHelperFactory;
import datawave.webservice.query.result.metadata.DefaultMetadataField;
import datawave.webservice.results.datadictionary.DefaultDataDictionary;
import datawave.webservice.results.datadictionary.DefaultDescription;
import datawave.webservice.results.datadictionary.DefaultDictionaryField;
import datawave.webservice.results.datadictionary.DefaultFields;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@Configuration
public class DictionaryServiceConfiguration {
    @Bean
    @Qualifier("warehouse")
    public AccumuloProperties warehouseAccumuloProperties(DataDictionaryProperties dataDictionaryProperties) {
        return dataDictionaryProperties.getAccumuloProperties();
    }
    
    @Bean
    @ConditionalOnMissingBean
    public UserAuthFunctions userAuthFunctions() {
        return UserAuthFunctions.getInstance();
    }
    
    @Bean
    @RefreshScope
    @ConditionalOnMissingBean
    public DataDictionaryProperties dataDictionaryConfiguration() {
        return new DataDictionaryProperties();
    }
    
    @Bean
    @Scope("prototype")
    @ConditionalOnMissingBean
    public MetadataDescriptionsHelper<DefaultDescription> metadataHelperWithDescriptions(MarkingFunctions markingFunctions,
                    ResponseObjectFactory<DefaultDescription,DefaultDataDictionary,DefaultMetadataField,DefaultDictionaryField,DefaultFields> responseObjectFactory) {
        return new MetadataDescriptionsHelper<>(markingFunctions, responseObjectFactory);
    }
    
    @Bean
    @ConditionalOnMissingBean
    public DatawaveDataDictionary datawaveDataDictionary(MarkingFunctions markingFunctions,
                    ResponseObjectFactory<DefaultDescription,DefaultDataDictionary,DefaultMetadataField,DefaultDictionaryField,DefaultFields> responseObjectFactory,
                    MetadataHelperFactory metadataHelperFactory, MetadataDescriptionsHelperFactory metadataDescriptionsHelperFactory) {
        return new DatawaveDataDictionaryImpl(markingFunctions, responseObjectFactory, metadataHelperFactory, metadataDescriptionsHelperFactory);
    }
    
    @Bean
    @ConditionalOnMissingBean
    public ResponseObjectFactory<DefaultDescription,DefaultDataDictionary,DefaultMetadataField,DefaultDictionaryField,DefaultFields> responseObjectFactory() {
        return new ResponseObjectFactory<DefaultDescription,DefaultDataDictionary,DefaultMetadataField,DefaultDictionaryField,DefaultFields>() {
            @Override
            public DefaultDataDictionary getDataDictionary() {
                return new DefaultDataDictionary();
            }
            
            @Override
            public DefaultDescription getDescription() {
                return new DefaultDescription();
            }
            
            @Override
            public DefaultFields getFields() {
                return new DefaultFields();
            }
        };
    }
    
    @Bean
    public WebMvcConfigurer messageConverterConfiguration(final DatawaveServerProperties serverProperties) {
        return new WebMvcConfigurer() {
            @Override
            public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
                converters.add(new DataDictionaryHttpMessageConverter(serverProperties));
            }
        };
    }
    
}
