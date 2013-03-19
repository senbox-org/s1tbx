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
import org.esa.beam.timeseries.core.timeseries.datamodel.ProductLocationType;

import javax.swing.AbstractListModel;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * A default implementation of ${@link ProductLocationsPaneModel}.
 *
 * @author Marco Peters
 */
public class DefaultProductLocationsPaneModel extends AbstractListModel implements ProductLocationsPaneModel {

    private final List<ProductLocation> productLocationList;

    public DefaultProductLocationsPaneModel() {
        productLocationList = new ArrayList<ProductLocation>();
    }

    @Override
    public int getSize() {
        return productLocationList.size();
    }

    @Override
    public ProductLocation getElementAt(int index) {
        return productLocationList.get(index);
    }

    @Override
    public void addFiles(File... files) {
        final int startIndex = productLocationList.size();
        for (File file : files) {
            productLocationList.add(new ProductLocation(ProductLocationType.FILE, file.getAbsolutePath()));
        }
        final int stopIndex = productLocationList.size() - 1;
        fireIntervalAdded(this, startIndex, stopIndex);
    }

    @Override
    public void addDirectory(File directory, boolean recursive) {
        final ProductLocationType locationType = recursive ? ProductLocationType.DIRECTORY_REC : ProductLocationType.DIRECTORY;
        productLocationList.add(new ProductLocation(locationType, directory.getPath()));
        final int index = productLocationList.size() - 1;
        fireIntervalAdded(this, index, index);
    }

    @Override
    public void remove(int... indices) {
        if (indices.length > 0) {
            final List<ProductLocation> toRemoveList = new ArrayList<ProductLocation>();
            for (int index : indices) {
                toRemoveList.add(productLocationList.get(index));
            }
            productLocationList.removeAll(toRemoveList);
            fireContentsChanged(this, indices[0], indices[indices.length - 1]);
        }
    }

    @Override
    public List<ProductLocation> getProductLocations() {
        return new ArrayList<ProductLocation>(productLocationList);
    }
}
