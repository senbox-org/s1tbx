package org.esa.beam.dataio;


import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.Iterator;

class ProductList implements Iterable<TestProduct> {

    @JsonProperty
    private ArrayList<TestProduct> testProducts;

    ProductList() {
        this.testProducts = new ArrayList<TestProduct>();
    }

    TestProduct getById(String id) {
        for (TestProduct testProduct : testProducts) {
            if (testProduct.getId().equalsIgnoreCase(id)) {
                return testProduct;
            }
        }
        return null;
    }

    void add(TestProduct testProduct) {
        testProducts.add(testProduct);
    }

    @Override
    public Iterator<TestProduct> iterator() {
        return testProducts.iterator();
    }
}
