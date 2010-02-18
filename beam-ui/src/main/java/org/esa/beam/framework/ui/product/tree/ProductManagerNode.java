package org.esa.beam.framework.ui.product.tree;

import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductManager;

/**
 * User: Marco
 * Date: 13.02.2010
 */
class ProductManagerNode extends ProductTreeNode {

    private final ProductManager manager;

    ProductManagerNode(ProductManager manager) {
        super("Open Products", manager, null);
        this.manager = manager;
    }

    public ProductManager getProductManager() {
        return manager;
    }

    @Override
    public ProductTreeNode getChildAt(int index) {
        return new ProductNode(manager.getProduct(index), this);
    }

    @Override
    public int getChildCount() {
        return manager.getProductCount();
    }

    @Override
    protected int getIndex(ProductTreeNode child) {
        Product product = ((ProductNode) child).getProduct();
        return manager.getProductIndex(product);
    }

}
