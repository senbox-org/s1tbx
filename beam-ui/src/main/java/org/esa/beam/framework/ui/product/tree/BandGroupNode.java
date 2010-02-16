package org.esa.beam.framework.ui.product.tree;

import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;

/**
* User: Marco
* Date: 12.02.2010
*/
class BandGroupNode extends ProductTreeNode {
    private Product product;

    BandGroupNode(String name, Product product, ProductTreeNode parent) {
        super(name, null, parent);
        this.product = product;
    }

    @Override
    public ProductTreeNode getChildAt(int index) {
        Band band = product.getBandAt(index);
        return new ProductNodeNode(band.getName(), band, this);
    }

    @Override
    public int getChildCount() {
        return product.getNumBands();
    }

    @Override
    protected int getIndex(ProductTreeNode child) {
        ProductNodeNode node = (ProductNodeNode) child;
        return product.getBandIndex(node.getProductNode().getName());
    }

}
