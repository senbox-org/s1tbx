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
package org.esa.beam.framework.ui.product;

import org.esa.beam.framework.datamodel.ProductNode;

import java.awt.Rectangle;

/**
 * An interface which can be used to mark a visible component as a view displaying a product node. Applications can ask
 * a component whether it implements this interface in order to find out which product node is currently displayed.
 */
public interface ProductNodeView {

    /**
     * @return The currently visible product node.
     */
    ProductNode getVisibleProductNode();

    /**
     * Releases all of the resources used by this view and all of its owned children. Its primary use is to allow the
     * garbage collector to perform a vanilla job.
     * <p/>
     * <p>This method should be called only if it is for sure that this object instance will never be used again. The
     * results of referencing an instance of this class after a call to <code>dispose()</code> are undefined.
     */
    void dispose();

    /**
     * Gets the bounds of this view in the form of a
     * <code>Rectangle</code> object. The bounds specify this
     * view's width, height, and location relative to
     * its parent GUI widget.
     *
     * @return a rectangle indicating this view's bounds
     */
    public Rectangle getBounds();
}
