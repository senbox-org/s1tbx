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

package org.esa.beam.gpf.operators.meris;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.framework.datamodel.*;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.Tile;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.annotations.SourceProduct;
import org.esa.beam.framework.gpf.annotations.TargetProduct;
import org.esa.beam.util.ProductUtils;

import java.awt.*;
import java.util.Map;

/**
 * The <code>NdviOp</code> uses MERIS Level-1b TOA radiances of bands 6 and 10
 * to retrieve the Normalized Difference Vegetation Index (NDVI).
 *
 * @author Maximilian Aulinger
 */
@OperatorMetadata(alias = "NdviSample", internal = true)
public class NdviOp extends Operator {

    // constants
    public static final String NDVI_PRODUCT_TYPE = "MER_NDVI2P";
    public static final String NDVI_BAND_NAME = "ndvi";
    public static final String NDVI_FLAGS_BAND_NAME = "ndvi_flags";
    public static final String NDVI_ARITHMETIC_FLAG_NAME = "NDVI_ARITHMETIC";
    public static final String NDVI_LOW_FLAG_NAME = "NDVI_NEGATIVE";
    public static final String NDVI_HIGH_FLAG_NAME = "NDVI_SATURATION";
    public static final int NDVI_ARITHMETIC_FLAG_VALUE = 1;
    public static final int NDVI_LOW_FLAG_VALUE = 1 << 1;
    public static final int NDVI_HIGH_FLAG_VALUE = 1 << 2;
    public static final String L1FLAGS_INPUT_BAND_NAME = "l1_flags";
    public static final String LOWER_INPUT_BAND_NAME = "radiance_6";
    public static final String UPPER_INPUT_BAND_NAME = "radiance_10";
    private Band _lowerInputBand;
    private Band _upperInputBand;

    @SourceProduct(alias = "input")
    private Product sourceProduct;
    @TargetProduct
    private Product targetProduct;

    @Override
    public void initialize() throws OperatorException {
        loadSourceBands(sourceProduct);
        int sceneWidth = sourceProduct.getSceneRasterWidth();
        int sceneHeight = sourceProduct.getSceneRasterHeight();
        // create the in memory represenation of the output product
        // ---------------------------------------------------------
        // the product itself
        targetProduct = new Product("ndvi", NDVI_PRODUCT_TYPE, sceneWidth, sceneHeight);

        // create and add the NDVI band
        Band ndviOutputBand = new Band(NDVI_BAND_NAME, ProductData.TYPE_FLOAT32, sceneWidth,
                                       sceneHeight);
        targetProduct.addBand(ndviOutputBand);

        // copy all tie point grids to output product
        ProductUtils.copyTiePointGrids(sourceProduct, targetProduct);

        // copy geo-coding and the lat/lon tiepoints to the output product
        ProductUtils.copyGeoCoding(sourceProduct, targetProduct);

        ProductUtils.copyFlagBands(sourceProduct, targetProduct, true);

        // create and add the NDVI flags coding
        FlagCoding ndviFlagCoding = createNdviFlagCoding();
        targetProduct.getFlagCodingGroup().add(ndviFlagCoding);

        // create and add the NDVI flags band
        Band ndviFlagsOutputBand = new Band(NDVI_FLAGS_BAND_NAME, ProductData.TYPE_INT32,
                                            sceneWidth, sceneHeight);
        ndviFlagsOutputBand.setDescription("NDVI specific flags");
        ndviFlagsOutputBand.setSampleCoding(ndviFlagCoding);
        targetProduct.addBand(ndviFlagsOutputBand);

        ProductUtils.copyMasks(sourceProduct, targetProduct);
        ProductUtils.copyOverlayMasks(sourceProduct, targetProduct);

        final Mask arithMask = Mask.BandMathsType.create(NDVI_ARITHMETIC_FLAG_NAME, "An arithmetic exception occured.",
                                                        sceneWidth, sceneHeight, 
                                                        (NDVI_FLAGS_BAND_NAME + "." + NDVI_ARITHMETIC_FLAG_NAME), Color.red.brighter(), 0.7);
        targetProduct.getMaskGroup().add(arithMask);
        final Mask lowMask = Mask.BandMathsType.create(NDVI_LOW_FLAG_NAME, "NDVI value is too low.",
                                                      sceneWidth, sceneHeight, 
                                                      (NDVI_FLAGS_BAND_NAME + "." + NDVI_LOW_FLAG_NAME), Color.red, 0.7);
        targetProduct.getMaskGroup().add(lowMask);
        final Mask highMask = Mask.BandMathsType.create(NDVI_HIGH_FLAG_NAME, "NDVI value is too high.",
                                                       sceneWidth, sceneHeight, 
                                                       (NDVI_FLAGS_BAND_NAME + "." + NDVI_HIGH_FLAG_NAME), Color.red.darker(), 0.7);
        targetProduct.getMaskGroup().add(highMask);
    }

    @Override
    public void computeTileStack(Map<Band, Tile> targetTiles, Rectangle rectangle, ProgressMonitor pm) throws OperatorException {
        pm.beginTask("Computing NDVI", rectangle.height);
        try {

            Tile lowerTile = getSourceTile(_lowerInputBand, rectangle);
            Tile upperTile = getSourceTile(_upperInputBand, rectangle);

            Tile ndvi = targetTiles.get(targetProduct.getBand(NDVI_BAND_NAME));
            Tile ndviFlags = targetTiles.get(targetProduct.getBand(NDVI_FLAGS_BAND_NAME));

            float ndviValue;
            int ndviFlagsValue;

            for (int y = rectangle.y; y < rectangle.y + rectangle.height; y++) {
                for (int x = rectangle.x; x < rectangle.x + rectangle.width; x++) {
                    final float upper = upperTile.getSampleFloat(x, y);
                    final float lower = lowerTile.getSampleFloat(x, y);
                    ndviValue = (upper - lower) / (upper + lower);
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
        _lowerInputBand = product.getBand(LOWER_INPUT_BAND_NAME);
        if (_lowerInputBand == null) {
            throw new OperatorException("Can not load band " + LOWER_INPUT_BAND_NAME);
        }

        _upperInputBand = product.getBand(UPPER_INPUT_BAND_NAME);
        if (_upperInputBand == null) {
            throw new OperatorException("Can not load band " + UPPER_INPUT_BAND_NAME);
        }

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
