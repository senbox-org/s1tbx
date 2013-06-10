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
package org.esa.beam.processor.binning;

import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.util.math.MathUtils;

import java.awt.Point;

@Deprecated
/**
 * This class implements the mathematics for the SeaWifs binning raster. In contrast to the original NASA document (see
 * documentation), this raster class allows arbitrary cell widths.
 * <p/>
 * The class implements conversion methods to convert a point in geo coordinates (lat/lon) to grid cell index and vice
 * versa.
 *
 * @Deprecated since beam-binning 2.1.2 as part of the BEAM 4.11-release. Use module 'beam-binning2' instead.
 */
public class L3ProjectionRaster {

    protected int _rowMin;
    protected int _rowMax;
    protected int _colMin;
    protected int _colMax;
    protected float _degreesPerStep;
    protected float _stepsPerDegree;

    /**
     * Constructs the object with default values
     */
    public L3ProjectionRaster() {
    }

    /**
     * Initializes the ProjectionRaster with given cell size and corner points
     *
     * @param stepsPerDegree the number of steps per degree
     * @param upperLeft      upper left corner point (lat/lon)
     * @param upperRight     upper right corner point (lat/lon)
     * @param lowerRight     lower right corner point (lat/lon)
     * @param lowerLeft      lower left corner point (lat/lon)
     */
    public void init(float stepsPerDegree, GeoPos upperLeft, GeoPos upperRight, GeoPos lowerRight, GeoPos lowerLeft) {
        calculateStepSize(stepsPerDegree);
        calculateBorders(upperLeft, upperRight, lowerRight, lowerLeft);
    }

    /**
     * Converts a point given in projection raster coordinates to a geo position
     *
     * @param pt      the grid point to be converted
     * @param recycle GeoPos object to be reused (can be null)
     */
    public GeoPos pointToGeoPos(Point pt, GeoPos recycle) {
        if (recycle == null) {
            recycle = new GeoPos();
        }
        recycle.lon = (_colMin + pt.x + 0.5f) * _degreesPerStep - 180.f;
        recycle.lat = 90.f - (_rowMin + pt.y + 0.5f) * _degreesPerStep;

        return recycle;
    }

    /**
     * Converts a given geo position (lat(lon) to a point in projection raster coordinates
     */
    public Point geoPosToPoint(GeoPos pos, Point recycle) {
        if (recycle == null) {
            recycle = new Point();
        }

        recycle.x = MathUtils.floorInt((pos.lon + 180.f) * _stepsPerDegree);
        recycle.y = MathUtils.floorInt((90.f - pos.lat) * _stepsPerDegree);

        return recycle;
    }

    /**
     * Returns the width of the projection grid
     */
    public int getWidth() {
        return _colMax - _colMin;
    }

    /**
     * Returns the height of the projection grid
     */
    public int getHeight() {
        return _rowMax - _rowMin;
    }

    /**
     * Retrieves the pixel size in degrees
     */
    public float getPixelSize() {
        return _degreesPerStep;
    }

    ///////////////////////////////////////////////////////////////////////////
    /////// END OF PUBLIC
    ///////////////////////////////////////////////////////////////////////////


    /**
     * Calculates the gris step width in decimal degrees.
     *
     * @param stepsPerDegree the number of steps per degree
     */
    protected void calculateStepSize(float stepsPerDegree) {
        _stepsPerDegree = stepsPerDegree;
        _degreesPerStep = 1.f / stepsPerDegree;

    }

    /**
     * Calculates the bounding rectangle for the projection raster
     */
    private void calculateBorders(GeoPos upperLeft, GeoPos upperRight, GeoPos lowerRight, GeoPos lowerLeft) {
        Point pt = new Point();
        initBorderVariables();

        pt = geoPosToPoint(upperRight, pt);
        _colMin = Math.min(_colMin, pt.x);
        _colMax = Math.max(_colMax, pt.x);
        _rowMin = Math.min(_rowMin, pt.y);
        _rowMax = Math.max(_rowMax, pt.y);

        pt = geoPosToPoint(upperLeft, pt);
        _colMin = Math.min(_colMin, pt.x);
        _colMax = Math.max(_colMax, pt.x);
        _rowMin = Math.min(_rowMin, pt.y);
        _rowMax = Math.max(_rowMax, pt.y);

        pt = geoPosToPoint(lowerLeft, pt);
        _colMin = Math.min(_colMin, pt.x);
        _colMax = Math.max(_colMax, pt.x);
        _rowMin = Math.min(_rowMin, pt.y);
        _rowMax = Math.max(_rowMax, pt.y);

        pt = geoPosToPoint(lowerRight, pt);
        _colMin = Math.min(_colMin, pt.x);
        _colMax = Math.max(_colMax, pt.x);
        _rowMin = Math.min(_rowMin, pt.y);
        _rowMax = Math.max(_rowMax, pt.y);
    }

    /**
     * Initializes the member filed setting up the grid borders
     */
    private void initBorderVariables() {
        _rowMin = Integer.MAX_VALUE;
        _rowMax = Integer.MIN_VALUE;
        _colMin = Integer.MAX_VALUE;
        _colMax = Integer.MIN_VALUE;
    }
}
