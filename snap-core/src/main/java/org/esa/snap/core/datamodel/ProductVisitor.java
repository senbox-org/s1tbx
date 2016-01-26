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

/**
 * A visitor for a product and all other product nodes. This interface is part of the <i>visitor pattern</i> used to
 * visit all nodes of a data product. Implementations of this interface can be passed to the <code>acceptVisitor</code>
 * method of an <code>Product</code> (or any other <code>ProductNode</code>).
 *
 * @author Norman Fomferra
 * @version $Revision$ $Date$
 * @see Product#acceptVisitor(ProductVisitor)
 * @see ProductNode#acceptVisitor(ProductVisitor)
 */
public interface ProductVisitor {

    /**
     * Visits a product.
     *
     * @param product the product to be visited
     */
    void visit(Product product);

    /**
     * Visits a group whithin a product.
     *
     * @param group the group to be visited
     */
    void visit(MetadataElement group);

    /**
     * Visits a tie-point grid within a product or group.
     *
     * @param grid the tie-point grid to be visited
     */
    void visit(TiePointGrid grid);

    /**
     * Visits a band within a product or group.
     *
     * @param band the band to be visited
     */
    void visit(Band band);

    /**
     * Visits a virtual band.
     *
     * @param virtualBand the bitmask definition to be visited
     */
    void visit(VirtualBand virtualBand);

    /**
     * Visits an attribute.
     *
     * @param attribute the attribute to be visited
     */
    void visit(MetadataAttribute attribute);

    /**
     * Visits a flag coding.
     *
     * @param flagCoding the flag coding to be visited
     */
    void visit(FlagCoding flagCoding);

    /**
     * Visits an index coding.
     *
     * @param indexCoding the index coding to be visited
     */
    void visit(IndexCoding indexCoding);

    /**
     * Visits a node group.
     *
     * @param group the group to be visited
     */
    void visit(ProductNodeGroup group);

    /**
     * Visits a node group.
     *
     * @param mask the mask to be visited
     */
    void visit(Mask mask);

    /**
     * Visits a node group.
     *
     * @param vectorDataNode the group to be visited
     */
    void visit(VectorDataNode vectorDataNode);

    /**
     * Visits a node group.
     *
     * @param ql the Quicklook to be visited
     */
    void visit(Quicklook ql);
}
