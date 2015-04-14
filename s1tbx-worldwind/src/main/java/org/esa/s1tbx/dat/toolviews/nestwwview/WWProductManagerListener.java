package org.esa.s1tbx.dat.toolviews.nestwwview;

import org.esa.snap.framework.datamodel.Product;
import org.esa.snap.framework.datamodel.ProductManager;
import org.esa.snap.rcp.SnapApp;

/**
 * Created by luis on 30/03/2015.
 */
public class WWProductManagerListener implements ProductManager.Listener {

    private final WWView wwView;

    public WWProductManagerListener(final WWView wwView) {
        this.wwView = wwView;
    }

    @Override
    public void productAdded(ProductManager.Event event) {
        final Product product = event.getProduct();
        wwView.setSelectedProduct(product);
        wwView.setProducts(SnapApp.getDefault().getProductManager().getProducts());
    }

    @Override
    public void productRemoved(ProductManager.Event event) {
        final Product product = event.getProduct();
        if (wwView.getSelectedProduct() == product) {
            wwView.setSelectedProduct(null);
        }
        wwView.removeProduct(product);
    }
}
