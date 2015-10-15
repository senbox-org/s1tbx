/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.snap.pixex.output;

import org.esa.snap.core.datamodel.Product;

import java.io.File;

/**
 * Used to identify a product within the {@link org.esa.snap.measurement.writer.MeasurementWriter}.
 */
class ProductIdentifier {

    private final String productName;
    private final String productType;
    private final String productLocation;

    static ProductIdentifier create(Product product) {
        String location = getProductLocation(product);
        return new ProductIdentifier(product.getName(), product.getProductType(), location);
    }

    private static String getProductLocation(Product product) {
        final File fileLocation = product.getFileLocation();
        if (fileLocation != null) {
            return fileLocation.getAbsolutePath();
        } else {
            return String.format("Not saved to disk [%s]", product.getName());
        }
    }

    ProductIdentifier(String name, String type, String location) {
        productName = name;
        productType = type;
        productLocation = location;
    }

    public String getLocation() {
        return productLocation;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        ProductIdentifier that = (ProductIdentifier) o;

        if (!productLocation.equals(that.productLocation)) {
            return false;
        }
        if (!productName.equals(that.productName)) {
            return false;
        }
        if (!productType.equals(that.productType)) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = productName.hashCode();
        result = 31 * result + productType.hashCode();
        result = 31 * result + productLocation.hashCode();
        return result;
    }
}
