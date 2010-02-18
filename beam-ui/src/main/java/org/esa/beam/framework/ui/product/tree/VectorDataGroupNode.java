package org.esa.beam.framework.ui.product.tree;

import org.esa.beam.framework.datamodel.ProductNodeGroup;
import org.esa.beam.framework.datamodel.VectorDataNode;

/**
 * User: Marco
 * Date: 13.02.2010
 */
class VectorDataGroupNode extends ProductNodeNode {
    private ProductNodeGroup<VectorDataNode> vectorGroup;

    VectorDataGroupNode(String name, ProductNodeGroup<VectorDataNode> vectorGroup, ProductNode parent) {
        super(name, vectorGroup, parent);
        this.vectorGroup = vectorGroup;
    }

    @Override
    public ProductTreeNode getChildAt(int index) {
        int childIndex = -1;
        VectorDataNode[] vectorNodes = vectorGroup.toArray(new VectorDataNode[vectorGroup.getNodeCount()]);
        for (VectorDataNode vectorDataNode : vectorNodes) {
            if (mustCount(vectorDataNode)) {
                childIndex++;
                if (childIndex == index) {
                    return new ProductNodeNode(vectorDataNode.getName(), vectorDataNode, this);
                }
            }
        }
        throw new IndexOutOfBoundsException(String.format("No child for index <%d>.", index));
    }

    @Override
    public int getChildCount() {
        int childCount = 0;
        VectorDataNode[] vectorNodes = vectorGroup.toArray(new VectorDataNode[vectorGroup.getNodeCount()]);
        for (VectorDataNode vectorDataNode : vectorNodes) {
            if (mustCount(vectorDataNode)) {
                childCount++;
            }
        }
        return childCount;
    }

    @Override
    protected int getIndex(ProductTreeNode child) {
        int childIndex = -1;
        VectorDataNode[] vectorNodes = vectorGroup.toArray(new VectorDataNode[vectorGroup.getNodeCount()]);
        for (VectorDataNode vectorDataNode : vectorNodes) {
            if (mustCount(vectorDataNode)) {
                childIndex++;
                if (child.getContent() == vectorDataNode) {
                    return childIndex;
                }
            }
        }
        return childIndex;
    }

    private boolean mustCount(VectorDataNode vectorDataNode) {
        return !vectorDataNode.isInternalNode() || !vectorDataNode.getFeatureCollection().isEmpty();
    }
}
