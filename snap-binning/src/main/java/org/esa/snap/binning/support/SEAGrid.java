/*
 * Copyright (C) 2012 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.snap.binning.support;

import org.esa.snap.binning.PlanetaryGrid;

import static java.lang.Math.*;

/**
 * Implementation of a PlanetaryGrid that uses a Sinusoidal Equal Area (SEA) grid layout.
 * It is similar to the SEA grid used in the NASA  SeaDAS software for creating the binned Level-3 MODIS and SeaWiFS products.
 * The mayor difference is the way how we count bin indexes and row indexes.
 * <p>
 *
 * @author Norman Fomferra
 * @author Marco Zühlke
 * @see <a href="http://oceancolor.gsfc.nasa.gov/SeaWiFS/TECH_REPORTS/PreLPDF/PreLVol32.pdf">SeaWiFS Technical Report Series Volume 32, Level-3 SeaWiFS Data</a>
 * @see <a href="http://oceancolor.gsfc.nasa.gov/DOCS/Ocean_Level-3_Binned_Data_Products.pdf">Ocean Level-3 Binned Data Products</a>
 */
public final class SEAGrid implements PlanetaryGrid {

    /**
     * Default number of rows. This results in a vertical bin cell size of approx. 9.28 km.
     */
    public static final int DEFAULT_NUM_ROWS = 2160;

    /**
     * Average Earth radius: see NASA SeaWiFS Technical Report Series Vol. 32;
     */
    public static double RE = 6378.145;

    private final int numRows;
    private final double[] latBin;  // latitude of first bin in row
    private final long[] baseBin; // bin-index of the first bin in this row
    private final int[] numBin;  // number of bins in this row
    private final long numBins;

    public SEAGrid() {
        this(DEFAULT_NUM_ROWS);
    }

    public SEAGrid(int numRows) {
        if (numRows < 2) {
            throw new IllegalArgumentException("numRows < 2");
        }
        if (numRows % 2 != 0) {
            throw new IllegalArgumentException("numRows % 2 != 0");
        }

        this.numRows = numRows;
        latBin = new double[numRows];
        baseBin = new long[numRows];
        numBin = new int[numRows];
        baseBin[0] = 0;
        for (int row = 0; row < numRows; row++) {
            // Note: this differs from SeaDAS. We start counting IDs from North to South
            // In SeaDAS L3 and ACRI MERIS L3: latBin[row] = -90.0 + (row + 0.5) * 180.0 / numRows;
            latBin[row] = 90.0 - (row + 0.5) * 180.0 / numRows;
            numBin[row] = (int) (0.5 + 2 * numRows * cos(toRadians(latBin[row])));
            if (row > 0) {
                baseBin[row] = baseBin[row - 1] + numBin[row - 1];
            }
        }
        numBins = baseBin[numRows - 1] + numBin[numRows - 1];
    }

    public static int computeRowCount(double res) {
        int numRows = 1 + (int) Math.floor(0.5 * (2 * Math.PI * RE) / res);
        if (numRows % 2 == 0) {
            return numRows;
        } else {
            return numRows + 1;
        }
    }

    @Override
    public int getNumRows() {
        return numRows;
    }

    @Override
    public int getNumCols(int row) {
        return numBin[row];
    }

    @Override
    public long getNumBins() {
        return numBins;
    }

    @Override
    public long getFirstBinIndex(int row) {
        return baseBin[row];
    }

    @Override
    public double getCenterLat(int row) {
        return latBin[row];
    }

    @Override
    public long getBinIndex(double lat, double lon) {
        final int row = getRowIndex(lat);
        final int col = getColIndex(lon, row);
        return baseBin[row] + col;
    }

    /**
     * Pseudo-code:
     * <pre>
     *       int row = numRows - 1;
     *       while (idx &lt; baseBin[row]) {
     *            row--;
     *       }
     *       return row;
     * </pre>
     *
     * @param binIndex The bin ID.
     *
     * @return The row index.
     */
    @Override
    public int getRowIndex(long binIndex) {
        // compute max constant
        final int max = baseBin.length - 1;
        // avoid field access within body of while loop
        final long[] rowBinIds = this.baseBin;
        int low = 0;
        int high = max;
        while (true) {
            int mid = (low + high) >>> 1;
            if (binIndex < rowBinIds[mid]) {
                high = mid - 1;
            } else if (mid == max) {
                return mid;
            } else if (binIndex < rowBinIds[mid + 1]) {
                return mid;
            } else {
                low = mid + 1;
            }
        }
    }

    @Override
    public double[] getCenterLatLon(long binIndex) {
        final int row = getRowIndex(binIndex);
        return new double[]{
                latBin[row],
                getCenterLon(row, (int) (binIndex - baseBin[row]))
        };
    }

    public double getCenterLon(int row, int col) {
        return 360.0 * (col + 0.5) / numBin[row] - 180.0;
    }


    public int getColIndex(double lon, int row) {
        if (lon <= -180.0) {
            return 0;
        }
        if (lon >= 180.0) {
            return numBin[row] - 1;
        }
        return (int) ((180.0 + lon) * numBin[row] / 360.0);
    }

    public int getRowIndex(double lat) {
        if (lat <= -90.0) {
            return numRows - 1;
        }
        if (lat >= 90.0) {
            return 0;
        }
        return (numRows - 1) - (int) ((90.0 + lat) * (numRows / 180.0));
    }

}
