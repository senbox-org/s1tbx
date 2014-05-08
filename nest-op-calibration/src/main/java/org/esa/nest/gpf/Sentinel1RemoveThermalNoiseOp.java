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
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.MetadataElement;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.Tile;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.framework.gpf.annotations.SourceProduct;
import org.esa.beam.framework.gpf.annotations.TargetProduct;
import org.esa.beam.util.ProductUtils;
import org.esa.nest.datamodel.AbstractMetadata;
import org.esa.nest.datamodel.Unit;

import java.awt.*;
import java.util.Arrays;
import java.util.HashMap;

/**
 * Apply thermal noise correction to Sentinel-1 Level-1 products.
 */
@OperatorMetadata(alias = "Sentinel1RemoveThermalNoise",
        category = "SAR Tools\\Radiometric Correction",
        authors = "Cecilia Wong, Jun Lu, Luis Veci",
        copyright = "Copyright (C) 2014 by Array Systems Computing Inc.",
        description = "Removes thermal noise from Sentinel-1 products")
public final class Sentinel1RemoveThermalNoiseOp extends Operator {

    @SourceProduct
    private Product sourceProduct;
    @TargetProduct
    private Product targetProduct;

    @Parameter(description = "The list of polarisations", label = "Polarisations")
    private String[] selectedPolarisations;

    @Parameter(description = "Remove thermal noise", defaultValue = "true", label = "Remove Thermal Noise")
    private Boolean removeThermalNoise = true;

    @Parameter(description = "Re-introduce thermal noise", defaultValue = "false", label = "Re-Introduce Thermal Noise")
    private Boolean reIntroduceThermalNoise = false;

    private MetadataElement absRoot = null;
    private MetadataElement origMetadataRoot = null;
    private boolean thermalNoiseCorrectionPerformed = false;
    private boolean absoluteCalibrationPerformed = false;
    private boolean inputSigmaBand = false;
    private boolean inputBetaBand = false;
    private boolean inputGammaBand = false;
    private boolean inputDNBand = false;
    private boolean isSLC = false;
    private String productType = null;
    private int numOfSubSwath = 1;
    private ThermalNoiseInfo[] noise = null;
    private Sentinel1Calibrator.CalibrationInfo[] calibration = null;
    private java.util.List<String> selectedPolList = null;
    private final HashMap<String, String[]> targetBandNameToSourceBandName = new HashMap<String, String[]>(2);

    /**
     * Default constructor. The graph processing framework
     * requires that an operator has a default constructor.
     */
    public Sentinel1RemoveThermalNoiseOp() {
    }

    /**
     * Initializes this operator and sets the one and only target product.
     * <p>The target product can be either defined by a field of type {@link org.esa.beam.framework.datamodel.Product} annotated with the
     * {@link org.esa.beam.framework.gpf.annotations.TargetProduct TargetProduct} annotation or
     * by calling {@link #setTargetProduct} method.</p>
     * <p>The framework calls this method after it has created this operator.
     * Any client code that must be performed before computation of tile data
     * should be placed here.</p>
     *
     * @throws org.esa.beam.framework.gpf.OperatorException If an error occurs during operator initialisation.
     * @see #getTargetProduct()
     */
    @Override
    public void initialize() throws OperatorException {

        try {
            absRoot = AbstractMetadata.getAbstractedMetadata(sourceProduct);
            origMetadataRoot = AbstractMetadata.getOriginalProductMetadata(sourceProduct);

            getMission();

            getProductType();

            getAcquisitionMode();

            getProductPolarization();

            getThermalNoiseCorrectionFlag();

            setSelectedPolarisations();

            getThermalNoiseVectors();

            getCalibrationFlag();

            if (absoluteCalibrationPerformed) {
                getCalibrationVectors();
            }

            createTargetProduct();

            updateTargetProductMetadata();

        } catch (Throwable e) {
            OperatorUtils.catchOperatorException(getId(), e);
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

        isSLC = productType.equals("SLC");
    }

    /**
     * Get acquisition mode from abstracted metadata.
     */
    private void getAcquisitionMode() {

        final String acquisitionMode = absRoot.getAttributeString(AbstractMetadata.ACQUISITION_MODE);

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

        final String polarization = productName.substring(14, 16);
        if (!polarization.equals("SH") && !polarization.equals("SV") && !polarization.equals("DH") &&
                !polarization.equals("DV") && !polarization.equals("HH") && !polarization.equals("HV") &&
                !polarization.equals("VV") && !polarization.equals("VH")) {
            throw new OperatorException("Invalid source product");
        }
    }

    /**
     * Get thermal noise correction flag from the original product metadata.
     */
    private void getThermalNoiseCorrectionFlag() {

        final MetadataElement annotationElem = origMetadataRoot.getElement("annotation");
        final MetadataElement[] annotationDataSetListElem = annotationElem.getElements();
        final MetadataElement productElem = annotationDataSetListElem[0].getElement("product");
        final MetadataElement imageAnnotationElem = productElem.getElement("imageAnnotation");
        final MetadataElement processingInformationElem = imageAnnotationElem.getElement("processingInformation");

        thermalNoiseCorrectionPerformed = Boolean.parseBoolean(
                processingInformationElem.getAttribute("thermalNoiseCorrectionPerformed").getData().getElemString());

        if (removeThermalNoise && thermalNoiseCorrectionPerformed) {
            throw new OperatorException("Thermal noise correction has already been performed for the product");
        }

        if (reIntroduceThermalNoise && !thermalNoiseCorrectionPerformed) {
            throw new OperatorException("Thermal noise correction has never been performed for the product");
        }
    }

    /**
     * Get thermal noise vectors from the original product metadata.
     */
    private void getThermalNoiseVectors() {

        noise = new ThermalNoiseInfo[numOfSubSwath * selectedPolList.size()];
        final MetadataElement noiseElem = origMetadataRoot.getElement("noise");
        final MetadataElement[] noiseDataSetListElem = noiseElem.getElements();

        int dataSetIndex = 0;
        for (MetadataElement dataSetListElem : noiseDataSetListElem) {

            final MetadataElement noiElem = dataSetListElem.getElement("noise");
            final MetadataElement adsHeaderElem = noiElem.getElement("adsHeader");
            final String pol = adsHeaderElem.getAttributeString("polarisation");
            if (!selectedPolList.contains(pol)) {
                continue;
            }

            final MetadataElement noiseVectorListElem = noiElem.getElement("noiseVectorList");

            noise[dataSetIndex] = new ThermalNoiseInfo();
            noise[dataSetIndex].polarization = pol;
            noise[dataSetIndex].subSwath = adsHeaderElem.getAttributeString("swath");
            noise[dataSetIndex].firstLineTime = Sentinel1Utils.getTime(adsHeaderElem, "startTime").getMJD();
            noise[dataSetIndex].lastLineTime = Sentinel1Utils.getTime(adsHeaderElem, "stopTime").getMJD();
            noise[dataSetIndex].numOfLines = Sentinel1Calibrator.getNumOfLines(
                    absRoot, noise[dataSetIndex].polarization, noise[dataSetIndex].subSwath);
            noise[dataSetIndex].count = Integer.parseInt(noiseVectorListElem.getAttributeString("count"));
            noise[dataSetIndex].noiseVectorList = Sentinel1Utils.getNoiseVector(noiseVectorListElem);

            dataSetIndex++;
        }
    }

    /**
     * Get absolute calibration flag from the abstracted metadata.
     */
    private void getCalibrationFlag() {
        absoluteCalibrationPerformed =
                absRoot.getAttribute(AbstractMetadata.abs_calibration_flag).getData().getElemBoolean();

        if (absoluteCalibrationPerformed) {
            final String[] sourceBandNames = sourceProduct.getBandNames();
            for (String bandName : sourceBandNames) {
                if (bandName.contains("Sigma0")) {
                    inputSigmaBand = true;
                } else if (bandName.contains("Gamma0")) {
                    inputGammaBand = true;
                } else if (bandName.contains("Beta0")) {
                    inputBetaBand = true;
                } else if (bandName.contains("DN")) {
                    inputDNBand = true;
                }
            }
        }
    }

    /**
     * Get calibration vectors from the original product metadata.
     */
    private void getCalibrationVectors() {

        calibration = new Sentinel1Calibrator.CalibrationInfo[numOfSubSwath * selectedPolList.size()];

        Sentinel1Calibrator.getCalibrationVectors(
                sourceProduct,
                selectedPolList,
                inputSigmaBand,
                inputBetaBand,
                inputGammaBand,
                inputDNBand,
                calibration);
    }

    /**
     * Set user selected polarisations.
     */
    private void setSelectedPolarisations() {

        String[] selectedPols = selectedPolarisations;
        if (selectedPols == null || selectedPols.length == 0) {
            MetadataElement absRoot = AbstractMetadata.getAbstractedMetadata(sourceProduct);
            selectedPols = Sentinel1DeburstTOPSAROp.getProductPolarizations(absRoot);
        }
        selectedPolList = Arrays.asList(selectedPols);
    }

    /**
     * Create a target product for output.
     */
    private void createTargetProduct() {

        targetProduct = new Product(sourceProduct.getName(),
                sourceProduct.getProductType(),
                sourceProduct.getSceneRasterWidth(),
                sourceProduct.getSceneRasterHeight());

        addSelectedBands();

        ProductUtils.copyProductNodes(sourceProduct, targetProduct);
    }

    /**
     * Add user selected bands to target product.
     */
    private void addSelectedBands() {

        final Band[] sourceBands = sourceProduct.getBands();
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

            } else { // GRD

                srcBandNames = new String[1];
                srcBandNames[0] = srcBand.getName();
            }

            final String pol = srcBandNames[0].substring(srcBandNames[0].lastIndexOf("_") + 1);
            if (!selectedPolList.contains(pol)) {
                continue;
            }

            final String targetBandName = createTargetBandName(srcBandNames[0]);
            if (targetProduct.getBand(targetBandName) == null) {

                targetBandNameToSourceBandName.put(targetBandName, srcBandNames);

                final Band targetBand = new Band(
                        targetBandName,
                        ProductData.TYPE_FLOAT32,
                        srcBand.getSceneRasterWidth(),
                        srcBand.getSceneRasterHeight());

                targetBand.setUnit(Unit.INTENSITY);
                targetProduct.addBand(targetBand);
            }
        }
    }

    /**
     * Create target band name for given source bane name.
     *
     * @param sourceBandName Source band name string.
     * @return Target band name string.
     */
    private String createTargetBandName(final String sourceBandName) {

        if (absoluteCalibrationPerformed) {
            return sourceBandName;
        }

        final String pol = sourceBandName.substring(sourceBandName.indexOf("_"));
        return "Intensity" + pol;
    }

    /**
     * Update target product metadata.
     */
    private void updateTargetProductMetadata() {

        final MetadataElement origMetadataRoot = AbstractMetadata.getOriginalProductMetadata(targetProduct);
        final MetadataElement annotationElem = origMetadataRoot.getElement("annotation");
        final MetadataElement[] annotationDataSetListElem = annotationElem.getElements();
        for (MetadataElement elem : annotationDataSetListElem) {
            final MetadataElement productElem = elem.getElement("product");
            final MetadataElement imageAnnotationElem = productElem.getElement("imageAnnotation");
            final MetadataElement processingInformationElem = imageAnnotationElem.getElement("processingInformation");
            if (removeThermalNoise) {
                processingInformationElem.getAttribute("thermalNoiseCorrectionPerformed").getData().setElems("true");
            }

            if (reIntroduceThermalNoise) {
                processingInformationElem.getAttribute("thermalNoiseCorrectionPerformed").getData().setElems("false");
            }
        }
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
    @Override
    public void computeTile(Band targetBand, Tile targetTile, ProgressMonitor pm) throws OperatorException {

        final Rectangle targetTileRectangle = targetTile.getRectangle();
        final int x0 = targetTileRectangle.x;
        final int y0 = targetTileRectangle.y;
        final int w = targetTileRectangle.width;
        final int h = targetTileRectangle.height;
        //System.out.println("x0 = " + x0 + ", y0 = " + y0 + ", w = " + w + ", h = " + h + ", target band = " + targetBand.getName());

        final String targetBandName = targetBand.getName();
        final ThermalNoiseInfo noiseInfo = getNoiseInfo(targetBandName);
        Sentinel1Calibrator.CalibrationInfo calInfo = null;
        if (absoluteCalibrationPerformed) {
            calInfo = getCalInfo(targetBandName);
        }

        Tile sourceRaster1 = null;
        ProductData srcData1 = null;
        ProductData srcData2 = null;
        Band sourceBand1 = null;

        final String[] srcBandNames = targetBandNameToSourceBandName.get(targetBand.getName());
        if (srcBandNames.length == 1) {
            sourceBand1 = sourceProduct.getBand(srcBandNames[0]);
            sourceRaster1 = getSourceTile(sourceBand1, targetTileRectangle);
            srcData1 = sourceRaster1.getDataBuffer();
        } else {
            sourceBand1 = sourceProduct.getBand(srcBandNames[0]);
            final Band sourceBand2 = sourceProduct.getBand(srcBandNames[1]);
            sourceRaster1 = getSourceTile(sourceBand1, targetTileRectangle);
            final Tile sourceRaster2 = getSourceTile(sourceBand2, targetTileRectangle);
            srcData1 = sourceRaster1.getDataBuffer();
            srcData2 = sourceRaster2.getDataBuffer();
        }

        final Unit.UnitType bandUnit = Unit.getUnitType(sourceBand1);
        final ProductData trgData = targetTile.getDataBuffer();
        final TileIndex srcIndex = new TileIndex(sourceRaster1);
        final TileIndex tgtIndex = new TileIndex(targetTile);
        final int maxY = y0 + h;
        final int maxX = x0 + w;

        double dn, dn2, i, q;
        int srcIdx, tgtIdx;
        for (int y = y0; y < maxY; ++y) {
            srcIndex.calculateStride(y);
            tgtIndex.calculateStride(y);
            final double[] lut = new double[w];
            computeTileScaledNoiseLUT(y, x0, y0, w, noiseInfo, calInfo, targetBandName, lut);

            for (int x = x0; x < maxX; ++x) {
                final int xx = x - x0;
                srcIdx = srcIndex.getIndex(x);
                tgtIdx = tgtIndex.getIndex(x);
                if (bandUnit == Unit.UnitType.AMPLITUDE) {
                    dn = srcData1.getElemDoubleAt(srcIdx);
                    dn2 = dn * dn;
                } else if (bandUnit == Unit.UnitType.REAL || bandUnit == Unit.UnitType.IMAGINARY) {
                    i = srcData1.getElemDoubleAt(srcIdx);
                    q = srcData2.getElemDoubleAt(srcIdx);
                    dn2 = i * i + q * q;
                } else if (bandUnit == Unit.UnitType.INTENSITY) {
                    dn2 = srcData1.getElemDoubleAt(srcIdx);
                } else {
                    throw new OperatorException("Unhandled unit");
                }

                trgData.setElemDoubleAt(tgtIdx, dn2 - lut[xx]);
            }
        }
    }

    /**
     * Get thermal noise information for given target band.
     *
     * @param targetBandName Target band name.
     * @return The ThermalNoiseInfo object.
     */
    private ThermalNoiseInfo getNoiseInfo(final String targetBandName) {

        for (ThermalNoiseInfo noiseInfo : noise) {
            final String pol = noiseInfo.polarization;
            final String ss = noiseInfo.subSwath;
            if (isSLC) {
                if (targetBandName.contains(pol) && targetBandName.contains(ss)) {
                    return noiseInfo;
                }
            } else {
                if (targetBandName.contains(pol)) {
                    return noiseInfo;
                }
            }
        }
        return null;
    }

    /**
     * Get calibration information for given target band.
     *
     * @param targetBandName Target band name.
     * @return The CalibrationInfo object.
     */
    private Sentinel1Calibrator.CalibrationInfo getCalInfo(final String targetBandName) {

        for (Sentinel1Calibrator.CalibrationInfo cal : calibration) {
            final String pol = cal.polarization;
            final String ss = cal.subSwath;
            if (isSLC) {
                if (targetBandName.contains(pol) && targetBandName.contains(ss)) {
                    return cal;
                }
            } else {
                if (targetBandName.contains(pol)) {
                    return cal;
                }
            }
        }
        return null;
    }

    /**
     * Compute scaled noise LUTs for the given range line.
     *
     * @param y              Index of the given range line.
     * @param x0             X coordinate of the upper left corner pixel of the given tile.
     * @param y0             Y coordinate of the upper left corner pixel of the given tile.
     * @param w              Tile width.
     * @param noiseInfo      Object of ThermalNoiseInfo class.
     * @param calInfo        Object of CalibrationInfo class.
     * @param targetBandName Target band name.
     * @param lut            The scaled noise LUT.
     */
    private void computeTileScaledNoiseLUT(final int y, final int x0, final int y0, final int w,
                                           final ThermalNoiseInfo noiseInfo,
                                           final Sentinel1Calibrator.CalibrationInfo calInfo,
                                           final String targetBandName,
                                           final double[] lut) {

        if (!absoluteCalibrationPerformed) {

            computeTileNoiseLUT(y, x0, y0, w, noiseInfo, lut);

        } else {

            final double[] noiseLut = new double[w];
            computeTileNoiseLUT(y, x0, y0, w, noiseInfo, noiseLut);

            final double[] calLut = new double[w];
            Sentinel1Calibrator.computeTileCalibrationLUTs(y, x0, y0, w, calInfo, targetBandName, calLut);

            if (removeThermalNoise) {
                for (int i = 0; i < w; i++) {
                    lut[i] = noiseLut[i] / calLut[i];
                }
            } else { // reIntroduceThermalNoise
                for (int i = 0; i < w; i++) {
                    lut[i] = -noiseLut[i] / calLut[i];
                }
            }
        }
    }

    /**
     * Compute noise LUTs for the given range line.
     *
     * @param y         Index of the given range line.
     * @param x0        X coordinate of the upper left corner pixel of the given tile.
     * @param y0        Y coordinate of the upper left corner pixel of the given tile.
     * @param w         Tile width.
     * @param noiseInfo Object of ThermalNoiseInfo class.
     * @param lut       The noise LUT.
     */
    private void computeTileNoiseLUT(final int y, final int x0, final int y0, final int w,
                                     final ThermalNoiseInfo noiseInfo, final double[] lut) {

        final double lineTimeInterval = (noiseInfo.lastLineTime - noiseInfo.firstLineTime) / (noiseInfo.numOfLines - 1);

        double v00, v01, v10, v11, muX, muY;
        int noiseVecIdx = getNoiseVectorIndex(y0, noiseInfo);
        int pixelIdx = getPixelIndex(x0, noiseVecIdx, noiseInfo);

        final double azTime = noiseInfo.firstLineTime + y * lineTimeInterval;
        final double azT0 = noiseInfo.noiseVectorList[noiseVecIdx].timeMJD;
        final double azT1 = noiseInfo.noiseVectorList[noiseVecIdx + 1].timeMJD;
        muY = (azTime - azT0) / (azT1 - azT0);

        for (int x = x0; x < x0 + w; x++) {

            if (x > noiseInfo.noiseVectorList[noiseVecIdx].pixels[pixelIdx + 1]) {
                pixelIdx++;
            }

            final int xx0 = noiseInfo.noiseVectorList[noiseVecIdx].pixels[pixelIdx];
            final int xx1 = noiseInfo.noiseVectorList[noiseVecIdx].pixels[pixelIdx + 1];
            muX = (double) (x - xx0) / (double) (xx1 - xx0);

            v00 = noiseInfo.noiseVectorList[noiseVecIdx].noiseLUT[pixelIdx];
            v01 = noiseInfo.noiseVectorList[noiseVecIdx].noiseLUT[pixelIdx + 1];
            v10 = noiseInfo.noiseVectorList[noiseVecIdx + 1].noiseLUT[pixelIdx];
            v11 = noiseInfo.noiseVectorList[noiseVecIdx + 1].noiseLUT[pixelIdx + 1];

            lut[x - x0] = org.esa.nest.util.MathUtils.interpolationBiLinear(v00, v01, v10, v11, muX, muY);
        }
    }

    /**
     * Get index of the noise vector in the list for a given line.
     *
     * @param y         Line coordinate.
     * @param noiseInfo Object of ThermalNoiseInfo class.
     * @return The noise vector index.
     */
    private static int getNoiseVectorIndex(final int y, final ThermalNoiseInfo noiseInfo) {

        for (int i = 0; i < noiseInfo.count; i++) {
            if (y < noiseInfo.noiseVectorList[i].line) {
                return i - 1;
            }
        }
        return -1;
    }

    /**
     * Get pixel index in a given noise vector for a given pixel.
     *
     * @param x           Pixel coordinate.
     * @param noiseVecIdx Noise vector index.
     * @param noiseInfo   Object of ThermalNoiseInfo class.
     * @return The pixel index.
     */
    private static int getPixelIndex(final int x, final int noiseVecIdx, final ThermalNoiseInfo noiseInfo) {

        for (int i = 0; i < noiseInfo.noiseVectorList[noiseVecIdx].pixels.length; i++) {
            if (x < noiseInfo.noiseVectorList[noiseVecIdx].pixels[i]) {
                return i - 1;
            }
        }
        return -1;
    }


    public static class ThermalNoiseInfo {
        public String subSwath;
        public String polarization;
        public double firstLineTime;
        public double lastLineTime;
        public int numOfLines;
        public int count; // number of noiseVector records within the list
        public Sentinel1Utils.NoiseVector[] noiseVectorList;
    }


    /**
     * The SPI is used to register this operator in the graph processing framework
     * via the SPI configuration file
     * {@code META-INF/services/org.esa.beam.framework.gpf.OperatorSpi}.
     * This class may also serve as a factory for new operator instances.
     *
     * @see org.esa.beam.framework.gpf.OperatorSpi#createOperator()
     * @see org.esa.beam.framework.gpf.OperatorSpi#createOperator(java.util.Map, java.util.Map)
     */
    public static class Spi extends OperatorSpi {
        public Spi() {
            super(Sentinel1RemoveThermalNoiseOp.class);
            super.setOperatorUI(Sentinel1RemoveThermalNoiseOpUI.class);
        }
    }
}