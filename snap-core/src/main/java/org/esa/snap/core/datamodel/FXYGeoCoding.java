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

package org.esa.snap.core.datamodel;

import org.esa.snap.core.dataio.ProductSubsetDef;
import org.esa.snap.core.dataop.maptransf.Datum;
import org.esa.snap.core.util.math.FXYSum;

/**
 * A geo-coding based on equations. The geo-coordinates (lat, lon) and the pixel coordinates (x,y) are
 * computed by the given {@link FXYSum functions}.
 *
 * @author Marco Peters
 */
public class FXYGeoCoding extends AbstractGeoCoding {

    private final float _pixelOffsetX;
    private final float _pixelOffsetY;
    private final float _pixelSizeX;
    private final float _pixelSizeY;
    private final FXYSum _pixelXFunction;
    private final FXYSum _pixelYFunction;
    private final FXYSum _latFunction;
    private final FXYSum _lonFunction;
    private final Datum _datum;

    public FXYGeoCoding(final float pixelOffsetX, final float pixelOffsetY,
                        final float pixelSizeX, final float pixelSizeY,
                        final FXYSum xFunction, final FXYSum yFunction,
                        final FXYSum latFunction, final FXYSum lonFunction,
                        final Datum datum) {
        _pixelOffsetX = pixelOffsetX;
        _pixelOffsetY = pixelOffsetY;
        _pixelSizeX = pixelSizeX;
        _pixelSizeY = pixelSizeY;
        _pixelXFunction = xFunction;
        _pixelYFunction = yFunction;
        _latFunction = latFunction;
        _lonFunction = lonFunction;
        _datum = datum;
    }

    public float getPixelOffsetX() {
        return _pixelOffsetX;
    }

    public float getPixelOffsetY() {
        return _pixelOffsetY;
    }

    public float getPixelSizeX() {
        return _pixelSizeX;
    }

    public float getPixelSizeY() {
        return _pixelSizeY;
    }

    public FXYSum getPixelXFunction() {
        return _pixelXFunction;
    }

    public FXYSum getPixelYFunction() {
        return _pixelYFunction;
    }

    public FXYSum getLatFunction() {
        return _latFunction;
    }

    public FXYSum getLonFunction() {
        return _lonFunction;
    }

    /**
     * Checks whether or not the longitudes of this geo-coding cross the +/- 180 degree meridian.
     *
     * @return <code>true</code>, if so
     */
    @Override
    public boolean isCrossingMeridianAt180() {
        return false;
    }

    /**
     * Checks whether or not this geo-coding can determine the pixel position from a geodetic position.
     *
     * @return <code>true</code>, if so
     */
    @Override
    public boolean canGetPixelPos() {
        return true;
    }

    /**
     * Checks whether or not this geo-coding can determine the geodetic position from a pixel position.
     *
     * @return <code>true</code>, if so
     */
    @Override
    public boolean canGetGeoPos() {
        return true;
    }

    /**
     * Returns the pixel co-ordinates as x/y for a given geographical position given as lat/lon.
     *
     * @param geoPos   the geographical position as lat/lon.
     * @param pixelPos an instance of <code>Point</code> to be used as retun value. If this parameter is
     *                 <code>null</code>, the method creates a new instance which it then returns.
     *
     * @return the pixel co-ordinates as x/y
     */
    @Override
    public PixelPos getPixelPos(final GeoPos geoPos, PixelPos pixelPos) {
        if (pixelPos == null) {
            pixelPos = new PixelPos();
        }
        pixelPos.setInvalid();
        final double x = _pixelXFunction.computeZ(geoPos.getLat(), geoPos.getLon());
        final double y = _pixelYFunction.computeZ(geoPos.getLat(), geoPos.getLon());
        pixelPos.setLocation((x - _pixelOffsetX) / _pixelSizeX,
                             (y - _pixelOffsetY) / _pixelSizeY);
        return pixelPos;
    }

    /**
     * Returns the latitude and longitude value for a given pixel co-ordinate.
     *
     * @param pixelPos the pixel's co-ordinates given as x,y
     * @param geoPos   an instance of <code>GeoPos</code> to be used as retun value. If this parameter is
     *                 <code>null</code>, the method creates a new instance which it then returns.
     *
     * @return the geographical position as lat/lon.
     */
    @Override
    public GeoPos getGeoPos(final PixelPos pixelPos, GeoPos geoPos) {
        if (geoPos == null) {
            geoPos = new GeoPos(0.0f, 0.0f);
        }
        final double x = _pixelOffsetX + _pixelSizeX * pixelPos.x;
        final double y = _pixelOffsetY + _pixelSizeY * pixelPos.y;
        final double lat = _latFunction.computeZ(x, y);
        final double lon = _lonFunction.computeZ(x, y);
        geoPos.setLocation(lat, lon);
        return geoPos;

    }

    /**
     * Gets the datum, the reference point or surface against which {@link GeoPos} measurements are made.
     *
     * @return the datum
     */
    @Override
    public Datum getDatum() {
        return _datum;
    }

    /**
     * Releases all of the resources used by this object instance and all of its owned children. Its primary use is to
     * allow the garbage collector to perform a vanilla job.
     * <p>This method should be called only if it is for sure that this object instance will never be used again. The
     * results of referencing an instance of this class after a call to <code>dispose()</code> are undefined.
     */
    @Override
    public void dispose() {
    }

    /**
     * Transfers the geo-coding of the {@link Scene srcScene} to the {@link Scene destScene} with respect to the given
     * {@link ProductSubsetDef subsetDef}.
     *
     * @param srcScene  the source scene
     * @param destScene the destination scene
     * @param subsetDef the definition of the subset, may be <code>null</code>
     *
     * @return true, if the geo-coding could be transferred.
     */
    @Override
    public boolean transferGeoCoding(final Scene srcScene, final Scene destScene, final ProductSubsetDef subsetDef) {
        float pixelOffsetX = getPixelOffsetX();
        float pixelOffsetY = getPixelOffsetY();
        float pixelSizeX = getPixelSizeX();
        float pixelSizeY = getPixelSizeY();

        if (subsetDef != null) {
            if (subsetDef.getRegion() != null) {
                pixelOffsetX += subsetDef.getRegion().getX() * pixelSizeX;
                pixelOffsetY += subsetDef.getRegion().getY() * pixelSizeY;
            }
            pixelSizeX *= subsetDef.getSubSamplingX();
            pixelSizeY *= subsetDef.getSubSamplingY();
        }

        destScene.setGeoCoding(createCloneWithNewOffsetAndSize(pixelOffsetX, pixelOffsetY,
                                                               pixelSizeX, pixelSizeY));
        return true;
    }

    public FXYGeoCoding createCloneWithNewOffsetAndSize(float pixelOffsetX, float pixelOffsetY,
                                                        float pixelSizeX, float pixelSizeY) {
        final FXYSum pixelXFunction = FXYSum.createCopy(_pixelXFunction);
        final FXYSum pixelYFunction = FXYSum.createCopy(_pixelYFunction);
        final FXYSum latFunction = FXYSum.createCopy(_latFunction);
        final FXYSum lonFunction = FXYSum.createCopy(_lonFunction);

        return new FXYGeoCoding(pixelOffsetX, pixelOffsetY,
                                pixelSizeX, pixelSizeY,
                                pixelXFunction, pixelYFunction,
                                latFunction, lonFunction,
                                _datum);
    }
}
