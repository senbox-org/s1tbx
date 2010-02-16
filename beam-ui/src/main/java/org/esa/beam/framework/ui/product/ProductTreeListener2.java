/*
 * $Id: ProductTreeListener.java,v 1.1 2006/10/10 14:47:37 norman Exp $
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
package org.esa.beam.framework.ui.product;

import org.esa.beam.framework.datamodel.ProductNode;
import org.esa.beam.framework.datamodel.VectorDataNode;

// todo - this is a stupid interface, use SelectionService/SelectionProvide instead (nf, 10.2009)


/**
 * A listener which is listening for events occuring in a <code>ProductTree</code>.
 *
 * @author Norman Fomferra
 * @author Sabine Embacher
 * @version $Revision$ $Date$
 * @see ProductTree
 * @since BEAM 4.7
 */
public interface ProductTreeListener2 extends ProductTreeListener {
    /**
     * Called when a product's node has been selected in the tree.
     *
     * @param productNode The selected product node.
     * @param clickCount  The number of mouse clicks.
     *
     * @since BEAM 4.7
     */
    void productNodeSelected(ProductNode productNode, int clickCount);

    /**
     * Called when a product's node has been selected in the tree.
     *
     * @param vectorDataNode The selected vector data.
     * @param clickCount The number of mouse clicks.
     *
     * @since BEAM 4.7
     */
    void vectorDataSelected(VectorDataNode vectorDataNode, int clickCount);
}