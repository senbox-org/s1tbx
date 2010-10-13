/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
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

import com.bc.ceres.core.Assert;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.framework.gpf.annotations.SourceProduct;
import org.esa.beam.framework.gpf.annotations.TargetProduct;
import org.esa.beam.framework.gpf.experimental.SampleOperator;
import org.esa.beam.meris.radiometry.calibration.CalibrationAlgorithm;
import org.esa.beam.meris.radiometry.calibration.Resolution;
import org.esa.beam.meris.radiometry.equalization.EqualizationAlgorithm;
import org.esa.beam.meris.radiometry.equalization.ReprocessingVersion;
import org.esa.beam.meris.radiometry.smilecorr.SmileCorrectionAlgorithm;
import org.esa.beam.meris.radiometry.smilecorr.SmileCorrectionAuxdata;
import org.esa.beam.util.ProductUtils;
import org.esa.beam.util.math.RsMathUtils;

import java.io.File;

import static org.esa.beam.dataio.envisat.EnvisatConstants.*;


@OperatorMetadata(alias = "Meris.CorrectRadiometry",
                  description = "Performs radiometric corrections on MERIS L1b data products.",
                  authors = "Marc Bouvet (ESTEC); Marco Peters, Ralf Quast, Thomas Storm, Marco Zuehlke (Brockmann Consult)",
                  copyright = "(c) 2010 by Brockmann Consult",
                  version = "1.0")
public class MerisRadiometryCorrectionOp extends SampleOperator {

    private static final String UNIT_DL = "dl";
    private static final String INVALID_MASK_NAME = "invalid";
    private static final String LAND_MASK_NAME = "land";

    @Parameter(defaultValue = "false",
               label = "Perform calibration",
               description = "Whether to perform the calibration.")
    private boolean doCalibration;

    @Parameter(defaultValue = "MER_RAC_AXVIEC20050708_135553_20021224_121445_20041213_220000",
               label = "Source radiometric correction file",
               description = "The radiometric correction auxiliary file for the source product")
    private File sourceRacFile;

    @Parameter(defaultValue = "MER_RAC_AXVACR20091016_154511_20021224_121445_20041213_220000",
               label = "Target radiometric correction file",
               description = "The radiometric correction auxiliary file for the target product")
    private File targetRacFile;

    @Parameter(defaultValue = "true",
               label = "Perform SMILE correction",
               description = "Whether to perform SMILE correction.")
    private boolean doSmile;

    @Parameter(defaultValue = "true",
               label = "Perform equalization",
               description = "Perform removal of detector-to-detector systematic radiometric differences in MERIS L1b data products")
    private boolean doEqualization;

    @Parameter(label = "Reprocessing version", valueSet = {"AUTO_DETECT", "REPROCESSING_2", "REPROCESSING_3"},
               defaultValue = "AUTO_DETECT",
               description = "The version of the reprocessing the product comes from. Is only used if " +
                             "equalisation is enabled.")
    private ReprocessingVersion reproVersion;

    @Parameter(defaultValue = "true",
               label = "Perform radiance-to-reflectance conversion",
               description = "Whether to perform radiance-to-reflectance conversion.")
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

    @TargetProduct(description = "The target product.")
    private Product targetProduct;

    private transient CalibrationAlgorithm calibrationAlgorithm;
    private transient EqualizationAlgorithm equalizationAlgorithm;
    private transient SmileCorrectionAlgorithm smileCorrectionAlgorithm;

    private transient int detectorIndexSampleIndex;
    private transient int sunZenithAngleSampleIndex;
    private transient int invalidMaskSampleIndex;
    private transient int landMaskSampleIndex;

    @Override
    protected void configureSourceSamples(Configurator configurator) {
        initAlgorithms();
        validateSourceProduct();
        int i = -1;
        // define samples corresponding to spectral bands, using the spectral band index as sample index
        for (final Band band : sourceProduct.getBands()) {
            final int spectralBandIndex = band.getSpectralBandIndex();
            if (spectralBandIndex != -1) {
                configurator.defineSample(spectralBandIndex, band.getName());
                if (spectralBandIndex > i) {
                    i = spectralBandIndex;
                }
            }
        }
        detectorIndexSampleIndex = i + 1;
        if (doCalibration || doSmile || doEqualization) {
            configurator.defineSample(detectorIndexSampleIndex, MERIS_DETECTOR_INDEX_DS_NAME);
        }
        sunZenithAngleSampleIndex = i + 2;
        if (doRadToRefl) {
            configurator.defineSample(sunZenithAngleSampleIndex, MERIS_SUN_ZENITH_DS_NAME);
        }
        invalidMaskSampleIndex = i + 3;
        landMaskSampleIndex = i + 4;
        if (doSmile) {
            configurator.defineSample(invalidMaskSampleIndex, INVALID_MASK_NAME);
            configurator.defineSample(landMaskSampleIndex, LAND_MASK_NAME);
        }
    }

    @Override
    protected void configureTargetSamples(Configurator configurator) {
        // define samples corresponding to spectral bands, using the spectral band index as sample index
        for (final Band band : getTargetProduct().getBands()) { // pitfall: using targetProduct field here throws NPE
            final int spectralBandIndex = band.getSpectralBandIndex();
            if (spectralBandIndex != -1) {
                configurator.defineSample(spectralBandIndex, band.getName());
            }
        }
    }

    @Override
    protected void configureTargetProduct(Product targetProduct) {
        targetProduct.setName(sourceProduct.getName());
        if (doRadToRefl) {
            targetProduct.setProductType(String.format("%s_REFL", sourceProduct.getProductType()));
            targetProduct.setAutoGrouping("reflec");
        } else {
            targetProduct.setProductType(sourceProduct.getProductType());
            targetProduct.setAutoGrouping("radiance");
        }
        targetProduct.setDescription("MERIS L1b Radiometric Correction");
        ProductUtils.copyMetadata(sourceProduct, targetProduct);

        for (final Band sourceBand : sourceProduct.getBands()) {
            if (sourceBand.getSpectralBandIndex() != -1) {
                final String targetBandName;
                final String targetBandDescription;
                if (doRadToRefl) {
                    targetBandName = sourceBand.getName().replace("radiance", "reflec");
                    targetBandDescription = "Radiometry-corrected TOA reflectance";
                } else {
                    targetBandName = sourceBand.getName();
                    targetBandDescription = "Radiometry-corrected TOA radiance";
                }
                final Band targetBand = targetProduct.addBand(targetBandName, ProductData.TYPE_FLOAT32);
                targetBand.setDescription(targetBandDescription);
                if (doRadToRefl) {
                    targetBand.setUnit(UNIT_DL);
                } else {
                    targetBand.setUnit(sourceBand.getUnit());
                }
                targetBand.setValidPixelExpression(sourceBand.getValidPixelExpression());
                ProductUtils.copySpectralBandProperties(sourceBand, targetBand);
            }
        }
        copySourceBand(MERIS_DETECTOR_INDEX_DS_NAME, targetProduct);
        ProductUtils.copyFlagBands(sourceProduct, targetProduct);
        final Band sourceFlagBand = sourceProduct.getBand(MERIS_L1B_FLAGS_DS_NAME);
        final Band targetFlagBand = targetProduct.getBand(MERIS_L1B_FLAGS_DS_NAME);

        targetFlagBand.setSourceImage(sourceFlagBand.getSourceImage());
        targetProduct.setStartTime(sourceProduct.getStartTime());
        targetProduct.setEndTime(sourceProduct.getEndTime());

        // copy all source bands yet ignored
        for (final Band sourceBand : sourceProduct.getBands()) {
            if (sourceBand.getSpectralBandIndex() == -1 && !targetProduct.containsBand(sourceBand.getName())) {
                copySourceBand(sourceBand.getName(), targetProduct);
            }
        }
    }

    @Override
    protected void computeSample(int x, int y, Sample[] sourceSamples, WritableSample targetSample) {
        final int bandIndex = targetSample.getIndex();
        final Sample sourceRadiance = sourceSamples[bandIndex];
        int detectorIndex = -1;
        if (doCalibration || doSmile || doEqualization) {
            detectorIndex = sourceSamples[detectorIndexSampleIndex].getInt();
        }
        double value = sourceRadiance.getDouble();
        if (doCalibration && detectorIndex != -1 && value < sourceRadiance.getNode().scale(65435.0)) {
            value = calibrationAlgorithm.calibrate(bandIndex, detectorIndex, value);
        }
        if (doSmile) {
            final boolean invalid = sourceSamples[invalidMaskSampleIndex].getBoolean();
            if (!invalid && detectorIndex != -1) {
                final boolean land = sourceSamples[landMaskSampleIndex].getBoolean();
                value = smileCorrectionAlgorithm.correct(bandIndex, detectorIndex, sourceSamples, land);
            }
        }
        if (doRadToRefl) {
            final float solarFlux = ((Band) sourceRadiance.getNode()).getSolarFlux();
            final float sunZenithSample = sourceSamples[sunZenithAngleSampleIndex].getFloat();
            value = RsMathUtils.radianceToReflectance((float) value, sunZenithSample, solarFlux);
        }
        if (doEqualization && detectorIndex != -1) {
            value = equalizationAlgorithm.performEqualization(value, bandIndex, detectorIndex);
        }
        targetSample.set(value);
    }

    private void initAlgorithms() {
        if (doCalibration) {
            try {
                calibrationAlgorithm = new CalibrationAlgorithm(
                        sourceProduct.getProductType().contains("RR") ? Resolution.RR : Resolution.FR,
                        0.5 * (sourceProduct.getStartTime().getMJD() + sourceProduct.getEndTime().getMJD()),
                        sourceRacFile,
                        targetRacFile);
            } catch (Exception e) {
                throw new OperatorException(e);
            }
            // If calibration is performed the equalization  has to use the LUTs of Reprocessing 3
            reproVersion = ReprocessingVersion.REPROCESSING_3;
        }
        if (doSmile) {
            try {
                smileCorrectionAlgorithm = new SmileCorrectionAlgorithm(
                        SmileCorrectionAuxdata.loadAuxdata(sourceProduct.getProductType()));
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

    private void validateSourceProduct() {
        Assert.state(MERIS_L1_TYPE_PATTERN.matcher(sourceProduct.getProductType()).matches(),
                     "Source product must be of type MERIS L1b.");
        final String msgPattern = "Source product must contain '%s'.";
        if (doSmile) {
            Assert.state(sourceProduct.containsBand(MERIS_DETECTOR_INDEX_DS_NAME),
                         String.format(msgPattern, MERIS_DETECTOR_INDEX_DS_NAME));
            Assert.state(sourceProduct.containsBand(MERIS_L1B_FLAGS_DS_NAME),
                         String.format(msgPattern, MERIS_L1B_FLAGS_DS_NAME));
        }
        if (doEqualization) {
            Assert.state(sourceProduct.getStartTime() != null, "Source product must have a start time");
            Assert.state(sourceProduct.containsBand(MERIS_DETECTOR_INDEX_DS_NAME),
                         String.format(msgPattern, MERIS_DETECTOR_INDEX_DS_NAME));
        }
        if (doRadToRefl) {
            Assert.state(sourceProduct.containsRasterDataNode(MERIS_SUN_ZENITH_DS_NAME),
                         String.format(msgPattern, MERIS_SUN_ZENITH_DS_NAME));
        }
    }

    private void copySourceBand(String bandName, Product targetProduct) {
        final Band sourceBand = sourceProduct.getBand(bandName);
        final Band targetBand = ProductUtils.copyBand(bandName, sourceProduct, targetProduct);
        targetBand.setSourceImage(sourceBand.getSourceImage());
    }

    public static class Spi extends OperatorSpi {

        public Spi() {
            super(MerisRadiometryCorrectionOp.class);
        }
    }
}
