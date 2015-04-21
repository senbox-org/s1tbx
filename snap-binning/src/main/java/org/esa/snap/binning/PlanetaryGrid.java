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

package org.esa.snap.binning;

/**
 * The planetary grid used for the binning. It subdivides the planet into approximately equal-area bin cells
 * organised in rows with a number of columns varying with the latitude.
 * <p>
 * Implementing classes must provide 2 constructors. One no-args constructor which creates the grid with standard
 * settings and the second receives on single integer value specifying the number of grid rows.
 *
 * @author Norman Fomferra
 */
public interface PlanetaryGrid {

    /**
     * Transforms a geographical point into a unique bin index.
     *
     * @param lat The latitude in degrees. Must be in the range -90 to +90.
     * @param lon The longitude in degrees. Must be in the range -180 to +180.
     *
     * @return The unique bin index.
     */
    long getBinIndex(double lat, double lon);

    /**
     * Gets the row index for the given bin index. The pseudo code for the implementation is
     * <pre>
     *       int row = numRows - 1;
     *       while (idx &lt; baseBin[row]) {
     *            row--;
     *       }
     *       return row;
     * </pre>
     *
     * @param bin The bin index. Must be in the range 0 to {@link #getNumBins()} - 1.
     *
     * @return The row index.
     */
    int getRowIndex(long bin);

    /**
     * Gets the total number of bins in the binning grid.
     *
     * @return The total number of bins.
     */
    long getNumBins();

    /**
     * Gets the number of rows in this grid.
     *
     * @return The number of rows.
     */
    int getNumRows();

    /**
     * Gets the number of columns in the given row.
     *
     * @param row The row index. Must be in the range 0 to {@link #getNumRows()} - 1.
     *
     * @return The number of columns.
     */
    int getNumCols(int row);

    /**
     * Gets the first bin index in the given row.
     *
     * @param row The row index. Must be in the range 0 to {@link #getNumRows()} - 1.
     *
     * @return The bin index.
     */
    long getFirstBinIndex(int row);

    /**
     * Gets the center latitude of the given row.
     *
     * @param row The row index. Must be in the range 0 to {@link #getNumRows()} - 1.
     *
     * @return The center latitude.
     */
    double getCenterLat(int row);

    /**
     * Gets geographical latitude and longitude (in this order) for the center of the given bin.
     *
     * @param bin The bin index. Must be in the range 0 to {@link #getNumBins()} - 1.
     *
     * @return latitude and longitude (in this order)
     */
    double[] getCenterLatLon(long bin);
}
