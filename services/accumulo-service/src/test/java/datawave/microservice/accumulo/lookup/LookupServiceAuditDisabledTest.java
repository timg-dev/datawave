package datawave.microservice.accumulo.lookup;

import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.module.jaxb.JaxbAnnotationIntrospector;
import datawave.accumulo.inmemory.InMemoryInstance;
import datawave.microservice.authorization.jwt.JWTRestTemplate;
import datawave.microservice.authorization.user.ProxiedUserDetails;
import datawave.microservice.config.accumulo.AccumuloProperties;
import datawave.webservice.response.LookupResponse;
import datawave.webservice.response.objects.DefaultKey;
import datawave.webservice.response.objects.KeyBase;
import org.apache.accumulo.core.client.AccumuloException;
import org.apache.accumulo.core.client.AccumuloSecurityException;
import org.apache.accumulo.core.client.Connector;
import org.apache.accumulo.core.client.Instance;
import org.apache.accumulo.core.client.security.tokens.PasswordToken;
import org.apache.commons.codec.binary.Base64;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Tests LookupService functionality ({@code accumulo.lookup.enabled=true}) with auditing disabled ({@code audit.enabled=false})
 */
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ContextConfiguration(classes = LookupServiceAuditDisabledTest.TestConfiguration.class)
@ActiveProfiles({"LookupServiceAuditDisabledTest", "lookup-with-audit-disabled"})
public class LookupServiceAuditDisabledTest {
    
    public static final String TEST_TABLE_NAME = "test";
    
    @LocalServerPort
    private int webServicePort;
    
    @Autowired
    private Connector connector;
    
    @Autowired
    private RestTemplateBuilder restTemplateBuilder;
    
    @Autowired
    private ApplicationContext context;
    
    private ObjectMapper objectMapper;
    
    private JWTRestTemplate jwtRestTemplate;
    
    private ProxiedUserDetails defaultUserDetails;
    
    @Before
    public void setup() throws Exception {
        defaultUserDetails = TestUtils.userDetails(Collections.singleton("AuthorizedUser"), Arrays.asList("A", "B", "C", "D", "E", "F", "G", "H", "I"));
        jwtRestTemplate = restTemplateBuilder.build(JWTRestTemplate.class);
        objectMapperSetup();
        TestUtils.setupTestTable(connector, TEST_TABLE_NAME);
    }
    
    @Test
    public void verifyAutoConfig() {
        assertFalse("auditServiceConfiguration bean should not be present", context.containsBean("auditServiceConfiguration"));
        assertFalse("auditServiceInstanceProvider bean should not be present", context.containsBean("auditServiceInstanceProvider"));
        assertTrue("auditLookupSecurityMarking bean not found", context.containsBean("auditLookupSecurityMarking"));
        assertTrue("lookupService bean not found", context.containsBean("lookupService"));
        assertTrue("lookupController bean not found", context.containsBean("lookupController"));
        assertTrue("lookupConfiguration bean not found", context.containsBean("lookupConfiguration"));
    }
    
    private void objectMapperSetup() {
        objectMapper = new ObjectMapper();
        // Map LookupResponse's abstract KeyBase field to concrete impl
        SimpleModule module = new SimpleModule("keybase", Version.unknownVersion());
        module.addAbstractTypeMapping(KeyBase.class, DefaultKey.class);
        objectMapper.registerModule(module);
        // Enable json deserialization via jaxb annotations
        objectMapper.setAnnotationIntrospector(new JaxbAnnotationIntrospector(TypeFactory.defaultInstance()));
    }
    
    @Test
    public void testLookupAllRowsAndVerifyResults() throws Exception {
        
        String queryString = TestUtils.queryString("useAuthorizations=A,C,E,G,I", "columnVisibility=foo");
        
        for (String rowid : Arrays.asList("row1", "row2", "row3")) {
            LookupResponse response = doLookup(defaultUserDetails, "/accumulo/v1/lookup/" + TEST_TABLE_NAME + "/" + rowid, queryString);
            
            assertEquals("LookupResponse should have had 5 entries", 5, response.getEntries().size());
            
            //@formatter:off
            assertEquals("Key(s) having unexpected auth tokens [B,D,F,H] found in response", 0,
                response.getEntries().stream().filter(
                    e -> ((DefaultKey) e.getKey()).getColumnVisibility().contains("B")
                      || ((DefaultKey) e.getKey()).getColumnVisibility().contains("D")
                      || ((DefaultKey) e.getKey()).getColumnVisibility().contains("F")
                      || ((DefaultKey) e.getKey()).getColumnVisibility().contains("H")).count());

            assertEquals("Key(s) having unexpected column family found in response", 5,
                response.getEntries().stream().filter(e -> ((DefaultKey) e.getKey()).getColFam().equals("cf2")).count());

            assertEquals("Key(s) having unexpected column qualifier found in response", 0,
                response.getEntries().stream().filter(
                    e -> !((DefaultKey) e.getKey()).getColQual().equals("cq1")
                      && !((DefaultKey) e.getKey()).getColQual().equals("cq3")
                      && !((DefaultKey) e.getKey()).getColQual().equals("cq5")
                      && !((DefaultKey) e.getKey()).getColQual().equals("cq7")
                      && !((DefaultKey) e.getKey()).getColQual().equals("cq9")).count());
            //@formatter:on
        }
    }
    
    @Test
    public void testLookupWithColFamAndColQual() throws Exception {
        
        //@formatter:off
        String queryString = TestUtils.queryString(
            "useAuthorizations=B",
            "columnVisibility=foo",
            LookupService.Parameter.CF + "=cf2",
            LookupService.Parameter.CF_ENCODING + "=none",
            LookupService.Parameter.CQ + "=cq2",
            LookupService.Parameter.CQ_ENCODING + "=none");
        //@formatter:on
        
        String rowid = "row3";
        LookupResponse response = doLookup(defaultUserDetails, "/accumulo/v1/lookup/" + TEST_TABLE_NAME + "/" + rowid, queryString);
        assertEquals("Lookup should have matched 1 entry", 1, response.getEntries().size());
    }
    
    @Test
    public void testLookupWithBase64Params() throws Exception {
        
        //@formatter:off
        String queryString = TestUtils.queryString(
            "useAuthorizations=B",
            "columnVisibility=foo",
            LookupService.Parameter.CF + "=" + Base64.encodeBase64URLSafeString("cf2".getBytes()),
            LookupService.Parameter.CF_ENCODING + "=base64",
            LookupService.Parameter.CQ + "=" + Base64.encodeBase64URLSafeString("cq2".getBytes()),
            LookupService.Parameter.CQ_ENCODING + "=base64",
            LookupService.Parameter.ROW_ENCODING + "=base64");
        //@formatter:on
        
        String rowidBase64 = Base64.encodeBase64URLSafeString("row3".getBytes());
        LookupResponse response = doLookup(defaultUserDetails, "/accumulo/v1/lookup/" + TEST_TABLE_NAME + "/" + rowidBase64, queryString);
        assertEquals("Lookup should have matched 1 entry", 1, response.getEntries().size());
    }
    
    @Test
    public void testLookupBeginEndSubset() throws Exception {
        
        //@formatter:off
        String queryString = TestUtils.queryString(
            "useAuthorizations=A,B,C,D,E,F,G,H,I",
            "columnVisibility=foo",
            LookupService.Parameter.CF + "=cf2",
            LookupService.Parameter.BEGIN_ENTRY + "=2",
            LookupService.Parameter.END_ENTRY + "=5");
        //@formatter:on
        
        String rowid = "row1";
        LookupResponse response = doLookup(defaultUserDetails, "/accumulo/v1/lookup/" + TEST_TABLE_NAME + "/" + rowid, queryString);
        assertEquals("Lookup should have returned 4 entries", 4, response.getEntries().size());
        assertTrue("First result should be cq3", ((DefaultKey) (response.getEntries().get(0).getKey())).getColQual().equals("cq3"));
        assertTrue("Last result should be cq6", ((DefaultKey) (response.getEntries().get(3).getKey())).getColQual().equals("cq6"));
    }
    
    @Test
    public void testErrorOnBeginGreaterThanEnd() throws Exception {
        
        //@formatter:off
        String queryString = TestUtils.queryString(
            "useAuthorizations=A,B,C,D,E,F,G,H,I",
            "columnVisibility=foo",
            LookupService.Parameter.CF + "=cf2",
            LookupService.Parameter.BEGIN_ENTRY + "=7",
            LookupService.Parameter.END_ENTRY + "=5");
        //@formatter:on
        
        try {
            doLookup(defaultUserDetails, "/accumulo/v1/lookup/" + TEST_TABLE_NAME + "/row2", queryString);
            fail("This code should never be reached");
        } catch (HttpServerErrorException ex) {
            assertEquals("Test should have returned 500 status", 500, ex.getStatusCode().value());
        } catch (Throwable t) {
            t.printStackTrace();
            fail("Unexpected throwable type was caught");
        }
    }
    
    @Test
    public void testLookupWithBeginEqualToEnd() throws Exception {
        
        //@formatter:off
        String queryString = TestUtils.queryString(
            "useAuthorizations=A,B,C,D,E,F,G,H,I",
            "columnVisibility=foo",
            LookupService.Parameter.CF + "=cf2",
            LookupService.Parameter.BEGIN_ENTRY + "=3",
            LookupService.Parameter.END_ENTRY + "=3");
        //@formatter:on
        
        String rowid = "row1";
        LookupResponse response = doLookup(defaultUserDetails, "/accumulo/v1/lookup/" + TEST_TABLE_NAME + "/" + rowid, queryString);
        assertEquals("Lookup should have matched 1 entry", 1, response.getEntries().size());
        assertEquals("Result should be cq4", "cq4", ((DefaultKey) (response.getEntries().get(0).getKey())).getColQual());
    }
    
    @Test
    public void testLookupWithAllAssignedAuths() throws Exception {
        
        // Query with useAuthorizations param with all assigned auths requested. Should get all 12 entries returned
        
        String queryString = TestUtils.queryString("useAuthorizations=A,B,C,D,E,F,G,H,I", "columnVisibility=foo");
        for (String row : Arrays.asList("row1", "row2", "row3")) {
            doLookup(defaultUserDetails, "/accumulo/v1/lookup/" + TEST_TABLE_NAME + "/" + row, queryString);
        }
        
        // Now query without useAuthorizations param. All of user's assigned auths should be utilized by default
        // (same as above)
        
        for (String row : Arrays.asList("row1", "row2", "row3")) {
            queryString = TestUtils.queryString("columnVisibility=foo");
            doLookup(defaultUserDetails, "/accumulo/v1/lookup/" + TEST_TABLE_NAME + "/" + row, queryString);
        }
    }
    
    @Test
    public void testErrorOnUserWithInsufficientRoles() {
        ProxiedUserDetails userDetails = TestUtils.userDetails(Arrays.asList("ThisRoleIsNoGood", "IAmRoot"),
                        Arrays.asList("A", "B", "C", "D", "E", "F", "G", "H", "I"));
        String queryString = TestUtils.queryString("useAuthorizations=A,C,E,G,I", "columnVisibility=foo");
        try {
            doLookup(userDetails, "/accumulo/v1/lookup/" + TEST_TABLE_NAME + "/row1", queryString);
            fail("This code should never be reached");
        } catch (HttpClientErrorException ex) {
            assertEquals("Test should have returned 403 Forbidden status", 403, ex.getStatusCode().value());
        } catch (Throwable t) {
            t.printStackTrace();
            fail("Unexpected throwable type was caught");
        }
    }
    
    @Test
    public void testErrorOnUserWithInsufficientAuths() {
        ProxiedUserDetails userDetails = TestUtils.userDetails(Collections.singleton("AuthorizedUser"), Arrays.asList("A", "C"));
        String queryString = TestUtils.queryString("useAuthorizations=A,C,E,G,I", "columnVisibility=foo");
        try {
            doLookup(userDetails, "/accumulo/v1/lookup/" + TEST_TABLE_NAME + "/row2", queryString);
            fail("This code should never be reached");
        } catch (HttpServerErrorException ex) {
            assertEquals("Test should have returned 500 status", 500, ex.getStatusCode().value());
        } catch (Throwable t) {
            t.printStackTrace();
            fail("Unexpected throwable type was caught");
        }
    }
    
    @Test
    public void testErrorOnMissingColVizParam() {
        ProxiedUserDetails userDetails = TestUtils.userDetails(Collections.singleton("AuthorizedUser"), Arrays.asList("A"));
        String queryString = TestUtils.queryString("NotColumnVisibility=foo");
        try {
            doLookup(userDetails, "/accumulo/v1/lookup/" + TEST_TABLE_NAME + "/row2", queryString);
            fail("This code should never be reached");
        } catch (HttpClientErrorException ex) {
            assertEquals("Test should have returned 400 status", 400, ex.getStatusCode().value());
        } catch (Throwable t) {
            t.printStackTrace();
            fail("Unexpected throwable type was caught");
        }
    }
    
    @Test
    public void testErrorOnTableDoesNotExist() {
        ProxiedUserDetails userDetails = TestUtils.userDetails(Collections.singleton("AuthorizedUser"), Arrays.asList("A", "B", "C"));
        String queryString = TestUtils.queryString("useAuthorizations=A,B,C", "columnVisibility=foo");
        try {
            doLookup(userDetails, "/accumulo/v1/lookup/THIS_TABLE_DOES_NOT_EXIST/row2", queryString);
            fail("This code should never be reached");
        } catch (HttpServerErrorException ex) {
            assertEquals("Test should have returned 500 status", 500, ex.getStatusCode().value());
        } catch (Throwable t) {
            t.printStackTrace();
            fail("Unexpected throwable type was caught");
        }
    }
    
    @Test
    public void testLookupRowDoesNotExist() throws Exception {
        ProxiedUserDetails userDetails = TestUtils.userDetails(Collections.singleton("AuthorizedUser"), Arrays.asList("A", "B", "C"));
        String queryString = TestUtils.queryString("useAuthorizations=A,B,C", "columnVisibility=foo");
        LookupResponse lr = doLookup(userDetails, "/accumulo/v1/lookup/" + TEST_TABLE_NAME + "/ThisRowDoesNotExist", queryString);
        assertEquals("Test should have returned response with zero entries", 0, lr.getEntries().size());
    }
    
    /**
     * Lookups here should return one or more valid Accumulo table entries. If not, an exception is thrown
     */
    private LookupResponse doLookup(ProxiedUserDetails authUser, String path, String query) throws Exception {
        UriComponents uri = UriComponentsBuilder.newInstance().scheme("https").host("localhost").port(webServicePort).path(path).query(query).build();
        ResponseEntity<String> entity = jwtRestTemplate.exchange(authUser, HttpMethod.GET, uri, String.class);
        assertEquals("Lookup request to " + uri + " did not return 200 status", HttpStatus.OK, entity.getStatusCode());
        return objectMapper.readValue(entity.getBody(), LookupResponse.class);
    }
    
    @Configuration
    @Profile("LookupServiceAuditDisabledTest")
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
