package datawave.microservice.metadata;

import datawave.webservice.results.datadictionary.DefaultDescription;
import org.springframework.beans.factory.annotation.Lookup;
import org.springframework.stereotype.Component;

@Component
public class MetadataDescriptionsHelperFactory {
    @Lookup
    public MetadataDescriptionsHelper<DefaultDescription> createMetadataDescriptionsHelper() {
        // return nothing since spring will create a proxy for this method
        return null;
    }
}
