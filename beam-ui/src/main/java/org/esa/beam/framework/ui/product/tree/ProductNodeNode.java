package org.esa.beam.framework.ui.product.tree;

import org.esa.beam.framework.datamodel.ProductNode;
import org.esa.beam.framework.datamodel.ProductNodeGroup;

class ProductNodeNode extends ProductTreeNode {

    private final ProductNode productNode;

    ProductNodeNode(String name, ProductNode productNode, ProductTreeNode parent) {
        super(name, productNode, parent);
        this.productNode = productNode;
    }

    public ProductNode getProductNode() {
        return productNode;
    }

    @Override
    public ProductTreeNode getChildAt(int index) {
        if (productNode instanceof ProductNodeGroup) {
            ProductNodeGroup nodeGroup = (ProductNodeGroup) productNode;
            ProductNode node = nodeGroup.get(index);
            return new ProductNodeNode(node.getName(), node, this);
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
    protected int getIndex(ProductTreeNode child) {
        if (productNode instanceof ProductNodeGroup) {
            ProductNodeGroup nodeGroup = (ProductNodeGroup) productNode;
            ProductNodeNode childNode = (ProductNodeNode) child;
            return nodeGroup.indexOf(childNode.getProductNode().getName());
        }
        throw new IndexOutOfBoundsException("node has no children");
    }
}
