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
package org.esa.s1tbx.gpf;

import com.bc.ceres.core.ProgressMonitor;
import org.apache.commons.math3.util.FastMath;
import org.esa.s1tbx.datamodel.BaseCalibrator;
import org.esa.s1tbx.datamodel.Calibrator;
import org.esa.snap.datamodel.AbstractMetadata;
import org.esa.snap.datamodel.Unit;
import org.esa.snap.framework.datamodel.Band;
import org.esa.snap.framework.datamodel.MetadataElement;
import org.esa.snap.framework.datamodel.Product;
import org.esa.snap.framework.datamodel.ProductData;
import org.esa.snap.framework.gpf.Operator;
import org.esa.snap.framework.gpf.OperatorException;
import org.esa.snap.framework.gpf.Tile;
import org.esa.snap.gpf.InputProductValidator;
import org.esa.snap.gpf.OperatorUtils;
import org.esa.snap.gpf.TileIndex;
import org.esa.snap.util.ProductUtils;

import java.awt.Rectangle;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Calibration for Sentinel1 data products.
 */

public class Sentinel1Calibrator extends BaseCalibrator implements Calibrator {

    private String productType = null;
    private String acquisitionMode = null;
    private String polarization = null;
    private CalibrationInfo[] calibration = null;
    private boolean isMultiSwath = false;
    protected final HashMap<String, CalibrationInfo> targetBandToCalInfo = new HashMap<>(2);
    private java.util.List<String> selectedPolList = null;
    private boolean outputSigmaBand = false;
    private boolean outputGammaBand = false;
    private boolean outputBetaBand = false;
    private boolean outputDNBand = false;
    private CALTYPE dataType = null;

    public enum CALTYPE {SIGMA0, BETA0, GAMMA, DN}

    /**
     * Default constructor. The graph processing framework
     * requires that an operator has a default constructor.
     */
    public Sentinel1Calibrator() {
    }

    /**
     * Set external auxiliary file.
     */
    public void setExternalAuxFile(File file) throws OperatorException {
        if (file != null) {
            throw new OperatorException("No external auxiliary file should be selected for Sentinel1 product");
        }
    }

    /**
     * Set auxiliary file flag.
     */
    @Override
    public void setAuxFileFlag(String file) {
    }

    public void setUserSelections(final Product sourceProduct,
                                  final String[] selectedPolarisations,
                                  final boolean outputSigmaBand,
                                  final boolean outputGammaBand,
                                  final boolean outputBetaBand,
                                  final boolean outputDNBand) {

        this.outputSigmaBand = outputSigmaBand;
        this.outputGammaBand = outputGammaBand;
        this.outputBetaBand = outputBetaBand;
        this.outputDNBand = outputDNBand;

        String[] selectedPols = selectedPolarisations;
        if (selectedPols == null || selectedPols.length == 0) {
            final MetadataElement absRoot = AbstractMetadata.getAbstractedMetadata(sourceProduct);
            selectedPols = Sentinel1Utils.getProductPolarizations(absRoot);
        }

        selectedPolList = new ArrayList<String>(4);
        for (String pol : selectedPols) {
            selectedPolList.add(pol.toUpperCase());
        }
        //selectedPolList = Arrays.asList(selectedPols);

        if (!outputSigmaBand && !outputGammaBand && !outputBetaBand && !outputDNBand) {
            throw new OperatorException("No output product is selected");
        }
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

            final InputProductValidator validator = new InputProductValidator(sourceProduct);
            validator.checkIfSentinel1Product();
            validator.checkAcquisitionMode(new String[] {"IW","EW","SM"});
            validator.checkProductType(new String[] {"SLC","GRD"});

            absRoot = AbstractMetadata.getAbstractedMetadata(sourceProduct);
            isMultiSwath = validator.isMultiSwath();

            getProductType();

            getProductPolarization();

            getSampleType();

            if (absRoot.getAttribute(AbstractMetadata.abs_calibration_flag).getData().getElemBoolean()) {
                dataType = getCalibrationType(sourceProduct.getBandAt(0).getName());
            }

            getVectors();

            createTargetBandToCalInfoMap();

            if (mustUpdateMetadata) {
                updateTargetProductMetadata();
            }

        } catch (Exception e) {
            throw new OperatorException(e);
        }
    }

    /**
     * Get product type from abstracted metadata.
     */
    private void getProductType() {
        productType = absRoot.getAttributeString(AbstractMetadata.PRODUCT_TYPE);
    }

    /**
     * Get product polarization.
     */
    private void getProductPolarization() {

        final String productName = absRoot.getAttributeString(AbstractMetadata.PRODUCT);
        final String level = productName.substring(12, 14);
        if (!level.equals("1S")) {
            throw new OperatorException("Invalid source product");
        }

        polarization = productName.substring(14, 16);
        if (!polarization.equals("SH") && !polarization.equals("SV") && !polarization.equals("DH") &&
                !polarization.equals("DV") && !polarization.equals("HH") && !polarization.equals("HV") &&
                !polarization.equals("VV") && !polarization.equals("VH")) {
            throw new OperatorException("Invalid source product");
        }
    }

    private void getVectors() {

        boolean getSigmaLUT = outputSigmaBand;
        boolean getBetaLUT = outputBetaBand;
        boolean getGammaLUT = outputGammaBand;
        boolean getDNLUT = outputDNBand;

        if (dataType != null) {
            if (dataType.equals(CALTYPE.SIGMA0)) {
                getSigmaLUT = true;
            } else if (dataType.equals(CALTYPE.BETA0)) {
                getBetaLUT = true;
            } else if (dataType.equals(CALTYPE.GAMMA)) {
                getGammaLUT = true;
            } else {
                getDNLUT = true;
            }
        }

        calibration = getCalibrationVectors(sourceProduct, selectedPolList,
                getSigmaLUT, getBetaLUT, getGammaLUT, getDNLUT);
    }

    /**
     * Get calibration vectors from metadata.
     */
    public static CalibrationInfo[] getCalibrationVectors(
            final Product sourceProduct, final java.util.List<String> selectedPolList,
            final boolean getSigmaLUT, final boolean getBetaLUT, final boolean getGammaLUT,
            final boolean getDNLUT) {

        final List<CalibrationInfo> calibrationInfoList = new ArrayList<>();
        final MetadataElement origProdRoot = AbstractMetadata.getOriginalProductMetadata(sourceProduct);
        final MetadataElement calibrationElem = origProdRoot.getElement("calibration");
        final MetadataElement[] calibrationDataSetListElem = calibrationElem.getElements();

        for (MetadataElement dataSetListElem : calibrationDataSetListElem) {

            final MetadataElement calElem = dataSetListElem.getElement("calibration");
            final MetadataElement adsHeaderElem = calElem.getElement("adsHeader");
            final String pol = adsHeaderElem.getAttributeString("polarisation");
            if (!selectedPolList.contains(pol)) {
                continue;
            }

            final MetadataElement calVecListElem = calElem.getElement("calibrationVectorList");

            final String subSwath = adsHeaderElem.getAttributeString("swath");
            final double firstLineTime = Sentinel1Utils.getTime(adsHeaderElem, "startTime").getMJD();
            final double lastLineTime = Sentinel1Utils.getTime(adsHeaderElem, "stopTime").getMJD();
            final int numOfLines = getNumOfLines(origProdRoot, pol, subSwath);
            final int count = calVecListElem.getAttributeInt("count");
            final Sentinel1Utils.CalibrationVector[] calibrationVectorList =
                    Sentinel1Utils.getCalibrationVector(
                            calVecListElem, getSigmaLUT, getBetaLUT, getGammaLUT, getDNLUT);

            calibrationInfoList.add(new CalibrationInfo(subSwath, pol,
                    firstLineTime, lastLineTime, numOfLines, count, calibrationVectorList));
        }

        return calibrationInfoList.toArray(new CalibrationInfo[calibrationInfoList.size()]);
    }

    /**
     * Create a target band name to CalibrationInfo map.
     */
    private void createTargetBandToCalInfoMap() {

        final String[] targetBandNames = targetProduct.getBandNames();
        for (CalibrationInfo cal : calibration) {
            final String pol = cal.polarization;
            final String ss = cal.subSwath;
            for (String bandName : targetBandNames) {
                if (isMultiSwath) {
                    if (bandName.contains(pol) && bandName.contains(ss)) {
                        targetBandToCalInfo.put(bandName, cal);
                    }
                } else {
                    if (bandName.contains(pol)) {
                        targetBandToCalInfo.put(bandName, cal);
                    }
                }
            }
        }
    }

    /**
     * Get the number of output lines of a given swath.
     *
     * @param origProdRoot Root of the original metadata of the source product.
     * @param polarization Polarization of the given swath.
     * @param swath        Swath name.
     * @return The number of output lines.
     */
    public static int getNumOfLines(final MetadataElement origProdRoot, final String polarization, final String swath) {

        final MetadataElement annotationElem = origProdRoot.getElement("annotation");
        final MetadataElement[] annotationDataSetListElem = annotationElem.getElements();

        for (MetadataElement dataSetListElem : annotationDataSetListElem) {
            final String elemName = dataSetListElem.getName();
            if (elemName.contains(swath.toLowerCase()) && elemName.contains(polarization.toLowerCase())) {
                final MetadataElement productElem = dataSetListElem.getElement("product");
                final MetadataElement imageAnnotationElem = productElem.getElement("imageAnnotation");
                final MetadataElement imageInformationElem = imageAnnotationElem.getElement("imageInformation");
                return imageInformationElem.getAttributeInt("numberOfLines");
            }
        }

        return -1;
    }

    /**
     * Update the metadata in the target product.
     */
    private void updateTargetProductMetadata() {

        final MetadataElement absRoot = AbstractMetadata.getAbstractedMetadata(targetProduct);
        absRoot.getAttribute(AbstractMetadata.abs_calibration_flag).getData().setElemBoolean(true);

        final String[] targetBandNames = targetProduct.getBandNames();
        Sentinel1Utils.updateBandNames(absRoot, selectedPolList, targetBandNames);

        final MetadataElement[] bandMetadataList = AbstractMetadata.getBandAbsMetadataList(absRoot);
        for (MetadataElement bandMeta : bandMetadataList) {
            boolean polFound = false;
            for (String pol : selectedPolList) {
                if (bandMeta.getName().contains(pol)) {
                    polFound = true;
                    break;
                }
            }
            if (!polFound) {
                // remove band metadata if polarization is not included
                absRoot.removeElement(bandMeta);
            }
        }
    }

    /**
     * Create target product.
     */
    @Override
    public Product createTargetProduct(final Product sourceProduct, final String[] sourceBandNames) {

        targetProduct = new Product(sourceProduct.getName(),
                sourceProduct.getProductType(),
                sourceProduct.getSceneRasterWidth(),
                sourceProduct.getSceneRasterHeight());

        addSelectedBands(sourceProduct, sourceBandNames);

        ProductUtils.copyProductNodes(sourceProduct, targetProduct);

        return targetProduct;
    }

    private void addSelectedBands(final Product sourceProduct, final String[] sourceBandNames) {

        final Band[] sourceBands = OperatorUtils.getSourceBands(sourceProduct, sourceBandNames, false);

        for (int i = 0; i < sourceBands.length; i++) {

            final Band srcBand = sourceBands[i];
            final String unit = srcBand.getUnit();
            if (unit == null) {
                throw new OperatorException("band " + srcBand.getName() + " requires a unit");
            }

            if (!unit.contains(Unit.REAL) && !unit.contains(Unit.AMPLITUDE) && !unit.contains(Unit.INTENSITY)) {
                continue;
            }

            String[] srcBandNames;
            if (unit.contains(Unit.REAL)) { // SLC

                if (i + 1 >= sourceBands.length) {
                    throw new OperatorException("Real and imaginary bands are not in pairs");
                }

                final String nextUnit = sourceBands[i + 1].getUnit();
                if (nextUnit == null || !nextUnit.contains(Unit.IMAGINARY)) {
                    throw new OperatorException("Real and imaginary bands are not in pairs");
                }

                srcBandNames = new String[2];
                srcBandNames[0] = srcBand.getName();
                srcBandNames[1] = sourceBands[i + 1].getName();
                ++i;

            } else { // GRD or calibrated product

                srcBandNames = new String[1];
                srcBandNames[0] = srcBand.getName();
            }

            final String pol = srcBandNames[0].substring(srcBandNames[0].lastIndexOf("_") + 1);
            if (!selectedPolList.contains(pol)) {
                continue;
            }

            final String[] targetBandNames = createTargetBandNames(srcBandNames[0]);
            for (String tgtBandName : targetBandNames) {
                if (targetProduct.getBand(tgtBandName) == null) {

                    targetBandNameToSourceBandName.put(tgtBandName, srcBandNames);

                    final Band targetBand = new Band(tgtBandName,
                            ProductData.TYPE_FLOAT32,
                            srcBand.getSceneRasterWidth(),
                            srcBand.getSceneRasterHeight());

                    targetBand.setUnit(Unit.INTENSITY);
                    targetProduct.addBand(targetBand);
                }
            }
        }
    }

    /**
     * Create target band names for given source band name.
     *
     * @param srcBandName The given source band name.
     * @return The target band name array.
     */
    private String[] createTargetBandNames(final String srcBandName) {

        final int cnt = (outputSigmaBand ? 1 : 0) + (outputGammaBand ? 1 : 0) + (outputBetaBand ? 1 : 0) + (outputDNBand ? 1 : 0);
        String[] targetBandNames = new String[cnt];

        final String pol = srcBandName.substring(srcBandName.indexOf("_"));
        int k = 0;
        if (outputSigmaBand) {
            targetBandNames[k++] = "Sigma0" + pol;
        }

        if (outputGammaBand) {
            targetBandNames[k++] = "Gamma0" + pol;
        }

        if (outputBetaBand) {
            targetBandNames[k++] = "Beta0" + pol;
        }

        if (outputDNBand) {
            targetBandNames[k] = "DN" + pol;
        }

        return targetBandNames;
    }

    /**
     * Called by the framework in order to compute a tile for the given target band.
     * <p>The default implementation throws a runtime exception with the message "not implemented".</p>
     *
     * @param targetBand The target band.
     * @param targetTile The current tile associated with the target band to be computed.
     * @param pm         A progress monitor which should be used to determine computation cancelation requests.
     * @throws org.esa.snap.framework.gpf.OperatorException If an error occurs during computation of the target raster.
     */
    public void computeTile(Band targetBand, Tile targetTile, ProgressMonitor pm) throws OperatorException {

        final Rectangle targetTileRectangle = targetTile.getRectangle();
        final int x0 = targetTileRectangle.x;
        final int y0 = targetTileRectangle.y;
        final int w = targetTileRectangle.width;
        final int h = targetTileRectangle.height;
        //System.out.println("x0 = " + x0 + ", y0 = " + y0 + ", w = " + w + ", h = " + h + ", target band = " + targetBand.getName());

        Tile sourceRaster1 = null;
        ProductData srcData1 = null;
        ProductData srcData2 = null;
        Band sourceBand1 = null;

        final String targetBandName = targetBand.getName();
        final String[] srcBandNames = targetBandNameToSourceBandName.get(targetBandName);
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

        final double noDataValue = sourceBand1.getNoDataValue();
        final Unit.UnitType bandUnit = Unit.getUnitType(sourceBand1);
        final ProductData trgData = targetTile.getDataBuffer();
        final TileIndex srcIndex = new TileIndex(sourceRaster1);
        final TileIndex trgIndex = new TileIndex(targetTile);
        final int maxY = y0 + h;
        final int maxX = x0 + w;

        final boolean complexData = bandUnit == Unit.UnitType.REAL || bandUnit == Unit.UnitType.IMAGINARY;
        final CalibrationInfo calInfo = targetBandToCalInfo.get(targetBandName);
        final Sentinel1Calibrator.CALTYPE calType = Sentinel1Calibrator.getCalibrationType(targetBandName);

        double dn, dn2, i, q, muX, lutVal, retroLutVal = 1.0;
        int srcIdx, trgIdx;
        for (int y = y0; y < maxY; ++y) {
            srcIndex.calculateStride(y);
            trgIndex.calculateStride(y);

            final int calVecIdx = calInfo.getCalibrationVectorIndex(y);
            final Sentinel1Utils.CalibrationVector vec0 = calInfo.getCalibrationVector(calVecIdx);
            final Sentinel1Utils.CalibrationVector vec1 = calInfo.getCalibrationVector(calVecIdx + 1);
            final float[] vec0LUT = getVector(calType, vec0);
            final float[] vec1LUT = getVector(calType, vec1);
            float[] retroVec0LUT = null;
            float[] retroVec1LUT = null;
            if (dataType != null) {
                retroVec0LUT = getVector(dataType, vec0);
                retroVec1LUT = getVector(dataType, vec1);
            }
            final double azTime = calInfo.firstLineTime + y * calInfo.lineTimeInterval;
            final double muY = (azTime - vec0.timeMJD) / (vec1.timeMJD - vec0.timeMJD);

            for (int x = x0; x < maxX; ++x) {
                srcIdx = srcIndex.getIndex(x);
                trgIdx = trgIndex.getIndex(x);

                if (srcData1.getElemDoubleAt(srcIdx) == noDataValue) {
                    continue;
                }

                final int pixelIdx = calInfo.getPixelIndex(x, calVecIdx);
                muX = (x - vec0.pixels[pixelIdx]) / (double)(vec0.pixels[pixelIdx + 1] - vec0.pixels[pixelIdx]);

                lutVal = (1 - muY) * ((1 - muX) * vec0LUT[pixelIdx] + muX * vec0LUT[pixelIdx + 1]) +
                        muY * ((1 - muX) * vec1LUT[pixelIdx] + muX * vec1LUT[pixelIdx + 1]);

                if (complexData) {
                    i = srcData1.getElemDoubleAt(srcIdx);
                    q = srcData2.getElemDoubleAt(srcIdx);
                    trgData.setElemDoubleAt(trgIdx, (i * i + q * q) / (lutVal*lutVal));
                } else if (bandUnit == Unit.UnitType.AMPLITUDE) {
                    dn = srcData1.getElemDoubleAt(srcIdx);
                    trgData.setElemDoubleAt(trgIdx, (dn * dn) / (lutVal*lutVal));
                } else { // intensity
                    if (dataType != null) {
                        retroLutVal = (1 - muY) * ((1 - muX) * retroVec0LUT[pixelIdx] + muX * retroVec0LUT[pixelIdx + 1]) +
                                muY * ((1 - muX) * retroVec1LUT[pixelIdx] + muX * retroVec1LUT[pixelIdx + 1]);
                    }
                    dn2 = srcData1.getElemDoubleAt(srcIdx);
                    trgData.setElemDoubleAt(trgIdx, dn2 * retroLutVal / (lutVal*lutVal));
                }
            }
        }
    }

    public static CALTYPE getCalibrationType(final String bandName) {
        CALTYPE calType;
        if (bandName.contains("Sigma")) {
            calType = CALTYPE.SIGMA0;
        } else if (bandName.contains("Beta")) {
            calType = CALTYPE.BETA0;
        } else if (bandName.contains("Gamma")) {
            calType = CALTYPE.GAMMA;
        } else {
            calType = CALTYPE.DN;
        }
        return calType;
    }

    public static float[] getVector(final CALTYPE calType, final Sentinel1Utils.CalibrationVector vec) {
        if (calType == null) {
            return null;
        }else if (calType.equals(CALTYPE.SIGMA0)) {
            return vec.sigmaNought;
        } else if (calType.equals(CALTYPE.BETA0)) {
            return vec.betaNought;
        } else if (calType.equals(CALTYPE.GAMMA)) {
            return vec.gamma;
        } else {
            return vec.dn;
        }
    }


    public double applyCalibration(
            final double v, final double rangeIndex, final double azimuthIndex, final double slantRange,
            final double satelliteHeight, final double sceneToEarthCentre, final double localIncidenceAngle,
            final String bandPolar, final Unit.UnitType bandUnit, int[] subSwathIndex) {

        final String targetBandName = "Sigma0_" + bandPolar.toUpperCase();
        final CalibrationInfo calInfo = targetBandToCalInfo.get(targetBandName);
        final int calVecIdx = calInfo.getCalibrationVectorIndex((int)azimuthIndex);
        final Sentinel1Utils.CalibrationVector vec0 = calInfo.getCalibrationVector(calVecIdx);
        final Sentinel1Utils.CalibrationVector vec1 = calInfo.getCalibrationVector(calVecIdx + 1);
        final Sentinel1Calibrator.CALTYPE calType = Sentinel1Calibrator.getCalibrationType(targetBandName);
        final float[] vec0LUT = Sentinel1Calibrator.getVector(calType, vec0);
        final float[] vec1LUT = Sentinel1Calibrator.getVector(calType, vec1);
        final int pixelIdx = calInfo.getPixelIndex((int)rangeIndex, calVecIdx);
        final double azTime = calInfo.firstLineTime + azimuthIndex * calInfo.lineTimeInterval;
        final double muY = (azTime - vec0.timeMJD) / (vec1.timeMJD - vec0.timeMJD);
        final double muX =
                (rangeIndex - vec0.pixels[pixelIdx]) / (double)(vec0.pixels[pixelIdx + 1] - vec0.pixels[pixelIdx]);

        final double lutVal =
                (1 - muY) * ((1 - muX) * vec0LUT[pixelIdx] + muX * vec0LUT[pixelIdx + 1]) +
                        muY * ((1 - muX) * vec1LUT[pixelIdx] + muX * vec1LUT[pixelIdx + 1]);

        double sigma = 0.0;
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

        return sigma / lutVal;
    }

    public double applyRetroCalibration(
            int x, int y, double v, String bandPolar, final Unit.UnitType bandUnit, int[] subSwathIndex) {

        // no need to do anything because s-1 GRD product has not yet been calibrated
        return v;
    }

    public void removeFactorsForCurrentTile(final Band targetBand, final Tile targetTile,
                                            final String srcBandName) throws OperatorException {

        final Band sourceBand = sourceProduct.getBand(targetBand.getName());
        final Tile sourceTile = calibrationOp.getSourceTile(sourceBand, targetTile.getRectangle());
        targetTile.setRawSamples(sourceTile.getRawSamples());
    }

    public final static class CalibrationInfo {
        public final String subSwath;
        public final String polarization;
        public final double firstLineTime;
        public final double lastLineTime;
        public final int numOfLines;
        public final int count; // number of calibrationVector records within the list
        public final Sentinel1Utils.CalibrationVector[] calibrationVectorList;
        public final double lineTimeInterval;

        CalibrationInfo(String subSwath, String polarization, final double firstLineTime, final double lastLineTime,
                        final int numOfLines, final int count,
                        final Sentinel1Utils.CalibrationVector[] calibrationVectorList) {
            this.subSwath = subSwath;
            this.polarization = polarization;
            this.firstLineTime = firstLineTime;
            this.lastLineTime = lastLineTime;
            this.numOfLines = numOfLines;
            this.count = count;
            this.calibrationVectorList = calibrationVectorList;

            this.lineTimeInterval = (lastLineTime - firstLineTime) / (numOfLines - 1);
        }

        public int getCalibrationVectorIndex(final int y) {
            for (int i = 1; i < count; i++) {
                if (y < calibrationVectorList[i].line) {
                    return i - 1;
                }
            }
            return -1;
        }

        public Sentinel1Utils.CalibrationVector getCalibrationVector(final int calVecIdx) {
            return calibrationVectorList[calVecIdx];
        }

        public int getPixelIndex(final int x, final int calVecIdx) {

            final Sentinel1Utils.CalibrationVector calVec = calibrationVectorList[calVecIdx];
            final int length = calVec.pixels.length;
            for (int i = 0; i < length; i++) {
                if (x < calVec.pixels[i]) {
                    return i - 1;
                }
            }
            return length - 2;
        }
    }
}
