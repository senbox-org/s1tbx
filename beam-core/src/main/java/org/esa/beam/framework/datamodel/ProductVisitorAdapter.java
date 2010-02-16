/*
 * $Id: ProductVisitorAdapter.java,v 1.1.1.1 2006/09/11 08:16:45 norman Exp $
 *
 * Copyright (C) 2002 by Brockmann Consult (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation. This program is distributed in the hope it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.esa.beam.framework.datamodel;


import java.util.Stack;

/**
 * A default implementation of the <code>ProductVisitor</code> interface.
 *
 * @author Norman Fomferra
 * @version $Revision$ $Date$
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
    public void visit(BitmaskDef bitmaskDef) {
    }

    @Override
    public void visit(ProductNodeGroup group) {
    }

    @Override
    public void visit(Mask mask) {
    }

    @Override
    public void visit(VectorDataNode dataNode) {
    }
}
