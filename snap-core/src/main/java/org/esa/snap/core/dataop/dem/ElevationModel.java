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
package org.esa.snap.core.dataop.dem;

import org.esa.snap.core.datamodel.GeoPos;
import org.esa.snap.core.datamodel.PixelPos;
import org.esa.snap.core.dataop.resamp.Resampling;

/**
 * An <code>ElevationModel</code> is used to obtain an elevation above a
 * specified geographical datum for a given geographical position.
 *
 * @author Norman Fomferra
 */
public interface ElevationModel {

    /**
     * Gets the descriptor of this DEM.
     * @return the descriptor which is never null
     */
    ElevationModelDescriptor getDescriptor();

    /**
     * Gets the elevation at the geographical coordinate in meters.
     * @param geoPos  the geographical coordinate
     * @return  an elevation in meters, or the special value returned by {@link ElevationModelDescriptor#getNoDataValue()} if an elevation is not available
     * @exception Exception if a non-runtime error occurs, e.g I/O error
     */
    double getElevation(GeoPos geoPos) throws Exception;

    /**
     * Gets the pixel index in the DEM reference system at the geographical coordinate in meters.
     *
     * @param geoPos the geographical coordinate
     * @return (x, y) coordinates in the reference system of a given DEM
     * @throws Exception if a non-runtime error occurs, e.g I/O error
     */
    PixelPos getIndex(GeoPos geoPos) throws Exception;

    /**
     * Gets the geographical coordinates for the input pixel coordinates in the DEM reference system.
     *
     * @param pixelPos the pixel (x,y) coordinate
     * @return (lat, lon) geographical coordinates in the reference system of a given DEM
     * @throws Exception if a non-runtime error occurs, e.g I/O error
     */
    GeoPos getGeoPos(PixelPos pixelPos) throws Exception;

    /**
     * Gets the elevation at the point defined by (x,y) coordinates in DEM reference system. This method does not interpolated the elevation map!
     *
     * @param x coordinate
     * @param y coordinate
     * @return an elevation in meters, or the special value returned by {@link ElevationModelDescriptor#getNoDataValue()} if an elevation is not available
     * @throws Exception if a non-runtime error occurs, e.g I/O error
     */
    double getSample(double x, double y) throws Exception;

    /**
     * Gets the elevations at the points defined by (x,y) coordinates in DEM reference system. This method does not interpolated the elevation map!
     *
     * @param x coordinate
     * @param y coordinate
     * @param samples output elevation in meters, or the special value returned by {@link ElevationModelDescriptor#getNoDataValue()} if an elevation is not available
     * @return false if all values are nodata value
     * @throws Exception if a non-runtime error occurs, e.g I/O error
     */
    boolean getSamples(int[] x, int[] y, double[][] samples) throws Exception;

    /**
     * @return The resampling method used.
     * @since BEAM 4.6
     */
    Resampling getResampling();

    /**
     * Releases all of the resources used by this object instance and all of its owned children. Its primary use is to
     * allow the garbage collector to perform a vanilla job.
     * <p>This method should be called only if it is for sure that this object instance will never be used again. The
     * results of referencing an instance of this class after a call to <code>dispose()</code> are undefined.
     * <p>Overrides of this method should always call <code>super.dispose();</code> after disposing this instance.
     * <p>
     */
    void dispose();
}
