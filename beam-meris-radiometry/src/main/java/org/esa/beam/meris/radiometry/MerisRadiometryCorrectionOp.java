/*
 * Copyright (C) 2012 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.beam.meris.radiometry;

import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.framework.gpf.annotations.SourceProduct;
import org.esa.beam.framework.gpf.pointop.*;
import org.esa.beam.meris.radiometry.calibration.CalibrationAlgorithm;
import org.esa.beam.meris.radiometry.calibration.Resolution;
import org.esa.beam.meris.radiometry.equalization.EqualizationAlgorithm;
import org.esa.beam.meris.radiometry.equalization.ReprocessingVersion;
import org.esa.beam.meris.radiometry.smilecorr.SmileCorrectionAlgorithm;
import org.esa.beam.meris.radiometry.smilecorr.SmileCorrectionAuxdata;
import org.esa.beam.util.ProductUtils;
import org.esa.beam.util.math.RsMathUtils;

import java.io.*;
import java.util.Map;
import java.util.TreeMap;

import static org.esa.beam.dataio.envisat.EnvisatConstants.*;


/**
 * This operator is used to perform radiometric corrections on MERIS L1b data products.
 * The corrections include the following optional steps:
 * <ul>
 * <li><b>Radiometric re-calibration</b><br/>
 * Multiplies the inverse gains of the 2nd reprocessing and multiplies the gains of 3rd reprocessing
 * to the radiance values.</li>
 * <li><b>Smile-effect correction</b><br/>
 * Corrects the radiance values for the small variations of the spectral wavelength
 * of each pixel along the image (smile-effect).
 * </li>
 * <li><b>Meris equalisation</b><br/>
 * Removes systematic detector-to-detector radiometric differences in MERIS L1b data products. </li>
 * <li><b>Radiance-to-reflectance conversion</b><br/>
 * Converts the TOA radiance values into TOA reflectance values. </li>
 * </ul>
 *
 * @author Marco Peters
 * @since BEAM 4.9
 */
@OperatorMetadata(alias = "Meris.CorrectRadiometry",
                  description = "Performs radiometric corrections on MERIS L1b data products.",
                  authors = "Marc Bouvet (ESTEC); Marco Peters, Ralf Quast, Thomas Storm, Marco Zuehlke (Brockmann Consult)",
                  copyright = "(c) 2014 by Brockmann Consult",
                  version = "1.1.3")
public class MerisRadiometryCorrectionOp extends SampleOperator {

    private static final String UNIT_DL = "dl";
    private static final double RAW_SATURATION_THRESHOLD = 65435.0;
    private static final String DEFAULT_SOURCE_RAC_RESOURCE = "MER_RAC_AXVIEC20050708_135553_20021224_121445_20041213_220000";
    private static final String DEFAULT_TARGET_RAC_RESOURCE = "MER_RAC_AXVACR20091016_154511_20021224_121445_20041213_220000";
    private static final int INVALID_BIT_INDEX = 7;
    private static final int LAND_BIT_INDEX = 4;

    @Parameter(defaultValue = "true",
               label = "Perform calibration",
               description = "Whether to perform the calibration.")
    private boolean doCalibration;

    @Parameter(label = "Source radiometric correction file (optional)",
               description = "The radiometric correction auxiliary file for the source product. " +
                             "The default '" + DEFAULT_SOURCE_RAC_RESOURCE + "'")
    private File sourceRacFile;

    @Parameter(label = "Target radiometric correction file (optional)",
               description = "The radiometric correction auxiliary file for the target product. " +
                             "The default '" + DEFAULT_TARGET_RAC_RESOURCE + "'")
    private File targetRacFile;

    @Parameter(defaultValue = "true",
               label = "Perform Smile-effect correction",
               description = "Whether to perform Smile-effect correction.")
    private boolean doSmile;

    @Parameter(defaultValue = "true",
               label = "Perform equalisation",
               description = "Perform removal of detector-to-detector systematic radiometric differences in MERIS L1b data products.")
    private boolean doEqualization;

    @Parameter(label = "Reprocessing version", valueSet = {"AUTO_DETECT", "REPROCESSING_2", "REPROCESSING_3"},
               defaultValue = "AUTO_DETECT",
               description = "The version of the reprocessing the product comes from. Is only used if " +
                             "equalisation is enabled.")
    private ReprocessingVersion reproVersion;

    @Parameter(defaultValue = "false",
               label = "Perform radiance-to-reflectance conversion",
               description = "Whether to perform radiance-to-reflectance conversion. " +
                             "When selecting ENVISAT as target format, the radiance to reflectance conversion can not be performed.")
    private boolean doRadToRefl;

    @SourceProduct(alias = "source", label = "Name", description = "The source product.",
                   bands = {
                           MERIS_L1B_FLAGS_DS_NAME, MERIS_DETECTOR_INDEX_DS_NAME,
                           MERIS_L1B_RADIANCE_1_BAND_NAME,
                           MERIS_L1B_RADIANCE_2_BAND_NAME,
                           MERIS_L1B_RADIANCE_3_BAND_NAME,
                           MERIS_L1B_RADIANCE_4_BAND_NAME,
                           MERIS_L1B_RADIANCE_5_BAND_NAME,
                           MERIS_L1B_RADIANCE_6_BAND_NAME,
                           MERIS_L1B_RADIANCE_7_BAND_NAME,
                           MERIS_L1B_RADIANCE_8_BAND_NAME,
                           MERIS_L1B_RADIANCE_9_BAND_NAME,
                           MERIS_L1B_RADIANCE_10_BAND_NAME,
                           MERIS_L1B_RADIANCE_11_BAND_NAME,
                           MERIS_L1B_RADIANCE_12_BAND_NAME,
                           MERIS_L1B_RADIANCE_13_BAND_NAME,
                           MERIS_L1B_RADIANCE_14_BAND_NAME,
                           MERIS_L1B_RADIANCE_15_BAND_NAME
                   })
    private Product sourceProduct;

    private transient CalibrationAlgorithm calibrationAlgorithm;
    private transient EqualizationAlgorithm equalizationAlgorithm;
    private transient SmileCorrectionAlgorithm smileCorrAlgorithm;

    private transient int detectorIndexSampleIndex;
    private transient int sunZenithAngleSampleIndex;
    private transient int flagBandIndex;
    private transient int currentPixel = 0;
    private Map<Integer, Double> bandIndexToMaxValueMap;

    @Override
    protected void prepareInputs() throws OperatorException {
        super.prepareInputs();
        validateSourceProduct();
        initAlgorithms();
    }

    @Override
    protected void configureSourceSamples(SampleConfigurer sampleConfigurer) {
        int i = -1;
        // define samples corresponding to spectral bands, using the spectral band index as sample index
        for (final Band band : sourceProduct.getBands()) {
            final int spectralBandIndex = band.getSpectralBandIndex();
            if (spectralBandIndex != -1) {
                sampleConfigurer.defineSample(spectralBandIndex, band.getName());
                if (spectralBandIndex > i) {
                    i = spectralBandIndex;
                }
            }
        }
        detectorIndexSampleIndex = i + 1;
        if (doCalibration || doSmile || doEqualization) {
            sampleConfigurer.defineSample(detectorIndexSampleIndex, MERIS_DETECTOR_INDEX_DS_NAME);
        }
        sunZenithAngleSampleIndex = i + 2;
        if (doRadToRefl) {
            sampleConfigurer.defineSample(sunZenithAngleSampleIndex, MERIS_SUN_ZENITH_DS_NAME);
        }
        flagBandIndex = i + 3;
        if (doSmile) {
            sampleConfigurer.defineSample(flagBandIndex, MERIS_L1B_FLAGS_DS_NAME);
        }
    }

    @Override
    protected void configureTargetSamples(SampleConfigurer sampleConfigurer) {
        // define samples corresponding to spectral bands, using the spectral band index as sample index
        for (final Band band : getTargetProduct().getBands()) { // pitfall: using targetProduct field here throws NPE
            final int spectralBandIndex = band.getSpectralBandIndex();
            if (spectralBandIndex != -1) {
                sampleConfigurer.defineSample(spectralBandIndex, band.getName());
            }
        }
    }

    @Override
    protected void configureTargetProduct(ProductConfigurer productConfigurer) {
        productConfigurer.copyMetadata();
        productConfigurer.copyTimeCoding();

        Product targetProduct = productConfigurer.getTargetProduct();
        targetProduct.setName(sourceProduct.getName());
        if (doRadToRefl) {
            targetProduct.setProductType(String.format("%s_REFL", sourceProduct.getProductType()));
            targetProduct.setAutoGrouping("reflec");
        } else {
            targetProduct.setProductType(sourceProduct.getProductType());
            targetProduct.setAutoGrouping("radiance");
        }
        targetProduct.setDescription("MERIS L1b Radiometric Correction");

        bandIndexToMaxValueMap = new TreeMap<>();
        Band[] bands = sourceProduct.getBands();
        for (int i = 0; i < bands.length; i++) {
            Band sourceBand = bands[i];
            if (sourceBand.getSpectralBandIndex() != -1) {
                final String targetBandName;
                final String targetBandDescription;
                final int dataType;
                final String unit;
                final double scalingFactor;
                final double scalingOffset;
                if (doRadToRefl) {
                    targetBandName = sourceBand.getName().replace("radiance", "reflec");
                    targetBandDescription = "Radiometry-corrected TOA reflectance";
                    dataType = ProductData.TYPE_FLOAT32;
                    unit = UNIT_DL;
                    scalingFactor = 1.0;
                    scalingOffset = 0;
                } else {
                    targetBandName = sourceBand.getName();
                    targetBandDescription = "Radiometry-corrected TOA radiance";
                    dataType = sourceBand.getDataType();
                    unit = sourceBand.getUnit();
                    scalingFactor = sourceBand.getScalingFactor();
                    scalingOffset = sourceBand.getScalingOffset();
                }
                final Band targetBand = targetProduct.addBand(targetBandName, dataType);
                targetBand.setScalingFactor(scalingFactor);
                targetBand.setScalingOffset(scalingOffset);
                bandIndexToMaxValueMap.put(i, targetBand.scale(0xFFFF));
                targetBand.setDescription(targetBandDescription);
                targetBand.setUnit(unit);
                targetBand.setValidPixelExpression(sourceBand.getValidPixelExpression());
                ProductUtils.copySpectralBandProperties(sourceBand, targetBand);
            }
        }

        productConfigurer.copyTiePointGrids(); // fixme: always need to copy tie-points before copying geo-coding (nf)
        productConfigurer.copyGeoCoding();

        // copy all source bands yet ignored
        for (final Band sourceBand : sourceProduct.getBands()) {
            if (sourceBand.getSpectralBandIndex() == -1 && !targetProduct.containsBand(sourceBand.getName())) {
                productConfigurer.copyBands(sourceBand.getName());
            }
        }
        productConfigurer.copyMasks();
    }

    @Override
    protected void computeSample(int x, int y, Sample[] sourceSamples, WritableSample targetSample) {
        checkCancellation();

        final int bandIndex = targetSample.getIndex();
        final Sample sourceRadiance = sourceSamples[bandIndex];
        int detectorIndex = -1;
        if (doCalibration || doSmile || doEqualization) {
            detectorIndex = sourceSamples[detectorIndexSampleIndex].getInt();
        }
        double value = sourceRadiance.getDouble();
        boolean isValidDetectorIndex = detectorIndex >= 0;
        if (doCalibration && isValidDetectorIndex && value < sourceRadiance.getNode().scale(RAW_SATURATION_THRESHOLD)) {
            value = calibrationAlgorithm.calibrate(bandIndex, detectorIndex, value);
        }
        if (doSmile) {
            final Sample flagSample = sourceSamples[flagBandIndex];
            final boolean invalid = flagSample.getBit(INVALID_BIT_INDEX);
            if (!invalid && detectorIndex != -1) {
                final boolean land = flagSample.getBit(LAND_BIT_INDEX);
                double[] sourceValues = new double[15];
                for (int i = 0; i < sourceValues.length; i++) {
                    sourceValues[i] = sourceSamples[i].getDouble();
                }
                value = smileCorrAlgorithm.correct(bandIndex, detectorIndex, sourceValues, land);
            }
        }
        if (doRadToRefl) {
            final float solarFlux = ((Band) sourceRadiance.getNode()).getSolarFlux();
            final float sunZenithSample = sourceSamples[sunZenithAngleSampleIndex].getFloat();
            value = RsMathUtils.radianceToReflectance((float) value, sunZenithSample, solarFlux);
        }
        if (doEqualization && isValidDetectorIndex) {
            value = equalizationAlgorithm.performEqualization(value, bandIndex, detectorIndex);
        }

        final double croppedValue = Math.min(bandIndexToMaxValueMap.get(bandIndex), value);
        targetSample.set(croppedValue);

    }

    private void initAlgorithms() {
        final String productType = sourceProduct.getProductType();
        if (doCalibration) {
            InputStream sourceRacStream = null;
            InputStream targetRacStream = null;
            try {
                sourceRacStream = openStream(sourceRacFile, DEFAULT_SOURCE_RAC_RESOURCE);
                targetRacStream = openStream(targetRacFile, DEFAULT_TARGET_RAC_RESOURCE);
                final double cntJD = 0.5 * (sourceProduct.getStartTime().getMJD() + sourceProduct.getEndTime().getMJD());
                final Resolution resolution = productType.contains("RR") ? Resolution.RR : Resolution.FR;
                calibrationAlgorithm = new CalibrationAlgorithm(resolution, cntJD, sourceRacStream, targetRacStream);
            } catch (IOException e) {
                throw new OperatorException(e);
            } finally {
                try {
                    if (sourceRacStream != null) {
                        sourceRacStream.close();
                    }
                    if (targetRacStream != null) {
                        targetRacStream.close();
                    }
                } catch (IOException ignore) {
                }
            }
            // If calibration is performed the equalization  has to use the LUTs of Reprocessing 3
            reproVersion = ReprocessingVersion.REPROCESSING_3;
        }
        if (doSmile) {
            try {
                smileCorrAlgorithm = new SmileCorrectionAlgorithm(SmileCorrectionAuxdata.loadAuxdata(productType));
            } catch (Exception e) {
                throw new OperatorException(e);
            }
        }
        if (doEqualization) {
            try {
                equalizationAlgorithm = new EqualizationAlgorithm(sourceProduct, reproVersion);
            } catch (Exception e) {
                throw new OperatorException(e);
            }
        }
    }

    private static InputStream openStream(File racFile, String defaultRacResource) throws FileNotFoundException {
        if (racFile == null) {
            return CalibrationAlgorithm.class.getResourceAsStream(defaultRacResource);
        } else {
            return new FileInputStream(racFile);
        }
    }

    private void validateSourceProduct() throws OperatorException {
        if (!MERIS_L1_TYPE_PATTERN.matcher(sourceProduct.getProductType()).matches()) {
            String msg = String.format("Source product must be of type MERIS Level 1b. Product type is: '%s'",
                                       sourceProduct.getProductType());
            getLogger().warning(msg);
        }
        boolean isReprocessing2 = reproVersion == ReprocessingVersion.REPROCESSING_2 ||
                                  ReprocessingVersion.autoDetect(sourceProduct) == ReprocessingVersion.REPROCESSING_2;
        if (!isReprocessing2 && doCalibration) {
            getLogger().warning("Skipping calibration. Source product is already of 3rd reprocessing.");
            doCalibration = false;
        }
        if (doCalibration || doEqualization) {
            if (sourceProduct.getStartTime() == null) {
                throw new OperatorException("Source product must have a start time");
            }
        }
        if (doCalibration) {
            if (sourceProduct.getEndTime() == null) {
                throw new OperatorException("Source product must have an end time");
            }
        }

        final String msgPatternMissingBand = "Source product must contain '%s'.";
        if (doSmile) {
            if (!sourceProduct.containsBand(MERIS_DETECTOR_INDEX_DS_NAME)) {
                throw new OperatorException(String.format(msgPatternMissingBand, MERIS_DETECTOR_INDEX_DS_NAME));
            }
            if (!sourceProduct.containsBand(MERIS_L1B_FLAGS_DS_NAME)) {
                throw new OperatorException(String.format(msgPatternMissingBand, MERIS_L1B_FLAGS_DS_NAME));
            }
            if (!sourceProduct.getBand(MERIS_L1B_FLAGS_DS_NAME).isFlagBand()) {
                throw new OperatorException(
                        String.format("Flag-coding is missing for band '%s' ", MERIS_L1B_FLAGS_DS_NAME));
            }
        }
        if (doEqualization) {
            if (!sourceProduct.containsBand(MERIS_DETECTOR_INDEX_DS_NAME)) {
                throw new OperatorException(String.format(msgPatternMissingBand, MERIS_DETECTOR_INDEX_DS_NAME));
            }
        }
        if (doRadToRefl) {
            if (!sourceProduct.containsRasterDataNode(MERIS_SUN_ZENITH_DS_NAME)) {
                throw new OperatorException(String.format(msgPatternMissingBand, MERIS_SUN_ZENITH_DS_NAME));
            }
        }
    }

    private void checkCancellation() {
        if (currentPixel % 1000 == 0) {
            checkForCancellation();
            currentPixel = 0;
        }
        currentPixel++;
    }

    public static class Spi extends OperatorSpi {

        public Spi() {
            super(MerisRadiometryCorrectionOp.class);
        }
    }
}
