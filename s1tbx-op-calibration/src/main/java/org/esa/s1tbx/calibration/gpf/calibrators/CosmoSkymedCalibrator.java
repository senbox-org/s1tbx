/*
 * Copyright (C) 2002-2010 by Jason Fritz
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation. This program is distributed in the hope it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.esa.s1tbx.calibration.gpf.calibrators;

import com.bc.ceres.core.ProgressMonitor;
import org.apache.commons.math3.util.FastMath;
import org.esa.s1tbx.calibration.gpf.support.BaseCalibrator;
import org.esa.s1tbx.calibration.gpf.support.Calibrator;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.MetadataElement;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.gpf.Operator;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.Tile;
import org.esa.snap.engine_utilities.datamodel.AbstractMetadata;
import org.esa.snap.engine_utilities.datamodel.Unit;
import org.esa.snap.engine_utilities.datamodel.Unit.UnitType;
import org.esa.snap.engine_utilities.eo.Constants;
import org.esa.snap.engine_utilities.gpf.OperatorUtils;
import org.esa.snap.engine_utilities.gpf.TileIndex;

import java.awt.*;
import java.io.File;
import java.util.HashMap;

/**
 * Calibration for Cosmo-Skymed data products.
 */

public class CosmoSkymedCalibrator extends BaseCalibrator implements Calibrator {

    private static final String[] SUPPORTED_MISSIONS = new String[] {"CSK"};

    private double referenceSlantRange = 0.;
    private double referenceSlantRangeExp = 0.;
    private double referenceIncidenceAngle = 0.;
    private double rescalingFactor = 0.;
    private final HashMap<String, Double> calibrationFactor = new HashMap<>(2);

    private boolean inputSigma0 = false;
    private boolean applyRangeSpreadingLossCorrection = false;
    private boolean applyIncidenceAngleCorrection = false;
    private boolean applyConstantCorrection = false;

    /**
     * Default constructor. The graph processing framework
     * requires that an operator has a default constructor.
     */
    public CosmoSkymedCalibrator() {
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
            throw new OperatorException("No external auxiliary file should be selected for Cosmo-Skymed product");
        }
    }

    /**
     * Set auxiliary file flag.
     */
    @Override
    public void setAuxFileFlag(String file) {
    }

    public void initialize(final Operator op, final Product srcProduct, final Product tgtProduct,
                           final boolean mustPerformRetroCalibration, final boolean mustUpdateMetadata)
            throws OperatorException {
        try {
            calibrationOp = op;
            sourceProduct = srcProduct;
            targetProduct = tgtProduct;

            absRoot = AbstractMetadata.getAbstractedMetadata(sourceProduct);
            origMetadataRoot = AbstractMetadata.getOriginalProductMetadata(sourceProduct);

            final String mission = absRoot.getAttributeString(AbstractMetadata.MISSION);
            if (!mission.startsWith("CSK"))
                throw new OperatorException(mission + " is not a valid mission for Cosmo-Skymed Calibration");

            final String productType = absRoot.getAttributeString(AbstractMetadata.PRODUCT_TYPE);
            if (productType.equals("SCS_U"))
                throw new OperatorException(productType + " calibration is not supported");

            getCalibrationFlags();

            getCalibrationFactors();

            getSampleType();

            if (mustUpdateMetadata) {
                updateTargetProductMetadata();
            }

        } catch (Exception e) {
            throw new OperatorException(e);
        }
    }

    /**
     * Update the metadata in the target product.
     */
    private void updateTargetProductMetadata() {

        final MetadataElement abs = AbstractMetadata.getAbstractedMetadata(targetProduct);
        if (abs != null) {
            abs.getAttribute(AbstractMetadata.abs_calibration_flag).getData().setElemBoolean(true);
        }
    }

    /**
     * Get the antenna pattern correction flag and range spreading loss flag.
     *
     * @throws Exception The exceptions.
     */
    private void getCalibrationFlags() throws Exception {

        if (absRoot.getAttribute(AbstractMetadata.abs_calibration_flag).getData().getElemBoolean()) {
            if (outputImageInComplex) {
                throw new OperatorException("Absolute radiometric calibration has already been applied to the product");
            }
            inputSigma0 = true;
        }

        final boolean incAngleCompFlag =
                AbstractMetadata.getAttributeBoolean(absRoot, AbstractMetadata.inc_angle_comp_flag);
        if (incAngleCompFlag) {
            applyIncidenceAngleCorrection = true;
        }

        final boolean rangeSpreadCompFlag =
                AbstractMetadata.getAttributeBoolean(absRoot, AbstractMetadata.range_spread_comp_flag);
        if (rangeSpreadCompFlag) {
            applyRangeSpreadingLossCorrection = true;
        }

        final MetadataElement globalElem = origMetadataRoot.getElement("Global_Attributes");
        final boolean constantCompFlag = globalElem.getAttributeInt("Calibration_Constant_Compensation_Flag", 0) == 1;
        if (!constantCompFlag) {
            applyConstantCorrection = true;
        }
    }

    /**
     * Get calibration factors from abstracted metadata.
     */
    private void getCalibrationFactors() {

        String pol;
        double factor;
        final MetadataElement globalElem = origMetadataRoot.getElement("Global_Attributes");
        final MetadataElement s01Elem = globalElem.getElement("S01");
        if (s01Elem != null) {
            pol = s01Elem.getAttributeString("Polarisation").toUpperCase();
            factor = s01Elem.getAttributeDouble("Calibration_Constant");
            calibrationFactor.put(pol, factor);
        } else {
            pol = globalElem.getAttributeString("S01_" + "Polarisation", "").toUpperCase();
            if (!pol.isEmpty()) {
                factor = globalElem.getAttributeDouble("S01_" + "Calibration_Constant");
                calibrationFactor.put(pol, factor);
            }
        }

        final MetadataElement s02Elem = globalElem.getElement("S02");
        if (s02Elem != null) {
            pol = s02Elem.getAttributeString("Polarisation").toUpperCase();
            factor = s02Elem.getAttributeDouble("Calibration_Constant");
            calibrationFactor.put(pol, factor);
        } else {
            pol = globalElem.getAttributeString("S02_" + "Polarisation", "").toUpperCase();
            if (!pol.isEmpty()) {
                factor = globalElem.getAttributeDouble("S02_" + "Calibration_Constant");
                calibrationFactor.put(pol, factor);
            }
        }

        referenceSlantRange = absRoot.getAttributeDouble(
                AbstractMetadata.ref_slant_range);
        referenceSlantRangeExp = absRoot.getAttributeDouble(
                AbstractMetadata.ref_slant_range_exp);
        referenceIncidenceAngle = absRoot.getAttributeDouble(
                AbstractMetadata.ref_inc_angle) * Constants.PI / 180.0;
        rescalingFactor = absRoot.getAttributeDouble(
                AbstractMetadata.rescaling_factor);

        //System.out.println("Calibration factor is " + calibrationFactor);
    }

    /**
     * Apply calibrations to the given point. The following calibrations are included: calibration constant,
     * antenna pattern compensation, range spreading loss correction and incidence angle correction.
     *
     * @param v                   The pixel value.
     * @param slantRange          The slant range (in m).
     * @param satelliteHeight     The distance from satellite to earth centre (in m).
     * @param sceneToEarthCentre  The distance from the backscattering element position to earth centre (in m).
     * @param localIncidenceAngle The local incidence angle (in degrees).
     * @param bandPolar           The source band polarization index.
     * @param bandUnit            The source band unit.
     * @param subSwathIndex       The sub swath index for current pixel for wide swath product case.
     * @return The calibrated pixel value.
     */
    public double applyCalibration(
            final double v, final double rangeIndex, final double azimuthIndex, final double slantRange,
            final double satelliteHeight, final double sceneToEarthCentre, final double localIncidenceAngle,
            final String bandName, final String bandPolar, final Unit.UnitType bandUnit, int[] subSwathIndex) {

        double Ks = 1.0;
        if (applyConstantCorrection) {
            Ks = calibrationFactor.get(bandPolar.toUpperCase());
        }

        double sigma;
        if (bandUnit == Unit.UnitType.AMPLITUDE) {
            sigma = v * v;
        } else if (bandUnit == Unit.UnitType.INTENSITY || bandUnit == Unit.UnitType.REAL || bandUnit == Unit.UnitType.IMAGINARY) {
            sigma = v;
        } else if (bandUnit == Unit.UnitType.INTENSITY_DB) {
            sigma = FastMath.pow(10, v / 10.0); // convert dB to linear scale
        } else {
            throw new OperatorException("Unknown band unit");
        }

        if (applyRangeSpreadingLossCorrection)
            sigma *= FastMath.pow(referenceSlantRange, 2 * referenceSlantRangeExp);

        if (applyIncidenceAngleCorrection)
            sigma *= FastMath.sin(referenceIncidenceAngle);

        sigma /= (rescalingFactor * rescalingFactor * Ks);

        if (outputImageScaleInDb) { // convert calibration result to dB
            if (sigma < underFlowFloat) {
                sigma = -underFlowFloat;
            } else {
                sigma = 10.0 * Math.log10(sigma);
            }
        }
        return sigma;
    }

    public double applyRetroCalibration(int x, int y, double v,
                                        String bandPolar, UnitType bandUnit, int[] subSwathIndex) {
        return v;
    }

    public void computeTile(Band targetBand, Tile targetTile, ProgressMonitor pm) throws OperatorException {

        final Rectangle targetTileRectangle = targetTile.getRectangle();
        final int x0 = targetTileRectangle.x;
        final int y0 = targetTileRectangle.y;
        final int w = targetTileRectangle.width;
        final int h = targetTileRectangle.height;

        Tile sourceRaster1;
        ProductData srcData1;
        ProductData srcData2 = null;
        Band sourceBand1;

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

        // copy band if unit is phase
        if (tgtBandUnit == Unit.UnitType.PHASE) {
            targetTile.setRawSamples(sourceRaster1.getRawSamples());
            return;
        }

        final String pol = OperatorUtils.getBandPolarization(srcBandNames[0], absRoot).toUpperCase();
        double Ks = 1.0;
        if (pol != null && !pol.isEmpty() && applyConstantCorrection) {
            Ks = calibrationFactor.get(pol);
        }

        final ProductData trgData = targetTile.getDataBuffer();
        final TileIndex srcIndex = new TileIndex(sourceRaster1);
        final TileIndex tgtIndex = new TileIndex(targetTile);

        final int maxY = y0 + h;
        final int maxX = x0 + w;

        double sigma, dn, i, q, phaseTerm = 0.0;
        int srcIdx, tgtIdx;
        final double powFactor = FastMath.pow(referenceSlantRange, 2 * referenceSlantRangeExp);
        final double sinRefIncidenceAngle = FastMath.sin(referenceIncidenceAngle);
        final double rescaleCalFactor = rescalingFactor * rescalingFactor * Ks;

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
                    throw new OperatorException("CosmoSkymed Calibration: unhandled unit");
                }

                if (inputSigma0) {
                    sigma = dn;
                } else {
                    double calFactor = 1.0;
                    if (applyRangeSpreadingLossCorrection)
                        calFactor *= powFactor;

                    if (applyIncidenceAngleCorrection)
                        calFactor *= sinRefIncidenceAngle;

                    calFactor /= rescaleCalFactor;

                    sigma = dn * calFactor;

                    if (isComplex && outputImageInComplex) {
                        sigma = Math.sqrt(sigma) * phaseTerm;
                    }
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

    public void removeFactorsForCurrentTile(Band targetBand, Tile targetTile,
                                            String srcBandName) throws OperatorException {
        Band sourceBand = sourceProduct.getBand(targetBand.getName());
        Tile sourceTile = calibrationOp.getSourceTile(sourceBand, targetTile.getRectangle());
        targetTile.setRawSamples(sourceTile.getRawSamples());
    }
}
