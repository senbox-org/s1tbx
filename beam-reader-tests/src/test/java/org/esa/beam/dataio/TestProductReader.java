package org.esa.beam.dataio;

import org.esa.beam.framework.dataio.ProductReaderPlugIn;

import java.util.ArrayList;

class TestProductReader {

    private ArrayList<String> intendedProductIds;
    private ArrayList<String> suitableProductIds;
    private ProductReaderPlugIn productReaderPlugin;
    private ArrayList<ExpectedContent> expectedContentList;

    TestProductReader() {
        intendedProductIds = new ArrayList<String>();
        suitableProductIds = new ArrayList<String>();
        expectedContentList = new ArrayList<ExpectedContent>();
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

    ArrayList<ExpectedContent> getExpectedContentList() {
        return expectedContentList;
    }

    ExpectedContent getExpectedContent(String productId) {
        for (ExpectedContent expectedContent : expectedContentList) {
            if (expectedContent.getId().equalsIgnoreCase(productId)) {
                return expectedContent;
            }
        }

        return null;
    }

    void setExpectedContentList(ArrayList<ExpectedContent> expectedContentList) {
        this.expectedContentList = expectedContentList;
    }
}
