package org.esa.beam.dataio.spot;

import com.bc.ceres.binding.PropertySet;

import java.io.File;
import java.io.IOException;
import java.io.Reader;

final class LogVolDescriptor {
    private final PropertySet propertySet;
    private final String productId;

    public LogVolDescriptor(Reader reader) throws IOException {
        this.propertySet = SpotVgtProductReaderPlugIn.readKeyValuePairs(reader);
        this.productId = getValue("PRODUCT_ID");
    }

    public PropertySet getPropertySet() {
        return propertySet;
    }

    public String getValue(String key) {
        return (String) propertySet.getValue(key);
    }

    public String getProductId() {
        return productId;
    }
}
