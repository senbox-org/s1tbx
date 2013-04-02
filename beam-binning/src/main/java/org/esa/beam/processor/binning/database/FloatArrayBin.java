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
 * A implementation of the Bin interface which uses an float array.
 *
 * @Deprecated since beam-binning 2.1.2 as part of the BEAM 4.11-release. Use module 'beam-binning2' instead.
 */
public class FloatArrayBin implements Bin {

    private float[] data;
    private int[] offsets;

    private int numBands;
    private int bandIndex;

    /**
     * Create a new FloatArrayBin which contains for each band the in bandNum given data items (floats).
     *
     * @param numData Array of the for each band required floats.
     */
    public FloatArrayBin(int[] numData) {
        numBands = numData.length;
        offsets = new int[numBands];

        int totalFields = 0;
        for (int band = 0; band < numBands; band++) {
            offsets[band] = totalFields;
            totalFields += numData[band];
        }

        data = new float[totalFields];
    }

    /**
     * Sets the band on which to which read and write operations should operate.
     *
     * @param bandIndex
     */
    @Override
    public void setBandIndex(int bandIndex) {
        if (bandIndex >= numBands) {
            throw new IndexOutOfBoundsException("A band with the given Index of " + bandIndex + " didn't exist");
        }
        this.bandIndex = bandIndex;
    }

    /**
     * Reads the value of the bin at a given field.
     *
     * @param index the field index
     */
    @Override
    public float read(int index) {
        return data[offsets[bandIndex] + index];
    }

   /**
     * Writes data to a given field.
     *
     * @param index the field to be written to
     * @param value the value to be written
     */
    @Override
    public void write(int index, float value) {
        data[offsets[bandIndex] + index] = value;
    }


    /**
     * Returns whether the bin contains data or not.
     * The argument for containing data is that at least one data element
     * contains a non 0.f value.
     */
    @Override
    public boolean containsData() {
        boolean bRet = false;

        for (int n = 0; n < data.length; n++) {
            if (data[n] != 0.f) {
                bRet = true;
                break;
            }
        }
        return bRet;
    }

    /**
     * Clears the bin, i.e. sets all data fields to 0.f.
     */
    @Override
    public void clear() {
        for (int n = 0; n < data.length; n++) {
            data[n] = 0.f;
        }
    }

    /**
     * Fills the bin with the values from the given array.
     *
     * @param dataToLoad The data which is loaded into the bin.
     */
    @Override
    public void load(float[] dataToLoad) {
        System.arraycopy(dataToLoad, 0, data, 0, data.length);
    }

    /**
     * Gives the data of the bin as flat array.
     *
     * @param recycledSaveData a float arry that can be used if given.
     * @return A flat array with all values of the bin.
     */
    @Override
    public float[] save(float[] recycledSaveData) {
        float[] savedData;
        if(recycledSaveData == null || recycledSaveData.length != data.length) {
            savedData = new float[data.length];
        }else{
            savedData = recycledSaveData;
        }
        System.arraycopy(data, 0, savedData, 0, data.length);
        return savedData;
    }
}
