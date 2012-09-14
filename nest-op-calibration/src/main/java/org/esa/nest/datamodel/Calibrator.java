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

import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.Tile;

import java.io.File;
import java.util.HashMap;

/**
* The abstract base class for all calibration operators intended to be extended by clients.
 * The following methods are intended to be implemented or overidden:
 */
public interface Calibrator {

    /**
     * @param op The calibration operator
     * @param sourceProduct The source product.
     * @param targetProduct The target product.
     * @param mustPerformRetroCalibration For absolut calibration, this flag is false because retro-calibration may not
     *        be needed in case XCA file is not available or the old and new XCA files are identical. For radiometric
     *        normalization in Terrain Correction, this flag is true because retro-calibration is always needed.
     * @param mustUpdateMetadata For Pre-Calibration, thie flag is false because calibration has net been performed.
     *        For absolut calibration or radiometric normalization, the flag is true.
     * @throws OperatorException The exception.
     */
    public void initialize(final Operator op, final Product sourceProduct, final Product targetProduct,
                           final boolean mustPerformRetroCalibration, final boolean mustUpdateMetadata)
                           throws OperatorException;

    public void computeTile(final Band targetBand, final Tile targetTile,
                            final HashMap<String, String[]> targetBandNameToSourceBandName,
                            final com.bc.ceres.core.ProgressMonitor pm) throws OperatorException;

    public void setOutputImageInComplex(final boolean flag);

    public void setOutputImageIndB(final boolean flag);

    public void setIncidenceAngleForSigma0(final String incidenceAngleForSigma0);

    public void setExternalAuxFile(final File file);

    public void setAuxFileFlag(final String auxFile);

    public double applyRetroCalibration(final int x, final int y, final double v, final String bandPolar,
                                        final Unit.UnitType bandUnit, final int[] subSwathIndex);

    public double applyCalibration(
            final double v, final double rangeIndex, final double azimuthIndex, final double slantRange,
            final double satelliteHeight, final double sceneToEarthCentre, final double localIncidenceAngle,
            final String bandPolar, final Unit.UnitType bandUnit, int[] subSwathIndex);
    
    public void removeFactorsForCurrentTile(final Band targetBand, final Tile targetTile, final String srcBandName);
}