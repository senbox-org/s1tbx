/*
 * $Id: LatLonBinLocator.java,v 1.1 2006/09/11 10:47:32 norman Exp $
 *
 * Copyright (C) 2003 by Infoterra Ltd (thomas.lankester@infoterra-global.com)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation. This program is distributed in the hope it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.esa.beam.processor.binning.database;

import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.util.Guardian;

import java.awt.Point;
import java.awt.Rectangle;

@Deprecated
/**
 *
 * Implementation of the BinLocator interface used for resampling.
 * Note: the resampling grid follows a Plate Carre (latlon) mapping
 * with the bounds falling at exact degrees east and north.
 *
 * @author T.H.G.Lankester, Infoterra Ltd.
 * @version 0.1, 26/04/05
 * @Deprecated since beam-binning 2.1.2 as part of the BEAM 4.11-release. Use module 'beam-binning2' instead.
 */
public class LatLonBinLocator implements BinLocator {

    /*- ATTRIBUTES -*/

    /**
     * Size, in degrees, of each pixel on the latlon resampling grid
     */
    private double _degreesPerRixel;

    /**
     * Latitude and longitude of the top left of the resampling grid
     */
    private Point _topLeftLatLon;

    /**
     * Extent, in degrees latitude, of the resampling grid
     */
    private int _latHeight;

    /**
     * Extent, in degrees longiude, of the resampling grid
     */
    private int _lonWidth;

    /**
     * X-dimension of resampling grid
     */
    private int _rixDimX;

    /**
     * Y-dimension of resampling grid
     */
    private int _rixDimY;


    /*-  METHODS   -*/

    /**
     * Initializes a PolySamp object with the image to be sampled.
     *
     * @param resample_latlon position & size of the resampling area in degrees
     * @param cellsPerDegree  nominal number of cells per degree
     */
    public LatLonBinLocator(Rectangle resample_latlon, int cellsPerDegree) {
        //final int rixels_per_degree = (int) Math.round(BinDatabaseConstants.PI_EARTH_RADIUS / (180.0 * nom_rixel_size));
        Guardian.assertGreaterThan("cellsPerDegree", cellsPerDegree, 0);

        _degreesPerRixel = 1.0 / cellsPerDegree;
        _topLeftLatLon = new Point(resample_latlon.x, resample_latlon.y + resample_latlon.height);
        _latHeight = resample_latlon.height;
        _lonWidth = resample_latlon.width;
        _rixDimX = _lonWidth * cellsPerDegree;
        _rixDimY = _latHeight * cellsPerDegree;
    }


    /**
     * Retrieves the latitude and longitude of the resampled pixel at
     * the given index.
     *
     * @param index   the linear index of a resampled pixel
     * @param lat_lon reference to a GeoPos object to be populated on return
     */
    public GeoPos getLatLon(int index, GeoPos lat_lon) {
        final Point rowcol = new Point();

        rowcol.y = index / _rixDimX;
        rowcol.x = index - (_rixDimY * rowcol.y);
        return getLatLon(rowcol, lat_lon);
    }


    /**
     * Retrieves the bin index for a given latitude and longitude.
     *
     * @param lat_lon object encapsulating the latitude and longitude
     *
     * @return linear (1D) index for the resampled pixel covering the
     *         geo-position or -1 if the geo-position is outside the
     *         resampling grid
     */
    public int getIndex(GeoPos lat_lon) {
        final Point rowcol = getRowCol(lat_lon, null);
        if (rowcol == null) {
            return -1;
        }
        return rowcol.y * _rixDimX + rowcol.x;
    }


    /**
     * Retrieves latitude and longitude for a given column/row pair.
     *
     * @param row_col grid coordinates of a resampled pixel
     * @param lat_lon reference to a GeoPos object to be populated on return
     */
    public GeoPos getLatLon(Point row_col, GeoPos lat_lon) {
        // set to null if the coordinates are not within the grid
        if (!isValidPosition(row_col)) {
            lat_lon = null;
        } else {
            // ensure that the GeoPos is not null
            if (lat_lon == null) {
                lat_lon = new GeoPos();
            }

            // determine the lat and lon for the rixel centre
            lat_lon.lon = (float) (_topLeftLatLon.x + (_degreesPerRixel * row_col.x));
            lat_lon.lat = (float) (_topLeftLatLon.y - (_degreesPerRixel * row_col.y));
        }

        return lat_lon;
    }


    /**
     * Retrieves the row / column pair for a given lat/lon pair.
     *
     * @param lat_lon object encapsulating the latitude and longitude
     *
     * @return grid coordinates for the resampled pixel covering the
     *         geo-position or null if the geo-position is outside the
     *         resampling grid
     */
    public Point getRowCol(GeoPos lat_lon, Point row_col) {
        final int minLat = _topLeftLatLon.y - _latHeight;
        final int minLon = _topLeftLatLon.x;
        final int maxLat = _topLeftLatLon.y;
        final int maxLon = _topLeftLatLon.x + _lonWidth;

        if (lat_lon == null || lat_lon.lat < minLat || lat_lon.lon < minLon
            || lat_lon.lat > maxLat || lat_lon.lon > maxLon) {
            row_col = null;
        } else {
            // ensure that the GeoPos is not null
            if (row_col == null) {
                row_col = new Point();
            }

            final float relLat = maxLat - lat_lon.lat;
            final float relLon = lat_lon.lon - minLon;
            row_col.x = (int) (relLon / _degreesPerRixel);
            row_col.y = (int) (relLat / _degreesPerRixel);
        }

        return row_col;
    }


    /**
     * Transforms a two dimensional grid coordinate to a one dimensional
     *
     * @param row_col grid coordinates of a resampled pixel
     *
     * @return linear (1D) index for the resampled pixel position or -1
     *         if the specified grid coordinates are off the resampling grid
     */
    public int rowColToIndex(Point row_col) {
        int index = -1;

        if (isValidPosition(row_col)) {
            index = row_col.y * _rixDimX + row_col.x;
        }

        return index;
    }


    /**
     * Returns whether the desired position is within the defined
     * resampling grid.
     *
     * @param row_col grid coordinates of a resampled pixel
     */
    public boolean isValidPosition(Point row_col) {
        boolean validPos = true;

        // set to null if the coordinates are not within the grid
        if (row_col == null || row_col.x < 0 || row_col.y < 0
            || row_col.x >= _rixDimX || row_col.y >= _rixDimY) {
            validPos = false;
        }

        return validPos;
    }


    /**
     * Returns the number of pixels in the resampling grid.
     */
    public int getNumCells() {
        return _rixDimX * _rixDimY;
    }


    /**
     * Returns the width of the resampling grid.
     */
    public int getWidth() {
        return _rixDimX;
    }


    /**
     * Returns the height of the resampling grid
     */
    public int getHeight() {
        return _rixDimY;
    }
}
