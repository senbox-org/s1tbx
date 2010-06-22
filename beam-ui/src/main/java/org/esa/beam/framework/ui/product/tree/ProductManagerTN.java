package org.esa.beam.framework.ui.product.tree;

import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductManager;

/**
 * User: Marco
 * Date: 13.02.2010
 */
class ProductManagerTN extends AbstractTN {

    private final ProductManager manager;

    ProductManagerTN(ProductManager manager) {
        super("Open Products", manager, null);
        this.manager = manager;
    }

    public ProductManager getProductManager() {
        return manager;
    }

    @Override
    public AbstractTN getChildAt(int index) {
        return new ProductTN(manager.getProduct(index), this);
    }

    @Override
    public int getChildCount() {
        return manager.getProductCount();
    }

    @Override
    protected int getIndex(AbstractTN child) {
        Product product = ((ProductTN) child).getProduct();
        return manager.getProductIndex(product);
    }

}
