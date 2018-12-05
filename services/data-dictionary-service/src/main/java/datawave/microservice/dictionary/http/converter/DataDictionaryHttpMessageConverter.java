package datawave.microservice.dictionary.http.converter;

import datawave.microservice.config.web.DatawaveServerProperties;
import datawave.webservice.query.result.metadata.MetadataFieldBase;
import datawave.webservice.results.datadictionary.DataDictionaryBase;
import datawave.webservice.results.datadictionary.DefaultFields;
import datawave.webservice.results.datadictionary.DescriptionBase;
import datawave.webservice.results.datadictionary.DictionaryFieldBase;
import datawave.webservice.results.datadictionary.FieldsBase;
import io.protostuff.Message;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.AbstractHttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Collections;

public class DataDictionaryHttpMessageConverter extends AbstractHttpMessageConverter<Object> {
    private static final Charset utf8 = Charset.forName("UTF-8");
    private static final String EMPTY_STR = "", SEP = ", ";
    
    private final DatawaveServerProperties datawaveServerProperties;
    
    public DataDictionaryHttpMessageConverter(DatawaveServerProperties datawaveServerProperties) {
        this.datawaveServerProperties = datawaveServerProperties;
        setSupportedMediaTypes(Collections.singletonList(MediaType.TEXT_HTML));
    }
    
    @Override
    protected boolean supports(Class<?> clazz) {
        return DataDictionaryBase.class.isAssignableFrom(clazz) || FieldsBase.class.isAssignableFrom(clazz);
    }
    
    @Override
    protected boolean canRead(MediaType mediaType) {
        return false;
    }
    
    @Override
    protected Message<?> readInternal(Class<?> clazz, HttpInputMessage inputMessage) throws IOException, HttpMessageNotReadableException {
        return null;
    }
    
    @Override
    protected void writeInternal(Object object, HttpOutputMessage outputMessage) throws IOException, HttpMessageNotWritableException {
        byte[] data = null;
        
        if (object instanceof DataDictionaryBase) {
            data = getDataDictionaryHtml((DataDictionaryBase) object);
        } else if (object instanceof FieldsBase) {
            data = getFieldDescriptionsHtml((FieldsBase) object);
        }
        
        outputMessage.getBody().write(data);
    }
    
    private byte[] getDataDictionaryHtml(DataDictionaryBase<?,? extends MetadataFieldBase> dict) {
        StringBuilder builder = new StringBuilder();
        builder.append("<html>");
        builder.append("<title>");
        builder.append("Data Dictionary");
        builder.append("</title>");
        builder.append("<body>");
        builder.append("<link rel='stylesheet' type='text/css' href='" + datawaveServerProperties.getCssUri() + "' media='screen' />");
        builder.append("<div style=\"font-size: 14px;\">Data Dictionary</div>\n");
        builder.append("<table class=\"creds\">\n");
        
        builder.append("<tr><th>FieldName</th><th>Internal FieldName</th><th>DataType</th>");
        builder.append("<th>Index only</th><th>Forward Indexed</th><th>Reverse Indexed</th><th>Normalized</th></tr>");
        builder.append("<th>Types</th><th>Descriptions</th><th>LastUpdated</th></tr>");
        
        int x = 0;
        for (MetadataFieldBase<?,? extends DescriptionBase> f : dict.getFields()) {
            // highlight alternating rows
            if (x % 2 == 0) {
                builder.append("<tr class=\"highlight\">");
            } else {
                builder.append("<tr>");
            }
            x++;
            
            String fieldName = (null == f.getFieldName()) ? EMPTY_STR : f.getFieldName();
            String internalFieldName = (null == f.getInternalFieldName()) ? EMPTY_STR : f.getInternalFieldName();
            String datatype = (null == f.getDataType()) ? EMPTY_STR : f.getDataType();
            
            StringBuilder types = new StringBuilder();
            if (null != f.getTypes()) {
                
                for (String type : f.getTypes()) {
                    if (0 != types.length()) {
                        types.append(SEP);
                    }
                    types.append(type);
                }
            }
            
            builder.append("<td>").append(fieldName).append("</td>");
            builder.append("<td>").append(internalFieldName).append("</td>");
            builder.append("<td>").append(datatype).append("</td>");
            builder.append("<td>").append(f.isIndexOnly()).append("</td>");
            builder.append("<td>").append(f.isForwardIndexed() ? true : "").append("</td>");
            builder.append("<td>").append(f.isReverseIndexed() ? true : "").append("</td>");
            builder.append("<td>").append(f.isNormalized() ? true : "").append("</td>");
            builder.append("<td>").append(types).append("</td>");
            builder.append("<td>");
            
            boolean first = true;
            for (DescriptionBase desc : f.getDescriptions()) {
                if (!first) {
                    builder.append(", ");
                } else {
                    first = false;
                }
                builder.append(desc);
            }
            builder.append("</td>");
            builder.append("<td>").append(f.getLastUpdated()).append("</td>");
            builder.append("</tr>");
        }
        
        builder.append("</table></body></html>\n");
        return builder.toString().getBytes(utf8);
    }
    
    private byte[] getFieldDescriptionsHtml(FieldsBase<?,? extends DictionaryFieldBase,? extends DescriptionBase> descs) {
        StringBuilder builder = new StringBuilder();
        builder.append("<html>");
        builder.append("<title>");
        builder.append("Field Descriptions");
        builder.append("</title>");
        builder.append("<body>");
        builder.append("<link rel='stylesheet' type='text/css' href='" + datawaveServerProperties.getCssUri() + "' media='screen' />");
        builder.append("<div style=\"font-size: 14px;\">Field Descriptions</div>\n");
        builder.append("<table class=\"creds\">\n");
        
        builder.append("<tr><th>Datatype</th><th>FieldName</th><th>Description</th></tr>");
        int x = 0;
        for (DictionaryFieldBase<?,? extends DescriptionBase> field : descs.getFields()) {
            for (DescriptionBase desc : field.getDescriptions()) {
                DefaultFields.addDescriptionRow(field, desc, x, builder);
            }
        }
        
        builder.append("</table></body></html>\n");
        return builder.toString().getBytes(utf8);
    }
}
