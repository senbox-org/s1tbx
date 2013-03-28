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

package org.esa.beam.timeseries.ui;

import org.esa.beam.timeseries.core.timeseries.datamodel.ProductLocation;

import javax.swing.ListModel;
import java.io.File;
import java.io.Serializable;
import java.util.List;

/**
 * Simple model interface for ${@link ProductLocation}s.
 */
public interface ProductLocationsPaneModel extends ListModel, Serializable {

    /**
     * Returns the value at the specified index.
     * @param index the requested index
     * @return the value at <code>index</code>
     */
    @Override
    ProductLocation getElementAt(int index);

    /**
     * Adds single files to the model.
     * @param files the files to be added
     */
    void addFiles(File... files);

    /**
     * Adds a complete directory to the model.
     * @param directory the directory to add.
     * @param recursive specify if directory is to be added recursively
     */
    void addDirectory(File directory, boolean recursive);

    /**
     * Removes {@link ProductLocation}s with the given indices.
     * @param indices the indices of the product locations to be removed
     */
    void remove(int... indices);

    /**
     * Returns the {@link ProductLocation}s.
     * @return the product locations
     */
    List<ProductLocation> getProductLocations();
}
