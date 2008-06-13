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

    protected Stack stack = new Stack();

    public ProductVisitorAdapter() {
    }

    protected void push(Object node) {
        stack.push(node);
    }

    protected Object pop() {
        return stack.pop();
    }

    protected Object peek() {
        return stack.peek();
    }

    public void visit(Product product) {
    }

    public void visit(TiePointGrid grid) {
    }

    public void visit(Band band) {
    }

    public void visit(VirtualBand virtualBand) {
    }

    public void visit(MetadataAttribute attribute) {
    }

    public void visit(MetadataElement group) {
    }

    public void visit(FlagCoding flagCoding) {
    }

    public void visit(IndexCoding indexCoding) {
    }

    public void visit(BitmaskDef bitmaskDef) {
    }

    public void visit(ProductNodeGroup group) {
    }
}
