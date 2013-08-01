package org.esa.beam.dataio;


import java.util.ArrayList;

class ProductList {

    private ArrayList<TestProduct> testProducts;

    ArrayList<TestProduct> getTestProducts() {
        return testProducts;
    }

    void setTestProducts(ArrayList<TestProduct> testProducts) {
        this.testProducts = testProducts;
    }

    TestProduct geById(String id) {
        for (TestProduct testProduct : testProducts) {
            if (testProduct.getId().equalsIgnoreCase(id)) {
                return testProduct;
            }
        }
        return null;
    }
}
