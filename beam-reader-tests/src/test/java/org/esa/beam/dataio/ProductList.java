package org.esa.beam.dataio;


import java.util.ArrayList;
import java.util.Iterator;

class ProductList implements Iterable<TestProduct> {

    private ArrayList<TestProduct> testProducts;

    ProductList() {
        this.testProducts = new ArrayList<TestProduct>();
    }

    // used by JSON
    @SuppressWarnings("UnusedDeclaration")
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

    public void add(TestProduct testProduct) {
        testProducts.add(testProduct);
    }
}
