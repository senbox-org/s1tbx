package org.esa.beam.framework.ui.product.tree;

import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.TiePointGrid;

class TiePointGroupNode extends ProductTreeNode {
    private Product product;

    TiePointGroupNode(String name, Product product, ProductTreeNode parent) {
        super(name, null, parent);
        this.product = product;
    }

    @Override
    public ProductTreeNode getChildAt(int index) {
        TiePointGrid grid = product.getTiePointGridAt(index);
        return new ProductNodeNode(grid.getName(), grid, this);
    }

    @Override
    public int getChildCount() {
        return product.getNumTiePointGrids();
    }

    @Override
    protected int getIndex(ProductTreeNode child) {
        ProductNodeNode node = (ProductNodeNode) child;
        return product.getTiePointGridIndex(node.getProductNode().getName());
    }
}
