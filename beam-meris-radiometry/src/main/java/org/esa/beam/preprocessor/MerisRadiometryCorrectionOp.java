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

package org.esa.beam.preprocessor;

import com.bc.ceres.core.Assert;
import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.Tile;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.framework.gpf.annotations.SourceProduct;
import org.esa.beam.framework.gpf.annotations.TargetProduct;
import org.esa.beam.preprocessor.equalization.EqualizationAlgorithm;
import org.esa.beam.preprocessor.equalization.ReprocessingVersion;
import org.esa.beam.preprocessor.smilecorr.SmileCorrectionAlgorithm;
import org.esa.beam.preprocessor.smilecorr.SmileCorrectionAuxdata;
import org.esa.beam.util.ProductUtils;
import org.esa.beam.util.math.RsMathUtils;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static org.esa.beam.dataio.envisat.EnvisatConstants.*;


@OperatorMetadata(alias = "CorrectMerisRadiometry",
                  description = "Performs radiometric corrections on MERIS L1b data products.",
                  authors = "Marc Bouvet (ESTEC), Marco Peters (Brockmann Consult), Marco Zuehlke (Brockmann Consult)," +
                            "Thomas Storm (Brockmann Consult)",
                  copyright = "(c) 2010 by Brockmann Consult",
                  version = "1.0")
public class MerisRadiometryCorrectionOp extends Operator {

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

    private static final String UNIT_DL = "dl";
    private static final String INVALID_MASK_NAME = "invalid";
    private static final String LAND_MASK_NAME = "land";

    private EqualizationAlgorithm equalizationAlgorithm;
    private SmileCorrectionAlgorithm smileCorrectionAlgorithm;
    private HashMap<String, String> bandNameMap;

    @Override
    public void initialize() throws OperatorException {
        initAlgorithms();
        validateSourceProduct();
        createTargetProduct();
    }

    @Override
    public void computeTile(Band targetBand, Tile targetTile, ProgressMonitor pm) throws OperatorException {
        final Rectangle targetRegion = targetTile.getRectangle();
        final int spectralIndex = targetBand.getSpectralBandIndex();
        final String sourceBandName = bandNameMap.get(targetBand.getName());
        final Band sourceBand = sourceProduct.getBand(sourceBandName);
        final Tile sourceBandTile = loadSourceTile(sourceBandName, targetRegion);
        Tile detectorSourceTile = null;
        if (doSmile || doEqualization) {
            detectorSourceTile = loadSourceTile(MERIS_DETECTOR_INDEX_DS_NAME, targetRegion);
        }
        Tile sunZenithTile = null;
        if (doRadToRefl) {
            sunZenithTile = loadSourceTile(MERIS_SUN_ZENITH_DS_NAME, targetRegion);
        }

        Tile[] radianceTiles = new Tile[0];
        Tile landMaskTile = null;
        Tile invalidMaskTile = null;
        if (doSmile) {
            radianceTiles = loadRequiredRadianceTiles(spectralIndex, targetRegion);
            invalidMaskTile = loadSourceTile(INVALID_MASK_NAME, targetRegion);
            landMaskTile = loadSourceTile(LAND_MASK_NAME, targetRegion);
        }

        pm.beginTask("Performing MERIS preprocessing...", targetTile.getHeight());
        try {
            for (int y = targetTile.getMinY(); y <= targetTile.getMaxY(); y++) {
                checkForCancellation(pm);
                for (int x = targetTile.getMinX(); x <= targetTile.getMaxX(); x++) {

                    int detectorIndex = -1;
                    if (doSmile || doEqualization) {
                        detectorIndex = detectorSourceTile.getSampleInt(x, y);
                    }
                    double sample = sourceBandTile.getSampleDouble(x, y);
                    if (doSmile && !invalidMaskTile.getSampleBoolean(x, y) && detectorIndex != -1) {
                        sample = smileCorrectionAlgorithm.correct(x, y, spectralIndex, detectorIndex, radianceTiles,
                                                                  landMaskTile.getSampleBoolean(x, y));
                    }
                    if (doRadToRefl) {
                        final float solarFlux = sourceBand.getSolarFlux();
                        final double sunZenithSample = sunZenithTile.getSampleDouble(x, y);
                        sample = RsMathUtils.radianceToReflectance((float) sample, (float) sunZenithSample, solarFlux);
                    }
                    if (doEqualization && detectorIndex != -1) {
                        sample = equalizationAlgorithm.performEqualization(sample, spectralIndex, detectorIndex);
                    }
                    targetTile.setSample(x, y, sample);
                }
                pm.worked(1);
            }
        } finally {
            pm.done();
        }
    }

    private void createTargetProduct() {
        final String productType = String.format("%s_Preprocessed", sourceProduct.getProductType());
        final String productDescription = "MERIS L1b Preprocessed";
        final String targetBandPrefix;
        final String bandDescriptionPrefix;
        if (doRadToRefl) {
            targetBandPrefix = "reflec";
            bandDescriptionPrefix = "Preprocessed TOA reflectance band";
        } else {
            targetBandPrefix = "radiance";
            bandDescriptionPrefix = "Preprocessed TOA radiance band";
        }

        final int rasterWidth = sourceProduct.getSceneRasterWidth();
        final int rasterHeight = sourceProduct.getSceneRasterHeight();
        targetProduct = new Product(String.format("%s_Preprocessed", sourceProduct.getName()), productType,
                                    rasterWidth, rasterHeight);
        targetProduct.setDescription(productDescription);
        ProductUtils.copyMetadata(sourceProduct, targetProduct);
        ProductUtils.copyTiePointGrids(sourceProduct, targetProduct);


        targetProduct.setAutoGrouping(targetBandPrefix);

        bandNameMap = new HashMap<String, String>();
        List<String> sourceSpectralBandNames = getSpectralBandNames(sourceProduct);
        for (String spectralBandName : sourceSpectralBandNames) {
            final Band sourceBand = sourceProduct.getBand(spectralBandName);
            final int bandIndex = sourceBand.getSpectralBandIndex() + 1;
            final String targetBandName = String.format("%s_%d", targetBandPrefix, bandIndex);
            final Band targetBand = targetProduct.addBand(targetBandName, ProductData.TYPE_FLOAT32);
            bandNameMap.put(targetBandName, spectralBandName);
            targetBand.setDescription(String.format("%s %d", bandDescriptionPrefix, bandIndex));
            if (doRadToRefl) {
                targetBand.setUnit(UNIT_DL);
            } else {
                targetBand.setUnit(sourceBand.getUnit());
            }
            targetBand.setValidPixelExpression(sourceBand.getValidPixelExpression());
            ProductUtils.copySpectralBandProperties(sourceBand, targetBand);
        }

        copyBand(MERIS_DETECTOR_INDEX_DS_NAME);
        ProductUtils.copyFlagBands(sourceProduct, targetProduct);
        final Band sourceFlagBand = sourceProduct.getBand(MERIS_L1B_FLAGS_DS_NAME);
        final Band targetFlagBand = targetProduct.getBand(MERIS_L1B_FLAGS_DS_NAME);

        targetFlagBand.setSourceImage(sourceFlagBand.getSourceImage());
        ProductUtils.copyGeoCoding(sourceProduct, targetProduct);
        targetProduct.setStartTime(sourceProduct.getStartTime());
        targetProduct.setEndTime(sourceProduct.getEndTime());

        // copy all bands not yet considered
        final String[] bandNames = sourceProduct.getBandNames();
        for (String bandName : bandNames) {
            if (!targetProduct.containsBand(bandName) && !sourceSpectralBandNames.contains(bandName)) {
                copyBand(bandName);
            }
        }
    }

    private void initAlgorithms() {
        if (doSmile) {
            try {
                smileCorrectionAlgorithm = new SmileCorrectionAlgorithm(SmileCorrectionAuxdata.loadAuxdata(
                        sourceProduct.getProductType()));
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

    private Tile[] loadRequiredRadianceTiles(int spectralBandIndex, Rectangle targetRectangle) {
        final int[] requiredBandIndices = smileCorrectionAlgorithm.computeRequiredBandIndexes(spectralBandIndex);
        Tile[] radianceTiles = new Tile[MERIS_L1B_NUM_SPECTRAL_BANDS];
        for (int requiredBandIndex : requiredBandIndices) {
            final Band band = sourceProduct.getBandAt(requiredBandIndex);
            radianceTiles[requiredBandIndex] = getSourceTile(band, targetRectangle, ProgressMonitor.NULL);
        }
        return radianceTiles;
    }

    private Tile loadSourceTile(String sourceNodeName, Rectangle rectangle) {
        final RasterDataNode sourceNode = sourceProduct.getRasterDataNode(sourceNodeName);
        return getSourceTile(sourceNode, rectangle, ProgressMonitor.NULL);
    }

    private void copyBand(String sourceBandName) {
        final Band destBand = ProductUtils.copyBand(sourceBandName, sourceProduct, targetProduct);
        Band srcBand = sourceProduct.getBand(sourceBandName);
        destBand.setSourceImage(srcBand.getSourceImage());
    }

    private List<String> getSpectralBandNames(Product sourceProduct) {
        final Band[] bands = sourceProduct.getBands();
        final List<String> spectralBandNames = new ArrayList<String>(bands.length);
        for (Band band : bands) {
            if (band.getSpectralBandIndex() != -1) {
                spectralBandNames.add(band.getName());
            }
        }
        return spectralBandNames;
    }

    public static class Spi extends OperatorSpi {

        public Spi() {
            super(MerisRadiometryCorrectionOp.class);
        }

    }

}
