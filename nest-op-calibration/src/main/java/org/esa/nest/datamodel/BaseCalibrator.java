/*
 * Copyright (C) 2012 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.nest.datamodel;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.framework.datamodel.*;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.Tile;
import org.esa.beam.util.math.MathUtils;
import org.esa.nest.datamodel.AbstractMetadata;
import org.esa.nest.datamodel.Calibrator;
import org.esa.nest.datamodel.Unit;
import org.esa.nest.gpf.OperatorUtils;
import org.esa.nest.gpf.TileIndex;

import java.awt.*;
import java.io.File;
import java.util.HashMap;

/**
 * Calibration base class.
 */

public class BaseCalibrator {

    protected Operator calibrationOp;
    protected Product sourceProduct;
    protected Product targetProduct;

    protected boolean outputImageInComplex = false;
    protected boolean outputImageScaleInDb = false;
    protected boolean isComplex = false;
    protected String incidenceAngleSelection = null;

    protected MetadataElement absRoot = null;

    protected static final double underFlowFloat = 1.0e-30;

    /**
     * Default constructor. The graph processing framework
     * requires that an operator has a default constructor.
     */
    public BaseCalibrator() {
    }

    /**
     * Set flag indicating if target image is output in complex.
     */
    public void setOutputImageInComplex(boolean flag) {
        outputImageInComplex = flag;
    }

    /**
     * Set flag indicating if target image is output in dB scale.
     */
    public void setOutputImageIndB(boolean flag) {
        outputImageScaleInDb = flag;
    }

    public void setIncidenceAngleForSigma0(String incidenceAngleForSigma0) {
        incidenceAngleSelection = incidenceAngleForSigma0;
    }

    /**
     * Get calibration flag from abstract metadata.
     */
    public void getCalibrationFlag() {
        if (absRoot.getAttribute(AbstractMetadata.abs_calibration_flag).getData().getElemBoolean()) {
            throw new OperatorException("Absolute radiometric calibration has already been applied to the product");
        }
    }

    /**
     * Get sample type from abstract metadata.
     */
    public void getSampleType() {
        final String sampleType = absRoot.getAttributeString(AbstractMetadata.SAMPLE_TYPE);
        if(sampleType.equals("COMPLEX")) {
            isComplex = true;
        }
    }
}