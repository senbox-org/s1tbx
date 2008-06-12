/*
 * $Id: Pointing.java,v 1.1.1.1 2006/09/11 08:16:45 norman Exp $
 *
 * Copyright (c) 2003 Brockmann Consult GmbH. All right reserved.
 * http://www.brockmann-consult.de
 */
package org.esa.beam.framework.datamodel;

/**
 * The interface <code>Pointing</code> wraps a {@link GeoCoding} and optionally provides more geometry
 * information such as sun direction, satellite (view) direction and elevation at a given pixel position.
 * <p/>
 * <p>All <code>Pointing</code> implementations should override
 * the {@link Object#equals(Object) equals()} and  {@link Object#hashCode() hashCode()} methods.</p>
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
     * <p/>
     * This method is called by the {@link org.esa.beam.framework.dataop.dem.Orthorectifier} in the case that no
     * {@link org.esa.beam.framework.dataop.dem.ElevationModel ElevationModel} is available.
     * <p/>
     * Note that a particular implementation is not able to retrieve a meaningful
     * elevation, it should return zero.
     *
     * @param pixelPos the pixel position
     *
     * @return the elevation at the given pixel position
     *
     * @see #canGetElevation
     */
    float getElevation(PixelPos pixelPos);

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
