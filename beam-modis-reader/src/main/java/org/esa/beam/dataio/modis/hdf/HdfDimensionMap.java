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
 * The DimensionMap class holds the content of a HDF-EOS dimension map.
 */
public class HdfDimensionMap {

    private String _geoDim;
    private String _dataDim;
    private int _offset;
    private int _increment;

    /**
     * Constructs the object with default parameters.
     */
    HdfDimensionMap() {
        _geoDim = "";
        _dataDim = "";
        _offset = 0;
        _increment = 0;
    }

    /**
     * Sets the geo dimension.
     *
     * @param geoDim
     */
    void setGeoDim(String geoDim) {
        _geoDim = geoDim;
    }

    /**
     * Retrieves the geo dimension
     *
     * @return the geo dimension
     */
    String getGeoDim() {
        return _geoDim;
    }

    /**
     * Sets the data dimension
     */
    void setDataDim(String dataDim) {
        _dataDim = dataDim;
    }

    /**
     * Retrieves the data dimension
     *
     * @return the data dimension
     */
    String getDataDim() {
        return _dataDim;
    }

    /**
     * Sets the offset value for the conversion
     *
     * @param offs
     */
    void setOffset(int offs) {
        _offset = offs;
    }

    /**
     * Retrieves the offset value
     *
     * @return the offset value
     */
    int getOffset() {
        return _offset;
    }

    /**
     * Sets the increment value used for the conversion
     *
     * @param incr
     */
    void setIncrement(int incr) {
        _increment = incr;
    }

    /**
     * Retrieves the increment value
     *
     * @return the increment value
     */
    int getIncrement() {
        return _increment;
    }
}
