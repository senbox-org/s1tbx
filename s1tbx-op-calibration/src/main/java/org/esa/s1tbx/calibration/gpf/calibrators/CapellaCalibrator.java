/*
 * Copyright (C) 2023 by SkyWatch Space Applications http://www.skywatch.com
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
package org.esa.s1tbx.calibration.gpf.calibrators;

import com.bc.ceres.core.ProgressMonitor;
import org.apache.commons.math3.util.FastMath;
import org.esa.s1tbx.calibration.gpf.support.BaseCalibrator;
import org.esa.s1tbx.calibration.gpf.support.Calibrator;
import org.esa.snap.core.datamodel.*;
import org.esa.snap.core.gpf.Operator;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.Tile;
import org.esa.snap.engine_utilities.datamodel.AbstractMetadata;
import org.esa.snap.engine_utilities.datamodel.Unit;
import org.esa.snap.engine_utilities.eo.Constants;
import org.esa.snap.engine_utilities.gpf.OperatorUtils;
import org.esa.snap.engine_utilities.gpf.TileIndex;

import java.awt.*;
import java.io.File;

/**
 * Calibration for Capella data products.
 */

public class CapellaCalibrator extends BaseCalibrator implements Calibrator {

    private static final String[] SUPPORTED_MISSIONS = new String[] {"Capella"};

    private boolean inputSigma0 = false;
    private boolean inputBeta0 = false;
    private double centerIncidenceAngle = 0.0;
    private TiePointGrid incidenceAngle = null;

    private static final String USE_INCIDENCE_ANGLE_FROM_DEM = "Use projected local incidence angle from DEM";

    /**
     * Default constructor. The graph processing framework
     * requires that an operator has a default constructor.
     */
    public CapellaCalibrator() {
    }

    @Override
    public String[] getSupportedMissions() {
        return SUPPORTED_MISSIONS;
    }

    /**
     * Set external auxiliary file.
     */
    public void setExternalAuxFile(File file) throws OperatorException {
        if (file != null) {
            throw new OperatorException("No external auxiliary file should be selected for Capella product");
        }
    }

    /**
     * Set auxiliary file flag.
     */
    @Override
    public void setAuxFileFlag(String file) {
    }

    /**

     */
    public void initialize(final Operator op, final Product srcProduct, final Product tgtProduct,
                           final boolean mustPerformRetroCalibration, final boolean mustUpdateMetadata)
            throws OperatorException {
        try {
            calibrationOp = op;
            sourceProduct = srcProduct;
            targetProduct = tgtProduct;

            absRoot = AbstractMetadata.getAbstractedMetadata(sourceProduct);

            final String mission = absRoot.getAttributeString(AbstractMetadata.MISSION);
            if (!mission.equals("Capella"))
                throw new OperatorException(mission + " is not a valid mission for Capella Calibration");

            if (absRoot.getAttribute(AbstractMetadata.abs_calibration_flag).getData().getElemBoolean()) { // GEO
                if (outputImageInComplex) {
                    throw new OperatorException("Absolute radiometric calibration has already been applied to the product");
                }
                inputSigma0 = true;
            } else { // SLC
                inputBeta0 = true; // it is true only when SLC product is calibrated for the first time
            }

            getSampleType();

            if (inputBeta0 && !isComplex) {
                throw new OperatorException("For beta0 product, complex data is expected");
            }

            getTiePointGridData(sourceProduct);

            centerIncidenceAngle = 0.5 * (absRoot.getAttributeDouble(AbstractMetadata.incidence_near) +
                    absRoot.getAttributeDouble(AbstractMetadata.incidence_far));

            if (mustUpdateMetadata) {
                updateTargetProductMetadata();
            }

        } catch (Exception e) {
            throw new OperatorException(e);
        }
    }

    /**
     * Get incidence angle and slant range time tie point grids.
     *
     * @param sourceProduct the source
     */
    private void getTiePointGridData(Product sourceProduct) {
        incidenceAngle = OperatorUtils.getIncidenceAngle(sourceProduct);
    }

    /**
     * Update the metadata in the target product.
     */
    private void updateTargetProductMetadata() {

        final MetadataElement abs = AbstractMetadata.getAbstractedMetadata(targetProduct);

        abs.getAttribute(AbstractMetadata.abs_calibration_flag).getData().setElemBoolean(true);
    }

    /**
     * Called by the framework in order to compute a tile for the given target band.
     * <p>The default implementation throws a runtime exception with the message "not implemented".</p>
     *
     * @param targetBand The target band.
     * @param targetTile The current tile associated with the target band to be computed.
     * @param pm         A progress monitor which should be used to determine computation cancelation requests.
     * @throws OperatorException If an error occurs during computation of the target raster.
     */
    public void computeTile(Band targetBand, Tile targetTile,
                            ProgressMonitor pm) throws OperatorException {

        final Rectangle targetTileRectangle = targetTile.getRectangle();
        final int x0 = targetTileRectangle.x;
        final int y0 = targetTileRectangle.y;
        final int w = targetTileRectangle.width;
        final int h = targetTileRectangle.height;

        Tile sourceRaster1 = null;
        ProductData srcData1 = null;
        ProductData srcData2 = null;
        Band sourceBand1 = null;

        final String[] srcBandNames = targetBandNameToSourceBandName.get(targetBand.getName());
        if (srcBandNames.length == 1) {
            sourceBand1 = sourceProduct.getBand(srcBandNames[0]);
            sourceRaster1 = calibrationOp.getSourceTile(sourceBand1, targetTileRectangle);
            srcData1 = sourceRaster1.getDataBuffer();
        } else {
            sourceBand1 = sourceProduct.getBand(srcBandNames[0]);
            final Band sourceBand2 = sourceProduct.getBand(srcBandNames[1]);
            sourceRaster1 = calibrationOp.getSourceTile(sourceBand1, targetTileRectangle);
            final Tile sourceRaster2 = calibrationOp.getSourceTile(sourceBand2, targetTileRectangle);
            srcData1 = sourceRaster1.getDataBuffer();
            srcData2 = sourceRaster2.getDataBuffer();
        }

        final Unit.UnitType tgtBandUnit = Unit.getUnitType(targetBand);
        final Unit.UnitType srcBandUnit = Unit.getUnitType(sourceBand1);

        final ProductData trgData = targetTile.getDataBuffer();
        final TileIndex srcIndex = new TileIndex(sourceRaster1);
        final TileIndex tgtIndex = new TileIndex(targetTile);

        final int maxY = y0 + h;
        final int maxX = x0 + w;

        double sigma, dn, i, q, phaseTerm = 0.0;
        int srcIdx, tgtIdx;

        for (int y = y0; y < maxY; ++y) {
            srcIndex.calculateStride(y);
            tgtIndex.calculateStride(y);

            for (int x = x0; x < maxX; ++x) {
                srcIdx = srcIndex.getIndex(x);
                tgtIdx = tgtIndex.getIndex(x);

                dn = srcData1.getElemDoubleAt(srcIdx);
                if (srcBandUnit == Unit.UnitType.AMPLITUDE) {
                    dn *= dn;
                } else if (srcBandUnit == Unit.UnitType.INTENSITY) {

                } else if (srcBandUnit == Unit.UnitType.REAL) {
                    i = dn;
                    q = srcData2.getElemDoubleAt(srcIdx);
                    dn = i * i + q * q;
                    if (dn > 0.0) {
                        if (tgtBandUnit == Unit.UnitType.REAL) {
                            phaseTerm = i / Math.sqrt(dn);
                        } else if (tgtBandUnit == Unit.UnitType.IMAGINARY) {
                            phaseTerm = q / Math.sqrt(dn);
                        }
                    } else {
                        phaseTerm = 0.0;
                    }
                } else if (srcBandUnit == Unit.UnitType.INTENSITY_DB) {
                    dn = FastMath.pow(10, dn / 10.0); // convert dB to linear scale
                } else {
                    throw new OperatorException("ALOS Calibration: unhandled unit");
                }

                if (inputSigma0) {
                    sigma = dn;
                } else {
                    sigma = dn * Math.sin(centerIncidenceAngle * Constants.DTOR);
                }

                if (isComplex && outputImageInComplex) {
                    sigma = Math.sqrt(sigma) * phaseTerm;
                }

                if (outputImageScaleInDb) { // convert calibration result to dB
                    if (sigma < underFlowFloat) {
                        sigma = -underFlowFloat;
                    } else {
                        sigma = 10.0 * Math.log10(sigma);
                    }
                }

                trgData.setElemDoubleAt(tgtIdx, sigma);
            }
        }
    }

    public double applyCalibration(
            final double v, final double rangeIndex, final double azimuthIndex, final double slantRange,
            final double satelliteHeight, final double sceneToEarthCentre, final double localIncidenceAngle,
            final String bandName, final String bandPolar, final Unit.UnitType bandUnit, int[] subSwathIndex) {

        double sigma = 0.0;
        if (bandUnit == Unit.UnitType.AMPLITUDE) {
            sigma = v * v;
        } else if (bandUnit == Unit.UnitType.INTENSITY || bandUnit == Unit.UnitType.REAL || bandUnit == Unit.UnitType.IMAGINARY) {
            sigma = v;
        } else if (bandUnit == Unit.UnitType.INTENSITY_DB) {
            sigma = FastMath.pow(10, v / 10.0); // convert dB to linear scale
        } else {
            throw new OperatorException("Unknown band unit");
        }

        if (incidenceAngleSelection.contains(USE_INCIDENCE_ANGLE_FROM_DEM)) {
            if (inputBeta0) {
                return sigma * FastMath.sin(localIncidenceAngle * Constants.DTOR);
            }
            return sigma * FastMath.sin(localIncidenceAngle * Constants.DTOR) /
                    FastMath.sin(localIncidenceAngle * Constants.DTOR);
        } else { // USE_INCIDENCE_ANGLE_FROM_ELLIPSOID
            if (inputBeta0) {
                return sigma * FastMath.sin(centerIncidenceAngle * Constants.DTOR);
            }
            return sigma;
        }
    }

    public double applyRetroCalibration(int x, int y, double v, String bandPolar, final Unit.UnitType bandUnit, int[] subSwathIndex) {
        if (incidenceAngleSelection.contains(USE_INCIDENCE_ANGLE_FROM_DEM)) {
            return v / FastMath.sin(incidenceAngle.getPixelDouble(x, y) * Constants.DTOR);
        } else { // USE_INCIDENCE_ANGLE_FROM_ELLIPSOID
            return v;
        }
    }

    public void removeFactorsForCurrentTile(Band targetBand, Tile targetTile, String srcBandName) throws OperatorException {

        Band sourceBand = sourceProduct.getBand(targetBand.getName());
        Tile sourceTile = calibrationOp.getSourceTile(sourceBand, targetTile.getRectangle());
        targetTile.setRawSamples(sourceTile.getRawSamples());
    }
}
