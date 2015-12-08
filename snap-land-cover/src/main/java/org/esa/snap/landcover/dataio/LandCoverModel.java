/*
 * Copyright (C) 2015 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.snap.landcover.dataio;

import org.esa.snap.core.datamodel.GeoPos;
import org.esa.snap.core.datamodel.PixelPos;
import org.esa.snap.core.dataop.resamp.Resampling;

/**
 * An <code>LandCoverModel</code> is used to obtain a land cover above a
 * specified geographical datum for a given geographical position.
 */
public interface LandCoverModel {

    /**
     * Gets the descriptor of this LCM.
     *
     * @return the descriptor which is never null
     */
    LandCoverModelDescriptor getDescriptor();

    /**
     * Gets the land cover at the geographical coordinate.
     *
     * @param geoPos the geographical coordinate
     * @return a land cover class, or the special value returned by LandCoverModelDescriptor#getNoDataValue() if a class is not available
     * @throws Exception if a non-runtime error occurs, e.g I/O error
     */
    double getLandCover(final GeoPos geoPos) throws Exception;

    /**
     * Gets the pixel index in the reference system at the geographical coordinate.
     *
     * @param geoPos the geographical coordinate
     * @return (x, y) coordinates in the reference system of a given LandCover
     * @throws Exception if a non-runtime error occurs, e.g I/O error
     */
    PixelPos getIndex(final GeoPos geoPos) throws Exception;

    /**
     * Gets the geographical coordinates for the input pixel coordinates in the LandCover reference system.
     *
     * @param pixelPos the pixel (x,y) coordinate
     * @return (lat, lon) geographical coordinates in the reference system of a given LandCover
     * @throws Exception if a non-runtime error occurs, e.g I/O error
     */
    GeoPos getGeoPos(final PixelPos pixelPos) throws Exception;

    /**
     * Gets the land cover at the point defined by (x,y) coordinates in LandCover reference system. This method does not interpolated the map!
     *
     * @param x coordinate
     * @param y coordinate
     * @return a land cover class, or the special value returned by LandCoverModelDescriptor#getNoDataValue() if a class is not available
     * @throws Exception if a non-runtime error occurs, e.g I/O error
     */
    float getSample(final double x, final double y) throws Exception;

    /**
     * @return The resampling method used.
     * @since BEAM 4.6
     */
    Resampling getResampling();

    /**
     * Releases all of the resources used by this object instance and all of its owned children. Its primary use is to
     * allow the garbage collector to perform a vanilla job.
     * <p>
     * <p>This method should be called only if it is for sure that this object instance will never be used again. The
     * results of referencing an instance of this class after a call to <code>dispose()</code> are undefined.
     * <p>
     * <p>Overrides of this method should always call <code>super.dispose();</code> after disposing this instance.
     * <p>
     */
    void dispose();

    boolean getSamples(final int[] x, final int[] y, final double[][] samples) throws Exception;
}
