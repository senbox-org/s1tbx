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
package org.esa.snap.core.dataop.maptransf;

import org.esa.snap.core.datamodel.CrsGeoCoding;
import org.esa.snap.core.dataop.resamp.Resampling;
import org.esa.snap.core.util.Guardian;

import java.awt.geom.AffineTransform;


/**
 * The <code>MapInfo</code> class holds information required to bring the cartographic map co-ordinate system to a
 * raster co-ordinate system and back.
 *
 * @author Norman Fomferra (norman.fomferra@brockmann-consult.de)
 * @version $Revision$ $Date$
 * 
 * @deprecated since BEAM 4.7, use geotools and {@link CrsGeoCoding} instead.
 */
@Deprecated
public class MapInfo implements Cloneable {

    public static final double DEFAULT_NO_DATA_VALUE = 9999;

    //reference pixel X
    private float _pixelX;
    //reference pixel Y
    private float _pixelY;

    private MapProjection _mapProjection;
    private Datum _datum;
    private float _easting;
    private float _northing;
    private float _pixelSizeX;
    private float _pixelSizeY;
    private float _orientation;
    private int _sceneWidth;
    private int _sceneHeight;
    private boolean _sceneSizeFitted;
    private boolean _orthorectified;
    private double _noDataValue;
    private String _elevationModelName;
    private Resampling _resampling;

    /**
     * Constructs a new map information object.
     *
     * @param mapProjection the map projection, must not be null
     * @param pixelX        reference pixel position in image coordinates in X direction
     * @param pixelY        reference pixel position in image coordinates in Y direction
     * @param easting       easting in map units of the reference pixel in X direction
     * @param northing      norting in map units of the reference pixel in Y direction
     * @param pixelSizeX    size of pixel in map units in image X direction
     * @param pixelSizeY    size of pixel in map units in image Y direction
     * @param datum         the datum to be used, must not be null
     */
    public MapInfo(MapProjection mapProjection,
                   float pixelX,
                   float pixelY,
                   float easting,
                   float northing,
                   float pixelSizeX,
                   float pixelSizeY,
                   Datum datum) {
        Guardian.assertNotNull("mapProjection", mapProjection);
        Guardian.assertNotNull("datum", datum);
        _mapProjection = mapProjection;
        _pixelX = pixelX;
        _pixelY = pixelY;
        _easting = easting;
        _northing = northing;
        _pixelSizeX = pixelSizeX;
        _pixelSizeY = pixelSizeY;
        _datum = datum;
        _sceneSizeFitted = false;
        _noDataValue = DEFAULT_NO_DATA_VALUE;
        _elevationModelName = null;
        _resampling = Resampling.NEAREST_NEIGHBOUR;
        alterMapTransform();
    }

    private void alterMapTransform() {
        _mapProjection.alterMapTransform(_datum.getEllipsoid());
    }

    public final MapProjection getMapProjection() {
        return _mapProjection;
    }

    public final void setProjection(MapProjection projection) {
        _mapProjection = projection;
    }

    public final float getPixelX() {
        return _pixelX;
    }

    public final void setPixelX(float pixelX) {
        _pixelX = pixelX;
    }

    public final float getPixelY() {
        return _pixelY;
    }

    public final void setPixelY(float pixelY) {
        _pixelY = pixelY;
    }

    public final float getEasting() {
        return _easting;
    }

    public final void setEasting(float easting) {
        _easting = easting;
    }

    public float getNorthing() {
        return _northing;
    }

    public final void setNorthing(float northing) {
        _northing = northing;
    }

    public final float getPixelSizeX() {
        return _pixelSizeX;
    }

    public final void setPixelSizeX(float pixelSizeX) {
        _pixelSizeX = pixelSizeX;
    }

    public final float getPixelSizeY() {
        return _pixelSizeY;
    }

    public final void setPixelSizeY(float pixelSizeY) {
        _pixelSizeY = pixelSizeY;
    }

    /**
     * Gets the orientation angle in degrees. The orientation angle is the angle between geographic north and map grid
     * north (in degrees), with other words, the convergence angle of the projection's vertical axis from true north.
     * A positive angle means clockwise rotation, a negative angle means counter-clockwise rotation.
     *
     * @return the orientation angle in degree
     */
    public float getOrientation() {
        return _orientation;
    }

    /**
     * Sets the orientation angle in degrees. The orientation angle is the angle between geographic north and map grid
     * north (in degrees), with other words, the convergence angle of the projection's vertical axis from true north.
     * A positive angle means clockwise rotation, a negative angle means counter-clockwise rotation from map grid north
     * to geographic north.
     *
     * @param orientation the orientation angle in degrees.
     */
    public void setOrientation(final float orientation) {
        _orientation = orientation;
    }

    public final Datum getDatum() {
        return _datum;
    }

    public final void setDatum(Datum datum) {
        _datum = datum;
    }

    public final int getSceneWidth() {
        return _sceneWidth;
    }

    public final void setSceneWidth(int sceneWidth) {
        _sceneWidth = sceneWidth;
    }

    public final int getSceneHeight() {
        return _sceneHeight;
    }

    public final void setSceneHeight(int sceneHeight) {
        _sceneHeight = sceneHeight;
    }

    public final boolean isSceneSizeFitted() {
        return _sceneSizeFitted;
    }

    public final void setSceneSizeFitted(boolean sceneSizeFitted) {
        _sceneSizeFitted = sceneSizeFitted;
    }

    public final double getNoDataValue() {
        return _noDataValue;
    }

    public final void setNoDataValue(double noDataValue) {
        _noDataValue = noDataValue;
    }

    public final boolean isOrthorectified() {
        return _orthorectified;
    }

    public final void setOrthorectified(boolean orthorectified) {
        _orthorectified = orthorectified;
    }

    public final String getElevationModelName() {
        return _elevationModelName;
    }

    public final void setElevationModelName(String elevationModelName) {
        _elevationModelName = elevationModelName;
    }

    public final Resampling getResampling() {
        return _resampling;
    }

    public final void setResampling(Resampling resampling) {
        _resampling = resampling;
    }

    /**
     * Overrides toString() of object.
     *
     * @return a string representation of the object.
     */
    @Override
    public String toString() {
        StringBuffer buffer = new StringBuffer();
        buffer.append(getMapProjection().getName());
        buffer.append(", ");
        buffer.append(getPixelX());
        buffer.append(", ");
        buffer.append(getPixelY());
        buffer.append(", ");
        buffer.append(getEasting());
        buffer.append(", ");
        buffer.append(getNorthing());
        buffer.append(", ");
        buffer.append(getPixelSizeX());
        buffer.append(", ");
        buffer.append(getPixelSizeY());
        buffer.append(", ");
        buffer.append(getDatum().getName());
        buffer.append(", ");
        buffer.append("units=" + getMapProjection().getMapUnit());
        buffer.append(", ");
        buffer.append(getSceneWidth());
        buffer.append(", ");
        buffer.append(getSceneHeight());
        /*
        buffer.append(", ");
        buffer.append(isSceneSizeFitted());
        buffer.append(", ");
        buffer.append(getNoDataValue());
        */
        return buffer.toString();
    }

    @Override
    public Object clone() {
        try {
            final MapInfo mapInfo = (MapInfo) super.clone();
            final MapProjection mapProjection = mapInfo.getMapProjection();
            mapInfo.setProjection((MapProjection) mapProjection.clone());
            return mapInfo;
        } catch (CloneNotSupportedException e) {
            throw new IllegalStateException(e);
        }
    }

    public MapInfo createDeepClone() {
        return (MapInfo) clone();
    }

    public AffineTransform getPixelToMapTransform() {
        AffineTransform transform = new AffineTransform();
        transform.translate(getEasting(), getNorthing());
        transform.scale(getPixelSizeX(), -getPixelSizeY());
        transform.rotate(Math.toRadians(-getOrientation()));
        transform.translate(-getPixelX(), -getPixelY());
        return transform;
    }
}
