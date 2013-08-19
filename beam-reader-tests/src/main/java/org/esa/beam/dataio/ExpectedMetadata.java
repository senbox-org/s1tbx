package org.esa.beam.dataio;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.esa.beam.framework.datamodel.MetadataAttribute;
import org.esa.beam.framework.datamodel.MetadataElement;

/**
 * @author Marco Peters
 *
 * Must be with public access for json-framework usage tb 2013-08-19
 */
public class ExpectedMetadata {

    @JsonProperty(required = true)
    private String path;

    @JsonProperty(required = true)
    private String value;

    // needed by json engine tb 2013-08-19
    public ExpectedMetadata() {
    }

    public ExpectedMetadata(MetadataAttribute attribute) {
        final MetadataElement metadataRoot = attribute.getProduct().getMetadataRoot();
        MetadataElement currentElement = attribute.getParentElement();
        final StringBuilder sb = new StringBuilder();
        sb.insert(0, attribute.getName());
        while(currentElement != null) {
            sb.insert(0, "/");
            sb.insert(0, currentElement.getName());
            currentElement = currentElement.getParentElement();
            if(metadataRoot == currentElement) {
                break;
            }
        }
        this.path = sb.toString();
        this.value = attribute.getData().getElemString();
    }


    String getPath() {
        return path;
    }

    String getValue() {
        return value;
    }
}
