package org.esa.beam.dataio.spot;

import com.bc.ceres.binding.PropertySet;

import java.io.File;
import java.io.IOException;

final class ProductDescriptor {
    private final File file;
    private final PropertySet propertySet;
    private final String productId;

    public ProductDescriptor(File file) throws IOException {
        this.file = file;
        this.propertySet = SpotVgtProductReaderPlugIn.readKeyValuePairs(file);
        this.productId = getValue("PRODUCT_ID");
    }

    public String getValue(String key) {
        return (String) propertySet.getValue(key);
    }

    public String getProductId() {
        return productId;
    }
}
