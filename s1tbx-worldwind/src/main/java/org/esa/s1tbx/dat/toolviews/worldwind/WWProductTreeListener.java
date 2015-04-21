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
package org.esa.s1tbx.dat.toolviews.worldwind;

import org.esa.snap.framework.datamodel.Band;
import org.esa.snap.framework.datamodel.MetadataElement;
import org.esa.snap.framework.datamodel.Product;
import org.esa.snap.framework.datamodel.TiePointGrid;
import org.esa.snap.framework.ui.product.ProductTreeListener;
import org.esa.snap.rcp.SnapApp;

/**
 * listens for events from product tree
 */
public class WWProductTreeListener implements ProductTreeListener {
    private final WWView wwView;

    public WWProductTreeListener(final WWView wwView) {
        this.wwView = wwView;
    }

    public void productAdded(final Product product) {
        wwView.setSelectedProduct(product);
        wwView.setProducts(SnapApp.getDefault().getProductManager().getProducts());
    }

    public void productRemoved(final Product product) {
        wwView.removeProduct(product);
    }

    public void productSelected(final Product product, final int clickCount) {
        wwView.setSelectedProduct(product);
    }

    public void metadataElementSelected(final MetadataElement group, final int clickCount) {
        wwView.setSelectedProduct(group.getProduct());
    }

    public void tiePointGridSelected(final TiePointGrid tiePointGrid, final int clickCount) {
        wwView.setSelectedProduct(tiePointGrid.getProduct());
    }

    public void bandSelected(final Band band, final int clickCount) {
        wwView.setSelectedProduct(band.getProduct());
    }
}
