package org.esa.nest.dat.toolviews.nestwwview;

import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.MetadataElement;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.TiePointGrid;
import org.esa.beam.framework.ui.product.ProductTreeListener;
import org.esa.beam.visat.VisatApp;

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
        wwView.setProducts(VisatApp.getApp().getProductManager().getProducts());
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
