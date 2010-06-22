package org.esa.beam.framework.ui.product.tree;

import org.esa.beam.framework.datamodel.ProductNode;
import org.esa.beam.framework.datamodel.ProductNodeGroup;

class ProductNodeTN extends AbstractTN {

    private final ProductNode productNode;

    ProductNodeTN(String name, ProductNode productNode, AbstractTN parent) {
        super(name, productNode, parent);
        this.productNode = productNode;
    }

    public ProductNode getProductNode() {
        return productNode;
    }

    @Override
    public AbstractTN getChildAt(int index) {
        if (productNode instanceof ProductNodeGroup) {
            ProductNodeGroup nodeGroup = (ProductNodeGroup) productNode;
            ProductNode node = nodeGroup.get(index);
            return new ProductNodeTN(node.getName(), node, this);
        }
        throw new IndexOutOfBoundsException("node has no children");
    }

    @Override
    public int getChildCount() {
        if (productNode instanceof ProductNodeGroup) {
            ProductNodeGroup nodeGroup = (ProductNodeGroup) productNode;
            return nodeGroup.getNodeCount();
        }
        return 0;
    }

    @Override
    protected int getIndex(AbstractTN child) {
        if (productNode instanceof ProductNodeGroup) {
            ProductNodeGroup nodeGroup = (ProductNodeGroup) productNode;
            ProductNodeTN childNodeTN = (ProductNodeTN) child;
            return nodeGroup.indexOf(childNodeTN.getProductNode().getName());
        }
        throw new IndexOutOfBoundsException("node has no children");
    }
}
