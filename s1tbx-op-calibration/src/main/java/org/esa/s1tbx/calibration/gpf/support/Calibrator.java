/*
 * Copyright (C) 2014 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.s1tbx.calibration.gpf.support;

import org.esa.snap.datamodel.Unit;
import org.esa.snap.framework.datamodel.Band;
import org.esa.snap.framework.datamodel.Product;
import org.esa.snap.framework.gpf.Operator;
import org.esa.snap.framework.gpf.OperatorException;
import org.esa.snap.framework.gpf.Tile;

import java.io.File;

/**
 * The abstract base class for all calibration operators intended to be extended by clients.
 * The following methods are intended to be implemented or overidden:
 */
public interface Calibrator {

    /**
     * @param op                          The calibration operator
     * @param sourceProduct               The source product.
     * @param targetProduct               The target product.
     * @param mustPerformRetroCalibration For absolut calibration, this flag is false because retro-calibration may not
     *                                    be needed in case XCA file is not available or the old and new XCA files are identical. For radiometric
     *                                    normalization in Terrain Correction, this flag is true because retro-calibration is always needed.
     * @param mustUpdateMetadata          For Pre-Calibration, thie flag is false because calibration has net been performed.
     *                                    For absolut calibration or radiometric normalization, the flag is true.
     * @throws OperatorException The exception.
     */
    void initialize(final Operator op, final Product sourceProduct, final Product targetProduct,
                           final boolean mustPerformRetroCalibration, final boolean mustUpdateMetadata)
            throws OperatorException;

    void computeTile(final Band targetBand, final Tile targetTile,
                            final com.bc.ceres.core.ProgressMonitor pm) throws OperatorException;

    void setOutputImageInComplex(final boolean flag);

    void setOutputImageIndB(final boolean flag);

    void setIncidenceAngleForSigma0(final String incidenceAngleForSigma0);

    void setExternalAuxFile(final File file);

    void setAuxFileFlag(final String auxFile);

    double applyRetroCalibration(final int x, final int y, final double v, final String bandPolar,
                                        final Unit.UnitType bandUnit, final int[] subSwathIndex);

    double applyCalibration(
            final double v, final double rangeIndex, final double azimuthIndex, final double slantRange,
            final double satelliteHeight, final double sceneToEarthCentre, final double localIncidenceAngle,
            final String bandPolar, final Unit.UnitType bandUnit, int[] subSwathIndex);

    void removeFactorsForCurrentTile(final Band targetBand, final Tile targetTile, final String srcBandName);

    Product createTargetProduct(final Product sourceProduct, final String[] sourceBandNames);
}
