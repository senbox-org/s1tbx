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


/**
 * A filter for abstract product nodes.
 * <p> Instances of this interface may be passed to the {@link ProductNodeList#createSubset(ProductNodeFilter)} method.
 */
public interface ProductNodeFilter<T extends ProductNode> {

    /**
     * Tests whether or not the specified abstract product node should be included in a product node list.
     *
     * @param productNode the product node to be tested
     *
     * @return true if and only if product node should be included
     */
    boolean accept(T productNode);
}
