/*
 * Copyright (C) 2013 Brockmann Consult GmbH (info@brockmann-consult.de)
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

/**
 * Thin wrapper around a {@code PlanetaryGrid} used to convert from BEAM row and bin indexes to the ones
 * used in SeaDAS. BEAM row and bin indexes are 0-based and increase from North to South (top down), while the
 * SeaDAS ones are 1-based and increase from South to North (bottom up). In both grids, columns indexes increase from West
 * to East (left to right).
 *
 * @author Norman Fomferra
 */
public class SeadasGrid {

    public static final int MAX_NUM_BINS = Integer.MAX_VALUE - 1;
    private final PlanetaryGrid baseGrid;

    public SeadasGrid(PlanetaryGrid baseGrid) {

        if (!isCompatibleBaseGrid(baseGrid)) {
            throw new IllegalArgumentException("Base grid has more than " + MAX_NUM_BINS + " bins");
        }

        this.baseGrid = baseGrid;
    }

    public static boolean isCompatibleBaseGrid(PlanetaryGrid baseGrid) {
        return baseGrid.getNumBins() <= MAX_NUM_BINS;
    }

    public int getNumRows() {
        return baseGrid.getNumRows();
    }

    public int getNumCols(int rowIndex) {
        return baseGrid.getNumCols(rowIndex);
    }

    public int getFirstBinIndex(int rowIndex) {
        // SeaDAS uses FORTRAN-style, 1-based indexes
        return (int) (baseGrid.getFirstBinIndex(rowIndex) + 1L);
    }

    public int convertRowIndex(int rowIndex) {
        return baseGrid.getNumRows() - rowIndex - 1;
    }

    public int convertBinIndex(long beamBinIndex) {

        int rowIndex1 = baseGrid.getRowIndex(beamBinIndex);
        long binIndex1 = baseGrid.getFirstBinIndex(rowIndex1);
        long colIndex = beamBinIndex - binIndex1;

        int rowIndex2 = baseGrid.getNumRows() - (rowIndex1 + 1);
        long binIndex2 = baseGrid.getFirstBinIndex(rowIndex2);

        // SeaDAS uses FORTRAN-style, 1-based indexes
        return (int) (binIndex2 + colIndex + 1L);
    }

    public long reverseBinIndex(long seadasBinIndex) {
        long zeroBaseIndex = seadasBinIndex - 1;
        int rowIndex1 = baseGrid.getRowIndex(zeroBaseIndex);
        long binIndex1 = baseGrid.getFirstBinIndex(rowIndex1);
        long colIndex = zeroBaseIndex - binIndex1;

        int rowIndex2 = baseGrid.getNumRows() - (rowIndex1 + 1);
        long binIndex2 = baseGrid.getFirstBinIndex(rowIndex2);

        return binIndex2 + colIndex;
    }
}
