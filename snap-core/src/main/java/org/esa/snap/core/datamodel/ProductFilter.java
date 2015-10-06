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

package org.esa.snap.core.datamodel;

/**
 * A filter for products.
 *
 * @author Marco Peters
 */
public interface ProductFilter {

    /**
     * A filter that accepts all products (does not filter at all).
     */
    ProductFilter ALL = new ProductFilter() {
        @Override
        public boolean accept(Product product) {
            return true;
        }
    };

    /**
     * @param product The product.
     * @return {@code true}, if the given {@code product} is accepted by this filter.
     */
    boolean accept(Product product);
}
