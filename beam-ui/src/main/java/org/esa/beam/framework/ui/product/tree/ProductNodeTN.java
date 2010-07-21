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
