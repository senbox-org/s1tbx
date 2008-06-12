/*
 * $Id: ProductTreeListener.java,v 1.1 2006/10/10 14:47:37 norman Exp $
 *
 * Copyright (C) 2002 by Brockmann Consult (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation. This program is distributed in the hope it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.esa.beam.framework.ui.product;

import java.util.EventListener;

import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.MetadataElement;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.TiePointGrid;

/**
 * A listener which is listening for events occuring in a <code>ProductTree</code>.
 *
 * @author Norman Fomferra
 * @author Sabine Embacher
 * @version $Revision$ $Date$
 * @see org.esa.beam.framework.ui.product.ProductTree
 */
public interface ProductTreeListener extends EventListener {

    /**
     * Called when a product has been added to the tree.
     */
    void productAdded(Product product);

    /**
     * Called when a product has been removed from the tree.
     */
    void productRemoved(Product product);

    /**
     * Called when a product has been selected in the tree.
     */
    void productSelected(Product product, int clickCount);

    /**
     * Called when a product's metadata element has been selected in the tree.
     */
    void metadataElementSelected(MetadataElement group, int clickCount);

    /**
     * Called when a product's tie-point grid has been selected in the tree.
     */
    void tiePointGridSelected(TiePointGrid tiePointGrid, int clickCount);

    /**
     * Called when a product's band has been selected in the tree.
     */
    void bandSelected(Band band, int clickCount);
}
