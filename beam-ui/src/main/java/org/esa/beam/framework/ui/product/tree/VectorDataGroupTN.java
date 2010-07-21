/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */

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
