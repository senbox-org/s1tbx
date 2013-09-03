package org.esa.beam.dataio;

import org.esa.beam.framework.dataio.DecodeQualification;
import org.esa.beam.framework.dataio.ProductReaderPlugIn;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

class TestDefinition {

    private transient ProductReaderPlugIn productReaderPlugin;
    private final ArrayList<TestProduct> testProducts;
    private HashMap<String, ExpectedDataset> expectedDatasets;

    TestDefinition() {
        testProducts = new ArrayList<TestProduct>();
        expectedDatasets = new HashMap<String, ExpectedDataset>();
    }

    List<String> getIntendedProductIds() {
        final ArrayList<String> result = new ArrayList<String>();

        final Collection<ExpectedDataset> values = expectedDatasets.values();
        for (ExpectedDataset dataset : values) {
            if (dataset.getDecodeQualification() == DecodeQualification.INTENDED) {
                result.add(dataset.getId());
            }
        }
        return result;
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
