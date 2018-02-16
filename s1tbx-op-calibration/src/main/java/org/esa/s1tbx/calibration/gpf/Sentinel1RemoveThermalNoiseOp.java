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
import org.esa.s1tbx.insar.gpf.support.Sentinel1Utils;
import org.esa.snap.core.datamodel.*;
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

import java.awt.*;
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
    private boolean thermalNoiseCorrectionPerformed = false;
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
    private java.util.List<String> selectedPolList = null;
    private final HashMap<String, String[]> targetBandNameToSourceBandName = new HashMap<>(2);

    // For after IPF 2.9.0 ...
    private double version = 0.0f;
    private boolean isTOPS = false;
    private boolean isGRD = false;

    // The key is something like "s1a-iw-grd-hh-..."
    private HashMap<String, Double> t0Map = new HashMap<>();
    private HashMap<String, Double> deltaTsMap = new HashMap<>();

    // The key to these maps is pol, e.g. "HH"
    //private HashMap<String, Sentinel1Utils.NoiseAzimuthVector[] > noiseAzimuthVectorMap = new HashMap<>();
    private HashMap<String, NoiseAzimuthBlock[] > noiseAzimuthBlockMap = new HashMap<>();
    //private HashMap<String, Sentinel1Utils.NoiseVector[]> noiseRangeVectorMap = new HashMap<>();

    // key is pol+swath, e.g. "HH+IW1" or "HH+EW1"
    private HashMap<String, double[]> swathStartEndTimesMap = new HashMap<>();

    // Only for TOPS SLC. Key is something like "ew1_hh"
    private HashMap<String, BurstBlock[] > burstBlockMap = new HashMap<>();

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

            if (version >= 2.9) {
                if (isTOPS && isGRD) {
                    buildNoiseLUTForTOPSGRD();
                } else if (isTOPSARSLC) {
                    buildNoiseLUTForTOPSSLC();
                }
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

        System.out.println("Sentinel1RemoveThermalNoiseOp: productType = " + productType + " isTOPS = " + isTOPS + " isGRD = " + isGRD + " isTOPSARSLC = " + isTOPSARSLC);
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
            //System.out.println("noiseVectorListElem is null = " + (noiseVectorListElem == null));
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
                targetBand.setNoDataValueUsed(srcBand.isNoDataValueUsed());
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

            final boolean complexData = bandUnit == Unit.UnitType.REAL || bandUnit == Unit.UnitType.IMAGINARY;

            String key = "";
            if (version >= 2.9 && isTOPS) {
                if (complexData) { // SLC
                    key = targetBand.getName().substring(10).toLowerCase();
                } else { // GRD
                    key = getBandPol(targetBand);
                }
            }
            //System.out.println("key = " + key);

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
                if (version < 2.9 || !isTOPS) {
                    lut = new double[w];
                    final ThermalNoiseInfo noiseInfo = getNoiseInfo(targetBandName);
                    if (absoluteCalibrationPerformed) {
                        final int calVecIdx = calInfo.getCalibrationVectorIndex(y);
                        final Sentinel1Utils.CalibrationVector vec0 = calInfo.getCalibrationVector(calVecIdx);
                        final Sentinel1Utils.CalibrationVector vec1 = calInfo.getCalibrationVector(calVecIdx + 1);
                        final float[] vec0LUT = Sentinel1Calibrator.getVector(calType, vec0);
                        final float[] vec1LUT = Sentinel1Calibrator.getVector(calType, vec1);
                        final Sentinel1Utils.CalibrationVector calVec = calInfo.calibrationVectorList[calVecIdx];
                        final int pixelIdx0 = calVec.getPixelIndex(x0);

                        computeTileScaledNoiseLUT(y, x0, w, noiseInfo, calInfo, vec0.timeMJD, vec1.timeMJD,
                                vec0LUT, vec1LUT, vec0.pixels, pixelIdx0, lut);

                    } else {
                        computeTileNoiseLUT(y, x0, w, noiseInfo, lut);
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

                    double noise = 0;
                    if (version < 2.9 || !isTOPS) {
                        noise = lut[xx];
                    } else if (noiseAzimuthBlockMap.containsKey(key)) {
                        noise = getNoiseValue(x, y, noiseAzimuthBlockMap.get(key));
                    } else if (burstBlockMap.containsKey(key)) { // should be TOPS SLC and after IPF 2.9.0
                        noise = getNoiseValue(x, y, burstBlockMap.get(key));
                    }

                    double value = dn2 - noise;
                    if(value < 0) {
                        value = dn2;       // small intensity value; if too small, calibration will make it nodatavalue
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

    /**
     * Compute calibration LUTs for the given range line.
     *
     * @param y       Index of the given range line.
     * @param x0      X coordinate of the upper left corner pixel of the given tile.
     * @param w       Tile width.
     * @param calInfo Object of CalibrationInfo class.
     * @param lut     LUT for calibration.
     */
    public static void computeTileCalibrationLUTs(final int y, final int x0, final int w,
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
        System.out.println("Sentinel1RemoveThermalNoiseOp: IPF version = " + version);
    }

    private void buildNoiseLUTForTOPSSLC() {

        final MetadataElement noiseElem = origMetadataRoot.getElement("noise");
        final MetadataElement[] noiseDataSetListElem = noiseElem.getElements();

        Sentinel1Utils.NoiseAzimuthVector[] noiseAzimuthVectors = null;
        Sentinel1Utils.NoiseVector[] noiseRangeVectors = null;

        for (MetadataElement dataSetListElem : noiseDataSetListElem) {

            final String imageName = dataSetListElem.getName();

            getT0andDeltaTS(imageName);

            final MetadataElement noiElem = dataSetListElem.getElement("noise");

            // get the noise azimuth vectors
            MetadataElement noiseAzimuthVectorListElem = noiElem.getElement("noiseAzimuthVectorList");
            noiseAzimuthVectors = Sentinel1Utils.getAzimuthNoiseVector(noiseAzimuthVectorListElem);

            // get the noise range vectors
            MetadataElement noiseRangeVectorListElem = noiElem.getElement("noiseRangeVectorList");
            noiseRangeVectors = Sentinel1Utils.getNoiseVector(noiseRangeVectorListElem);

            //System.out.println("buildNoiseLUTForTOPSSLC: noiseRangeVectors.length = " + noiseRangeVectors.length);
        }

        final MetadataElement annotationElem = origMetadataRoot.getElement("annotation");
        final MetadataElement[] annotationDataSetListElem = annotationElem.getElements();

        for (MetadataElement elem : annotationDataSetListElem) {
            final String imageName = elem.getName();

            final MetadataElement productElem = elem.getElement("product");
            final MetadataElement swathTimingElem = productElem.getElement("swathTiming");

            final int linesPerBurst = swathTimingElem.getAttributeInt("linesPerBurst");
            final int samplesPerBurst = swathTimingElem.getAttributeInt("samplesPerBurst");

            final MetadataElement burstListElem = swathTimingElem.getElement("burstList");
            final MetadataElement[] burstListArray = burstListElem.getElements();
            /*
            System.out.println("buildNoiseLUTForTOPSSLC: linesPerBurst = " + linesPerBurst
                    + " samplesPerBurst = " + samplesPerBurst
                    + " burstListArray.length = " + burstListArray.length);
            */
            BurstBlock burstBlocks[] = new BurstBlock[burstListArray.length];

            final String key = getKey(imageName);
            String bandElemName = "Band_" + key.toUpperCase();
            //System.out.println("buildNoiseLUTForTOPSSLC: key = " + key + " bandElemName = " + bandElemName);

            // For TOPS SLC product, only one noise azimuth vector for the entire subswath; same one for all bursts
            Sentinel1Utils.NoiseAzimuthVector noiseAzimuthVector;
            if (noiseAzimuthVectors[0].firstAzimuthLine < 0) { // just in case; sample TOPS EW SLC product was missing firstAzimuthLine etc.
                MetadataElement bandElem = absRoot.getElement(bandElemName);
                final int lastAzimuthLine = bandElem.getAttributeInt("num_output_lines") - 1;
                //System.out.println("buildNoiseLUTForTOPSSLC: lastAzimuthLine = " + lastAzimuthLine);
                // swath, firstRangeSample and lastRangeSample are not used by interpolNoiseAzimuthVector()
                noiseAzimuthVector =
                        new  Sentinel1Utils.NoiseAzimuthVector("", 0, -1,
                                lastAzimuthLine, -1,
                                noiseAzimuthVectors[0].lines, noiseAzimuthVectors[0].noiseAzimuthLUT);
            } else {
                noiseAzimuthVector =  noiseAzimuthVectors[0];
            }
            final double azimuthNoise[] = interpolNoiseAzimuthVector(noiseAzimuthVector);

            for (int i = 0; i < burstListArray.length; i++) {
                /*
                MetadataElement[] elements = burstListArray[i].getElements();
                for (int j = 0; j < elements.length; j++) {
                    System.out.println("buildNoiseLUTForTOPSSLC: j = " + j + " elem name = " + elements[j].getName());
                }
                String[] strings = burstListArray[i].getAttributeNames();
                for (String s : strings) {
                    System.out.println("buildNoiseLUTForTOPSSLC: attr name = " + s);
                }
                */
                final double timeOfFirstLineInBurst = Sentinel1Utils.getTime(burstListArray[i], "azimuthTime").getMJD();
                final MetadataElement firstValidSampleElem = burstListArray[i].getElement("firstValidSample");
                final int firstValidSample[] = getIntArray(firstValidSampleElem, "firstValidSample");
                final MetadataElement lastValidSampleElem = burstListArray[i].getElement("lastValidSample");
                final int lastValidSample[] = getIntArray(lastValidSampleElem, "lastValidSample");
                /*
                System.out.println("buildNoiseLUTForTOPSSLC: i = " + i + " firstLineTime = " + firstLineTime
                    + " firstValidSample.length = " + firstValidSample.length
                    + " lastValidSample.length = " + lastValidSample.length);
                for (int j = 0; j < 20; j++) {
                    System.out.println("buildNoiseLUTForTOPSSLC: i = " + i + ": j = " + j + " " + firstValidSample[j] + " " + lastValidSample[j]);
                }
                */

                final int firstLineInBurst = getLineFromTime(imageName, timeOfFirstLineInBurst);
                // Get the closet range noise vector
                final double deltaT = deltaTsMap.get(imageName);
                final double burstCentreTime = timeOfFirstLineInBurst + deltaT * (linesPerBurst/2);

                int closest = 0;
                if (noiseRangeVectors != null) {
                    for (int j = 1; j < noiseRangeVectors.length; j++) {
                        //System.out.println("buildNoiseLUTForTOPSSLC: i = " + i + " j = " + j + " time = " + noiseRangeVectors[j].timeMJD);
                        if (Math.abs(burstCentreTime - noiseRangeVectors[j].timeMJD) <
                                Math.abs(burstCentreTime - noiseRangeVectors[closest].timeMJD)) {
                            closest = j;
                        }
                    }
                }
                //System.out.println("buildNoiseLUTForTOPSSLC: i = " + i + " burstCentreTime = " + burstCentreTime + " closet = " + closest);

                double rangeNoise[] = new double[samplesPerBurst];
                interpolNoiseRangeVector(noiseRangeVectors[closest], 0 , samplesPerBurst - 1, rangeNoise);

                burstBlocks[i] = new BurstBlock(linesPerBurst, samplesPerBurst,
                        timeOfFirstLineInBurst, firstValidSample, lastValidSample, firstLineInBurst, firstLineInBurst + linesPerBurst - 1,
                        rangeNoise, azimuthNoise);
                /*
                System.out.println("buildNoiseLUTForTOPSSLC: i = " + i + " linesPerBurst = " + linesPerBurst
                    + " samplesPerBurst = " + samplesPerBurst + " timeOfFirstLineInBurst = " + timeOfFirstLineInBurst
                    + " firstLineInBurst = " + firstLineInBurst + " lastLineInBurst = " + (firstLineInBurst + linesPerBurst - 1));
                */
            }

            burstBlockMap.put(key, burstBlocks);
        }
    }

    private void buildNoiseLUTForTOPSGRD() {

        final MetadataElement noiseElem = origMetadataRoot.getElement("noise");
        final MetadataElement[] noiseDataSetListElem = noiseElem.getElements();

        // loop through s1a-iw-grd-hh-..., s1a-iw-grd-hv-... (TOPS (IW and EW) GRD products)
        for (MetadataElement dataSetListElem : noiseDataSetListElem) {

            // imageName is s1a-iw-grd-hh-... or s1a-ew1-slc-hh-...
            final String imageName = dataSetListElem.getName();
            getT0andDeltaTS(imageName);
            final double firstLineTime = t0Map.get(imageName);
            final double lineTimeInterval = deltaTsMap.get(imageName);

            final MetadataElement noiElem = dataSetListElem.getElement("noise");
            final MetadataElement adsHeaderElem = noiElem.getElement("adsHeader");
            final String pol = adsHeaderElem.getAttributeString("polarisation");

            // get the noise azimuth vectors
            MetadataElement noiseAzimuthVectorListElem = noiElem.getElement("noiseAzimuthVectorList");
            final Sentinel1Utils.NoiseAzimuthVector[] noiseAzimuthVectors =
                    Sentinel1Utils.getAzimuthNoiseVector(noiseAzimuthVectorListElem);
            //noiseAzimuthVectorMap.put(pol, noiseAzimuthVectors);

            //System.out.println("getThermalNoiseVectors: # azim noise vectors = " + noiseAzimuthVectors.length);

            // get the noise range vectors
            MetadataElement noiseRangeVectorListElem = noiElem.getElement("noiseRangeVectorList");
            final Sentinel1Utils.NoiseVector[] noiseRangeVectors =
                    Sentinel1Utils.getNoiseVector(noiseRangeVectorListElem);
            /*
            for (int i = 0; i < noiseRangeVectors.length; i++) {
                System.out.println(pol + ": noiseRangeVectors[" + i + "].line = " + noiseRangeVectors[i].line
                    + "; azim time = " + noiseRangeVectors[i].timeMJD);
            }
            */
            //noiseRangeVectorMap.put(pol, noiseRangeVectors);

            NoiseAzimuthBlock[] noiseAzimuthBlocks = new NoiseAzimuthBlock[noiseAzimuthVectors.length];
            for (int i = 0; i < noiseAzimuthBlocks.length; i++) {

                //noiseAzimuthBlocks[i] = getNoiseAzimuthBlock(noiseAzimuthVectors[i]); // can be null but should never be
                double interpolatedAzimuthVector[] = interpolNoiseAzimuthVector(noiseAzimuthVectors[i]);

                final double startAzimTime = firstLineTime + noiseAzimuthVectors[i].firstAzimuthLine * lineTimeInterval;
                final double endAzimTime = firstLineTime + noiseAzimuthVectors[i].lastAzimuthLine * lineTimeInterval;
                final String swath = noiseAzimuthVectors[i].swath;
                int[] noiseRangeVecIndices = getNoiseRangeVectorIndices(
                        pol, swath, startAzimTime, endAzimTime, noiseRangeVectors,
                        noiseAzimuthVectors[i].firstAzimuthLine, noiseAzimuthVectors[i].lastAzimuthLine);
                /*
                System.out.println("pol = " + pol + " swath = " + swath + " firstAzimuthLine = " + noiseAzimuthVectors[i].firstAzimuthLine
                        + " lastAzimuthLine = " + noiseAzimuthVectors[i].lastAzimuthLine
                        + " startAzimTime = " + startAzimTime + " endAzimTime = " + endAzimTime
                        + " noiseRangeVecIdx.length = "
                        + ((noiseRangeVecIndices == null) ? "null" : noiseRangeVecIndices.length));
                */
                final int numLines = noiseAzimuthVectors[i].lastAzimuthLine
                        - noiseAzimuthVectors[i].firstAzimuthLine + 1;
                final int numSamples = noiseAzimuthVectors[i].lastRangeSample
                        - noiseAzimuthVectors[i].firstRangeSample + 1;

                double interpNoiseRangeMatrix[][] = new double[numLines][numSamples];

                if (noiseRangeVecIndices != null && noiseRangeVecIndices.length > 0) {

                    double interpolatedRangeVectors[][] = new double[noiseRangeVecIndices.length][numSamples];
                    int noiseRangeVectorLine[] = new int[noiseRangeVecIndices.length];
                    for (int j = 0; j < noiseRangeVecIndices.length; j++) {
                        //System.out.println("   noiseRangeVecIdx[" + j + "] = " + noiseRangeVecIndices[j]);
                        noiseRangeVectorLine[j] = noiseRangeVectors[noiseRangeVecIndices[j]].line;
                        interpolNoiseRangeVector(noiseRangeVectors[noiseRangeVecIndices[j]],
                                noiseAzimuthVectors[i].firstRangeSample, noiseAzimuthVectors[i].lastRangeSample,
                                interpolatedRangeVectors[j]);
                    }

                    computeNoiseRangeMatrix(noiseAzimuthVectors[i].firstAzimuthLine,
                            noiseAzimuthVectors[i].lastAzimuthLine, noiseRangeVectorLine,
                            interpolatedRangeVectors, interpNoiseRangeMatrix);
                } else {
                    for (int row = 0; row < numLines; row++) {
                        for (int col = 0; col < numSamples; col++) {
                            interpNoiseRangeMatrix[row][col] = 1.0;
                        }
                    }
                }

                final double noiseMatrix[][] = new double[numLines][numSamples];

                for (int row = 0; row < numLines; row++) {
                    for (int col = 0; col < numSamples; col++) {
                        noiseMatrix[row][col] = interpolatedAzimuthVector[row] * interpNoiseRangeMatrix[row][col];
                    }
                }

                noiseAzimuthBlocks[i] = new NoiseAzimuthBlock(swath, noiseAzimuthVectors[i].firstAzimuthLine,
                        noiseAzimuthVectors[i].firstRangeSample, noiseAzimuthVectors[i].lastAzimuthLine,
                        noiseAzimuthVectors[i].lastRangeSample, noiseMatrix);
            }

            noiseAzimuthBlockMap.put(pol, noiseAzimuthBlocks);
        }

        //System.out.println("getThermalNoiseVectors DONE");
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

    private double[] interpolNoiseAzimuthVector(final Sentinel1Utils.NoiseAzimuthVector noiseAzimuthVector) {
        final int numberOfLines = noiseAzimuthVector.lastAzimuthLine - noiseAzimuthVector.firstAzimuthLine + 1;
        if (numberOfLines < 0) {
            SystemUtils.LOG.warning("######### noise vector has no lines");
            return null;
        }
        final double[] interpNoiseAzimVec = new double[numberOfLines];

        int i = 0;
        if (noiseAzimuthVector.lines.length < 2) { // This is possible
            //SystemUtils.LOG.warning("######### noise azimuth vector has length 1");
            for (int line = noiseAzimuthVector.firstAzimuthLine;
                 line <= noiseAzimuthVector.lastAzimuthLine;
                 line++)  {
                interpNoiseAzimVec[i++] = noiseAzimuthVector.noiseAzimuthLUT[0];
            }
        }  else {
            int lineIdx = getLineIndex(noiseAzimuthVector.firstAzimuthLine, noiseAzimuthVector.lines);
            for (int line = noiseAzimuthVector.firstAzimuthLine;
                 line <= noiseAzimuthVector.lastAzimuthLine;
                 line++) {

                if (line > noiseAzimuthVector.lines[lineIdx + 1]
                        && lineIdx < noiseAzimuthVector.lines.length - 2) {
                    lineIdx++;
                }

                interpNoiseAzimVec[i++] = interpol(noiseAzimuthVector.lines[lineIdx],
                        noiseAzimuthVector.lines[lineIdx + 1],
                        noiseAzimuthVector.noiseAzimuthLUT[lineIdx],
                        noiseAzimuthVector.noiseAzimuthLUT[lineIdx + 1],
                        line);
            }
        }

        if (i != numberOfLines) {
            System.out.println("Sentinel1RemoveThermalNoiseOp: ERROR i = " + i + " numberOfLines = " + numberOfLines + " ");
        }
        /*
        System.out.println("i = " + i + "; firstAzimuthLine = " + noiseAzimuthVector.firstAzimuthLine);
        if (i == 51) {
            for (int j = 0; j < interpNoiseAzimVec.length; j++) {
                System.out.println("interpNoiseAzimVec[" + j + "] = " + interpNoiseAzimVec[j]);
            }
        }
        */

        return interpNoiseAzimVec;
    }

    private void computeNoiseRangeMatrix(final int firstAzimuthLine, // input
                                         final int lastAzimuthLine, // input
                                         final int noiseRangeVectorLine[], // input
                                         final double interpolatedRangeVectors[][], // input
                                         final double noiseRangeMatrix[][] // output
    ) {
        /*
        System.out.println("computeNoiseRangeMatrix: firstAzimuthLine = " + firstAzimuthLine
            + " lastAzimuthLine = " + lastAzimuthLine);
        for (int i = 0; i < noiseRangeVectorLine.length; i++) {
            System.out.println("computeNoiseRangeMatrix: noiseRangeVectorLine[" + i + "] = " + noiseRangeVectorLine[i]);
        }
        */

        final int numSamples = noiseRangeMatrix[0].length;

        if (noiseRangeVectorLine.length == 1) {
            for (int sample = 0; sample < numSamples; sample++) {
                for (int line = 0; line < (lastAzimuthLine - firstAzimuthLine + 1); line++) {
                    noiseRangeMatrix[line][sample] = interpolatedRangeVectors[0][sample];
                }
            }
        } else {
            final int line0Idx = getLineIndex(firstAzimuthLine, noiseRangeVectorLine);

            for (int sample = 0; sample < numSamples; sample++) {
                int i = 0;
                int lineIdx = line0Idx;
                for (int line = firstAzimuthLine; line <= lastAzimuthLine; line++) {

                    if (line > noiseRangeVectorLine[lineIdx + 1]
                            && lineIdx < noiseRangeVectorLine.length - 2) {
                        lineIdx++;
                    }
                    //System.out.println("computeNoiseRangeMatrix: i = " + i + " sample = " + sample);
                    noiseRangeMatrix[i++][sample] = interpol(noiseRangeVectorLine[lineIdx],
                            noiseRangeVectorLine[lineIdx + 1], interpolatedRangeVectors[lineIdx][sample],
                            interpolatedRangeVectors[lineIdx + 1][sample], line);
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

    int[] getNoiseRangeVectorIndices(final String pol, final String swath,
                                     final double startAzimTime, final double endAzimTime,
                                     final Sentinel1Utils.NoiseVector[] noiseRangeVectors,
                                     // for debugging...
                                     final int startAzimLine, final int endAzimLine) {

        // Each noise range vector has an azimuth time (and corresponding azimuth line) associated with it.
        // We want to find the noise range vectors in "noiseRangeVectors" whose azimuth time lies with
        // the interval defined by [startAzimTime, endAzimTime].
        // If no such range vector exists, then find the one that lies within the swath (of the azimuth block)
        // start and end times and is closest to the centre of the azimuth block.
        // Noise range vector is not associated with a swath.

        //System.out.println("getNoiseRangeVectorIndices: called");
        List<Integer> list = new ArrayList<>();

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
            getSwathStartEndTimes(pol, swath, startEndTimes);
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

    void getSwathStartEndTimes(final String pol, final String swath, final double[] startEndtimes) {

        final String key = pol + "+" + swath;

        startEndtimes[0] = 0; // start time
        startEndtimes[1] = 0; // end time

        if (swathStartEndTimesMap.containsKey(key)) {
            double[] times = swathStartEndTimesMap.get(key);
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
                for (int i = 0; i < swathMergeArray.length; i++) {
                    final String curSwath = swathMergeArray[i].getAttributeString("swath");
                    if (curSwath.equals(swath)) {
                        //System.out.println("getSwathStartEndTimes: found " + key);
                        MetadataElement swathBoundsListElem = swathMergeArray[i].getElement("swathBoundsList");
                        MetadataElement[] swathBoundList = swathBoundsListElem.getElements();
                        final int startLine = swathBoundList[0].getAttributeInt("firstAzimuthLine");
                        final int lastIdx = swathBoundList.length - 1;
                        final int endLine = swathBoundList[lastIdx].getAttributeInt("lastAzimuthLine");
                        if (t0Map.containsKey(imageName) && deltaTsMap.containsKey(imageName)) {
                            final double t0 = t0Map.get(imageName);
                            final double deltaTs = deltaTsMap.get(imageName);
                            startEndtimes[0] = t0 + (startLine * deltaTs);
                            startEndtimes[1] = t0 + (endLine * deltaTs);
                            swathStartEndTimesMap.put(key, startEndtimes);
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

    private void getT0andDeltaTS(final String imageName) {

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
                t0Map.put(imageName, t0);

                double deltaTS1 = absRoot.getAttributeDouble(AbstractMetadata.line_time_interval) / Constants.secondsInDay; // just for comparison
                double deltaTS = imageInformationElem.getAttributeDouble("azimuthTimeInterval") / Constants.secondsInDay; // s to day
                deltaTsMap.put(imageName, deltaTS);

                //System.out.println("getT0andDeltaTS: " + imageName + ": t01 = " + t01 + " t0 = " + t0 + " deltaTS1 = " + deltaTS1 + " deltaTS = " + deltaTS);

                break;
            }
        }
    }

    private String getBandPol(final Band band) {
        final String bandName = band.getName();
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

    private double getNoiseValue(final int x, final int y, final NoiseAzimuthBlock[] noiseAzimuthBlocks) {

        for (int i = 0; i < noiseAzimuthBlocks.length; i++) {
            final int firstAzimuthLine = noiseAzimuthBlocks[i].firstAzimuthLine;
            final int lastAzimuthLine = noiseAzimuthBlocks[i].lastAzimuthLine;
            final int firstRangeSample = noiseAzimuthBlocks[i].firstRangeSample;
            final int lastRangeSample = noiseAzimuthBlocks[i].lastRangeSample;
            if (isTOPS) {
                if (x >= firstRangeSample && x <= lastRangeSample && y >= firstAzimuthLine && y <= lastAzimuthLine) {
                    return noiseAzimuthBlocks[i].noiseMatrix[y - firstAzimuthLine][x - firstRangeSample];
                }
            } else if (x >= firstRangeSample && x <= lastRangeSample) {
                final double val = noiseAzimuthBlocks[i].noiseMatrix[0][x - firstRangeSample];
                if (removeThermalNoise) {
                    return val;
                } else {
                    return -val;
                }
            }
        }

        return 0;
    }

    private double getNoiseValue(final int x, final int y, final BurstBlock[] burstBlocks) {

        for (int i = 0; i < burstBlocks.length; i++) {
            final int firstLine = burstBlocks[i].firstLine;
            final int lastLine = burstBlocks[i].lastLine;
            // linesPerBurst = lastLine - firstLine + 1
            if (y >= firstLine && y <= lastLine) {
                final int firstValidSample = burstBlocks[i].firstValidSample[y - firstLine];
                final int lastValidSample = burstBlocks[i].lastValidSample[y - firstLine];
                if (x >= firstValidSample && x <= lastValidSample) {
                    if (y > burstBlocks[i].azimuthNoise.length-1) {
                        System.out.println("Sentinel1RemoveThermalNoiseOp: ERROR: i = " + i + " y = " + y + "burstBlocks[i].azimuthNoise.length = " + burstBlocks[i].azimuthNoise.length);
                    }
                    final double azimuthNoise = burstBlocks[i].azimuthNoise[y];
                    if (x > burstBlocks[i].rangeNoise.length - 1){
                        System.out.println("Sentinel1RemoveThermalNoiseOp: ERROR: i = " + i + " x = " + x + " burstBlocks[i].rangeNoise.length = " + burstBlocks[i].rangeNoise.length);
                    }
                    final double rangeNoise = burstBlocks[i].rangeNoise[x];
                    final double val = azimuthNoise * rangeNoise;
                    if (removeThermalNoise) {
                        return  val;
                    } else {
                        return -val;
                    }
                }
            }
        }

        return 0;
    }

    private static int getIndex(final String s, final char c, final int n) {
        // Find the index of the n'th occurrence of c in s
        int cnt = 0;
        for (int i = 0; i < s.length(); i++) {
            if (s.charAt(i) == c) {
                cnt++;
                if (cnt == n) {
                    return i;
                }
            }
        }
        return -1;
    }

    private static String getKey(final String imageName) {
        final int idx1 = getIndex(imageName, '-', 1);
        final int idx2 = getIndex(imageName, '-', 2);
        final int idx3 = getIndex(imageName, '-', 3);
        final int idx4 = getIndex(imageName, '-', 4);
        String s = imageName.substring(idx1+1, idx2) + imageName.substring(idx3, idx4);
        return s.replace('-', '_');
    }

    private int getLineFromTime(final String imageName, final double azimTime) {

        final double t0 = t0Map.get(imageName);
        final double lineTimeInterval = deltaTsMap.get(imageName);

        return (int) ((azimTime - t0) / lineTimeInterval);
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
        final String swath;
        final int firstAzimuthLine;
        final int firstRangeSample;
        final int lastAzimuthLine;
        final int lastRangeSample;

        final int numSamples;
        final int numLines;

        final double[][] noiseMatrix;

        //final double[] interpNoiseAzimVec; // length = lastAzimuthLine - firstAzimuthLine + 1
        //double[][] interpNoiseRangeMatrix;

        NoiseAzimuthBlock(final String swath,
                          final int firstAzimuthLine, final int firstRangeSample,
                          final int lastAzimuthLine, final int lastRangeSample,
                          final double [][] noiseMatrix)
        {
            this.swath = swath;
            this.firstAzimuthLine = firstAzimuthLine;
            this.firstRangeSample = firstRangeSample;
            this.lastAzimuthLine = lastAzimuthLine;
            this.lastRangeSample = lastRangeSample;
            this.noiseMatrix =  noiseMatrix;

            numSamples = lastRangeSample - firstRangeSample + 1;
            numLines = lastAzimuthLine - firstAzimuthLine + 1;

            //this.interpNoiseAzimVec = interpNoiseAzimVec;
            //interpNoiseRangeMatrix = new double[numLines][numSamples];
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

    // TODO This is taken from Sentinel1Utils. Should just make it public there.
    private static int[] getIntArray(final MetadataElement elem, final String tag) {

        final MetadataAttribute attribute = elem.getAttribute(tag);
        if (attribute == null) {
            throw new OperatorException(tag + " attribute not found");
        }

        int[] array = null;
        if (attribute.getDataType() == ProductData.TYPE_ASCII) {
            final String dataStr = attribute.getData().getElemString();
            final String[] items = dataStr.split(" ");
            array = new int[items.length];
            for (int i = 0; i < items.length; i++) {
                try {
                    array[i] = Integer.parseInt(items[i]);
                } catch (NumberFormatException e) {
                    throw new OperatorException("Failed in getting" + tag + " array");
                }
            }
        }

        return array;
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
