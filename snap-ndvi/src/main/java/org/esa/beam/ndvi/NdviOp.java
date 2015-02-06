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

package org.esa.beam.ndvi;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.FlagCoding;
import org.esa.beam.framework.datamodel.MetadataAttribute;
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

import java.awt.Color;
import java.awt.Rectangle;
import java.util.Map;

/**
 * The <code>NdviOp</code> retrieves the Normalized Difference Vegetation Index (NDVI).
 *
 * @author Maximilian Aulinger
 */
@OperatorMetadata(
        alias = "NdviOp",
        category = "Optical Processing/Thematic Land Processing",
        description = "The retrieves the Normalized Difference Vegetation Index (NDVI).",
        authors = "Maximilian Aulinger, Thomas Storm",
        copyright = "Copyright (C) 2002-2014 by Brockmann Consult (info@brockmann-consult.de)")
public class NdviOp extends Operator {

    // constants
    public static final String NDVI_BAND_NAME = "ndvi";
    public static final String NDVI_FLAGS_BAND_NAME = "ndvi_flags";
    public static final String NDVI_ARITHMETIC_FLAG_NAME = "NDVI_ARITHMETIC";
    public static final String NDVI_LOW_FLAG_NAME = "NDVI_NEGATIVE";
    public static final String NDVI_HIGH_FLAG_NAME = "NDVI_SATURATION";
    public static final int NDVI_ARITHMETIC_FLAG_VALUE = 1;
    public static final int NDVI_LOW_FLAG_VALUE = 1 << 1;
    public static final int NDVI_HIGH_FLAG_VALUE = 1 << 2;

    @SourceProduct(alias = "source", description="The source product.")
    private Product sourceProduct;
    @TargetProduct
    private Product targetProduct;

    @Parameter(label = "Red factor", defaultValue = "1.0F", description = "The value of the red source band is multiplied by this value.")
    private float redFactor;

    @Parameter(label = "NIR factor", defaultValue = "1.0F", description = "The value of the NIR source band is multiplied by this value.")
    private float nirFactor;

    @Parameter(label = "Red source band",
               description = "The red band for the NDVI computation. If not provided, the " +
                             "operator will try to find the best fitting band.",
               rasterDataNodeType = Band.class)
    private String redSourceBand;

    @Parameter(label = "NIR source band",
               description = "The near-infrared band for the NDVI computation. If not provided," +
                             " the operator will try to find the best fitting band.",
               rasterDataNodeType = Band.class)
    private String nirSourceBand;


    @Override
    public void initialize() throws OperatorException {
        loadSourceBands(sourceProduct);
        int sceneWidth = sourceProduct.getSceneRasterWidth();
        int sceneHeight = sourceProduct.getSceneRasterHeight();
        targetProduct = new Product("ndvi", sourceProduct.getProductType() + "_NDVI", sceneWidth, sceneHeight);

        Band ndviOutputBand = new Band(NDVI_BAND_NAME, ProductData.TYPE_FLOAT32, sceneWidth,
                                       sceneHeight);
        targetProduct.addBand(ndviOutputBand);

        ProductUtils.copyTiePointGrids(sourceProduct, targetProduct);
        ProductUtils.copyGeoCoding(sourceProduct, targetProduct);
        ProductUtils.copyFlagBands(sourceProduct, targetProduct, true);

        FlagCoding ndviFlagCoding = createNdviFlagCoding();
        targetProduct.getFlagCodingGroup().add(ndviFlagCoding);

        Band ndviFlagsOutputBand = new Band(NDVI_FLAGS_BAND_NAME, ProductData.TYPE_INT32,
                                            sceneWidth, sceneHeight);
        ndviFlagsOutputBand.setDescription("NDVI specific flags");
        ndviFlagsOutputBand.setSampleCoding(ndviFlagCoding);
        targetProduct.addBand(ndviFlagsOutputBand);

        ProductUtils.copyMasks(sourceProduct, targetProduct);
        ProductUtils.copyOverlayMasks(sourceProduct, targetProduct);

        targetProduct.addMask(NDVI_ARITHMETIC_FLAG_NAME, (NDVI_FLAGS_BAND_NAME + "." + NDVI_ARITHMETIC_FLAG_NAME),
                              "An arithmetic exception occurred.",
                              Color.red.brighter(), 0.7);
        targetProduct.addMask(NDVI_LOW_FLAG_NAME, (NDVI_FLAGS_BAND_NAME + "." + NDVI_LOW_FLAG_NAME),
                              "NDVI value is too low.",
                              Color.red, 0.7);
        targetProduct.addMask(NDVI_HIGH_FLAG_NAME, (NDVI_FLAGS_BAND_NAME + "." + NDVI_HIGH_FLAG_NAME),
                              "NDVI value is too high.",
                              Color.red.darker(), 0.7);
    }

    @Override
    public void computeTileStack(Map<Band, Tile> targetTiles, Rectangle rectangle, ProgressMonitor pm) throws OperatorException {
        pm.beginTask("Computing NDVI", rectangle.height);
        try {
            Tile redTile = getSourceTile(getSourceProduct().getBand(redSourceBand), rectangle);
            Tile nirTile = getSourceTile(getSourceProduct().getBand(nirSourceBand), rectangle);

            Tile ndvi = targetTiles.get(targetProduct.getBand(NDVI_BAND_NAME));
            Tile ndviFlags = targetTiles.get(targetProduct.getBand(NDVI_FLAGS_BAND_NAME));

            float ndviValue;
            int ndviFlagsValue;

            for (int y = rectangle.y; y < rectangle.y + rectangle.height; y++) {
                for (int x = rectangle.x; x < rectangle.x + rectangle.width; x++) {
                    final float nir = nirFactor * nirTile.getSampleFloat(x, y);
                    final float red = redFactor * redTile.getSampleFloat(x, y);

                    ndviValue = (nir - red) / (nir + red);
                    ndviFlagsValue = 0;
                    if (Float.isNaN(ndviValue) || Float.isInfinite(ndviValue)) {
                        ndviFlagsValue |= NDVI_ARITHMETIC_FLAG_VALUE;
                        ndviValue = 0.0f;
                    }
                    if (ndviValue < 0.0f) {
                        ndviFlagsValue |= NDVI_LOW_FLAG_VALUE;
                    }
                    if (ndviValue > 1.0f) {
                        ndviFlagsValue |= NDVI_HIGH_FLAG_VALUE;
                    }
                    ndvi.setSample(x, y, ndviValue);
                    ndviFlags.setSample(x, y, ndviFlagsValue);
                }
                checkForCancellation();
                pm.worked(1);
            }
        } finally {
            pm.done();
        }
    }

    private void loadSourceBands(Product product) throws OperatorException {
        if (redSourceBand == null) {
            redSourceBand = findBand(600, 650, product);
            getLogger().info("Using band '" + redSourceBand + "' as red input band.");
        }
        if (nirSourceBand == null) {
            nirSourceBand = findBand(800, 900, product);
            getLogger().info("Using band '" + nirSourceBand + "' as NIR input band.");
        }
        if (redSourceBand == null) {
            throw new OperatorException("Unable to find band that could be used as red input band. Please specify band.");
        }
        if (nirSourceBand == null) {
            throw new OperatorException("Unable to find band that could be used as nir input band. Please specify band.");
        }
    }

    // package local for testing reasons only
    static String findBand(float minWavelength, float maxWavelength, Product product) {
        String bestBand = null;
        float bestBandLowerDelta = Float.MAX_VALUE;
        for (Band band : product.getBands()) {
            float bandWavelength = band.getSpectralWavelength();
            if (bandWavelength != 0.0F) {
                float lowerDelta = bandWavelength - minWavelength;
                if (lowerDelta < bestBandLowerDelta && bandWavelength <= maxWavelength && bandWavelength >= minWavelength) {
                    bestBand = band.getName();
                    bestBandLowerDelta = lowerDelta;
                }
            }
        }
        return bestBand;
    }

    private static FlagCoding createNdviFlagCoding() {

        FlagCoding ndviFlagCoding = new FlagCoding("ndvi_flags");
        ndviFlagCoding.setDescription("NDVI Flag Coding");

        MetadataAttribute attribute;

        attribute = new MetadataAttribute(NDVI_ARITHMETIC_FLAG_NAME, ProductData.TYPE_INT32);
        attribute.getData().setElemInt(NDVI_ARITHMETIC_FLAG_VALUE);
        attribute.setDescription("NDVI value calculation failed due to an arithmetic exception");
        ndviFlagCoding.addAttribute(attribute);

        attribute = new MetadataAttribute(NDVI_LOW_FLAG_NAME, ProductData.TYPE_INT32);
        attribute.getData().setElemInt(NDVI_LOW_FLAG_VALUE);
        attribute.setDescription("NDVI value is too low");
        ndviFlagCoding.addAttribute(attribute);

        attribute = new MetadataAttribute(NDVI_HIGH_FLAG_NAME, ProductData.TYPE_INT32);
        attribute.getData().setElemInt(NDVI_HIGH_FLAG_VALUE);
        attribute.setDescription("NDVI value is too high");
        ndviFlagCoding.addAttribute(attribute);

        return ndviFlagCoding;
    }

    public static class Spi extends OperatorSpi {

        public Spi() {
            super(NdviOp.class);
        }

    }
}
