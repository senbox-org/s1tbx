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
package org.esa.snap.core.datamodel;


import org.esa.snap.core.datamodel.quicklooks.Quicklook;

import java.util.Stack;

/**
 * A default implementation of the <code>ProductVisitor</code> interface.
 *
 * @author Norman Fomferra
 */
public class ProductVisitorAdapter implements ProductVisitor {

    protected Stack<ProductNode> stack = new Stack<ProductNode>();

    public ProductVisitorAdapter() {
    }

    protected void push(ProductNode node) {
        stack.push(node);
    }

    protected ProductNode pop() {
        return stack.pop();
    }

    protected ProductNode peek() {
        return stack.peek();
    }

    @Override
    public void visit(Product product) {
    }

    @Override
    public void visit(TiePointGrid grid) {
    }

    @Override
    public void visit(Band band) {
    }

    @Override
    public void visit(VirtualBand virtualBand) {
    }

    @Override
    public void visit(MetadataAttribute attribute) {
    }

    @Override
    public void visit(MetadataElement group) {
    }

    @Override
    public void visit(FlagCoding flagCoding) {
    }

    @Override
    public void visit(IndexCoding indexCoding) {
    }

    @Override
    public void visit(ProductNodeGroup group) {
    }

    @Override
    public void visit(Mask mask) {
    }

    @Override
    public void visit(Quicklook ql) {
    }

    @Override
    public void visit(VectorDataNode dataNode) {
    }
}
