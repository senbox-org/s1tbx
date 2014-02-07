/*
 * Copyright (C) 2014 Brockmann Consult GmbH (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */

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

    int size() {
        return testProducts.size();
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
