/*
 * $Id: $
 * 
 * Copyright (C) 2009 by Brockmann Consult (info@brockmann-consult.de)
 * 
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation. This program is distributed in the hope it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 59 Temple
 * Place - Suite 330, Boston, MA 02111-1307, USA.
 */
package org.esa.beam.framework.gpf.internal;

import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Mask;
import org.esa.beam.framework.datamodel.Pin;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductNode;
import org.esa.beam.framework.datamodel.TiePointGrid;

/**
 * Returns the names of all elements of the given product that are of the given
 * subtype of {@code ProductNode}.
 * 
 * @author Marco Zuehlke
 * @since BEAM 4.7
 */
public class ProductNodeValues {

    /**
     * Returns the names of all elements of the given product that are of the
     * given subtype of {@code ProductNode}.
     * 
     * @param product
     *            The product.
     * @param productNodeType
     *            The subtype of {@code ProductNode}.
     * @return the names of all matching elements
     */
    public static String[] getNames(Product product, Class<? extends ProductNode> productNodeType) {
        return getNames(product, productNodeType, false);
    }

    /**
     * Returns the names of all elements of the given product that are of the
     * given subtype of {@code ProductNode}. Optionally an empty value is included.
     * 
     * @param product
     *            The product.
     * @param productNodeType
     *            The subtype of {@code ProductNode}.
     * @param includeEmptyValue
     *            If {code true} and empty {@code String} is included as the first value.
     * @return the names of all matching elements
     */
    public static String[] getNames(Product product, Class<? extends ProductNode> productNodeType,
                                          boolean includeEmptyValue) {
        String[] allValues;
        if (includeEmptyValue) {
            String[] valueNames = getValueNames(product, productNodeType);
            allValues = new String[valueNames.length + 1];
            allValues[0] = "";
            System.arraycopy(valueNames, 0, allValues, 1, valueNames.length);
        } else {
            allValues = getValueNames(product, productNodeType);
        }
        return allValues;
    }

    private static String[] getValueNames(Product product, Class<? extends ProductNode> productNodeType) {
        if (productNodeType == Band.class) {
            return product.getBandNames();
        } else if (productNodeType == Mask.class) {
            return product.getMaskGroup().getNodeNames();
        } else if (productNodeType == TiePointGrid.class) {
            return product.getTiePointGridNames();            
        }
        throw new IllegalArgumentException("Unsupported 'productNodeType': " + productNodeType);
    }
}
