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
package org.esa.s1tbx.calibration.gpf;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.s1tbx.calibration.gpf.calibrators.Sentinel1Calibrator;
import org.esa.s1tbx.commons.Sentinel1Utils;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.MetadataElement;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.datamodel.VirtualBand;
import org.esa.snap.core.gpf.Operator;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.Tile;
import org.esa.snap.core.gpf.annotations.OperatorMetadata;
import org.esa.snap.core.gpf.annotations.Parameter;
import org.esa.snap.core.gpf.annotations.SourceProduct;
import org.esa.snap.core.gpf.annotations.TargetProduct;
import org.esa.snap.core.util.ProductUtils;
import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.engine_utilities.datamodel.AbstractMetadata;
import org.esa.snap.engine_utilities.datamodel.Unit;
import org.esa.snap.engine_utilities.eo.Constants;
import org.esa.snap.engine_utilities.gpf.InputProductValidator;
import org.esa.snap.engine_utilities.gpf.OperatorUtils;
import org.esa.snap.engine_utilities.gpf.TileIndex;
import org.esa.snap.engine_utilities.util.Maths;

import java.awt.Rectangle;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

/**
 * Apply thermal noise correction to Sentinel-1 Level-1 products.
 */
@OperatorMetadata(alias = "ThermalNoiseRemoval",
        category = "Radar/Radiometric",
        authors = "Cecilia Wong, Jun Lu, Luis Veci",
        copyright = "Copyright (C) 2014 by Array Systems Computing Inc.",
        version = "1.0",
        description = "Removes thermal noise from products")
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
    private boolean absoluteCalibrationPerformed = false;
    private boolean isComplex = false;
    private boolean inputSigmaBand = false;
    private boolean inputBetaBand = false;
    private boolean inputGammaBand = false;
    private boolean inputDNBand = false;
    private boolean isTOPSARSLC = false;
    private String productType = null;
    private int numOfSubSwath = 1;
    private ThermalNoiseInfo[] noise = null;
    private Sentinel1Calibrator.CalibrationInfo[] calibration = null;
    private List<String> selectedPolList = null;
    private final HashMap<String, String[]> targetBandNameToSourceBandName = new HashMap<>(2);

    // For after IPF 2.9.0 ...
    private double version = 0.0f;
    private boolean isTOPS = false;
    private boolean isGRD = false;

    public static float trgFloorValue = 0.01234567890000f;

    private static final String PRODUCT_SUFFIX = "_NR";

    private static class TimeMaps {
        private final HashMap<String, Double> t0Map = new HashMap<>();
        private final HashMap<String, Double> deltaTsMap = new HashMap<>();
        private final HashMap<String, double[]> swathStartEndTimesMap = new HashMap<>();
    }

    /**
     * Default constructor. The graph processing framework
     * requires that an operator has a default constructor.
     */
    public Sentinel1RemoveThermalNoiseOp() {
    }

    /**
     * Initializes this operator and sets the one and only target product.
     * <p>The target product can be either defined by a field of type {@link Product} annotated with the
     * {@link TargetProduct TargetProduct} annotation or
     * by calling {@link #setTargetProduct} method.</p>
     * <p>The framework calls this method after it has created this operator.
     * Any client code that must be performed before computation of tile data
     * should be placed here.</p>
     *
     * @throws OperatorException If an error occurs during operator initialisation.
     * @see #getTargetProduct()
     */
    @Override
    public void initialize() throws OperatorException {

        try {
            final InputProductValidator validator = new InputProductValidator(sourceProduct);
            validator.checkIfSentinel1Product();
            validator.checkAcquisitionMode(new String[] {"IW","EW","SM"});
            validator.checkProductType(new String[] {"SLC","GRD"});

            absRoot = AbstractMetadata.getAbstractedMetadata(sourceProduct);
            origMetadataRoot = AbstractMetadata.getOriginalProductMetadata(sourceProduct);

            getIPFVersion();

            getProductType();

            getAcquisitionMode();

            getThermalNoiseCorrectionFlag();

            setSelectedPolarisations();

            if (version < 2.9 || !isTOPS) { // SM SLC/GRD do not have noise azimuth vectors
                noise = getThermalNoiseVectors(origMetadataRoot, selectedPolList, numOfSubSwath);
            }

            getSampleType();

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
     * Get product type from abstracted metadata.
     */
    private void getProductType() {
        productType = absRoot.getAttributeString(AbstractMetadata.PRODUCT_TYPE);
        String mode = absRoot.getAttributeString(AbstractMetadata.ACQUISITION_MODE);

        isTOPSARSLC = productType.contains("SLC") && (mode.contains("IW") || mode.contains("EW"));

        final MetadataElement annotationElem = origMetadataRoot.getElement("annotation");
        final MetadataElement[] annotationDataSetListElem = annotationElem.getElements();
        final String imageName = annotationDataSetListElem[0].getName();
        isTOPS = mode.contains("IW") || mode.contains("EW");
        isGRD = productType.contains("GRD");
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
     * Get thermal noise correction flag from the original product metadata.
     */
    private void getThermalNoiseCorrectionFlag() {

        final MetadataElement annotationElem = origMetadataRoot.getElement("annotation");
        final MetadataElement[] annotationDataSetListElem = annotationElem.getElements();
        final MetadataElement productElem = annotationDataSetListElem[0].getElement("product");
        final MetadataElement imageAnnotationElem = productElem.getElement("imageAnnotation");
        final MetadataElement processingInformationElem = imageAnnotationElem.getElement("processingInformation");

        Boolean thermalNoiseCorrectionPerformed = Boolean.parseBoolean(
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
    public static ThermalNoiseInfo[] getThermalNoiseVectors(final MetadataElement origMetadataRoot,
                                                            final List<String> selectedPolList,
                                                            final int numOfSubSwath) throws IOException {

        final ThermalNoiseInfo[] noise = new ThermalNoiseInfo[numOfSubSwath * selectedPolList.size()];
        if(origMetadataRoot == null) {
            throw new IOException("Unable to find original product metadata");
        }
        final MetadataElement noiseElem = origMetadataRoot.getElement("noise");
        if(noiseElem == null) {
            throw new IOException("Unable to find noise element in original product metadata");
        }
        final MetadataElement[] noiseDataSetListElem = noiseElem.getElements();

        int dataSetIndex = 0;
        for (MetadataElement dataSetListElem : noiseDataSetListElem) {

            final MetadataElement noiElem = dataSetListElem.getElement("noise");
            final MetadataElement adsHeaderElem = noiElem.getElement("adsHeader");
            final String pol = adsHeaderElem.getAttributeString("polarisation");
            if (!selectedPolList.contains(pol)) {
                continue;
            }

            MetadataElement noiseVectorListElem = noiElem.getElement("noiseVectorList");
            // Called by S1CalibrationTPGAction
            if (noiseVectorListElem == null) {
                noiseVectorListElem = noiElem.getElement("noiseRangeVectorList");
            }
            final String subSwath = adsHeaderElem.getAttributeString("swath");

            noise[dataSetIndex] = new ThermalNoiseInfo(pol, subSwath,
                    Sentinel1Utils.getTime(adsHeaderElem, "startTime").getMJD(),
                    Sentinel1Utils.getTime(adsHeaderElem, "stopTime").getMJD(),
                    Sentinel1Calibrator.getNumOfLines(origMetadataRoot, pol, subSwath),
                    Integer.parseInt(noiseVectorListElem.getAttributeString("count")),
                    Sentinel1Utils.getNoiseVector(noiseVectorListElem));

            dataSetIndex++;
        }

        return noise;
    }

    private void getSampleType() {
        final String sampleType = absRoot.getAttributeString(AbstractMetadata.SAMPLE_TYPE);
        if (sampleType.equals("COMPLEX")) {
            isComplex = true;
        }
    }

    /**
     * Get absolute calibration flag from the abstracted metadata.
     */
    private void getCalibrationFlag() {
        absoluteCalibrationPerformed =
                absRoot.getAttribute(AbstractMetadata.abs_calibration_flag).getData().getElemBoolean();

        if (absoluteCalibrationPerformed) {
            if (isComplex) {
                // Currently the calibrated complex product can only be sigma0, this should be changed later if
                // complex beta0 and gamma0 are available
                inputSigmaBand = true;
            } else {
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

                if (!inputSigmaBand && !inputGammaBand && !inputBetaBand && !inputDNBand) {
                    throw new OperatorException("For calibrated product, Sigma0 or Gamma0 or Beta0 or DN band is expected");
                }
            }
        }
    }

    /**
     * Get calibration vectors from the original product metadata.
     */
    private void getCalibrationVectors() throws IOException {

        calibration = Sentinel1Calibrator.getCalibrationVectors(
                sourceProduct,
                selectedPolList,
                inputSigmaBand,
                inputBetaBand,
                inputGammaBand,
                inputDNBand);
    }

    /**
     * Set user selected polarisations.
     */
    private void setSelectedPolarisations() {

        String[] selectedPols = selectedPolarisations;
        if (selectedPols == null || selectedPols.length == 0) {
            selectedPols = Sentinel1Utils.getProductPolarizations(absRoot);
        }
        selectedPolList = Arrays.asList(selectedPols);
    }

    /**
     * Create a target product for output.
     */
    private void createTargetProduct() {

        targetProduct = new Product(sourceProduct.getName() + PRODUCT_SUFFIX,
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
            if (srcBand instanceof VirtualBand) {
                continue;
            }

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
                        srcBand.getRasterWidth(),
                        srcBand.getRasterHeight());

                targetBand.setUnit(Unit.INTENSITY);
                targetBand.setDescription(srcBand.getDescription());
                targetBand.setNoDataValue(srcBand.getNoDataValue());
                targetBand.setNoDataValueUsed(true);
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

        final String pol = sourceBandName.substring(sourceBandName.indexOf('_'));

        if (absoluteCalibrationPerformed) {
            if (isComplex) {
                return "Sigma0" + pol;
            } else {
                return sourceBandName;
            }
        }

        return "Intensity" + pol;
    }

    /**
     * Update target product metadata.
     */
    private void updateTargetProductMetadata() {

        final MetadataElement abs = AbstractMetadata.getAbstractedMetadata(targetProduct);
        final String[] targetBandNames = targetProduct.getBandNames();
        Sentinel1Utils.updateBandNames(abs, selectedPolList, targetBandNames);

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

    private double[][] populateNoiseAzimuthBlock(
            final int x0, final int y0, final int w, final int h, final String targetBandName) {

        if (version >= 2.9) {
            final TimeMaps timeMaps = new TimeMaps();

            if (isGRD) {
                return buildNoiseLUTForTOPSGRD(x0, y0, w, h, targetBandName, timeMaps);
            } else if (isTOPSARSLC) {
                return buildNoiseLUTForTOPSSLC(x0, y0, w, h, targetBandName, timeMaps);
            }
        }
        return null;
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
    @Override
    public void computeTile(Band targetBand, Tile targetTile, ProgressMonitor pm) throws OperatorException {

        final Rectangle targetTileRectangle = targetTile.getRectangle();
        final int x0 = targetTileRectangle.x;
        final int y0 = targetTileRectangle.y;
        final int w = targetTileRectangle.width;
        final int h = targetTileRectangle.height;
        //System.out.println("x0 = " + x0 + ", y0 = " + y0 + ", w = " + w + ", h = " + h + ", target band = " + targetBand.getName());

        try {
            final String targetBandName = targetBand.getName();

            double[][] noiseBlock = null;
            if (version >= 2.9) {
                noiseBlock = populateNoiseAzimuthBlock(x0, y0, w, h, targetBandName);
            }

            Tile sourceRaster1 = null;
            ProductData srcData1 = null;
            ProductData srcData2 = null;
            Band sourceBand1 = null;

            final String[] srcBandNames = targetBandNameToSourceBandName.get(targetBandName);
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

            final double srcNoDataValue = sourceBand1.getNoDataValue();
            final Unit.UnitType bandUnit = Unit.getUnitType(sourceBand1);
            final ProductData trgData = targetTile.getDataBuffer();
            final TileIndex srcIndex = new TileIndex(sourceRaster1);
            final TileIndex tgtIndex = new TileIndex(targetTile);
            final int maxY = y0 + h;
            final int maxX = x0 + w;

            final boolean complexData = bandUnit == Unit.UnitType.REAL || bandUnit == Unit.UnitType.IMAGINARY;

            String key = "";
            if (version >= 2.9 && isTOPS) {
                if (complexData) { // SLC
                    key = targetBandName.substring(10).toLowerCase();
                } else { // GRD
                    key = getBandPol(targetBandName);
                }
            }

            Sentinel1Calibrator.CalibrationInfo calInfo = null;
            Sentinel1Calibrator.CALTYPE calType = null;
            if (absoluteCalibrationPerformed) {
                calInfo = getCalInfo(targetBandName);
                calType = Sentinel1Calibrator.getCalibrationType(targetBandName);
            }

            double dn, dn2, i, q;
            int srcIdx, tgtIdx;
            for (int y = y0; y < maxY; ++y) {
                srcIndex.calculateStride(y);
                tgtIndex.calculateStride(y);

                double[] lut = new double[w];
                if (absoluteCalibrationPerformed) {
                    final int calVecIdx = calInfo.getCalibrationVectorIndex(y);
                    final Sentinel1Utils.CalibrationVector vec0 = calInfo.getCalibrationVector(calVecIdx);
                    final Sentinel1Utils.CalibrationVector vec1 = calInfo.getCalibrationVector(calVecIdx + 1);
                    final float[] vec0LUT = Sentinel1Calibrator.getVector(calType, vec0);
                    final float[] vec1LUT = Sentinel1Calibrator.getVector(calType, vec1);
                    final Sentinel1Utils.CalibrationVector calVec = calInfo.calibrationVectorList[calVecIdx];
                    final int pixelIdx0 = calVec.getPixelIndex(x0);

                    if (version < 2.9) {
                        final ThermalNoiseInfo noiseInfo = getNoiseInfo(targetBandName);
                        computeTileScaledNoiseLUT(y, x0, w, noiseInfo, calInfo, vec0.timeMJD, vec1.timeMJD,
                                vec0LUT, vec1LUT, vec0.pixels, pixelIdx0, lut);
                    } else {
                        computeTileScaledNoiseLUT(y, x0, y0, w, noiseBlock, calInfo, vec0.timeMJD, vec1.timeMJD,
                                vec0LUT, vec1LUT, vec0.pixels, pixelIdx0, lut);
                    }

                } else {
                    if (version < 2.9) {
                        final ThermalNoiseInfo noiseInfo = getNoiseInfo(targetBandName);
                        computeTileNoiseLUT(y, x0, w, noiseInfo, lut);
                    } else {
                        computeTileNoiseLUT(y - y0, x0, w, noiseBlock, lut);
                    }
                }

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
                    } else if (bandUnit == Unit.UnitType.INTENSITY) {
                        dn2 = srcData1.getElemDoubleAt(srcIdx);
                    } else {
                        throw new OperatorException("Unhandled unit");
                    }

                    if(dn2 == srcNoDataValue) {
                        trgData.setElemDoubleAt(tgtIdx, srcNoDataValue);
                        continue;
                    }

                    double value = dn2 - lut[xx];
                    if(value < 0) {
                        //value = dn2;       // small intensity value; if too small, calibration will make it nodatavalue

                        // Eq-1 in Section 6 of MPC-0392 DI-MPC-TN Issue 1.1 2017,Nov.28 "Thermal Denoising of Products Generated by the S-1 IPF"
                        value = (dn2 == 0.0?trgFloorValue:dn2);
                    }
                    trgData.setElemDoubleAt(tgtIdx, value);
                }
            }
        } catch (Throwable e) {
            throw new OperatorException(e.getMessage());
        }
    }

    /**
     * Get thermal noise information for given target band.
     *
     * @param targetBandName Target band name.
     * @return The ThermalNoiseInfo object.
     */
    private ThermalNoiseInfo getNoiseInfo(final String targetBandName) throws OperatorException {

        for (ThermalNoiseInfo noiseInfo : noise) {
            if (isTOPSARSLC) {
                if (targetBandName.contains(noiseInfo.polarization) && targetBandName.contains(noiseInfo.subSwath)) {
                    return noiseInfo;
                }
            } else {
                if (targetBandName.contains(noiseInfo.polarization)) {
                    return noiseInfo;
                }
            }
        }
        throw new OperatorException("NoiseInfo not found for "+targetBandName);
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
            if (isTOPSARSLC) {
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
     * @param y         Index of the given range line.
     * @param x0        X coordinate of the upper left corner pixel of the given tile.
     * @param w         Tile width.
     * @param noiseInfo Object of ThermalNoiseInfo class.
     * @param calInfo   Object of CalibrationInfo class.
     * @param lut       The scaled noise LUT.
     */
    private void computeTileScaledNoiseLUT(final int y, final int x0, final int w,
                                           final ThermalNoiseInfo noiseInfo,
                                           final Sentinel1Calibrator.CalibrationInfo calInfo,
                                           final double azT0, final double azT1,
                                           final float[] vec0LUT, final float[] vec1LUT,
                                           final int[] vec0Pixels, final int pixelIdx0,
                                           final double[] lut) {

        final double[] noiseLut = new double[w];
        computeTileNoiseLUT(y, x0, w, noiseInfo, noiseLut);

        final double[] calLut = new double[w];
        computeTileCalibrationLUTs(y, x0, w, calInfo, azT0, azT1,
                vec0LUT, vec1LUT, vec0Pixels, pixelIdx0, calLut);

        if (removeThermalNoise) {
            for (int i = 0; i < w; i++) {
                lut[i] = noiseLut[i] / (calLut[i]*calLut[i]);
            }
        } else { // reIntroduceThermalNoise
            for (int i = 0; i < w; i++) {
                lut[i] = -noiseLut[i] / (calLut[i]*calLut[i]);
            }
        }
    }

    private void computeTileScaledNoiseLUT(final int y, final int x0, final int y0, final int w,
                                           final double[][] noiseBlock,
                                           final Sentinel1Calibrator.CalibrationInfo calInfo,
                                           final double azT0, final double azT1,
                                           final float[] vec0LUT, final float[] vec1LUT,
                                           final int[] vec0Pixels, final int pixelIdx0,
                                           final double[] lut) {

        final double[] calLut = new double[w];
        computeTileCalibrationLUTs(y, x0, w, calInfo, azT0, azT1,
                vec0LUT, vec1LUT, vec0Pixels, pixelIdx0, calLut);

        final int yy = y - y0;
        if (removeThermalNoise) {
            for (int i = 0; i < w; i++) {
                lut[i] = noiseBlock[yy][i] / (calLut[i]*calLut[i]);
            }
        } else { // reIntroduceThermalNoise
            for (int i = 0; i < w; i++) {
                lut[i] = -noiseBlock[yy][i] / (calLut[i]*calLut[i]);
            }
        }
    }

    /**
     * Compute calibration LUTs for the given range line.
     *
     * @param y       Index of the given range line.
     * @param x0      X coordinate of the upper left corner pixel of the given tile.
     * @param w       Tile width.
     * @param calInfo Object of CalibrationInfo class.
     * @param lut     LUT for calibration.
     */
    private static void computeTileCalibrationLUTs(final int y, final int x0, final int w,
                                                  final Sentinel1Calibrator.CalibrationInfo calInfo,
                                                  final double azT0, final double azT1,
                                                  final float[] vec0LUT, final float[] vec1LUT,
                                                  final int[] vec0Pixels, int pixelIdx0,
                                                  final double[] lut) {
        final double azTime = calInfo.firstLineTime + y * calInfo.lineTimeInterval;
        double muX, muY = (azTime - azT0) / (azT1 - azT0);

        int pixelIdx = pixelIdx0;
        final int maxX = x0 + w;
        for (int x = x0; x < maxX; x++) {
            if (x > vec0Pixels[pixelIdx + 1]) {
                pixelIdx++;
            }

            muX = (double) (x - vec0Pixels[pixelIdx]) / (double) (vec0Pixels[pixelIdx + 1] - vec0Pixels[pixelIdx]);
            lut[x - x0] = Maths.interpolationBiLinear(
                    vec0LUT[pixelIdx], vec0LUT[pixelIdx + 1], vec1LUT[pixelIdx], vec1LUT[pixelIdx + 1], muX, muY);
        }
    }

    /**
     * Compute noise LUTs for the given range line.
     *
     * @param y         Index of the given range line.
     * @param x0        X coordinate of the upper left corner pixel of the given tile.
     * @param w         Tile width.
     * @param noiseInfo Object of ThermalNoiseInfo class.
     * @param lut       The noise LUT.
     */
    private static void computeTileNoiseLUT(final int y, final int x0, final int w,
                                     final ThermalNoiseInfo noiseInfo, final double[] lut) {
        try {
            final int noiseVecIdx = getNoiseVectorIndex(y, noiseInfo);
            final Sentinel1Utils.NoiseVector noiseVector0 = noiseInfo.noiseVectorList[noiseVecIdx];
            final Sentinel1Utils.NoiseVector noiseVector1 = noiseInfo.noiseVectorList[noiseVecIdx + 1];

            final double azTime = noiseInfo.firstLineTime + y * noiseInfo.lineTimeInterval;
            final double azT0 = noiseVector0.timeMJD;
            final double azT1 = noiseVector1.timeMJD;
            final double muY = (azTime - azT0) / (azT1 - azT0);

            int pixelIdx0 = getPixelIndex(x0, noiseVector0);
            int pixelIdx1 = getPixelIndex(x0, noiseVector1);

            final int maxLength0 = noiseVector0.pixels.length - 2;
            final int maxLength1 = noiseVector1.pixels.length - 2;
            final int maxX = x0 + w;
            for (int x = x0; x < maxX; x++) {

                if (x > noiseVector0.pixels[pixelIdx0 + 1] && pixelIdx0 < maxLength0) {
                    pixelIdx0++;
                }
                final int x00 = noiseVector0.pixels[pixelIdx0];
                final int x01 = noiseVector0.pixels[pixelIdx0 + 1];
                final double muX0 = (double) (x - x00) / (double) (x01 - x00);
                final double noise0 = Maths.interpolationLinear(
                        noiseVector0.noiseLUT[pixelIdx0], noiseVector0.noiseLUT[pixelIdx0 + 1], muX0);

                if (x > noiseVector1.pixels[pixelIdx1 + 1] && pixelIdx1 < maxLength1) {
                    pixelIdx1++;
                }
                final int x10 = noiseVector1.pixels[pixelIdx1];
                final int x11 = noiseVector1.pixels[pixelIdx1 + 1];
                final double muX1 = (double) (x - x10) / (double) (x11 - x10);
                final double noise1 = Maths.interpolationLinear(
                        noiseVector1.noiseLUT[pixelIdx1], noiseVector1.noiseLUT[pixelIdx1 + 1], muX1);

                lut[x - x0] = Maths.interpolationLinear(noise0, noise1, muY);
            }
        } catch (Throwable e) {
            OperatorUtils.catchOperatorException("computeTileNoiseLUT", e);
        }
    }

    private static void computeTileNoiseLUT(final int yy, final int x0, final int w,
                                            final double[][] noiseBlock, final double[] lut) {
        try {
            final int maxX = x0 + w;
            for (int x = x0; x < maxX; x++) {
                final int xx = x - x0;
                lut[xx] = noiseBlock[yy][xx];
            }
        } catch (Throwable e) {
            OperatorUtils.catchOperatorException("computeTileNoiseLUT", e);
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
        for (int i = 1; i < noiseInfo.count; i++) {
            if (y < noiseInfo.noiseVectorList[i].line) {
                return i - 1;
            }
        }
        return noiseInfo.count - 2;
    }

    /**
     * Get pixel index in a given noise vector for a given pixel.
     *
     * @param x           Pixel coordinate.
     * @param noiseVector Noise vector.
     * @return The pixel index.
     */
    private static int getPixelIndex(final int x, final Sentinel1Utils.NoiseVector noiseVector) {

        for (int i = 0; i < noiseVector.pixels.length; i++) {
            if (x < noiseVector.pixels[i]) {
                return i - 1;
            }
        }
        return noiseVector.pixels.length - 2;
    }

    private void getIPFVersion() {
        final String procSysId = absRoot.getAttributeString(AbstractMetadata.ProcessingSystemIdentifier);
        version = Double.valueOf(procSysId.substring(procSysId.lastIndexOf(" ")));
        //System.out.println("Sentinel1RemoveThermalNoiseOp: IPF version = " + version);
    }

    private double[][] buildNoiseLUTForTOPSSLC(
            final int x0, final int y0, final int w, final int h, final String targetBandName, final TimeMaps timeMaps) {

        final String targetBandPol = getBandPol(targetBandName).toLowerCase();
        final String targetBandSwath = getBandSwath(targetBandName).toLowerCase();
        final MetadataElement noiseElem = origMetadataRoot.getElement("noise");
        final MetadataElement[] noiseDataSetListElem = noiseElem.getElements();

        Sentinel1Utils.NoiseAzimuthVector[] noiseAzimuthVectors = null;
        Sentinel1Utils.NoiseVector[] noiseRangeVectors = null;

        for (MetadataElement dataSetListElem : noiseDataSetListElem) {

            final String imageName = dataSetListElem.getName();
            if (!imageName.toLowerCase().contains(targetBandPol) || !imageName.toLowerCase().contains(targetBandSwath)){
                continue;
            }

            getT0andDeltaTS(imageName, timeMaps);

            final MetadataElement noiElem = dataSetListElem.getElement("noise");

            MetadataElement noiseAzimuthVectorListElem = noiElem.getElement("noiseAzimuthVectorList");
            noiseAzimuthVectors = Sentinel1Utils.getAzimuthNoiseVector(noiseAzimuthVectorListElem);

            MetadataElement noiseRangeVectorListElem = noiElem.getElement("noiseRangeVectorList");
            noiseRangeVectors = Sentinel1Utils.getNoiseVector(noiseRangeVectorListElem);
        }

        final MetadataElement annotationElem = origMetadataRoot.getElement("annotation");
        final MetadataElement[] annotationDataSetListElem = annotationElem.getElements();
        final HashMap<String, Sentinel1Utils.NoiseVector> burstToRangeVectorMap = new HashMap<>();

        int linesPerBurst = 0;
        for (MetadataElement elem : annotationDataSetListElem) {
            final String imageName = elem.getName();
            if (!imageName.toLowerCase().contains(targetBandPol) || !imageName.toLowerCase().contains(targetBandSwath)){
                continue;
            }

            final MetadataElement productElem = elem.getElement("product");
            final MetadataElement swathTimingElem = productElem.getElement("swathTiming");

            linesPerBurst = swathTimingElem.getAttributeInt("linesPerBurst");
            //final int samplesPerBurst = swathTimingElem.getAttributeInt("samplesPerBurst");
            final MetadataElement burstListElem = swathTimingElem.getElement("burstList");
            final MetadataElement[] burstListArray = burstListElem.getElements();
            for (int i = 0; i < burstListArray.length; i++) {
                final int burstCenterLine = i*linesPerBurst + linesPerBurst/2;
                burstToRangeVectorMap.put("burst_" + i, getBurstRangeVector(burstCenterLine, noiseRangeVectors));
            }
        }

        // create noise matrix for the tile
        double[][] noiseMatrix = new double[h][w];
        populateNoiseMatrixForTOPSSLC(
                noiseAzimuthVectors[0], burstToRangeVectorMap, linesPerBurst, x0, y0, w, h, noiseMatrix);

        return noiseMatrix;
    }

    private Sentinel1Utils.NoiseVector getBurstRangeVector(
            final int burstCenterLine, final Sentinel1Utils.NoiseVector[] noiseRangeVectors) {

        int closest = 0;
        for (int j = 1; j < noiseRangeVectors.length; j++) {
            if (Math.abs(burstCenterLine - noiseRangeVectors[j].line) <
                    Math.abs(burstCenterLine - noiseRangeVectors[closest].line)) {
                closest = j;
            }
        }

        return noiseRangeVectors[closest];
    }

    private void populateNoiseMatrixForTOPSSLC(final Sentinel1Utils.NoiseAzimuthVector noiseAzimuthVector,
                                               final HashMap<String, Sentinel1Utils.NoiseVector> burstToRangeVectorMap,
                                               final int linesPerBurst,
                                               final int x0, final int y0, final int w, final int h,
                                               final double[][] noiseMatrix) {

        final int xMax = x0 + w - 1;
        final int yMax = y0 + h - 1;

        final double[] interpolatedAzimuthVector = new double[h];
        interpolNoiseAzimuthVector(noiseAzimuthVector, y0, yMax, interpolatedAzimuthVector);

        int currentNoiseVectorLine = Integer.MAX_VALUE;
        double[] interpolatedRangeVector = new double[w];

        for (int y = y0; y <= yMax; ++y) {
            final int yy = y - y0;
            final int burstIdx = y / linesPerBurst;
            final Sentinel1Utils.NoiseVector noiseRangeVector = burstToRangeVectorMap.get("burst_" + burstIdx);
            if (noiseRangeVector.line != currentNoiseVectorLine) {
                currentNoiseVectorLine = noiseRangeVector.line;
                interpolNoiseRangeVector(noiseRangeVector, x0, xMax, interpolatedRangeVector);
            }

            for (int x = x0; x <= xMax; ++x) {
                final int xx = x - x0;
                noiseMatrix[yy][xx] = interpolatedAzimuthVector[yy] * interpolatedRangeVector[xx];
            }
        }
    }

    private double[][] buildNoiseLUTForTOPSGRD(
            final int x0, final int y0, final int w, final int h, final String targetBandName, final TimeMaps timeMaps) {

        final int xMax = x0 + w - 1;
        final int yMax = y0 + h - 1;
        final String targetBandPol = getBandPol(targetBandName);
        final MetadataElement noiseElem = origMetadataRoot.getElement("noise");
        final MetadataElement[] noiseDataSetListElem = noiseElem.getElements();

        // loop through s1a-iw-grd-hh-..., s1a-iw-grd-hv-... (TOPS (IW and EW) GRD products)
        for (MetadataElement dataSetListElem : noiseDataSetListElem) {

            // imageName is s1a-iw-grd-hh-... or s1a-ew1-slc-hh-...
            final MetadataElement noiElem = dataSetListElem.getElement("noise");
            final MetadataElement adsHeaderElem = noiElem.getElement("adsHeader");
            final String pol = adsHeaderElem.getAttributeString("polarisation");
            if (!pol.equals(targetBandPol)) {
                continue;
            }

            final String imageName = dataSetListElem.getName();

            // get the noise azimuth vectors
            MetadataElement noiseAzimuthVectorListElem = noiElem.getElement("noiseAzimuthVectorList");

            final MetadataElement firstVector;
            if(noiseAzimuthVectorListElem.getNumElements() == 0) {
                firstVector = noiseAzimuthVectorListElem;
            } else {
                firstVector = noiseAzimuthVectorListElem.getElementAt(0);
            }

            if (firstVector.getAttributeString("slice", null) != null) {
                throw new OperatorException("Noise removal should be applied prior to slice assembly");
            }

            final Sentinel1Utils.NoiseAzimuthVector[] noiseAzimuthVectors =
                    Sentinel1Utils.getAzimuthNoiseVector(noiseAzimuthVectorListElem);

            // get the noise range vectors
            MetadataElement noiseRangeVectorListElem = noiElem.getElement("noiseRangeVectorList");
            final Sentinel1Utils.NoiseVector[] noiseRangeVectors =
                    Sentinel1Utils.getNoiseVector(noiseRangeVectorListElem);

            // create noise matrix for the tile
            double[][] noiseMatrix = new double[h][w];
            for (Sentinel1Utils.NoiseAzimuthVector noiseAzimuthVector : noiseAzimuthVectors) {
                final int nx0 = Math.max(x0, noiseAzimuthVector.firstRangeSample);
                final int nxMax = Math.min(xMax, noiseAzimuthVector.lastRangeSample);
                final int ny0 = Math.max(y0, noiseAzimuthVector.firstAzimuthLine);
                final int nyMax = Math.min(yMax, noiseAzimuthVector.lastAzimuthLine);

                if (nx0 >= nxMax || ny0 >= nyMax) {
                    continue;
                }

                populateNoiseMatrixForTOPSGRD(pol, imageName, noiseAzimuthVector, noiseRangeVectors,
                        x0, y0, nx0, nxMax, ny0, nyMax, noiseMatrix, timeMaps);
            }

            return noiseMatrix;
        }
        return null;
    }

    private void populateNoiseMatrixForTOPSGRD(final String pol, final String imageName,
                                     final Sentinel1Utils.NoiseAzimuthVector noiseAzimuthVector,
                                     final Sentinel1Utils.NoiseVector[] noiseRangeVectors,
                                     final int x0, final int y0,
                                     final int nx0, final int nxMax, final int ny0, final int nyMax,
                                     final double[][] noiseMatrix, final TimeMaps timeMaps) {

        getT0andDeltaTS(imageName, timeMaps);
        final double firstLineTime = timeMaps.t0Map.get(imageName);
        final double lineTimeInterval = timeMaps.deltaTsMap.get(imageName);
        final double startAzimTime = firstLineTime + noiseAzimuthVector.firstAzimuthLine * lineTimeInterval;
        final double endAzimTime = firstLineTime + noiseAzimuthVector.lastAzimuthLine * lineTimeInterval;
        final String swath = noiseAzimuthVector.swath;

        int[] noiseRangeVecIndices = getNoiseRangeVectorIndices(pol, swath, startAzimTime, endAzimTime,
                noiseRangeVectors, noiseAzimuthVector.firstAzimuthLine, noiseAzimuthVector.lastAzimuthLine,
                timeMaps);

        if (noiseRangeVecIndices != null && noiseRangeVecIndices.length > 0) {

            final double[][] interpolatedRangeVectors = new double[noiseRangeVecIndices.length][nxMax - nx0 + 1];
            int noiseRangeVectorLine[] = new int[noiseRangeVecIndices.length];
            for (int j = 0; j < noiseRangeVecIndices.length; j++) {

                noiseRangeVectorLine[j] = noiseRangeVectors[noiseRangeVecIndices[j]].line;

                interpolNoiseRangeVector(
                        noiseRangeVectors[noiseRangeVecIndices[j]], nx0, nxMax, interpolatedRangeVectors[j]);
            }

            final double[] interpolatedAzimuthVector = new double[nyMax - ny0 + 1];
            interpolNoiseAzimuthVector(noiseAzimuthVector, ny0, nyMax, interpolatedAzimuthVector);

            computeNoiseMatrix(x0, y0, nx0, nxMax, ny0, nyMax, noiseRangeVectorLine, interpolatedRangeVectors,
                    interpolatedAzimuthVector, noiseMatrix);

        } else {

            for (int y = ny0; y <= nyMax; y++) {
                for (int x = nx0; x <= nxMax; x++) {
                    noiseMatrix[y - y0][x - x0] = 1.0;
                }
            }
        }
    }

    private void interpolNoiseRangeVector(final Sentinel1Utils.NoiseVector noiseRangeVector,
                                          final int firstRangeSample, final int lastRangeSample,
                                          final double[] result) {
        /*
        System.out.println("interpolNoiseRangeVector called firstRangeSample = " + firstRangeSample
            + " lastRangeSample = " + lastRangeSample + " pixels = " + noiseRangeVector.pixels[0]
            + ", " + noiseRangeVector.pixels[noiseRangeVector.pixels.length-1]);
        */

        if (noiseRangeVector.pixels.length < 2) {  // should never happen
            SystemUtils.LOG.warning("######### noise range vector has length 1");
            for (int sample = 0; sample < result.length; sample++) {
                result[sample] = noiseRangeVector.pixels[0];
            }
        }  else {

            int i = 0;
            int sampleIdx = getSampleIndex(firstRangeSample, noiseRangeVector);
            /*
            System.out.println("interpolNoiseRangeVector: sampleIdx = " + sampleIdx
                + ": " + noiseRangeVector.pixels[sampleIdx] + " " + noiseRangeVector.pixels[sampleIdx+1]);
            */
            for (int sample = firstRangeSample; sample <= lastRangeSample; sample++) {
                //System.out.println("**** sample = " + sample);
                if (sample > noiseRangeVector.pixels[sampleIdx + 1]
                        && sampleIdx < noiseRangeVector.pixels.length - 2) {
                    sampleIdx++;
                }

                result[i++] = interpol(noiseRangeVector.pixels[sampleIdx],
                        noiseRangeVector.pixels[sampleIdx + 1],
                        noiseRangeVector.noiseLUT[sampleIdx],
                        noiseRangeVector.noiseLUT[sampleIdx + 1], sample);
            }
        }
    }

    private void interpolNoiseAzimuthVector(final Sentinel1Utils.NoiseAzimuthVector noiseAzimuthVector,
                                            final int firstAzimuthLine, final int lastAzimuthLine,
                                            final double[] interpNoiseAzimVec) {

        if (noiseAzimuthVector.lines.length < 2) { // This is possible
            for (int line = firstAzimuthLine; line <= lastAzimuthLine; line++)  {
                interpNoiseAzimVec[line - firstAzimuthLine] = noiseAzimuthVector.noiseAzimuthLUT[0];
            }
        }  else {
            int lineIdx = getLineIndex(firstAzimuthLine, noiseAzimuthVector.lines);
            for (int line = firstAzimuthLine; line <= lastAzimuthLine; line++) {

                if (line > noiseAzimuthVector.lines[lineIdx + 1] && lineIdx < noiseAzimuthVector.lines.length - 2) {
                    lineIdx++;
                }

                interpNoiseAzimVec[line - firstAzimuthLine] = interpol(
                        noiseAzimuthVector.lines[lineIdx],
                        noiseAzimuthVector.lines[lineIdx + 1],
                        noiseAzimuthVector.noiseAzimuthLUT[lineIdx],
                        noiseAzimuthVector.noiseAzimuthLUT[lineIdx + 1],
                        line);
            }
        }
    }

    private void computeNoiseMatrix(final int x0, final int y0,
                                    final int nx0, final int nxMax, final int ny0, final int nyMax,
                                    final int[] noiseRangeVectorLine, final double[][] interpolatedRangeVectors,
                                    final double[] interpolatedAzimuthVector, final double[][] noiseMatrix) {

        if (noiseRangeVectorLine.length == 1) {
            for (int x = nx0; x <= nxMax; x++) {
                final int xx = x - nx0;
                for (int y = ny0; y <= nyMax; y++) {
                    noiseMatrix[y - y0][x - x0] = interpolatedAzimuthVector[y - ny0] * interpolatedRangeVectors[0][xx];
                }
            }

        } else {

            final int line0Idx = getLineIndex(ny0, noiseRangeVectorLine);

            for (int x = nx0; x <= nxMax; x++) {
                final int xx = x - nx0;
                int lineIdx = line0Idx;

                for (int y = ny0; y <= nyMax; y++) {
                    if (y > noiseRangeVectorLine[lineIdx + 1] && lineIdx < noiseRangeVectorLine.length - 2) {
                        lineIdx++;
                    }

                    noiseMatrix[y - y0][x - x0] = interpolatedAzimuthVector[y - ny0] *
                            interpol(noiseRangeVectorLine[lineIdx], noiseRangeVectorLine[lineIdx + 1],
                            interpolatedRangeVectors[lineIdx][xx], interpolatedRangeVectors[lineIdx + 1][xx], y);
                }
            }
        }
    }

    private static double interpol(final int x1, final int x2, final double y1, final double y2, final int x) {

        if (x1 == x2) { // should never happen
            SystemUtils.LOG.warning("######### noise vector duplicate indices: x1 == x2  = " + x1);
            return 0;
        }

        return y1 + ((double)(x - x1)/(double)(x2 - x1))*(y2 - y1);
    }

    private int[] getNoiseRangeVectorIndices(final String pol, final String swath,
                                     final double startAzimTime, final double endAzimTime,
                                     final Sentinel1Utils.NoiseVector[] noiseRangeVectors,
                                     // for debugging...
                                     final int startAzimLine, final int endAzimLine, final TimeMaps timeMaps) {

        // Each noise range vector has an azimuth time (and corresponding azimuth line) associated with it.
        // We want to find the noise range vectors in "noiseRangeVectors" whose azimuth time lies with
        // the interval defined by [startAzimTime, endAzimTime].
        // If no such range vector exists, then find the one that lies within the swath (of the azimuth block)
        // start and end times and is closest to the centre of the azimuth block.
        // Noise range vector is not associated with a swath.

        //System.out.println("getNoiseRangeVectorIndices: called");
        final List<Integer> list = new ArrayList<>();

        for (int i = 0; i < noiseRangeVectors.length; i++) {
            final double azimTime = noiseRangeVectors[i].timeMJD;
            if (azimTime >= startAzimTime && azimTime <= endAzimTime) {
                list.add(i);
            }
        }
        //System.out.println("getNoiseRangeVectorIndices: list.size() = " + list.size());

        if (list.size() == 0) {
            int idx = -1;
            final double[] startEndTimes = new double[2];
            getSwathStartEndTimes(pol, swath, startEndTimes, timeMaps);
            /*
            System.out.println("getNoiseRangeVectorIndices: " + pol + " " + swath
                    + " startAzimLine = " + startAzimLine + " endAzimLine = " + endAzimLine
                    + " startAximTime = " + startAzimTime + " endAzimTime = " + endAzimTime
                    + " startSwathTime = " + startEndTimes[0] + " endSwathTime = " + startEndTimes[1]);
            */
            final double blockCentreTime = (startAzimTime + endAzimTime) / 2.0;
            for (int i = 0; i < noiseRangeVectors.length; i++) {
                final double azimTime = noiseRangeVectors[i].timeMJD;
                if (azimTime >= startEndTimes[0] && azimTime <= startEndTimes[1]) {
                    if (idx < 0) {
                        idx = i;
                    } else if (Math.abs(blockCentreTime - noiseRangeVectors[i].timeMJD) <
                            Math.abs(blockCentreTime - noiseRangeVectors[idx].timeMJD)) {
                        idx = i;
                    }
                }
            }
            if (idx < 0) {
                SystemUtils.LOG.warning("######### No valid range vector found for startAzimTime = " + startAzimTime + " endAzimTime = " + endAzimTime + " swath = " + swath);
                return null;
            } else {
                list.add(idx);
            }
        }

        int[] indices = new int[list.size()];
        for (int i = 0; i < indices.length; i++) {
            indices[i] = list.get(i);
        }

        return indices;
    }
//
//    private int[] getNoiseRangeVectorIndices(
//            final int y0, final int h, final Sentinel1Utils.NoiseVector[] noiseRangeVectors) {
//
//        final List<Integer> list = new ArrayList<>();
//
//        for (int i = 0; i < noiseRangeVectors.length; i++) {
//            final int line = noiseRangeVectors[i].line;
//            if (line >= y0 && line <= y0 + h) {
//                list.add(i);
//            }
//        }
//
//        if (list.size() == 0) {
//            final int midLine = y0 + (h - 1)/2;
//            int nearestVecIdx = -1;
//            int minDist = Integer.MAX_VALUE;
//            for (int i = 0; i < noiseRangeVectors.length; i++) {
//                final int line = noiseRangeVectors[i].line;
//                final int dist = Math.abs(line - midLine);
//                if (dist < minDist) {
//                    minDist = dist;
//                    nearestVecIdx = i;
//                }
//            }
//            list.add(nearestVecIdx);
//        }
//
//        int[] indices = new int[list.size()];
//        for (int i = 0; i < indices.length; i++) {
//            indices[i] = list.get(i);
//        }
//
//        return indices;
//    }

    private void getSwathStartEndTimes(final String pol, final String swath, final double[] startEndtimes,
                                       final TimeMaps timeMaps) {

        final String key = pol + "+" + swath;

        startEndtimes[0] = 0; // start time
        startEndtimes[1] = 0; // end time

        if (timeMaps.swathStartEndTimesMap.containsKey(key)) {
            double[] times = timeMaps.swathStartEndTimesMap.get(key);
            startEndtimes[0] = times[0];
            startEndtimes[1] = times[1];
            return;
        }

        final MetadataElement annotationElem = origMetadataRoot.getElement("annotation");
        final MetadataElement[] annotationDataSetListElem = annotationElem.getElements();

        for (MetadataElement elem : annotationDataSetListElem)  {
            final String imageName = elem.getName();
            if (imageName.toLowerCase().contains(pol.toLowerCase())) {
                //System.out.println("getSwathStartEndTimes: found " + pol);
                final MetadataElement productElem = elem.getElement("product");
                final MetadataElement swathMergingElem = productElem.getElement("swathMerging");
                final MetadataElement swathMergeListElem = swathMergingElem.getElement("swathMergeList");
                final MetadataElement[] swathMergeArray = swathMergeListElem.getElements();
                for (MetadataElement aSwathMergeArray : swathMergeArray) {
                    final String curSwath = aSwathMergeArray.getAttributeString("swath");
                    if (curSwath.equals(swath)) {
                        //System.out.println("getSwathStartEndTimes: found " + key);
                        MetadataElement swathBoundsListElem = aSwathMergeArray.getElement("swathBoundsList");
                        MetadataElement[] swathBoundList = swathBoundsListElem.getElements();
                        final int startLine = swathBoundList[0].getAttributeInt("firstAzimuthLine");
                        final int lastIdx = swathBoundList.length - 1;
                        final int endLine = swathBoundList[lastIdx].getAttributeInt("lastAzimuthLine");
                        if (timeMaps.t0Map.containsKey(imageName) && timeMaps.deltaTsMap.containsKey(imageName)) {
                            final double t0 = timeMaps.t0Map.get(imageName);
                            final double deltaTs = timeMaps.deltaTsMap.get(imageName);
                            startEndtimes[0] = t0 + (startLine * deltaTs);
                            startEndtimes[1] = t0 + (endLine * deltaTs);
                            timeMaps.swathStartEndTimesMap.put(key, startEndtimes);
                            /*
                            System.out.println("getSwathStartEndTimes: " + key + " -> [" + startEndtimes[0] + ", " + startEndtimes[1] + "]"
                                + " [" + startLine + ", " + endLine + "]");
                            */
                        } else {
                            SystemUtils.LOG.warning("######### fail to find swath start and end times for "
                                    + pol + " " + swath);
                        }
                        return;
                    }
                }
            }
        }
    }

    private static int getLineIndex(final int line, final int lines[]) {

        //  lines.length is assumed to be >= 2

        for (int i = 0; i < lines.length; i++) {
            if (line < lines[i]) {
                return (i > 0) ? i - 1 : 0;
            }
        }

        //System.out.println("getLineIndex: reach the end for line = " + line);
        return lines.length - 2;
    }

    private static int getSampleIndex(final int sample, final Sentinel1Utils.NoiseVector noiseRangeVector) {

        for (int i = 0; i < noiseRangeVector.pixels.length; i++) {
            if (sample < noiseRangeVector.pixels[i]) {
                return (i > 0) ? i - 1 : 0;
            }
        }

        return noiseRangeVector.pixels.length - 2;
    }

    private void getT0andDeltaTS(final String imageName, final TimeMaps timeMaps) {

        // imageName is something like s1a-iw-grd-hh-...

        final MetadataElement annotationElem = origMetadataRoot.getElement("annotation");
        final MetadataElement[] annotationDataSetListElem = annotationElem.getElements();

        for (MetadataElement dataSetListElem : annotationDataSetListElem) {
            if (dataSetListElem.getName().equals(imageName)){
                //System.out.println("getT0andDeltaTS: found " + imageName);

                MetadataElement productElem = dataSetListElem.getElement("product");
                MetadataElement imageAnnotationElem = productElem.getElement("imageAnnotation");
                MetadataElement imageInformationElem = imageAnnotationElem.getElement("imageInformation");

                double t01 = absRoot.getAttributeUTC(AbstractMetadata.first_line_time).getMJD(); // just for comparison
                double t0 = Sentinel1Utils.getTime(imageInformationElem ,"productFirstLineUtcTime").getMJD();
                timeMaps.t0Map.put(imageName, t0);

                double deltaTS1 = absRoot.getAttributeDouble(AbstractMetadata.line_time_interval) / Constants.secondsInDay; // just for comparison
                double deltaTS = imageInformationElem.getAttributeDouble("azimuthTimeInterval") / Constants.secondsInDay; // s to day
                timeMaps.deltaTsMap.put(imageName, deltaTS);

                //System.out.println("getT0andDeltaTS: " + imageName + ": t01 = " + t01 + " t0 = " + t0 + " deltaTS1 = " + deltaTS1 + " deltaTS = " + deltaTS);

                break;
            }
        }
    }

    private String getBandPol(final String bandName) {

        if (bandName.contains("HH")) {
            return "HH";
        } else if (bandName.contains("HV")) {
            return "HV";
        } else if (bandName.contains("VV")) {
            return "VV";
        } else if (bandName.contains("VH")) {
            return "VH";
        }
        return "";
    }

    private String getBandSwath(final String bandName) {

        if (bandName.contains("IW1")) {
            return "IW1";
        } else if (bandName.contains("IW2")) {
            return "IW2";
        } else if (bandName.contains("IW3")) {
            return "IW3";
        } else if (bandName.contains("EW1")) {
            return "EW1";
        } else if (bandName.contains("EW2")) {
            return "EW2";
        } else if (bandName.contains("EW3")) {
            return "EW3";
        } else if (bandName.contains("EW4")) {
            return "EW4";
        } else if (bandName.contains("EW5")) {
            return "EW5";
        }
        return "";
    }

    public static class ThermalNoiseInfo {
        public String polarization;
        public String subSwath;
        public double firstLineTime;
        public double lastLineTime;
        public int numOfLines;
        public int count; // number of noiseVector records within the list
        public Sentinel1Utils.NoiseVector[] noiseVectorList;

        final double lineTimeInterval;

        ThermalNoiseInfo(final String pol, final String subSwath, final double firstLineTime, final double lastLineTime,
                         final int numOfLines, final int count, final Sentinel1Utils.NoiseVector[] noiseVectorList) {
            this.polarization = pol;
            this.subSwath = subSwath;
            this.firstLineTime = firstLineTime;
            this.lastLineTime = lastLineTime;
            this.numOfLines = numOfLines;
            this.count = count;
            this.noiseVectorList = noiseVectorList;

            lineTimeInterval = (lastLineTime - firstLineTime) / (numOfLines - 1);
        }
    }

    private final static class NoiseAzimuthBlock {
        final int firstAzimuthLine;
        final int firstRangeSample;
        final int lastAzimuthLine;
        final int lastRangeSample;

        final double[][] noiseMatrix;

        NoiseAzimuthBlock(final int firstAzimuthLine, final int firstRangeSample,
                          final int lastAzimuthLine, final int lastRangeSample,
                          final double [][] noiseMatrix)
        {
            this.firstAzimuthLine = firstAzimuthLine;
            this.firstRangeSample = firstRangeSample;
            this.lastAzimuthLine = lastAzimuthLine;
            this.lastRangeSample = lastRangeSample;
            this.noiseMatrix =  noiseMatrix;
        }
    }

    private final class BurstBlock {

        final int linesPerBurst; // same for all bursts
        final int samplesPerBurst; // same for all bursts
        final double firstLineTime; // from azimuthTime

        final int firstValidSample[]; // length is samplesPerBurst
        final int lastValidSample[]; // length is samplesPerBurst

        final int firstLine;
        final int lastLine;

        final double[] rangeNoise; // length is samplePerBurst; rangeNoise[0] is x = 0 in the image
        final double[] azimuthNoise; // length is height of subswath image; azimuthNoize[0] is y = 0 in image

        BurstBlock(final int linesPerBurst, final int samplesPerBurst, final double firstLineTime,
                   final int firstValidSample[], final int lastValidSample[],
                   final int firstLine, final int lastLine, final double[] rangeNoise, final double[] azimuthNoise) {
            this.linesPerBurst = linesPerBurst;
            this.samplesPerBurst = samplesPerBurst;
            this.firstLineTime = firstLineTime;
            this.firstValidSample = firstValidSample;
            this.lastValidSample = lastValidSample;

            this.firstLine = firstLine;
            this.lastLine = lastLine;
            this.rangeNoise = rangeNoise;
            this.azimuthNoise = azimuthNoise;
        }
    }

    /**
     * The SPI is used to register this operator in the graph processing framework
     * via the SPI configuration file
     * {@code META-INF/services/org.esa.snap.core.gpf.OperatorSpi}.
     * This class may also serve as a factory for new operator instances.
     *
     * @see OperatorSpi#createOperator()
     * @see OperatorSpi#createOperator(java.util.Map, java.util.Map)
     */
    public static class Spi extends OperatorSpi {
        public Spi() {
            super(Sentinel1RemoveThermalNoiseOp.class);
        }
    }
}
