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
import org.esa.snap.engine_utilities.gpf.TileIndex;

import java.awt.*;
import java.io.File;

/**
 * Calibration for Kompsat-5 data products.
 */

public class Kompsat5Calibrator extends BaseCalibrator implements Calibrator {

    private String acquisitionMode = null;
    private double referenceIncidenceAngle = 0.0;
    private double rescalingFactor = 0.0;
    private double calibrationConstant = 0.0;
    private double calibrationFactor = 0.0;
    private double pixelArea = 0.0;
    private int windowSize = 0;
    private int halfWindowSize = 0;
    private int sourceImageWidth = 0;
    private int sourceImageHeight = 0;
    private boolean highResolutionMode = false;

    /**
     * Default constructor. The graph processing framework
     * requires that an operator has a default constructor.
     */
    public Kompsat5Calibrator() {
    }

    /**
     * Set external auxiliary file.
     */
    public void setExternalAuxFile(File file) throws OperatorException {
        if (file != null) {
            throw new OperatorException("No external auxiliary file should be selected for Kompsat-5 product");
        }
    }

    /**
     * Set auxiliary file flag.
     */
    @Override
    public void setAuxFileFlag(String file) {
    }

    public void setUserSelections(final int windowSize) {
        this.windowSize = windowSize;
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
            if (!mission.startsWith("Kompsat5")) {
                throw new OperatorException(mission + " is not a valid mission for Kompsat-5 Calibration");
            }

            final String productType = absRoot.getAttributeString(AbstractMetadata.PRODUCT_TYPE);
            if (!productType.equals("SCS_B") && !productType.equals("SCS_U") &&
                    !productType.equals("SCS_A") && !productType.equals("SCS_W")) {
                throw new OperatorException(productType + " product is not supported");
            }

            if (absRoot.getAttribute(AbstractMetadata.abs_calibration_flag).getData().getElemBoolean()) {
                throw new OperatorException("Absolute radiometric calibration has already been applied to the product");
            }

            // HIGH RESOLUTION / STANDARD / WIDE SWATH
            acquisitionMode = absRoot.getAttributeString(AbstractMetadata.ACQUISITION_MODE);
            switch (acquisitionMode) {
                case "HIGH RESOLUTION":
                    final double rs = AbstractMetadata.getAttributeDouble(absRoot, AbstractMetadata.range_spacing);
                    final double as = AbstractMetadata.getAttributeDouble(absRoot, AbstractMetadata.azimuth_spacing);
                    pixelArea = rs * as;
                    highResolutionMode = true;
                    break;
                case "STANDARD":
                    highResolutionMode = false;
                    break;
                default:
                    throw new OperatorException("Only High Resolution and Standard modes are currently supported");
            }

            referenceIncidenceAngle = absRoot.getAttributeDouble(
                    AbstractMetadata.ref_inc_angle) * Constants.PI / 180.0;

            rescalingFactor = absRoot.getAttributeDouble(AbstractMetadata.rescaling_factor);
            if (rescalingFactor == 0.0) {
                throw new OperatorException("Cannot calibrate the product because rescaling factor is 0");
            }

            getCalibrationConstant();

            calibrationFactor = (rescalingFactor / calibrationConstant) * FastMath.sin(referenceIncidenceAngle);

            windowSize = 9; // hardcoded for now
            halfWindowSize = windowSize / 2;
            sourceImageWidth = sourceProduct.getSceneRasterWidth();
            sourceImageHeight = sourceProduct.getSceneRasterHeight();

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
     * Get calibration constant from product original metadata.
     */
    private void getCalibrationConstant() {
        final MetadataElement auxElem = origMetadataRoot.getElement("Auxiliary");
        final MetadataElement rootElem = auxElem.getElement("Root");
        final MetadataElement subSwathsElem = rootElem.getElement("SubSwaths");
        final MetadataElement subSwathElem = subSwathsElem.getElement("SubSwath");
        calibrationConstant = subSwathElem.getAttributeDouble("CalibrationConstant");
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

        double sigma;
        if (bandUnit == Unit.UnitType.AMPLITUDE) {
            sigma = v * v;
        } else if (bandUnit == Unit.UnitType.INTENSITY ||
                bandUnit == Unit.UnitType.REAL || bandUnit == Unit.UnitType.IMAGINARY) {
            sigma = v;
        } else if (bandUnit == Unit.UnitType.INTENSITY_DB) {
            sigma = FastMath.pow(10, v / 10.0); // convert dB to linear scale
        } else {
            throw new OperatorException("Unknown band unit");
        }

        sigma *= calibrationFactor;

        if (highResolutionMode) {
            sigma /= pixelArea;
        }

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
        final int maxY = y0 + h;
        final int maxX = x0 + w;

        final Rectangle sourceRectangle = getSourceTileRectangle(
                x0, y0, w, h, halfWindowSize, halfWindowSize, sourceImageWidth, sourceImageHeight);

        Tile sourceRaster1 = null, sourceRaster2 = null;
        ProductData srcData1 = null, srcData2 = null;
        Band sourceBand1 = null, sourceBand2 = null;

        final String[] srcBandNames = targetBandNameToSourceBandName.get(targetBand.getName());
        if (srcBandNames.length == 1) {
            sourceBand1 = sourceProduct.getBand(srcBandNames[0]);
            sourceRaster1 = calibrationOp.getSourceTile(sourceBand1, sourceRectangle);
            srcData1 = sourceRaster1.getDataBuffer();
        } else {
            sourceBand1 = sourceProduct.getBand(srcBandNames[0]);
            sourceBand2 = sourceProduct.getBand(srcBandNames[1]);
            sourceRaster1 = calibrationOp.getSourceTile(sourceBand1, sourceRectangle);
            sourceRaster2 = calibrationOp.getSourceTile(sourceBand2, sourceRectangle);
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

        final ProductData tgtData = targetTile.getDataBuffer();
        final TileIndex srcIndex = new TileIndex(sourceRaster1);
        final TileIndex tgtIndex = new TileIndex(targetTile);
        final Double noDataValue = targetBand.getNoDataValue();

        for (int y = y0; y < maxY; ++y) {
            srcIndex.calculateStride(y);
            tgtIndex.calculateStride(y);

            for (int x = x0; x < maxX; ++x) {
                final int srcIdx = srcIndex.getIndex(x);
                final int tgtIdx = tgtIndex.getIndex(x);

                final double dn2Mean = getMeanDN2(x, y, srcData1, srcData2, srcIndex, srcBandUnit, noDataValue);
                if(noDataValue.equals(dn2Mean)) {
                    tgtData.setElemDoubleAt(tgtIdx, noDataValue);
                    continue;
                }

                double sigma = dn2Mean * calibrationFactor;

                if (highResolutionMode) {
                    sigma /= pixelArea;
                }

                if (isComplex && outputImageInComplex) {
                    if (srcBandUnit == Unit.UnitType.REAL) {
                        final double i = srcData1.getElemDoubleAt(srcIdx);
                        final double q = srcData2.getElemDoubleAt(srcIdx);
                        final double dn2 = i * i + q * q;
                        double phaseTerm = 0.0;
                        if (dn2 > 0.0) {
                            if (tgtBandUnit == Unit.UnitType.REAL) {
                                phaseTerm = i / Math.sqrt(dn2);
                            } else if (tgtBandUnit == Unit.UnitType.IMAGINARY) {
                                phaseTerm = q / Math.sqrt(dn2);
                            }
                        }
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

                tgtData.setElemDoubleAt(tgtIdx, sigma);
            }
        }
    }

    private Rectangle getSourceTileRectangle(final int x0, final int y0, final int w, final int h,
                                             final int halfSizeX, final int halfSizeY,
                                             final int sourceImageWidth, final int sourceImageHeight) {
        final int sx0 = Math.max(0, x0 - halfSizeX);
        final int sy0 = Math.max(0, y0 - halfSizeY);
        final int sw = Math.min(x0 + w + halfSizeX, sourceImageWidth) - sx0;
        final int sh = Math.min(y0 + h + halfSizeY, sourceImageHeight) - sy0;
        return new Rectangle(sx0, sy0, sw, sh);
    }

    private double getMeanDN2(final int x, final int y, final ProductData srcData1, ProductData srcData2,
                              final TileIndex srcIndex, final Unit.UnitType srcBandUnit, final double noDataValue) {

        final int xMin = Math.max(0, x - halfWindowSize);
        final int yMin = Math.max(0, y - halfWindowSize);
        final int xMax = Math.min(x + halfWindowSize, sourceImageWidth - 1);
        final int yMax = Math.min(y + halfWindowSize, sourceImageHeight - 1);

        int count = 0;
        double dn = 0.0, dn2 = 0.0, i = 0.0, q = 0.0, dn2Sum = 0.0;
        for (int yy = yMin; yy <= yMax; ++yy) {
            srcIndex.calculateStride(yy);
            for (int xx = xMin; xx <= xMax; ++xx) {
                final int srcIdx = srcIndex.getIndex(x);
                if (srcBandUnit == Unit.UnitType.AMPLITUDE) {
                    dn = srcData1.getElemDoubleAt(srcIdx);
                    dn2 = dn * dn;
                } else if (srcBandUnit == Unit.UnitType.INTENSITY) {
                    dn2 = srcData1.getElemDoubleAt(srcIdx);
                } else if (srcBandUnit == Unit.UnitType.REAL) {
                    i = srcData1.getElemDoubleAt(srcIdx);
                    q = srcData2.getElemDoubleAt(srcIdx);
                    dn2 = i * i + q * q;
                } else if (srcBandUnit == Unit.UnitType.INTENSITY_DB) {
                    dn2 = FastMath.pow(10, srcData1.getElemDoubleAt(srcIdx) / 10.0);
                }

                dn2Sum += dn2;
                count++;
            }
        }

        if (count > 0) {
            return dn2Sum / count;
        }
        return noDataValue;
    }

    public void removeFactorsForCurrentTile(Band targetBand, Tile targetTile,
                                            String srcBandName) throws OperatorException {
        Band sourceBand = sourceProduct.getBand(targetBand.getName());
        Tile sourceTile = calibrationOp.getSourceTile(sourceBand, targetTile.getRectangle());
        targetTile.setRawSamples(sourceTile.getRawSamples());
    }
}
