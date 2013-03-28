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

import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.util.Guardian;
import org.esa.beam.util.math.MathUtils;

import java.awt.Point;

@Deprecated
/**
 * @Deprecated since beam-binning 2.1.2 as part of the BEAM 4.11-release. Use module 'beam-binning2' instead.
 */
public final class SeaWiFSBinLocator implements BinLocator {

    private final static double _oneDiv360 = 1.0 / 360.0;
    private int _numRows;
    private double _invNumRows;
    private float _numRowsDiv180;
    private int[] _baseBin;
    private int[] _numBin;
    private int _numBins;
    private int _maxBinsPerRow;

    /**
     * Creates the bin locator object with a given cell size in kilometers.
     *
     * @param cellSize the cell size in kilometers
     */
    public SeaWiFSBinLocator(float cellSize) {
        createGrid(cellSize);
    }

    /**
     * Retrieves the latitude and longitude of the bin at the given index.
     *
     * @param index  the bin index
     * @param latlon a Geopos object to be filled on return. Might be null - then a new object is created
     */
    public final GeoPos getLatLon(int index, GeoPos latlon) {
        if (latlon == null) {
            latlon = new GeoPos();
        }

        int row = _numRows - 1;
        while (index < _baseBin[row]) {
            --row;
        }

        latlon.lat = (float) ((row + 0.5) * 180.0 *_invNumRows - 90.0);
        latlon.lon = 360.f * (index - _baseBin[row] + 0.5f) / _numBin[row];
        latlon.lon = latlon.lon - 180.f;

        return latlon;
    }

    /**
     * Retrieves the bin index for a given latitude and longitude.
     *
     * @param latlon the geopos for which the index is to be retrieved
     */
    public final int getIndex(GeoPos latlon) {
        final float lonAdd = latlon.lon + 180.f;
        final int row = MathUtils.floorInt((90.f + latlon.lat) * _numRowsDiv180);
        final int col = MathUtils.floorInt(lonAdd * _numBin[row] * _oneDiv360);
        return _baseBin[row] + col;
    }

    /**
     * Retrieves latitude and longitude for a given row/column pair.
     *
     * @param rowcol  the location in grid coordinates
     * @param recycle the geopos to be filled with data. Might be null - then a new object is created
     */
    public final GeoPos getLatLon(Point rowcol, GeoPos recycle) {
        final GeoPos latlon;
        if (recycle == null) {
            latlon = new GeoPos();
        } else {
            latlon = recycle;
        }

        latlon.lat = (float) ((rowcol.y + 0.5) * 180.0 * _invNumRows - 90.0);
        latlon.lon = 360.f * (rowcol.x + 0.5f) / _numBin[rowcol.y];
        latlon.lon -= 180.f;

        return latlon;
    }

    /**
     * Retrieves the row / column pair for a given lat/lon pair.
     *
     * @param latlon  the goepos for which the location shall be retrieved
     * @param recycle a raster point to be filled with data. Might be null, then a new object is created
     */
    public final Point getRowCol(GeoPos latlon, Point recycle) {
        final float lonAdd = latlon.lon + 180.f;
        final Point rowcol;
        if (recycle == null) {
            rowcol = new Point();
        } else {
            rowcol = recycle;
        }
        rowcol.y = MathUtils.floorInt((90.0 + latlon.lat) * _numRowsDiv180);
        rowcol.x = MathUtils.floorInt(lonAdd * _numBin[rowcol.y] * _oneDiv360);

        return rowcol;
    }

    /**
     * Transforms a two dimensional grid coordina to a one dimensional.
     *
     * @param rowcol the point in twoD grid coordinates
     */
    public final int rowColToIndex(Point rowcol) {
        return _baseBin[rowcol.y] + rowcol.x;
    }

    /**
     * Returns whether the desired position is valid in the context of this grid implementation.
     *
     * @param rowcol the raster point to be checked
     */
    public final boolean isValidPosition(Point rowcol) {
        boolean bRet = true;

        if (rowcol.x >= _numBin[rowcol.y]) {
            bRet = false;
        }

        return bRet;
    }

    /**
     * Returns the number of cells of the locator grid.
     */
    public final int getNumCells() {
        return _numBins;
    }

    /**
     * Returns the width of the locator grid.
     */
    public final int getWidth() {
        return _maxBinsPerRow;
    }

    /**
     * Returns the height of the locator grid
     */
    public final int getHeight() {
        return _numRows;
    }

    ///////////////////////////////////////////////////////////////////////////
    /////// END OF PUBLIC
    ///////////////////////////////////////////////////////////////////////////


    /**
     * Creates the grid with given cell size in kilometers.
     *
     * @param cellSize the cell size in kilometers
     */
    private void createGrid(float cellSize) {
        Guardian.assertWithinRange("cellSize", cellSize, 0.0001, BinDatabaseConstants.PI_EARTH_RADIUS);
        final float fRows = BinDatabaseConstants.PI_EARTH_RADIUS / cellSize;
        _numRows = MathUtils.ceilInt(fRows);
        _numRowsDiv180 = _numRows / 180.f;
        _invNumRows = 1.0 / _numRows;
        _maxBinsPerRow = 0;

        _baseBin = new int[_numRows];
        _numBin = new int[_numRows];

        // the first bin is determined outside the loop
        _baseBin[0] = 0;
        float lat = (float) (90.0 * _invNumRows - 90.0);
        _numBin[0] = (int) (2.0 * _numRows * Math.cos(MathUtils.DTOR * lat) + 0.5);

        for (int row = 1; row < _numRows; row++) {
            lat = (float) ((row + 0.5) * 180.0 * _invNumRows - 90.0);
            _numBin[row] = (int) (2.0 * _numRows * Math.cos(MathUtils.DTOR * lat) + 0.5);
            if (_maxBinsPerRow < _numBin[row]) {
                _maxBinsPerRow = _numBin[row];
            }
            _baseBin[row] = _baseBin[row - 1] + _numBin[row - 1];
        }

        _numBins = _baseBin[_numRows - 1] + _numBin[_numRows - 1];
    }
}
