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
package org.esa.nest.gpf;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.framework.datamodel.*;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.Tile;
import org.esa.beam.util.ProductUtils;
import org.esa.nest.datamodel.AbstractMetadata;
import org.esa.nest.datamodel.BaseCalibrator;
import org.esa.nest.datamodel.Calibrator;
import org.esa.nest.datamodel.Unit;

import java.awt.*;
import java.io.File;
import java.util.Arrays;
import java.util.HashMap;

/**
 * Calibration for Sentinel1 data products.
 */

public class Sentinel1Calibrator extends BaseCalibrator implements Calibrator {

    private String productType = null;
    private String acquisitionMode = null;
    private String polarization = null;
    private int numOfSubSwath = 1;
    private CalibrationInfo[] calibration = null;
    private boolean isGRD = false;
    protected final HashMap<String, CalibrationInfo> targetBandToCalInfo = new HashMap<String, CalibrationInfo>(2);
    private java.util.List<String> selectedPolList = null;
    private boolean outputSigmaBand = false;
    private boolean outputGammaBand = false;
    private boolean outputBetaBand = false;
    private boolean outputDNBand = false;

    public enum CALTYPE { SIGMA0, BETA0, GAMMA0, DN }

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
            final MetadataElement origProdRoot = AbstractMetadata.getOriginalProductMetadata(sourceProduct);
            selectedPols = Sentinel1DeburstTOPSAROp.getProductPolarizations(origProdRoot);
        }
        selectedPolList = Arrays.asList(selectedPols);

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

            absRoot = AbstractMetadata.getAbstractedMetadata(sourceProduct);

            getCalibrationFlag();

            getMission();

            getProductType();

            getAcquisitionMode();

            getProductPolarization();

            getSampleType();

            calibration = new CalibrationInfo[numOfSubSwath * selectedPolList.size()];
            getCalibrationVectors(sourceProduct, selectedPolList, outputSigmaBand, outputBetaBand, outputGammaBand,
                    outputDNBand, calibration);

            createTargetBandToCalInfoMap();

            if (mustUpdateMetadata) {
                updateTargetProductMetadata();
            }

        } catch (Exception e) {
            throw new OperatorException(e);
        }
    }

    /**
     * Get product mission from abstracted metadata.
     */
    private void getMission() {
        final String mission = absRoot.getAttributeString(AbstractMetadata.MISSION);
        if (!mission.equals("SENTINEL-1A")) {
            throw new OperatorException(mission + " is not a valid mission for Sentinel1 product");
        }
    }

    /**
     * Get product type from abstracted metadata.
     */
    private void getProductType() {
        productType = absRoot.getAttributeString(AbstractMetadata.PRODUCT_TYPE);
        if (!productType.equals("SLC") && !productType.equals("GRD")) {
            throw new OperatorException(productType + " is not a valid product type for Sentinel1 product");
        }
        isGRD = productType.equals("GRD");
    }

    /**
     * Get acquisition mode from abstracted metadata.
     */
    private void getAcquisitionMode() {

        acquisitionMode = absRoot.getAttributeString(AbstractMetadata.ACQUISITION_MODE);

        if (productType.equals("SLC")) {
            if (acquisitionMode.equals("IW")) {
                numOfSubSwath = 3;
            } else if (acquisitionMode.equals("EW")) {
                numOfSubSwath = 5;
            }
        }
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

    /**
     * Get calibration vectors from metadata.
     */
    public static void getCalibrationVectors(final Product sourceProduct, final java.util.List<String> selectedPolList,
                                             final boolean outputSigmaBand, final boolean outputBetaBand,
                                             final boolean outputGammaBand, final boolean outputDNBand,
                                             CalibrationInfo[] calibration) {

        final MetadataElement origProdRoot = AbstractMetadata.getOriginalProductMetadata(sourceProduct);
        final MetadataElement calibrationElem = origProdRoot.getElement("calibration");
        final MetadataElement[] calibrationDataSetListElem = calibrationElem.getElements();

        int dataSetIndex = 0;
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
                            calVecListElem, outputSigmaBand, outputBetaBand, outputGammaBand, outputDNBand);

            calibration[dataSetIndex] = new CalibrationInfo(subSwath, pol,
                    firstLineTime, lastLineTime, numOfLines, count, calibrationVectorList);

            dataSetIndex++;
        }
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
                if (!isGRD) {
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

        final MetadataElement abs = AbstractMetadata.getAbstractedMetadata(targetProduct);
        abs.getAttribute(AbstractMetadata.abs_calibration_flag).getData().setElemBoolean(true);

        final String[] targetBandNames = targetProduct.getBandNames();
        final MetadataElement[] children = abs.getElements();
        for (MetadataElement child : children) {
            final String childName = child.getName();
            if (childName.startsWith(AbstractMetadata.BAND_PREFIX)) {
                final String pol = childName.substring(childName.lastIndexOf("_") + 1);
                final String sw_pol = childName.substring(childName.indexOf("_") + 1);
                if (selectedPolList.contains(pol)) {
                    String bandNameArray = "";
                    for (String bandName : targetBandNames) {
                        if (!isGRD && bandName.contains(sw_pol) || isGRD && bandName.contains(pol)) {
                            bandNameArray += bandName + " ";
                        }
                    }
                    child.setAttributeString(AbstractMetadata.band_names, bandNameArray);
                } else {
                    abs.removeElement(child);
                }
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

        final Band[] sourceBands = OperatorUtils.getSourceBands(sourceProduct, sourceBandNames);

        for (int i = 0; i < sourceBands.length; i++) {

            final Band srcBand = sourceBands[i];
            final String unit = srcBand.getUnit();
            if (unit == null) {
                throw new OperatorException("band " + srcBand.getName() + " requires a unit");
            }

            if (!unit.contains(Unit.REAL) && !unit.contains(Unit.AMPLITUDE)) {
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

            } else { // GRD

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
     * @throws org.esa.beam.framework.gpf.OperatorException If an error occurs during computation of the target raster.
     */
    public void computeTile(Band targetBand, Tile targetTile, ProgressMonitor pm) throws OperatorException {

        final Rectangle targetTileRectangle = targetTile.getRectangle();
        final int x0 = targetTileRectangle.x;
        final int y0 = targetTileRectangle.y;
        final int w = targetTileRectangle.width;
        final int h = targetTileRectangle.height;
        //System.out.println("x0 = " + x0 + ", y0 = " + y0 + ", w = " + w + ", h = " + h + ", target band = " + targetBand.getName());

        final String targetBandName = targetBand.getName();
        final CalibrationInfo calInfo = targetBandToCalInfo.get(targetBandName);

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
        final TileIndex tgtIndex = new TileIndex(targetTile);
        final int maxY = y0 + h;
        final int maxX = x0 + w;

        final boolean complexData = bandUnit == Unit.UnitType.REAL || bandUnit == Unit.UnitType.IMAGINARY;
        final CALTYPE calType = getCalibrationType(targetBandName);

        double dn, dn2, i, q;
        int srcIdx, tgtIdx;
        for (int y = y0; y < maxY; ++y) {
            srcIndex.calculateStride(y);
            tgtIndex.calculateStride(y);
            final double[] lut = new double[w];
            computeTileCalibrationLUTs(y, x0, y0, w, calInfo, calType, lut);

            for (int x = x0; x < maxX; ++x) {
                final int xx = x - x0;
                srcIdx = srcIndex.getIndex(x);
                tgtIdx = tgtIndex.getIndex(x);
                if (bandUnit == Unit.UnitType.AMPLITUDE) {
                    dn = srcData1.getElemDoubleAt(srcIdx);
                    dn2 = dn * dn;
                } else if (complexData) {
                    i = srcData1.getElemDoubleAt(srcIdx);
                    q = srcData2.getElemDoubleAt(srcIdx);
                    dn2 = i * i + q * q;
                } else {
                    throw new OperatorException("Calibration: unhandled unit");
                }

                // todo: check if lut should be squared
                trgData.setElemDoubleAt(tgtIdx, dn2 / lut[xx]);
            }
        }
    }

    public static CALTYPE getCalibrationType(final String targetBandName) {
        CALTYPE calType;
        if (targetBandName.contains("Sigma")) {
            calType = CALTYPE.SIGMA0;
        } else if (targetBandName.contains("Beta")) {
            calType = CALTYPE.BETA0;
        } else if (targetBandName.contains("Gamma")) {
            calType = CALTYPE.GAMMA0;
        } else {
            calType = CALTYPE.DN;
        }
        return calType;
    }

    /**
     * Compute calibration LUTs for the given range line.
     *
     * @param y              Index of the given range line.
     * @param x0             X coordinate of the upper left corner pixel of the given tile.
     * @param y0             Y coordinate of the upper left corner pixel of the given tile.
     * @param w              Tile width.
     * @param calInfo        Object of CalibrationInfo class.
     * @param calType        one of sigma0, beta0, gamma0 or dn.
     * @param lut            LUT for calibration.
     */
    public static void computeTileCalibrationLUTs(final int y, final int x0, final int y0, final int w,
                                                  final CalibrationInfo calInfo, final CALTYPE calType,
                                                  double[] lut) {

        double v00, v01, v10, v11, muX;
        int calVecIdx = getCalibrationVectorIndex(y0, calInfo);
        int pixelIdx = getPixelIndex(x0, calVecIdx, calInfo);

        final Sentinel1Utils.CalibrationVector vec0 = calInfo.calibrationVectorList[calVecIdx];
        final Sentinel1Utils.CalibrationVector vec1 = calInfo.calibrationVectorList[calVecIdx + 1];

        final double azTime = calInfo.firstLineTime + y * calInfo.lineTimeInterval;
        final double azT0 = vec0.timeMJD;
        final double azT1 = vec1.timeMJD;
        final double muY = (azTime - azT0) / (azT1 - azT0);

        final int maxX =  x0 + w;
        for (int x = x0; x < maxX; x++) {

            if (x > vec0.pixels[pixelIdx + 1]) {
                pixelIdx++;
            }

            muX = (double) (x - vec0.pixels[pixelIdx]) / (double) (vec0.pixels[pixelIdx + 1] - vec0.pixels[pixelIdx]);

            if (calType.equals(CALTYPE.SIGMA0)) {
                v00 = vec0.sigmaNought[pixelIdx];
                v01 = vec0.sigmaNought[pixelIdx + 1];
                v10 = vec1.sigmaNought[pixelIdx];
                v11 = vec1.sigmaNought[pixelIdx + 1];
            } else if (calType.equals(CALTYPE.BETA0)) {
                v00 = vec0.betaNought[pixelIdx];
                v01 = vec0.betaNought[pixelIdx + 1];
                v10 = vec1.betaNought[pixelIdx];
                v11 = vec1.betaNought[pixelIdx + 1];
            } else if (calType.equals(CALTYPE.GAMMA0)) {
                v00 = vec0.gamma[pixelIdx];
                v01 = vec0.gamma[pixelIdx + 1];
                v10 = vec1.gamma[pixelIdx];
                v11 = vec1.gamma[pixelIdx + 1];
            } else {
                v00 = vec0.dn[pixelIdx];
                v01 = vec0.dn[pixelIdx + 1];
                v10 = vec1.dn[pixelIdx];
                v11 = vec1.dn[pixelIdx + 1];
            }

            lut[x - x0] = org.esa.nest.util.MathUtils.interpolationBiLinear(v00, v01, v10, v11, muX, muY);
        }
    }

    /**
     * Get index of the calibration vector in the list for a given line.
     *
     * @param y       Line coordinate.
     * @param calInfo Object of CalibrationInfo class.
     * @return The calibration vector index.
     */
    private static int getCalibrationVectorIndex(final int y, final CalibrationInfo calInfo) {

        for (int i = 0; i < calInfo.count; i++) {
            if (y < calInfo.calibrationVectorList[i].line) {
                return i - 1;
            }
        }
        return -1;
    }

    /**
     * Get pixel index in a given calibration vector for a given pixel.
     *
     * @param x         Pixel coordinate.
     * @param calVecIdx Calibration vector index.
     * @param calInfo   Object of CalibrationInfo class.
     * @return The pixel index.
     */
    private static int getPixelIndex(final int x, final int calVecIdx, final CalibrationInfo calInfo) {

        for (int i = 0; i < calInfo.calibrationVectorList[calVecIdx].pixels.length; i++) {
            if (x < calInfo.calibrationVectorList[calVecIdx].pixels[i]) {
                return i - 1;
            }
        }
        return -1;
    }


    public double applyCalibration(
            final double v, final double rangeIndex, final double azimuthIndex, final double slantRange,
            final double satelliteHeight, final double sceneToEarthCentre, final double localIncidenceAngle,
            final String bandPolar, final Unit.UnitType bandUnit, int[] subSwathIndex) {

        return 0.0;
    }

    public double applyRetroCalibration(int x, int y, double v, String bandPolar, final Unit.UnitType bandUnit, int[] subSwathIndex) {
        return 0.0;
    }

    public void removeFactorsForCurrentTile(final Band targetBand, final Tile targetTile,
                                            final String srcBandName) throws OperatorException {

        final Band sourceBand = sourceProduct.getBand(targetBand.getName());
        final Tile sourceTile = calibrationOp.getSourceTile(sourceBand, targetTile.getRectangle());
        targetTile.setRawSamples(sourceTile.getRawSamples());
    }

    public static class CalibrationInfo {
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
    }
}