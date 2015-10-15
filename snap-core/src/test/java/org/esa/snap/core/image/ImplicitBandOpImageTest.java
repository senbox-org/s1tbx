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

package org.esa.snap.core.image;

import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.glevel.MultiLevelImage;
import org.esa.snap.core.dataio.AbstractProductReader;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.util.jai.SingleBandedSampleModel;
import org.junit.Before;
import org.junit.Test;

import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.SampleModel;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

public class ImplicitBandOpImageTest {

    private static final int IMAGE_W = 10;
    private static final int IMAGE_H = 10;
    private static final int TILE_SIZE = 6;

    private Product product;
    private ProductFactory productFactory;

    @Before
    public void setup() throws IOException {
        productFactory = new ProductFactory();
        product = productFactory.readProductNodes(null, null);
    }

    @Test
    public void testGeophysicalImages() throws IOException {
        testGeophysicalImage("B_FLOAT64", DataBuffer.TYPE_DOUBLE);
        testGeophysicalImage("B_FLOAT32", DataBuffer.TYPE_FLOAT);

        testGeophysicalImage("B_UINT32", DataBuffer.TYPE_DOUBLE);
        testGeophysicalImage("B_UINT16", DataBuffer.TYPE_USHORT);
        testGeophysicalImage("B_UINT8", DataBuffer.TYPE_BYTE);

        testGeophysicalImage("B_INT32", DataBuffer.TYPE_INT);
        testGeophysicalImage("B_INT16", DataBuffer.TYPE_SHORT);
        testGeophysicalImage("B_INT8", DataBuffer.TYPE_SHORT);
    }

    private void testGeophysicalImage(String bandName, int dataBufferType) {
        final Band band = product.getBand(bandName);

        // the source of the geophysical image is the source image, which is a {@code BandOpImage}
        final MultiLevelImage geophysicalImage = band.getGeophysicalImage();

        final SampleModel sampleModel = geophysicalImage.getSampleModel();
        assertTrue(sampleModel instanceof SingleBandedSampleModel);
        assertEquals(dataBufferType, sampleModel.getDataType());

        final Raster[] rasters = geophysicalImage.getTiles();
        assertEquals(4, rasters.length);

        final double[] coefficients = productFactory.getCoefficients(band);
        testTileData(geophysicalImage.getTile(0, 0), coefficients);
        testTileData(geophysicalImage.getTile(1, 0), coefficients);
        testTileData(geophysicalImage.getTile(0, 1), coefficients);
        testTileData(geophysicalImage.getTile(1, 1), coefficients);
    }

    private static void testTileData(Raster tile, double[] coefficients) {
        final DataBuffer dataBuffer = tile.getDataBuffer();
        assertEquals(TILE_SIZE * TILE_SIZE, dataBuffer.getSize());

        for (int dbIndex = 0, pdIndex = 0; dbIndex < dataBuffer.getSize(); dbIndex++) {
            final int x = tile.getMinX() + dbIndex % tile.getWidth();
            final int y = tile.getMinY() + dbIndex / tile.getWidth();
            final double actual = dataBuffer.getElemDouble(dbIndex);
            if (x >= 0 && x < IMAGE_W && y >= 0 && y < IMAGE_H) {
                final double expected = coefficients[0] * pdIndex + coefficients[1];
                assertEquals("Inside image bounds: dataBuffer.getElemDouble(" + dbIndex + ")", expected, actual, 0.0);
                pdIndex++;
            } else {
                assertEquals("Outside image bounds: dataBuffer.getElemDouble(" + dbIndex + ")", 0.0, actual, 0.0);
            }
        }
    }

    private static class ProductFactory extends AbstractProductReader {

        final Map<Band, double[]> coefficients = new HashMap<Band, double[]>(10);

        ProductFactory() {
            super(null);
        }

        Band createBand(Product p, String bandName, int productDataType, double factor, double offset) {
            final Band band = p.addBand(bandName, productDataType);
            coefficients.put(band, new double[]{factor, offset});

            return band;
        }

        double[] getCoefficients(Band band) {
            return coefficients.get(band);
        }

        @Override
        protected Product readProductNodesImpl() throws IOException {
            final Product product = new Product("N", "T", IMAGE_W, IMAGE_H);
            product.setPreferredTileSize(TILE_SIZE, TILE_SIZE);
            createBand(product, "B_INT8", ProductData.TYPE_INT8, 1.0, -8.0);
            createBand(product, "B_UINT8", ProductData.TYPE_UINT8, 1.0, 8.0);
            createBand(product, "B_INT16", ProductData.TYPE_INT16, 1.0, -16.0);
            createBand(product, "B_UINT16", ProductData.TYPE_UINT16, 1.0, 16.0);
            createBand(product, "B_INT32", ProductData.TYPE_INT32, 1.0, -32.0);
            createBand(product, "B_UINT32", ProductData.TYPE_UINT32, 1.0, 32.0);
            createBand(product, "B_FLOAT32", ProductData.TYPE_FLOAT32, 1.0, 32.5);
            createBand(product, "B_FLOAT64", ProductData.TYPE_FLOAT64, 1.0, 64.5);

            return product;
        }

        @Override
        protected void readBandRasterDataImpl(int sourceOffsetX,
                                              int sourceOffsetY,
                                              int sourceWidth,
                                              int sourceHeight,
                                              int sourceStepX,
                                              int sourceStepY,
                                              Band destBand,
                                              int destOffsetX,
                                              int destOffsetY,
                                              int destWidth,
                                              int destHeight,
                                              ProductData destBuffer,
                                              ProgressMonitor pm) throws IOException {
            final double[] coefficients = getCoefficients(destBand);
            for (int i = 0; i < destBuffer.getNumElems(); i++) {
                destBuffer.setElemDoubleAt(i, coefficients[0] * i + coefficients[1]);
            }
        }
    }
}
