package org.esa.beam.dataio;


import java.util.ArrayList;
import java.util.Iterator;

class ProductList implements Iterable<TestProduct> {

    private ArrayList<TestProduct> testProducts;

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

    @Override
    public Iterator<TestProduct> iterator() {
        return testProducts.iterator();
    }
}
