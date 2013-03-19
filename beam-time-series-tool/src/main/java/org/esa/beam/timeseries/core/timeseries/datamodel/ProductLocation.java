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

package org.esa.beam.timeseries.core.timeseries.datamodel;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.framework.datamodel.Product;

import java.util.Map;

/**
 * Class representing a location in the filesystem where data products reside.
 *
 * <p><i>Note that this class is not yet public API. Interface may change in future releases.</i></p>
 *
 * @author Marco Zühlke
 * @author Thomas Storm
 */
public class ProductLocation {

    private final ProductLocationType productLocationType;
    private final String path;
    private Map<String, Product> products;

    public ProductLocation(ProductLocationType productLocationType, String path) {
        this.productLocationType = productLocationType;
        this.path = path;
    }

    public String getPath() {
        return path;
    }

    public ProductLocationType getProductLocationType() {
        return productLocationType;
    }

    public synchronized void loadProducts(ProgressMonitor pm) {
        products = productLocationType.findProducts(path, pm);
    }

    /**
     * Returns the live map of products of the product location.
     * @return the live map of this product location's products.
     */
    public Map<String, Product> getProducts() {
        if (products == null) {
            loadProducts(ProgressMonitor.NULL);
        }
        return products;
    }

    public synchronized void closeProducts() {
        if (products != null) {
            for (Product product : products.values()) {
                product.dispose();
            }
            products = null;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ProductLocation that = (ProductLocation) o;

        if (!path.equals(that.path)) return false;
        if (productLocationType != that.productLocationType) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = productLocationType.hashCode();
        result = 31 * result + path.hashCode();
        return result;
    }
}
