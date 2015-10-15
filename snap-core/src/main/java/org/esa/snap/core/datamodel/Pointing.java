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

import org.esa.snap.core.dataop.dem.ElevationModel;
import org.esa.snap.core.dataop.dem.Orthorectifier;

/**
 * The interface <code>Pointing</code> wraps a {@link GeoCoding} and optionally provides more geometry
 * information such as sun direction, satellite (view) direction and elevation at a given pixel position.
 * <p>All <code>Pointing</code> implementations should override
 * the {@link Object#equals(Object) equals()} and  {@link Object#hashCode() hashCode()} methods.
 *
 * @author Norman Fomferra
 * @version $Revision$ $Date$
 */
public interface Pointing {

    /**
     * Gets the geo-coding.
     *
     * @return the geo-coding, never null.
     */
    GeoCoding getGeoCoding();

    /**
     * Gets the vector to the sun at the given pixel position as angular direction.
     *
     * @param pixelPos         the pixel position
     * @param angularDirection the return value to be re-used. If null, a new  {@link AngularDirection} will be returned
     *
     * @return the direction to the sun or null, if this information is not available
     *
     * @see #canGetSunDir
     */
    AngularDirection getSunDir(PixelPos pixelPos, AngularDirection angularDirection);

    /**
     * Gets the vector to the observer at the given pixel position as angular direction.
     *
     * @param pixelPos         the pixel position
     * @param angularDirection the return value to be re-used. If null, a new  {@link AngularDirection} will be returned
     *
     * @return the direction to the observer or null, if this information is not available
     *
     * @see #canGetViewDir
     */
    AngularDirection getViewDir(PixelPos pixelPos, AngularDirection angularDirection);

    /**
     * Gets the elevation above the given pixel position.
     * <p>
     * This method is called by the {@link Orthorectifier} in the case that no
     * {@link ElevationModel ElevationModel} is available.
     * <p>
     * Note that a particular implementation is not able to retrieve a meaningful
     * elevation, it should return zero.
     *
     * @param pixelPos the pixel position
     *
     * @return the elevation at the given pixel position
     *
     * @see #canGetElevation
     */
    double getElevation(PixelPos pixelPos);

    /**
     * Returns whether or not the sun direction is available.
     *
     * @return true, if and only if so
     */
    boolean canGetSunDir();

    /**
     * Returns whether or not the viewing direction is available.
     *
     * @return true, if and only if so
     */
    boolean canGetViewDir();

    /**
     * Returns whether or not the elevation is available.
     *
     * @return true, if and only if so
     */
    boolean canGetElevation();
}
