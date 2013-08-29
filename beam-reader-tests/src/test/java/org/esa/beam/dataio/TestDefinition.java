package org.esa.beam.dataio;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.esa.beam.framework.dataio.ProductReaderPlugIn;

import java.util.ArrayList;

class TestDefinition {

    @JsonProperty
    private ArrayList<String> intendedProductIds;
    @JsonProperty
    private ArrayList<String> suitableProductIds;
    @JsonProperty
    private ArrayList<ExpectedContent> expectedContentList;

    private transient ProductReaderPlugIn productReaderPlugin;

    TestDefinition() {
        intendedProductIds = new ArrayList<String>();
        suitableProductIds = new ArrayList<String>();
        expectedContentList = new ArrayList<ExpectedContent>();
    }

    ArrayList<String> getIntendedProductIds() {
        return intendedProductIds;
    }

    ArrayList<String> getSuitableProductIds() {
        return suitableProductIds;
    }

    ExpectedContent getExpectedContent(String productId) {
        for (ExpectedContent expectedContent : expectedContentList) {
            if (expectedContent.getId().equalsIgnoreCase(productId)) {
                return expectedContent;
            }
        }

        return null;
    }

    ProductReaderPlugIn getProductReaderPlugin() {
        return productReaderPlugin;
    }

    void setProductReaderPlugin(ProductReaderPlugIn productReaderPlugin) {
        this.productReaderPlugin = productReaderPlugin;
    }

}
