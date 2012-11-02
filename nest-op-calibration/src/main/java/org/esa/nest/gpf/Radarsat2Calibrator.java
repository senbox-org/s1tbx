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
package org.esa.nest.gpf;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.framework.datamodel.*;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.Tile;
import org.esa.beam.util.math.MathUtils;
import org.esa.nest.datamodel.AbstractMetadata;
import org.esa.nest.datamodel.BaseCalibrator;
import org.esa.nest.datamodel.Calibrator;
import org.esa.nest.datamodel.Unit;

import java.awt.*;
import java.io.File;
import java.util.HashMap;

/**
 * Calibration for Radarsat2 data products.
 */

public class Radarsat2Calibrator extends BaseCalibrator implements Calibrator {

    private static final String lutsigma = "lutSigma";
    private static final String lutgamma = "lutGamma";
    private static final String lutbeta = "lutBeta";
    private static final String USE_INCIDENCE_ANGLE_FROM_DEM = "Use projected local incidence angle from DEM";

    private TiePointGrid incidenceAngle = null;
    private double offset = 0.0;
    private double[] gains = null;

    private int subsetOffsetX = 0;
    private int subsetOffsetY = 0;

    /**
     * Default constructor. The graph processing framework
     * requires that an operator has a default constructor.
     */
    public Radarsat2Calibrator() {
    }

    /**
     * Set external auxiliary file.
     */
    public void setExternalAuxFile(File file) throws OperatorException {
        if (file != null) {
            throw new OperatorException("No external auxiliary file should be selected for Radarsat2 product");
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

            getMission();

            getCalibrationFlag();

            getSampleType();

            getSubsetOffset();

            getLUT();

            getTiePointGridData(sourceProduct);

            if (mustUpdateMetadata) {
                updateTargetProductMetadata();
            }

        } catch(Exception e) {
            throw new OperatorException(e);
        }
    }

    /**
     * Get product mission from abstract metadata.
     */
    private void getMission() {
        final String mission = absRoot.getAttributeString(AbstractMetadata.MISSION);
        if(!mission.equals("RS2")) {
            throw new OperatorException(mission + " is not a valid mission for Radarsat2 Calibration");
        }
    }

    /**
     * Get subset x and y offsets from abstract metadata.
     */
    private void getSubsetOffset() {
        subsetOffsetX = absRoot.getAttributeInt(AbstractMetadata.subset_offset_x);
        subsetOffsetY = absRoot.getAttributeInt(AbstractMetadata.subset_offset_y);
    }

    /**
     * Get antenna pattern gain array from metadata.
     */
    private void getLUT() {
        final MetadataElement origProdRoot = AbstractMetadata.getOriginalProductMetadata(sourceProduct);
        final MetadataElement lutSigmaElem = origProdRoot.getElement(lutsigma);

        if(lutSigmaElem != null) {
            offset = lutSigmaElem.getAttributeDouble("offset", 0);

            final MetadataAttribute gainsAttrib = lutSigmaElem.getAttribute("gains");
            if(gainsAttrib !=null) {
                gains = (double[])gainsAttrib.getData().getElems();
            }
        } else {
            throw new OperatorException(lutsigma+" not found. Please ensure the look up table "+lutsigma+".xml is in the same folder as the original product");
        }

        if(gains.length < sourceProduct.getSceneRasterWidth()) {
            throw new OperatorException("Calibration LUT is smaller than source product width");
        }
    }

    /**
     * Get incidence angle and slant range time tie point grids.
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

        final MetadataElement origProdRoot = AbstractMetadata.getOriginalProductMetadata(targetProduct);
        origProdRoot.removeElement(origProdRoot.getElement(lutsigma));
        origProdRoot.removeElement(origProdRoot.getElement(lutgamma));
        origProdRoot.removeElement(origProdRoot.getElement(lutbeta));
    }

    /**
     * Called by the framework in order to compute a tile for the given target band.
     * <p>The default implementation throws a runtime exception with the message "not implemented".</p>
     *
     * @param targetBand The target band.
     * @param targetTile The current tile associated with the target band to be computed.
     * @param pm         A progress monitor which should be used to determine computation cancelation requests.
     * @throws org.esa.beam.framework.gpf.OperatorException
     *          If an error occurs during computation of the target raster.
     */
    public void computeTile(Band targetBand, Tile targetTile,
                            HashMap<String, String[]> targetBandNameToSourceBandName,
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

        final Unit.UnitType bandUnit = Unit.getUnitType(sourceBand1);

        final ProductData trgData = targetTile.getDataBuffer();
        final TileIndex srcIndex = new TileIndex(sourceRaster1);

        final int maxY = y0 + h;
        final int maxX = x0 + w;

        double sigma, dn, i, q;
        int index;

        for (int y = y0; y < maxY; ++y) {
            srcIndex.calculateStride(y);
            for (int x = x0; x < maxX; ++x) {

                index = srcIndex.getIndex(x);

                if (bandUnit == Unit.UnitType.AMPLITUDE) {
                    dn = srcData1.getElemDoubleAt(index);
                    sigma = dn*dn;
                } else if (bandUnit == Unit.UnitType.INTENSITY) {
                    sigma = srcData1.getElemDoubleAt(index);
                } else if (bandUnit == Unit.UnitType.REAL || bandUnit == Unit.UnitType.IMAGINARY) {
                    if (outputImageInComplex) {
                        // the sigma below is actually i or q, we still call it sigma just for convenient
                        sigma = srcData1.getElemDoubleAt(index);
                    } else {
                        i = srcData1.getElemDoubleAt(index);
                        q = srcData2.getElemDoubleAt(index);
                        sigma = i * i + q * q;
                    }
                } else {
                    throw new OperatorException("Calibration: unhandled unit");
                }

                if(isComplex) {
                    if(gains != null) {
                        if (outputImageInComplex) {
                            sigma /= gains[x + subsetOffsetX];
                        } else {
                            sigma /= (gains[x + subsetOffsetX] * gains[x + subsetOffsetX]);
                        }
                    }
                } else {
                    sigma += offset;
                    if(gains != null) {
                        sigma /= gains[x + subsetOffsetX];
                    }
                }

                if (outputImageScaleInDb) { // convert calibration result to dB
                    if (sigma < underFlowFloat) {
                        sigma = -underFlowFloat;
                    } else {
                        sigma = 10.0 * Math.log10(sigma);
                    }
                }

                trgData.setElemDoubleAt(index, sigma);
            }
        }
    }

    public double applyCalibration(
            final double v, final double rangeIndex, final double azimuthIndex, final double slantRange,
            final double satelliteHeight, final double sceneToEarthCentre,final double localIncidenceAngle,
            final String bandPolar, final Unit.UnitType bandUnit, int[] subSwathIndex) {

        double sigma = 0.0;
        if (bandUnit == Unit.UnitType.AMPLITUDE) {
            sigma = v*v;
        } else if (bandUnit == Unit.UnitType.INTENSITY || bandUnit == Unit.UnitType.REAL || bandUnit == Unit.UnitType.IMAGINARY) {
            sigma = v;
        } else if (bandUnit == Unit.UnitType.INTENSITY_DB) {
            sigma = Math.pow(10, v/10.0); // convert dB to linear scale
        } else {
            throw new OperatorException("Unknown band unit");
        }

        if(isComplex) {
            if(gains != null) {
                sigma /= (gains[(int)rangeIndex] * gains[(int)rangeIndex]);
            }
        } else {
            sigma += offset;
            if(gains != null) {
                sigma /= gains[(int)rangeIndex];
            }
        }

        if (incidenceAngleSelection.contains(USE_INCIDENCE_ANGLE_FROM_DEM)) {
            return sigma*Math.sin(localIncidenceAngle * MathUtils.DTOR);
        } else { // USE_INCIDENCE_ANGLE_FROM_ELLIPSOID
            return sigma;
        }
    }

    public double applyRetroCalibration(int x, int y, double v, String bandPolar, final Unit.UnitType bandUnit, int[] subSwathIndex) {
        if (incidenceAngleSelection.contains(USE_INCIDENCE_ANGLE_FROM_DEM)) {
            return v / Math.sin(incidenceAngle.getPixelDouble(x, y) * MathUtils.DTOR);
        } else { // USE_INCIDENCE_ANGLE_FROM_ELLIPSOID
            return v;
        }
    }

    public void removeFactorsForCurrentTile(final Band targetBand, final Tile targetTile,
                                            final String srcBandName) throws OperatorException {

        final Band sourceBand = sourceProduct.getBand(targetBand.getName());
        final Tile sourceTile = calibrationOp.getSourceTile(sourceBand, targetTile.getRectangle());
        targetTile.setRawSamples(sourceTile.getRawSamples());
    }    
}