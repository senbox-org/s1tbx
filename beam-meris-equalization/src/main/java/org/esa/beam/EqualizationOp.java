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

package org.esa.beam;

import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.core.SubProgressMonitor;
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
import org.esa.beam.util.Guardian;
import org.esa.beam.util.ProductUtils;
import org.esa.beam.util.math.RsMathUtils;

import java.awt.Rectangle;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static org.esa.beam.dataio.envisat.EnvisatConstants.*;


@OperatorMetadata(alias = "Equalize",
                  description = "Performs removal of detector-to-detector systematic " +
                                "radiometric differences in MERIS L1b data products.",
                  authors = "Marc Bouvet (ESTEC), Marco Peters (Brockmann Consult)",
                  copyright = "(c) 2010 by Brockmann Consult",
                  version = "1.0")
public class EqualizationOp extends Operator {

    @Parameter(label = "Reprocessing version", valueSet = {"AUTO_DETECT", "REPROCESSING_2", "REPROCESSING_3"},
               defaultValue = "AUTO_DETECT",
               description = "The version of the reprocessing the product comes from.")
    private ReprocessingVersion reproVersion;

    @Parameter(defaultValue = "true",
               label = "Perform SMILE correction",
               description = "Whether to perform SMILE correction or not.")
    private boolean doSmile;

    @Parameter(defaultValue = "true",
               label = "Perform radiance-to-reflectance conversion",
               description = "Whether to perform radiance-to-reflectance conversion or not.")
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

    private SmileAlgorithm smileAlgorithm;
    private HashMap<String, String> bandNameMap;
    private EqualizationAlgorithm equalizationAlgorithm;


    @Override
    public void initialize() throws OperatorException {
        final String msgPattern = "Source product must contain '%s'.";
        Guardian.assertTrue(String.format(msgPattern, MERIS_DETECTOR_INDEX_DS_NAME),
                            sourceProduct.containsBand(MERIS_DETECTOR_INDEX_DS_NAME));
        Guardian.assertTrue(String.format(msgPattern, MERIS_L1B_FLAGS_DS_NAME),
                            sourceProduct.containsBand(MERIS_L1B_FLAGS_DS_NAME));
        Guardian.assertTrue(String.format(msgPattern, MERIS_SUN_ZENITH_DS_NAME),
                            sourceProduct.containsRasterDataNode(MERIS_SUN_ZENITH_DS_NAME));
        Guardian.assertTrue("Source product must be of type MERIS L1b.",
                            MERIS_L1_TYPE_PATTERN.matcher(sourceProduct.getProductType()).matches());
        Guardian.assertTrue("Source product does not contain radiance bands.", containsRadianceBands(sourceProduct));
        final ProductData.UTC startTime = sourceProduct.getStartTime();
        Guardian.assertNotNull("Source product must have a start time", startTime);


        equalizationAlgorithm = new EqualizationAlgorithm(sourceProduct, reproVersion);

        try {
            smileAlgorithm = new SmileAlgorithm(sourceProduct.getProductType());
        } catch (IOException e) {
            throw new OperatorException("Not able to initialise SMILE algorithm.", e);
        }

        // create the target product
        final String productType;
        final String productDescription;
        final String targetBandPrefix;
        final String bandDescriptionPrefix;
        if (doRadToRefl) {
            productType = String.format("%s_EQ", sourceProduct.getProductType());
            productDescription = "MERIS Equalized TOA Reflectance";
            targetBandPrefix = "reflec";
            bandDescriptionPrefix = "Equalized TOA reflectance band";
        } else {
            productType = sourceProduct.getProductType();
            productDescription = "MERIS Equalized TOA Radiance";
            targetBandPrefix = "radiance";
            bandDescriptionPrefix = "Equalized TOA radiance band";
        }

        final int rasterWidth = sourceProduct.getSceneRasterWidth();
        final int rasterHeight = sourceProduct.getSceneRasterHeight();
        targetProduct = new Product(String.format("%s_Equalized", sourceProduct.getName()), productType,
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
            targetBand.setUnit(UNIT_DL);
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

    private boolean containsRadianceBands(Product product) {
        for (String name : MERIS_L1B_SPECTRAL_BAND_NAMES) {
            product.containsBand(name);
            if (!product.containsBand(name)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void computeTile(Band targetBand, Tile targetTile, ProgressMonitor pm) throws OperatorException {
        pm.beginTask("Performing equalization...", 7);
        final Rectangle targetRegion = targetTile.getRectangle();
        final int spectralIndex = targetBand.getSpectralBandIndex();
        try {
            final String sourceBandName = bandNameMap.get(targetBand.getName());
            final Band sourceBand = sourceProduct.getBand(sourceBandName);
            final Tile sourceBandTile = loadSourceTile(sourceBandName, targetRegion, pm);
            final Tile detectorSourceTile = loadSourceTile(MERIS_DETECTOR_INDEX_DS_NAME, targetRegion, pm);
            final Tile sunZenithTile = loadSourceTile(MERIS_SUN_ZENITH_DS_NAME, targetRegion, pm);

            Tile[] radianceTiles = new Tile[0];
            Tile landMaskTile = null;
            Tile invalidMaskTile = null;
            if (doSmile) {
                radianceTiles = loadRequiredRadianceTiles(spectralIndex, targetRegion, new SubProgressMonitor(pm, 1));
                invalidMaskTile = loadSourceTile(INVALID_MASK_NAME, targetRegion, pm);
                landMaskTile = loadSourceTile(LAND_MASK_NAME, targetRegion, pm);
            }

            for (int y = targetTile.getMinY(); y <= targetTile.getMaxY(); y++) {
                checkForCancellation(pm);
                for (int x = targetTile.getMinX(); x <= targetTile.getMaxX(); x++) {

                    final int detectorIndex = detectorSourceTile.getSampleInt(x, y);
                    if (detectorIndex != -1) {
                        double sourceSample = sourceBandTile.getSampleDouble(x, y);
                        if (doSmile && !invalidMaskTile.getSampleBoolean(x, y)) {
                            sourceSample = smileAlgorithm.correct(x, y, spectralIndex,
                                                                  detectorIndex, radianceTiles,
                                                                  landMaskTile.getSampleBoolean(x, y));
                        }
                        if (doRadToRefl) {
                            final float solarFlux = sourceBand.getSolarFlux();
                            final double sunZenithSample = sunZenithTile.getSampleDouble(x, y);
                            sourceSample = RsMathUtils.radianceToReflectance((float) sourceSample,
                                                                             (float) sunZenithSample,
                                                                             solarFlux);
                        }
                        double equalizedResult = equalizationAlgorithm.performEqualization(sourceSample, spectralIndex,
                                                                                           detectorIndex);
                        targetTile.setSample(x, y, equalizedResult);
                    }
                }
            }
            pm.worked(1);
        } finally {
            pm.done();
        }
    }


    private Tile[] loadRequiredRadianceTiles(int spectralBandIndex, Rectangle targetRectangle, ProgressMonitor pm) {
        final int[] requiredBandIndices = smileAlgorithm.computeRequiredBandIndexes(spectralBandIndex);
        Tile[] radianceTiles = new Tile[MERIS_L1B_NUM_SPECTRAL_BANDS];
        pm.beginTask("Loading radiance tiles...", requiredBandIndices.length);
        try {
            for (int requiredBandIndex : requiredBandIndices) {
                final Band band = sourceProduct.getBandAt(requiredBandIndex);
                radianceTiles[requiredBandIndex] = getSourceTile(band, targetRectangle, new SubProgressMonitor(pm, 1));
            }
        } finally {
            pm.done();
        }
        return radianceTiles;
    }

    private Tile loadSourceTile(String sourceNodeName, Rectangle rectangle, ProgressMonitor pm) {
        final RasterDataNode sourceNode = sourceProduct.getRasterDataNode(sourceNodeName);
        return getSourceTile(sourceNode, rectangle, new SubProgressMonitor(pm, 1));
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
            super(EqualizationOp.class);
        }

    }

}
