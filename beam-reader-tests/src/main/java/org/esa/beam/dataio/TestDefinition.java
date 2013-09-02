package org.esa.beam.dataio;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.esa.beam.framework.dataio.ProductReaderPlugIn;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

class TestDefinition {

    @JsonProperty
    private ArrayList<String> intendedProductIds;
    @JsonProperty
    private ArrayList<String> suitableProductIds;
    @JsonProperty
    private ArrayList<ExpectedContent> expectedContentList;

    private transient ProductReaderPlugIn productReaderPlugin;
    private final ArrayList<TestProduct> testProducts;
    private HashMap<String, ExpectedDataset> expectedDatasets;

    TestDefinition() {
        intendedProductIds = new ArrayList<String>();
        suitableProductIds = new ArrayList<String>();
        expectedContentList = new ArrayList<ExpectedContent>();
        testProducts = new ArrayList<TestProduct>();
        expectedDatasets = new HashMap<String, ExpectedDataset>();
    }

    ArrayList<String> getIntendedProductIds() {
        return intendedProductIds;
    }

    ArrayList<String> getSuitableProductIds() {
        return suitableProductIds;
    }

    ExpectedContent getExpectedContent(String productId) {
        final ExpectedDataset expectedDataset = expectedDatasets.get(productId);
        if (expectedDataset != null) {
            return expectedDataset.getExpectedContent();
        }

        return null;
    }

    ProductReaderPlugIn getProductReaderPlugin() {
        return productReaderPlugin;
    }

    void setProductReaderPlugin(ProductReaderPlugIn productReaderPlugin) {
        this.productReaderPlugin = productReaderPlugin;
    }

    public List<TestProduct> getAllProducts() {
        return testProducts;
    }

    public void addTestProducts(List<TestProduct> testProducts) {
        this.testProducts.addAll(testProducts);
    }

    public ExpectedDataset getExpectedDataset(String id) {
        return expectedDatasets.get(id);
    }

    public void addExpectedDataset(ExpectedDataset expectedDataset) {
        final String id = expectedDataset.getId();
        expectedDatasets.put(id, expectedDataset);
    }
}
