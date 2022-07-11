/*
 * Copyright (C) 2019 by SkyWatch Space Applications http://www.skywatch.com
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
package org.csa.rstb.calibration;

import com.bc.ceres.core.ProgressMonitor;
import org.apache.commons.math3.util.FastMath;
import org.esa.s1tbx.calibration.gpf.support.BaseCalibrator;
import org.esa.s1tbx.calibration.gpf.support.Calibrator;
import org.esa.snap.core.datamodel.*;
import org.esa.snap.core.gpf.Operator;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.Tile;
import org.esa.snap.core.util.ProductUtils;
import org.esa.snap.engine_utilities.datamodel.AbstractMetadata;
import org.esa.snap.engine_utilities.datamodel.Unit;
import org.esa.snap.engine_utilities.eo.Constants;
import org.esa.snap.engine_utilities.gpf.OperatorUtils;
import org.esa.snap.engine_utilities.gpf.ReaderUtils;
import org.esa.snap.engine_utilities.gpf.TileIndex;

import java.awt.*;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

/**
 * Calibration for Radarsat2 data products.
 */

public class RCMCalibrator extends BaseCalibrator implements Calibrator {

    private static final String[] SUPPORTED_MISSIONS = new String[] {"RCM"};
    private static final String USE_INCIDENCE_ANGLE_FROM_DEM = "Use projected local incidence angle from DEM";

    private TiePointGrid incidenceAngle = null;
    private final Map<String, CalibrationLUT> gainsMap = new HashMap<>();
    private int subsetOffsetX = 0;
    private int subsetOffsetY = 0;
    private String productType = null;

    private boolean inputSigma0 = false;
    private boolean isSLC = false;

    /**
     * Default constructor. The graph processing framework
     * requires that an operator has a default constructor.
     */
    public RCMCalibrator() {
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
            throw new OperatorException("No external auxiliary file should be selected for RCM product");
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
            origMetadataRoot = AbstractMetadata.getOriginalProductMetadata(sourceProduct);

            getMission();

            if (absRoot.getAttribute(AbstractMetadata.abs_calibration_flag).getData().getElemBoolean()) {
                if (outputImageInComplex) {
                    throw new OperatorException("Absolute radiometric calibration has already been applied to the product");
                }
                inputSigma0 = true;
            }

            isSLC = sourceProduct.getProductType().toLowerCase().contains("slc");

            getSubsetOffset();

            getLUT();

            incidenceAngle = OperatorUtils.getIncidenceAngle(sourceProduct);

            if (mustUpdateMetadata) {
                updateTargetProductMetadata();
            }

        } catch (Exception e) {
            throw new OperatorException(e);
        }
    }

    /**
     * Get product mission from abstract metadata.
     */
    private void getMission() {
        final String mission = absRoot.getAttributeString(AbstractMetadata.MISSION);
        if (!mission.equals("RCM")) {
            throw new OperatorException(mission + " is not a valid mission for RCM Calibration");
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

        final MetadataElement calibrationElem = origMetadataRoot.getElement("calibration");
        final MetadataElement[] elements = calibrationElem.getElements();
        for (MetadataElement elem : elements) {

            final String elemName = elem.getName();
            if (elemName.contains("lutSigma")) {
                MetadataAttribute pixelFirstLutValueAttr = elem.getAttribute("pixelFirstLutValue");
                if (pixelFirstLutValueAttr == null) {
                    pixelFirstLutValueAttr = elem.getAttribute("pixelFirstAnglesValue");
                }
                final int pixelFirstLutValue = Integer.parseInt(pixelFirstLutValueAttr.getData().getElemString());
                final int stepSize = Integer.parseInt(elem.getAttributeString("stepSize"));
                final int numberOfValues = Integer.parseInt(elem.getAttributeString("numberOfValues"));
                final int offset = Integer.parseInt(elem.getAttributeString("offset"));
                final MetadataAttribute attribute = elem.getAttribute("gains");
                final String gainsStr = attribute.getData().getElemString();
                final double[] gainLUT = new double[numberOfValues];
                addToArray(gainLUT, 0, gainsStr, " ");
                final CalibrationLUT lut = new CalibrationLUT(pixelFirstLutValue, stepSize, numberOfValues, offset, gainLUT);
                final String pol = getPolarization(elemName);
                gainsMap.put(pol, lut);
            }
        }
    }

    private static String getPolarization(final String bandName) {
        final String bandNameLower = bandName.toLowerCase();
        if (bandNameLower.contains("_hh")) {
            return "hh";
        } else if (bandNameLower.contains("_hv")) {
            return "hv";
        } else if (bandNameLower.contains("_vv")) {
            return "vv";
        } else if (bandNameLower.contains("_vh")) {
            return "vh";
        } else if (bandNameLower.contains("_ch") || bandNameLower.contains("_rch") || bandNameLower.contains("_lch")) {
            return "ch";
        } else if (bandNameLower.contains("_cv") || bandNameLower.contains("_rcv") || bandNameLower.contains("_lcv")) {
            return "cv";
        } else if (bandNameLower.contains("_xc")) {
            return "xc";
        } else {
            throw new OperatorException("Not handled polarization");
        }
    }

    private static int addToArray(final double[] array, int index, final String csvString, final String delim) {
        final StringTokenizer tokenizer = new StringTokenizer(csvString, delim);
        while (tokenizer.hasMoreTokens()) {
            array[index++] = Double.parseDouble(tokenizer.nextToken());
        }
        return index;
    }

    /**
     * Update the metadata in the target product.
     */
    private void updateTargetProductMetadata() {
        final MetadataElement abs = AbstractMetadata.getAbstractedMetadata(targetProduct);
        abs.getAttribute(AbstractMetadata.abs_calibration_flag).getData().setElemBoolean(true);
    }

    /**
     * Create target product.
     */
    @Override
    public Product createTargetProduct(final Product sourceProduct, final String[] sourceBandNames) {

        targetProduct = new Product(sourceProduct.getName() + PRODUCT_SUFFIX,
                sourceProduct.getProductType(),
                sourceProduct.getSceneRasterWidth(),
                sourceProduct.getSceneRasterHeight());

        addSelectedBands(sourceProduct, sourceBandNames);

        ProductUtils.copyProductNodes(sourceProduct, targetProduct);

        return targetProduct;
    }

    private void addSelectedBands(final Product sourceProduct, final String[] sourceBandNames) {

        final MetadataElement absRoot = AbstractMetadata.getAbstractedMetadata(sourceProduct);
        productType = absRoot.getAttributeString(AbstractMetadata.PRODUCT_TYPE);

        if (outputImageInComplex) {
            if (productType != null && productType.equals("MLC")) {
                outputInComplexMLC(sourceProduct, sourceBandNames);
            } else {
                outputInComplex(sourceProduct, sourceBandNames);
            }
        } else {
            outputInIntensity(sourceProduct, sourceBandNames);
        }
    }

    private void outputInComplexMLC(final Product sourceProduct, final String[] sourceBandNames) {

        final Band[] sourceBands = getSourceBands(sourceProduct, sourceBandNames, false);

        for (int i = 0; i < sourceBands.length; ++i) {

            final Band srcBandI = sourceBands[i];
            final String unit = srcBandI.getUnit();
            String nextUnit = null;
            if (unit == null) {
                throw new OperatorException("band " + srcBandI.getName() + " requires a unit");
            } else if (unit.contains(Unit.DB)) {
                throw new OperatorException("Calibration of bands in dB is not supported");
            } else if (unit.contains(Unit.IMAGINARY)) {
                throw new OperatorException("I and Q bands should be selected in pairs");
            } else if (unit.contains(Unit.REAL)) {
                if (i + 1 >= sourceBands.length) {
                    throw new OperatorException("I and Q bands should be selected in pairs");
                }
                nextUnit = sourceBands[i + 1].getUnit();
                if (nextUnit == null || !nextUnit.contains(Unit.IMAGINARY)) {
                    throw new OperatorException("I and Q bands should be selected in pairs");
                }
            }

            if (unit.contains(Unit.INTENSITY)) {
                final String[] srcBandNames = {srcBandI.getName()};
                targetBandNameToSourceBandName.put(srcBandNames[0], srcBandNames);
                final Band targetBandI = targetProduct.addBand(srcBandNames[0], ProductData.TYPE_FLOAT32);
                targetBandI.setUnit(unit);
                targetBandI.setNoDataValueUsed(true);
            } else { // Unit.REAL
                final Band srcBandQ = sourceBands[i + 1];
                final String[] srcBandNames = {srcBandI.getName(), srcBandQ.getName()};
                targetBandNameToSourceBandName.put(srcBandNames[0], srcBandNames);
                final Band targetBandI = targetProduct.addBand(srcBandNames[0], ProductData.TYPE_FLOAT32);
                targetBandI.setUnit(unit);
                targetBandI.setNoDataValueUsed(true);

                targetBandNameToSourceBandName.put(srcBandNames[1], srcBandNames);
                final Band targetBandQ = targetProduct.addBand(srcBandNames[1], ProductData.TYPE_FLOAT32);
                targetBandQ.setUnit(nextUnit);
                targetBandQ.setNoDataValueUsed(true);
                i++;
            }
        }
    }

    private void outputInComplex(final Product sourceProduct, final String[] sourceBandNames) {

        final Band[] sourceBands = getSourceBands(sourceProduct, sourceBandNames, false);

        for (int i = 0; i < sourceBands.length; i += 2) {

            final Band srcBandI = sourceBands[i];
            final String unit = srcBandI.getUnit();
            String nextUnit = null;
            if (unit == null) {
                throw new OperatorException("band " + srcBandI.getName() + " requires a unit");
            } else if (unit.contains(Unit.DB)) {
                throw new OperatorException("Calibration of bands in dB is not supported");
            } else if (unit.contains(Unit.IMAGINARY)) {
                throw new OperatorException("I and Q bands should be selected in pairs");
            } else if (unit.contains(Unit.REAL)) {
                if (i + 1 >= sourceBands.length) {
                    throw new OperatorException("I and Q bands should be selected in pairs");
                }
                nextUnit = sourceBands[i + 1].getUnit();
                if (nextUnit == null || !nextUnit.contains(Unit.IMAGINARY)) {
                    throw new OperatorException("I and Q bands should be selected in pairs");
                }
            } else {
                throw new OperatorException("Please select I and Q bands in pairs only");
            }

            final Band srcBandQ = sourceBands[i + 1];
            final String[] srcBandNames = {srcBandI.getName(), srcBandQ.getName()};
            targetBandNameToSourceBandName.put(srcBandNames[0], srcBandNames);
            final Band targetBandI = targetProduct.addBand(srcBandNames[0], ProductData.TYPE_FLOAT32);
            targetBandI.setUnit(unit);
            targetBandI.setNoDataValueUsed(true);

            targetBandNameToSourceBandName.put(srcBandNames[1], srcBandNames);
            final Band targetBandQ = targetProduct.addBand(srcBandNames[1], ProductData.TYPE_FLOAT32);
            targetBandQ.setUnit(nextUnit);
            targetBandQ.setNoDataValueUsed(true);

            final String suffix = '_' + OperatorUtils.getSuffixFromBandName(srcBandI.getName());
            ReaderUtils.createVirtualIntensityBand(targetProduct, targetBandI, targetBandQ, suffix);
        }
    }

    private void outputInIntensity(final Product sourceProduct, final String[] sourceBandNames) {

        final Band[] sourceBands = getSourceBands(sourceProduct, sourceBandNames, false);

        final MetadataElement absRoot = AbstractMetadata.getAbstractedMetadata(sourceProduct);
        String targetBandName;
        for (int i = 0; i < sourceBands.length; i++) {

            final Band srcBand = sourceBands[i];
            final String unit = srcBand.getUnit();
            if (unit == null) {
                throw new OperatorException("band " + srcBand.getName() + " requires a unit");
            }

            String targetUnit = Unit.INTENSITY;
            int targetType = ProductData.TYPE_FLOAT32;

            if (unit.contains(Unit.DB)) {

                throw new OperatorException("Calibration of bands in dB is not supported");
            } else if (unit.contains(Unit.PHASE)) {

                final String[] srcBandNames = {srcBand.getName()};
                targetBandName = srcBand.getName();
                targetType = srcBand.getDataType();
                targetUnit = Unit.PHASE;
                if (targetProduct.getBand(targetBandName) == null) {
                    targetBandNameToSourceBandName.put(targetBandName, srcBandNames);
                }

            } else if (unit.contains(Unit.IMAGINARY)) {

                throw new OperatorException("Real and imaginary bands should be selected in pairs");

            } else if (unit.contains(Unit.REAL)) {
                if (i + 1 >= sourceBands.length)
                    throw new OperatorException("Real and imaginary bands should be selected in pairs");

                final String nextUnit = sourceBands[i + 1].getUnit();
                if (nextUnit == null || !nextUnit.contains(Unit.IMAGINARY)) {
                    throw new OperatorException("Real and imaginary bands should be selected in pairs");
                }
                final String[] srcBandNames = new String[2];
                srcBandNames[0] = srcBand.getName();
                srcBandNames[1] = sourceBands[i + 1].getName();
                targetBandName = createTargetBandName(srcBandNames[0], absRoot);
                ++i;
                if (targetProduct.getBand(targetBandName) == null) {
                    targetBandNameToSourceBandName.put(targetBandName, srcBandNames);
                }

            } else {

                final String[] srcBandNames = {srcBand.getName()};
                targetBandName = createTargetBandName(srcBandNames[0], absRoot);
                if (targetProduct.getBand(targetBandName) == null) {
                    targetBandNameToSourceBandName.put(targetBandName, srcBandNames);
                }
            }

            // add band only if it doesn't already exist
            if (targetProduct.getBand(targetBandName) == null) {
                final Band targetBand = new Band(targetBandName,
                        targetType,
                        srcBand.getRasterWidth(),
                        srcBand.getRasterHeight());

                if (outputImageScaleInDb && !targetUnit.equals(Unit.PHASE)) {
                    targetUnit = Unit.INTENSITY_DB;
                }
                targetBand.setUnit(targetUnit);
                targetBand.setNoDataValueUsed(true);
                targetBand.setNoDataValue(srcBand.getNoDataValue());
                targetProduct.addBand(targetBand);
            }
        }
    }

    private String createTargetBandName(final String srcBandName, final MetadataElement absRoot) {

        String pol;
        if (productType.contains("MLC")) {
            pol = getBandPolarizationMLC(srcBandName);
        } else {
            pol = OperatorUtils.getBandPolarization(srcBandName, absRoot);
        }

        String targetBandName = "Sigma0";
        if (pol != null && !pol.isEmpty()) {
            targetBandName = "Sigma0_" + pol.toUpperCase();
        }
        if (outputImageScaleInDb) {
            targetBandName += "_dB";
        }
        return targetBandName;
    }

    private String getBandPolarizationMLC(final String srcBandName) {
        if (srcBandName.contains("C11")) {
            return "ch";
        } else if (srcBandName.contains("C22")) {
            return "cv";
        } else {
            return "xc";
        }
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
    public void computeTile(Band targetBand, Tile targetTile, ProgressMonitor pm) throws OperatorException {

        if (productType != null && productType.contains("MLC")) {
            computeTileMLC(targetBand, targetTile);
        } else {
            computeTile(targetBand, targetTile);
        }
    }

    private void computeTileMLC(final Band targetBand, final Tile targetTile) throws OperatorException {

        final Rectangle targetTileRectangle = targetTile.getRectangle();
        final int x0 = targetTileRectangle.x;
        final int y0 = targetTileRectangle.y;
        final int w = targetTileRectangle.width;
        final int h = targetTileRectangle.height;
        final int maxY = y0 + h;
        final int maxX = x0 + w;

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

        final String pol = getBandPolarizationMLC(srcBandNames[0]);
        final CalibrationLUT sigmaLUT = gainsMap.get(pol);
        final int offset = sigmaLUT.offset;
        final double[] gains = sigmaLUT.getGains(x0 + subsetOffsetX, w);

        final Unit.UnitType tgtBandUnit = Unit.getUnitType(targetBand);
        final Unit.UnitType srcBandUnit = Unit.getUnitType(sourceBand1);

        final ProductData trgData = targetTile.getDataBuffer();
        final TileIndex srcIndex = new TileIndex(sourceRaster1);
        final TileIndex tgtIndex = new TileIndex(targetTile);

        double sigma = 0.0, dn, i, q, phaseTerm = 0.0;
        for (int y = y0; y < maxY; ++y) {
            srcIndex.calculateStride(y);
            tgtIndex.calculateStride(y);

            for (int x = x0; x < maxX; ++x) {
                final int srcIdx = srcIndex.getIndex(x);
                final int tgtIdx = tgtIndex.getIndex(x);

                if (srcBandUnit == Unit.UnitType.INTENSITY) {
                    dn = srcData1.getElemDoubleAt(srcIdx);
                    sigma = dn * dn / gains[x - x0];
                    trgData.setElemDoubleAt(tgtIdx, sigma);
                } else if (srcBandUnit == Unit.UnitType.REAL) {
                    i = srcData1.getElemDoubleAt(srcIdx);
                    q = srcData2.getElemDoubleAt(srcIdx);
                    dn = Math.sqrt(i * i + q * q);
                    if (dn > 0.0) {
                        if (tgtBandUnit == Unit.UnitType.REAL) {
                            phaseTerm = i / dn;
                        } else if (tgtBandUnit == Unit.UnitType.IMAGINARY) {
                            phaseTerm = q / dn;
                        }
                    } else {
                        phaseTerm = 0.0;
                    }

                    sigma = dn * dn / gains[x - x0];
                    if (outputImageInComplex) {
                        sigma = Math.sqrt(sigma) * phaseTerm;
//                        sigma *= phaseTerm;
                    }
                    trgData.setElemDoubleAt(tgtIdx, sigma);

                } else {
                    throw new OperatorException("RCM Calibration: unhandled unit");
                }
            }
        }
    }

    private void computeTile(final Band targetBand, final Tile targetTile) throws OperatorException {

        final Rectangle targetTileRectangle = targetTile.getRectangle();
        final int x0 = targetTileRectangle.x;
        final int y0 = targetTileRectangle.y;
        final int w = targetTileRectangle.width;
        final int h = targetTileRectangle.height;
        final int maxY = y0 + h;
        final int maxX = x0 + w;

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

        final String pol = getPolarization(srcBandNames[0]);
        final CalibrationLUT sigmaLUT = gainsMap.get(pol);
        final int offset = sigmaLUT.offset;
        final double[] gains = sigmaLUT.getGains(x0 + subsetOffsetX, w);

        final Unit.UnitType tgtBandUnit = Unit.getUnitType(targetBand);
        final Unit.UnitType srcBandUnit = Unit.getUnitType(sourceBand1);

        final ProductData trgData = targetTile.getDataBuffer();
        final TileIndex srcIndex = new TileIndex(sourceRaster1);
        final TileIndex tgtIndex = new TileIndex(targetTile);

        double sigma = 0.0, dn, i, q, phaseTerm = 0.0;
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
                    throw new OperatorException("RCM Calibration: unhandled unit");
                }

                if (inputSigma0) {
                    sigma = dn;
                } else {
                    if (isSLC) {
                        if (gains != null) {
                            sigma = dn / (gains[x - x0] * gains[x - x0]);
                            if (outputImageInComplex) {
                                sigma = Math.sqrt(sigma) * phaseTerm;
                            }
                        }
                    } else {
                        sigma = dn + offset;
                        if (gains != null) {
                            sigma /= gains[x - x0];
                        }
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

    public double applyCalibration(
            final double v, final double rangeIndex, final double azimuthIndex, final double slantRange,
            final double satelliteHeight, final double sceneToEarthCentre, final double localIncidenceAngle,
            final String bandName, final String bandPolar, final Unit.UnitType bandUnit, int[] subSwathIndex) {

        final String pol = getPolarization(bandName);
        final CalibrationLUT sigmaLUT = gainsMap.get(pol);
        final int offset = sigmaLUT.offset;
        final double[] gains = sigmaLUT.getGains((int)Math.round(rangeIndex), 1);

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

        if (isSLC) {
            if (gains != null) {
                sigma /= (gains[0] * gains[0]);
            }
        } else {
            sigma += offset;
            if (gains != null) {
                sigma /= gains[0];
            }
        }

        if (incidenceAngleSelection.contains(USE_INCIDENCE_ANGLE_FROM_DEM)) {
            return sigma * FastMath.sin(localIncidenceAngle * Constants.DTOR);
        } else { // USE_INCIDENCE_ANGLE_FROM_ELLIPSOID
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

    public void removeFactorsForCurrentTile(final Band targetBand, final Tile targetTile,
                                            final String srcBandName) throws OperatorException {

        final Band sourceBand = sourceProduct.getBand(targetBand.getName());
        final Tile sourceTile = calibrationOp.getSourceTile(sourceBand, targetTile.getRectangle());
        targetTile.setRawSamples(sourceTile.getRawSamples());
    }

    public final static class CalibrationLUT {
        private final int pixelFirstLutValue;
        private final int stepSize;
        private final int numberOfValues;
        private final int offset;
        private final double[] gainLUT;

        public CalibrationLUT(final int pixelFirstLutValue, final int stepSize, final int numberOfValues,
                              final int offset, final double[] gainLUT) {
            this.pixelFirstLutValue = pixelFirstLutValue;
            this.stepSize = stepSize;
            this.numberOfValues = numberOfValues;
            this.offset = offset;
            this.gainLUT = gainLUT;
        }

        public double[] getGains(final int x0, final int w) {

            final double[] gains = new double[w];
            for (int x = x0; x < x0 + w; ++x) {
                gains[x - x0] = gainLUT[(x - pixelFirstLutValue) / stepSize];
            }
            return gains;
        }
    }
}
