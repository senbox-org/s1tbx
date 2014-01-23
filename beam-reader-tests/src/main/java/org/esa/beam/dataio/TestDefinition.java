/*
 * Copyright (C) 2013 Brockmann Consult GmbH (info@brockmann-consult.de)
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

import org.esa.beam.framework.dataio.DecodeQualification;
import org.esa.beam.framework.dataio.ProductReaderPlugIn;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class TestDefinition {

    private transient ProductReaderPlugIn productReaderPlugin;
    private final ArrayList<TestProduct> testProducts;
    private final Map<String, ExpectedDataset> expectedDatasetsMap;
    private final List<ExpectedDataset> expectedDatasetsList;

    TestDefinition() {
        testProducts = new ArrayList<TestProduct>();
        expectedDatasetsMap = new HashMap<String, ExpectedDataset>();
        expectedDatasetsList = new ArrayList<ExpectedDataset>();
    }

    List<String> getDecodableProductIds() {
        final ArrayList<String> result = new ArrayList<String>();

        for (ExpectedDataset dataset : expectedDatasetsList) {
            DecodeQualification decodeQualification = dataset.getDecodeQualification();
            if (decodeQualification == DecodeQualification.INTENDED ||
                decodeQualification == DecodeQualification.SUITABLE) {
                result.add(dataset.getId());
            }
        }
        return result;
    }

    ExpectedContent getExpectedContent(String productId) {
        final ExpectedDataset expectedDataset = expectedDatasetsMap.get(productId);
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
        return expectedDatasetsMap.get(id);
    }

    public void addExpectedDataset(ExpectedDataset expectedDataset) {
        final String id = expectedDataset.getId();
        expectedDatasetsMap.put(id, expectedDataset);
        expectedDatasetsList.add(expectedDataset);
    }
}
