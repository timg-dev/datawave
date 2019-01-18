package datawave.microservice.accumulo.lookup;

import datawave.accumulo.inmemory.InMemoryInstance;
import datawave.microservice.accumulo.config.AccumuloServiceConfiguration;
import datawave.microservice.config.accumulo.AccumuloProperties;
import org.apache.accumulo.core.client.AccumuloException;
import org.apache.accumulo.core.client.AccumuloSecurityException;
import org.apache.accumulo.core.client.Connector;
import org.apache.accumulo.core.client.Instance;
import org.apache.accumulo.core.client.security.tokens.PasswordToken;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import static org.junit.Assert.assertFalse;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles({"LookupServiceDisabledTest", "lookup-disabled"})
public class LookupServiceDisabledTest {
    
    @Autowired
    private ApplicationContext context;
    
    @Test
    public void verifyAutoConfig() {
        assertFalse("auditServiceConfiguration bean should not have been found", context.containsBean("auditServiceConfiguration"));
        assertFalse("auditServiceInstanceProvider bean should not have been found", context.containsBean("auditServiceInstanceProvider"));
        assertFalse("auditLookupSecurityMarking bean should not have been found", context.containsBean("auditLookupSecurityMarking"));
        assertFalse("lookupService bean should not have been found", context.containsBean("lookupService"));
        assertFalse("lookupController bean should not have been found", context.containsBean("lookupController"));
    }
    
    /**
     * This config is here only to prevent {@link AccumuloServiceConfiguration} from detecting a "missing" connector bean, which would trigger initialization of
     * a real accumulo connection using our test application.yml's props, which would cause this test to fail
     */
    @Configuration
    @Profile("LookupServiceDisabledTest")
    @ComponentScan(basePackages = "datawave.microservice")
    public static class TestConfiguration {
        @Bean
        public Instance accumuloInstance(@Qualifier("warehouse") AccumuloProperties accumuloProperties) {
            return new InMemoryInstance(accumuloProperties.getInstanceName());
        }
        
        @Bean
        @Qualifier("warehouse")
        public Connector warehouseConnector(@Qualifier("warehouse") AccumuloProperties accumuloProperties, Instance accumuloInstance)
                        throws AccumuloSecurityException, AccumuloException {
            return accumuloInstance.getConnector(accumuloProperties.getUsername(), new PasswordToken(accumuloProperties.getPassword()));
        }
    }
}
