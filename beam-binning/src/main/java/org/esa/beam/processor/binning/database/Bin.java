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
package org.esa.beam.processor.binning.database;

@Deprecated
/**
 * @Deprecated since beam-binning 2.1.2 as part of the BEAM 4.11-release. Use module 'beam-binning2' instead.
 */
public interface Bin {

    /**
     * Sets the band on which to which read and write operations should operate.
     *
     * @param bandIndex
     */
    public void setBandIndex(int bandIndex);

    /**
     * Reads the value of the bin at a given field.
     *
     * @param index the field index
     */
    public float read(int index);

    /**
     * Writes data to a given field.
     *
     * @param index the field to be written to
     * @param value the value to be written
     */
    public void write(int index, float value);

    /**
     * Returns whether the bin contains data (any one value != 0)  - or not.
     */
    public boolean containsData();

    /**
     * Sets all fields of the bin to zero.
     */
    public void clear();

    /**
     * Fills the bin with the values from the given array.
     *
     * @param dataToLoad The data which is loaded into the bin.
     */
    public void load(float[] dataToLoad);

    /**
     * Gives the data of the bin as flat array.
     *
     * @param recycledSaveData a float arry that can be used if given.
     * @return A flat array with all values of the bin.
     */
    public float[] save(float[] recycledSaveData);

}
