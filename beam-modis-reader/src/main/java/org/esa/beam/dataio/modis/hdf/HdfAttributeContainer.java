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

import org.esa.beam.framework.datamodel.MetadataAttribute;
import org.esa.beam.util.StringUtils;

public class HdfAttributeContainer {

    private String _name;
    private int _hdfType;
    private String _strVal;
    private int _elemCount;

    /**
     * Creates a new object with given name, type and string value
     *
     * @param name    the attribute name
     * @param hdfType the hdf4 data type
     * @param strVal  the value as string
     */
    public HdfAttributeContainer(final String name, final int hdfType, final String strVal, final int elemCount) {
        _name = name;
        _hdfType = hdfType;
        _strVal = strVal;
        _elemCount = elemCount;
    }

    /**
     * Retrieves the name of the attribute
     *
     * @return the name
     */
    public String getName() {
        return _name;
    }

    /**
     * Retrieves the hdf4 data type of the attribute
     *
     * @return the type
     */
    public int getHdfType() {
        return _hdfType;
    }

    /**
     * Retrieves the number of data elements in the attribute.
     *
     * @return number of data elements in the attribute
     */
    public int getElemCount() {
        return _elemCount;
    }

    /**
     * Retrieves the string representation of the attribute
     *
     * @return the string
     */
    public String getStringValue() {
        return _strVal;
    }

    /**
     * Retrieves the attribute as floating point array. If the data cannot be interpreted as float - throws exception
     *
     * @return attribute as floating point array
     */
    public float[] getFloatValues() {
        return StringUtils.toFloatArray(_strVal, ",");
    }

    /**
     * Retrieves the attribute values as integer array. If data cannot be interpreted as integer - throws exception.
     *
     * @return attribute values as integer array
     */
    public int[] getIntValues() {
        return StringUtils.toIntArray(_strVal, ",");
    }

    /**
     * Retrieves the attribute as doubles point array. If the data cannot be interpreted as float - throws exception
     *
     * @return attribute as doubles point array
     */
    public double[] getDoubleValues() {
        return StringUtils.toDoubleArray(_strVal, ",");
    }

    /**
     * Converts the content of the attribute container to a metadata element.
     *
     * @return the converted metadata element
     */
    public MetadataAttribute toMetadataAttribute() {
        return HdfUtils.attributeToMetadata(this);
    }
}
