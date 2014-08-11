package org.esa.snap.datamodel;

import org.esa.beam.framework.datamodel.MetadataElement;
import org.esa.beam.framework.datamodel.Product;

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
