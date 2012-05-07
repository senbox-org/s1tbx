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
package org.esa.beam.framework.ui.product;

import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.MetadataElement;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.TiePointGrid;

import java.util.EventListener;

// todo - this is a stupid interface, use SelectionService/SelectionProvide instead (nf, 10.2009)


/**
 * A listener which is listening for events occurring in a <code>ProductTree</code>.
 *
 * @author Norman Fomferra
 * @author Sabine Embacher
 * @version $Revision$ $Date$
 * @see org.esa.beam.framework.ui.product.ProductTree
 */
public interface ProductTreeListener extends EventListener {

    /**
     * Called when a product has been added to the tree.
     *
     * @param product The product.
     */
    void productAdded(Product product);

    /**
     * Called when a product has been removed from the tree.
     *
     * @param product The product.
     */
    void productRemoved(Product product);

    /**
     * Called when a product has been selected in the tree.
     *
     * @param product    The selected product.
     * @param clickCount The number of mouse clicks.
     */
    void productSelected(Product product, int clickCount);

    /**
     * Called when a product's metadata element has been selected in the tree.
     *
     * @param matadataElement The selected metadata element.
     * @param clickCount      The number of mouse clicks.
     */
    void metadataElementSelected(MetadataElement matadataElement, int clickCount);

    /**
     * Called when a product's tie-point grid has been selected in the tree.
     *
     * @param tiePointGrid The selected tie-point grid.
     * @param clickCount   The number of mouse clicks.
     */
    void tiePointGridSelected(TiePointGrid tiePointGrid, int clickCount);

    /**
     * Called when a product's band has been selected in the tree.
     *
     * @param band       The selected band.
     * @param clickCount The number of mouse clicks.
     */
    void bandSelected(Band band, int clickCount);
}
