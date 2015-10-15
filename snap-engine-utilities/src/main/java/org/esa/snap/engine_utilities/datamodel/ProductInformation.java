/*
 * Copyright (C) 2014 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.snap.engine_utilities.datamodel;

import org.esa.snap.core.datamodel.MetadataElement;
import org.esa.snap.core.datamodel.Product;

/**
 * Summary of an output product
 */
public class ProductInformation {

    public static MetadataElement getProductInformation(final MetadataElement abstractedMetadata) {
        MetadataElement productElem = abstractedMetadata.getElement("Product_Information");
        if (productElem == null) {
            productElem = new MetadataElement("Product_Information");
            abstractedMetadata.addElement(productElem);
        }
        return productElem;
    }

    public static MetadataElement getInputProducts(final Product product) {
        final MetadataElement abstractedMetadata = AbstractMetadata.getAbstractedMetadata(product);
        return getInputProducts(abstractedMetadata);
    }

    private static MetadataElement getInputProducts(final MetadataElement abstractedMetadata) {
        final MetadataElement productElem = getProductInformation(abstractedMetadata);
        MetadataElement inputElem = productElem.getElement("InputProducts");
        if (inputElem == null) {
            inputElem = new MetadataElement("InputProducts");
            productElem.addElement(inputElem);
        }
        return inputElem;
    }
}
