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
package org.esa.beam.dataio.modis.productdb;

import org.esa.beam.dataio.modis.ModisConstants;

public class ModisBandDescription {

    private String _name;
    private boolean isSpectral;
    private String scaleMethod;
    private String _scaleName;
    private String _offsetName;
    private String _unitName;
    private String _bandName;
    private String _descName;
    private ModisSpectralInfo _specInfo;

    /**
     * Creates the object with given parameter set.
     *
     * @param name          the name of the band (without spectral extension)
     * @param isSpectral    whether the badnd is a spectral band or not
     * @param scalingMethod the scaling method to be used for this band (lin, exp ..)
     * @param scaleName     name of the attribute containing the scale factors
     * @param offsetName    name of the attribute containing the scale offsets
     * @param unitName      name off the attribute containing the physical unit
     * @param bandName      name of the attribute containing the spectral extensions (band names)
     * @param descName      name of the attribute containing a description of the band
     */
    public ModisBandDescription(final String name, final String isSpectral, final String scalingMethod,
                                final String scaleName, final String offsetName,
                                final String unitName, final String bandName, final String descName) {
        _name = name;
        this.isSpectral = isSpectral != null && isSpectral.equalsIgnoreCase("true");
        scaleMethod = scalingMethod;
        _scaleName = scaleName;
        _offsetName = offsetName;
        _unitName = unitName;
        _bandName = bandName;
        _descName = descName;
    }

    /**
     * Retrieves the name of the band
     *
     * @return the name
     */
    public String getName() {
        return _name;
    }

    /**
     * Retrieves the scaling method to be used for this band.
     *
     * @return the scaling method
     */
    public String getScalingMethod() {
        return scaleMethod;
    }

    /**
     * Retrieves the name of the attribute containing the scaling offset
     *
     * @return the name of the attribute
     */
    public String getOffsetAttribName() {
        return _offsetName;
    }

    /**
     * Retrieves the name of the attribute containing the scaling factor
     *
     * @return the name of the attribute
     */
    public String getScaleAttribName() {
        return _scaleName;
    }

    /**
     * Retrieves the name of the attribute containing the physical unit
     *
     * @return the name of the attribute
     */
    public String getUnitAttribName() {
        return _unitName;
    }

    /**
     * Retrieves the name of the attribute containing the band names (spectral extensions)
     *
     * @return the name of the attribute
     */
    public String getBandAttribName() {
        return _bandName;
    }

    /**
     * Retrieves whether this band is a spectral band or not
     *
     * @return <code>true</code> if this band is a spectral band, otherwise <code>false</code>.
     */
    public boolean isSpectral() {
        return isSpectral;
    }

    /**
     * Retrieves the name of the attribute containing the band descritpion.
     *
     * @return the name of the attribute
     */
    public String getDescriptionAttribName() {
        return _descName;
    }

    public void setSpecInfo(final ModisSpectralInfo specInfo) {
        _specInfo = specInfo;
    }

    public ModisSpectralInfo getSpecInfo() {
        return _specInfo;
    }

    public boolean hasSpectralInfo() {
        return _specInfo != null;
    }

    public boolean isExponentialScaled() {
        return ModisConstants.EXPONENTIAL_SCALE_NAME.equalsIgnoreCase(scaleMethod);
    }

    public boolean isLinearScaled() {
        return ModisConstants.LINEAR_SCALE_NAME.equalsIgnoreCase(scaleMethod);
    }

    public boolean isLinearInvertedScaled() {
        return ModisConstants.LINEAR_INVERTED_SCALE_NAME.equalsIgnoreCase(scaleMethod);
    }

    public boolean isSlopeInterceptScaled() {
        return ModisConstants.SLOPE_INTERCEPT_SCALE_NAME.equalsIgnoreCase(scaleMethod);
    }

    public boolean isPow10Scaled() {
        return ModisConstants.POW_10_SCALE_NAME.equalsIgnoreCase(scaleMethod);
    }
}
