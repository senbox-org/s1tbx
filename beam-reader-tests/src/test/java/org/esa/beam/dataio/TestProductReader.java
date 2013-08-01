package org.esa.beam.dataio;

import org.esa.beam.framework.dataio.ProductReaderPlugIn;

import java.util.ArrayList;

class TestProductReader {

    private ArrayList<String> intendedProductIds;
    private ArrayList<String> suitableProductIds;
    private ProductReaderPlugIn productReaderPlugin;

    TestProductReader() {
        intendedProductIds = new ArrayList<String>();
        suitableProductIds = new ArrayList<String>();
    }

    ArrayList<String> getIntendedProductIds() {
        return intendedProductIds;
    }

    void setIntendedProductIds(ArrayList<String> intendedProductIds) {
        this.intendedProductIds = intendedProductIds;
    }

    ArrayList<String> getSuitableProductIds() {
        return suitableProductIds;
    }

    void setSuitableProductIds(ArrayList<String> suitableProductIds) {
        this.suitableProductIds = suitableProductIds;
    }

    public ProductReaderPlugIn getProductReaderPlugin() {
        return productReaderPlugin;
    }

    void setProductReaderPlugin(ProductReaderPlugIn productReaderPlugin) {
        this.productReaderPlugin = productReaderPlugin;
    }
}
