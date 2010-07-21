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

package org.esa.beam.dataio.modis.hdf;

/**
 * The class HdfDataField contains all information contained in the HDF-EOS objects GeoField and HdfDataField
 */
public class HdfDataField {

    private String _name;
    private int _width;
    private int _height;
    private int _layers;
    private String[] _dimNames;

    /**
     * Constructs the object with default parameters.
     */
    public HdfDataField() {
        _name = "";
        _width = 0;
        _height = 0;
        _layers = 1;
    }

    /**
     * Sets the name of the object
     *
     * @param name
     */
    public void setName(final String name) {
        _name = name;
    }

    /**
     * Retrieves the name of the object
     *
     * @return the name
     */
    public String getName() {
        return _name;
    }

    /**
     * Sets the width of the object
     *
     * @param width
     */
    public void setWidth(final int width) {
        _width = width;
    }

    /**
     * Retrieves the width of the object
     *
     * @return the width
     */
    public int getWidth() {
        return _width;
    }

    /**
     * Sets the height of the object
     *
     * @param height
     */
    public void setHeight(final int height) {
        _height = height;
    }

    /**
     * Retrieves the height of the object
     *
     * @return the height
     */
    public int getHeight() {
        return _height;
    }

    /**
     * Sets the number of layer of the object
     *
     * @param layers
     */
    public void setLayers(final int layers) {
        _layers = layers;
    }

    /**
     * Retrieves the number of layer of the object
     *
     * @return the number of layer
     */
    public int getLayers() {
        return _layers;
    }

    /**
     * Sets the dimension names for this data field
     *
     * @param dimNames
     */
    public void setDimensionNames(final String[] dimNames) {
        _dimNames = dimNames;
    }

    /**
     * Retrieves the dimension names for this data field
     *
     * @return the dimension names
     */
    public String[] getDimensionNames() {
        return _dimNames;
    }
}
