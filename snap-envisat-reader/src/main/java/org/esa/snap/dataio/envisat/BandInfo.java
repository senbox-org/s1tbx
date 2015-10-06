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
package org.esa.snap.dataio.envisat;

import org.esa.snap.core.datamodel.FlagCoding;

/**
 * The <code>BandInfo</code> class provides information for an available band within an ENVISAT product.
 * <p> Note that this class is public only as a side-effect of the implementation.
 *
 * @author Norman Fomferra
 * @version $Revision$ $Date$
 */
public class BandInfo extends DataItemInfo {

    public final static int SCALE_NONE = 10;
    public final static int SCALE_LINEAR = 11;
    public final static int SCALE_LOG10 = 12;

    public final static int SMODEL_1OF1 = 20;
    public final static int SMODEL_1OF2 = 21;
    public final static int SMODEL_2OF2 = 22;
    public final static int SMODEL_2UB_TO_S = 23;
    public final static int SMODEL_3UB_TO_I = 24;

    /**
     * The (zero-based)  spectral band index.
     */
    private int _spectralBandIndex;

    /**
     * The sample model operation
     */
    private int _sampleModel;

    /**
     * The scaling method
     */
    private int _scalingMethod;

    /**
     * The scaling offset
     */
    private double _scalingOffset;

    /**
     * The scaling factor
     */
    private double _scalingFactor;

    /**
     * Optional bit-mask expression
     */
    private String _validExpression;

    /**
     * Optional flag-coding (for flag datasets only)
     */
    private FlagCoding _flagCoding;

    /**
     * The width of the band
     */
    private int _bandWidth;

    /**
     * The height of the band
     */
    private int _bandHeight;

    /**
     * Constructs a new band information object.
     */
    public BandInfo(String bandName,
                    int dataType,
                    int spectralBandIndex,
                    int sampleModel,
                    int scalingMethod,
                    double scalingOffset,
                    double scalingFactor,
                    String validExpression,
                    FlagCoding flagCoding,
                    String physicalUnit,
                    String description,
                    int width,
                    int height) {
        super(bandName, dataType, physicalUnit, description);
        _spectralBandIndex = spectralBandIndex;
        _sampleModel = sampleModel;
        _scalingMethod = scalingMethod;
        _scalingOffset = scalingOffset;
        _scalingFactor = scalingFactor;
        _validExpression = validExpression;
        _flagCoding = flagCoding;
        _bandWidth = width;
        _bandHeight = height;
    }

    /**
     * Returns the (zero-based) spectral band index.
     *
     * @return the (zero-based) spectral band index.
     */
    public int getSpectralBandIndex() {
        return _spectralBandIndex;
    }

    /**
     * Returns the sample model.
     *
     * @return the sample model, always one of the <code>SMODEL_</code>XXX constants defined in this class
     */
    public final int getSampleModel() {
        return _sampleModel;
    }

    /**
     * Returns the scaling method.
     *
     * @return the sample model, always one of the <code>SCALE_</code>XXX constants defined in this class
     */
    public final int getScalingMethod() {
        return _scalingMethod;
    }

    /**
     * Returns the scaling offset (or interception).
     */
    public final double getScalingOffset() {
        return _scalingOffset;
    }

    /**
     * Returns the scaling factor.
     */
    public final double getScalingFactor() {
        return _scalingFactor;
    }

    /**
     * Returns the optional bit-mask expression.
     */
    public String getValidExpression() {
        return _validExpression;
    }

    /**
     * Returns the optional flag-coding (for flag datasets only).
     */
    public FlagCoding getFlagCoding() {
        return _flagCoding;
    }

    /**
     * Returns the width of the band.
     */
    public int getWidth() {
        return _bandWidth;
    }

    /**
     * Returns the height of the band.
     */
    public int getHeight() {
        return _bandHeight;
    }
}
