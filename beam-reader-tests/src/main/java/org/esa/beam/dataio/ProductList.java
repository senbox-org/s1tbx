package org.esa.beam.dataio;


import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

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

    List<TestProduct> getAll() {
        return testProducts;
    }

    public String[] getAllIds() {
        final String[] ids = new String[testProducts.size()];
        int index = 0;
        for (TestProduct product : testProducts) {
            ids[index] = product.getId();
            ++index;
        }

        return ids;
    }
}
