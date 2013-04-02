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

import org.esa.beam.processor.binning.L3Context;
import org.esa.beam.processor.binning.store.BinStore;
import org.esa.beam.util.io.BeamFileFilter;

import javax.swing.filechooser.FileFilter;
import java.io.IOException;
import java.util.logging.Logger;

@Deprecated
/**
 * @Deprecated since beam-binning 2.1.2 as part of the BEAM 4.11-release. Use module 'beam-binning2' instead.
 */
abstract public class AbstractBinDatabase implements BinDatabase {

    // some key values for the properties file
    protected static final String COL_MIN_KEY = "colMin";
    protected static final String ROW_MIN_KEY = "rowMin";
    protected static final String COL_MAX_KEY = "colMax";
    protected static final String ROW_MAX_KEY = "rowMax";

    protected BinStore store;
    protected BinLocator locator;
    protected L3Context context;
    protected int[] numVarsPerBin;

    protected int rowMin;
    protected int rowMax;
    protected int colMin;
    protected int colMax;

    protected int width;

    protected Logger logger;

    public void flush() throws IOException {
        if (store != null) {
            store.flush();
        }
    }

    public void delete() throws IOException {
        if (store != null) {
            store.delete();
        }
    }

    /**
     * Sets the per band needed variables.
     */
    public void setNumVarsPerBand(int[] numVarsPerBand) {
        this.numVarsPerBin = numVarsPerBand;
    }

    /**
     * Creates a bin that can hold the in this databse stored data.
     *
     * @return a new created bin.
     */
    public Bin createBin() {
        return new FloatArrayBin(numVarsPerBin);
    }

    /**
     * Retrieves the row offset (in grid coordinate system) to the first bin containing data.
     */
    public int getRowOffset() {
        return rowMin;
    }

    /**
     * Retrieves the col offset (in grid coordinate system) to the first bin containing data.
     */
    public int getColOffset() {
        return colMin;
    }

    /**
     * Retrieves the width of the accumulating db
     */
    public int getWidth() {
        return width;
    }

    /**
     * Retrieves the height of the accumulating db
     */
    public int getHeight() {
        return rowMax - rowMin + 1;
    }

    public String getStorageType() {
        if (store != null) {
            return store.getClass().getName();
        } else {
            throw new IllegalStateException("Bin store is null. No storage type available");
        }
    }

    /**
     * Creates a file filter for the database files.
     */
    static public FileFilter createDbFileFilter() {
        return new BeamFileFilter(BinDatabaseConstants.FILE_EXTENSION_DESCRIPTION, BinDatabaseConstants.FILE_EXTENSION,
                                  BinDatabaseConstants.FILE_EXTENSION_DESCRIPTION + BinDatabaseConstants.FILE_EXTENSION);
    }

    /**
     * Initializes the internal minimum maximum tracing fields.
     */
    protected void initializeMinMax() {
        rowMin = Integer.MAX_VALUE;
        colMin = Integer.MAX_VALUE;
        rowMax = Integer.MIN_VALUE;
        colMax = Integer.MIN_VALUE;
    }

    /**
     * Sum up the vars needed for each bin into one number.
     *
     * @return the sum of all the needed vars.
     */
    protected int sumVarsPerBin() {
        int result = 0;
        for (int aNumVarsPerBin : numVarsPerBin) {
            result += aNumVarsPerBin;
        }
        return result;
    }
}
