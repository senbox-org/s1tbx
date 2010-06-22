package org.esa.beam.framework.ui.product.tree;

import org.esa.beam.framework.datamodel.ProductNodeGroup;
import org.esa.beam.framework.datamodel.VectorDataNode;

/**
 * Overrides methods in ProductNodeTN in order to account for empty VectorDataNodes.
 */
class VectorDataGroupTN extends ProductNodeTN {
    private ProductNodeGroup<VectorDataNode> vectorGroup;

    VectorDataGroupTN(String name, ProductNodeGroup<VectorDataNode> vectorGroup, ProductTN parent) {
        super(name, vectorGroup, parent);
        this.vectorGroup = vectorGroup;
    }

    @Override
    public AbstractTN getChildAt(int index) {
        int childIndex = -1;
        VectorDataNode[] vectorNodes = vectorGroup.toArray(new VectorDataNode[vectorGroup.getNodeCount()]);
        for (VectorDataNode vectorDataNode : vectorNodes) {
            if (mustCount(vectorDataNode)) {
                childIndex++;
                if (childIndex == index) {
                    return new ProductNodeTN(vectorDataNode.getName(), vectorDataNode, this);
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
    protected int getIndex(AbstractTN child) {
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
