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
package org.esa.s1tbx.insar.gpf.coregistration;

import com.bc.ceres.core.ProgressMonitor;
import org.apache.commons.math3.util.FastMath;
import org.esa.snap.core.datamodel.*;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.util.math.MathUtils;
import org.esa.snap.engine_utilities.datamodel.AbstractMetadata;
import org.esa.snap.engine_utilities.datamodel.Unit;
import org.esa.snap.engine_utilities.eo.Constants;
import org.esa.snap.engine_utilities.gpf.OperatorUtils;
import org.esa.snap.engine_utilities.util.TestUtils;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Unit test for GCPSelectionOp.
 */
public class TestCrossCorrelationOp {

    static {
        TestUtils.initTestEnvironment();
    }
    private final static OperatorSpi spi = new CrossCorrelationOp.Spi();

    @Test
    public void testOperator() throws Exception {

        final int sourceImageWidth = 200;
        final int sourceImageHeight = 200;
        final float xShift = 0.5f;
        final float yShift = -0.5f;
        final Product product = createTestMasterProduct(sourceImageWidth, sourceImageHeight, xShift, yShift);

        final ProductNodeGroup<Placemark> masterGcpGroup = GCPManager.instance().getGcpGroup(product.getBandAt(0));
        assertTrue(masterGcpGroup.getNodeCount() == 196);

        final CrossCorrelationOp op = (CrossCorrelationOp) spi.createOperator();
        assertNotNull(op);

        op.setSourceProduct(product);
        op.setTestParameters("32", "32", "2", "2", 2, 0.5);

        // get targetProduct gets initialize to be executed
        final Product targetProduct = op.getTargetProduct();
        assertNotNull(targetProduct);

        final Band band = targetProduct.getBandAt(1);
        assertNotNull(band);

        // readPixels gets computeTiles to be executed
        float[] floatValues = new float[1600];
        band.readPixels(0, 0, 40, 40, floatValues, ProgressMonitor.NULL);

        final ProductNodeGroup<Placemark> targetGcpGroup = GCPManager.instance().getGcpGroup(targetProduct.getBandAt(1));
        for (int i = 0; i < targetGcpGroup.getNodeCount(); ++i) {
            final String gcpName = targetGcpGroup.get(i).getName();
            final PixelPos slvPos = targetGcpGroup.get(i).getPixelPos();
            final PixelPos mstPos = masterGcpGroup.get(gcpName).getPixelPos();
            assertTrue(mstPos.x == slvPos.x + xShift);
            assertTrue(mstPos.y == slvPos.y + yShift);
        }
    }

    private static Product createTestMasterProduct(final int w, final int h, float xShift, float yShift) {

        final Product product = new Product("p", "ASA_IMP_1P", w, h);

        final MetadataElement absRoot = AbstractMetadata.getAbstractedMetadata(product);
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.coregistered_stack, 1);

        // create a band with random numbers between 0.0 and 1.0
        final Band band = product.addBand("amplitude_VV_mst", ProductData.TYPE_FLOAT32);
        band.setUnit(Unit.AMPLITUDE);
        final float[] floatValues = new float[w * h];
        int i;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                i = y * w + x;
                floatValues[i] = (float)Math.random();
            }
        }
        band.setData(ProductData.createInstance(floatValues));

        final Band slvBand = createTestSlaveBand(w, h, floatValues, xShift, yShift);
        product.addBand(slvBand);

        // create lat/lon tie point grids
        final float[] lat = new float[w * h];
        final float[] lon = new float[w * h];
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                i = y * w + x;
                lon[i] = 13.20f;
                lat[i] = 51.60f;
            }
        }
        final TiePointGrid latGrid = new TiePointGrid(OperatorUtils.TPG_LATITUDE, w, h, 0, 0, 1, 1, lat);
        final TiePointGrid lonGrid = new TiePointGrid(OperatorUtils.TPG_LONGITUDE, w, h, 0, 0, 1, 1, lon);
        product.addTiePointGrid(latGrid);
        product.addTiePointGrid(lonGrid);

        // create Geo coding
        product.setSceneGeoCoding(new TiePointGeoCoding(latGrid, lonGrid));

        // create GCP
        final ProductNodeGroup<Placemark> masterGcpGroup = GCPManager.instance().getGcpGroup(band);
        addGCPGrid(w, h, 200, masterGcpGroup, product.getSceneGeoCoding());

        return product;
    }

    private static Band createTestSlaveBand(
            final int w, final int h, final float[] mstValues, final float xShift, final float yShift) {

        final Band band = new Band("amplitude_VV_slv", ProductData.TYPE_FLOAT32, w, h);
        band.setUnit(Unit.AMPLITUDE);
        float[] floatValues = new float[w * h];
        int k = 0;
        for (int y = 0; y < h; y++) {
            float sy = y + yShift;
            int sy0, sy1;
            if (sy < 0.0f) {
                sy0 = 0;
                sy1 = 1;
            } else if (sy >= h - 1) {
                sy0 = h - 2;
                sy1 = h - 1;
            } else {
                sy0 = (int)sy;
                sy1 = sy0 + 1;
            }
            final float wy = sy - sy0;

            for (int x = 0; x < w; x++) {
                float sx = x + xShift;
                int sx0, sx1;
                if (sx < 0.0f) {
                    sx0 = 0;
                    sx1 = 1;
                } else if (sx >= w - 1) {
                    sx0 = w - 2;
                    sx1 = w - 1;
                } else {
                    sx0 = (int)sx;
                    sx1 = sx0 + 1;
                }
                final float wx = sx - sx0;

                double m00, m01, m10, m11;
                m00 = mstValues[sy0*w + sx0];
                m01 = mstValues[sy0*w + sx1];
                m10 = mstValues[sy1*w + sx0];
                m11 = mstValues[sy1*w + sx1];
                floatValues[k++] = (float)MathUtils.interpolate2D(wy, wx, m00, m10, m01, m11);
            }
        }
        band.setData(ProductData.createInstance(floatValues));

        return band;
    }

    private static float sinc(float x) {

        if (Float.compare(x, 0.0f) == 0) {
            return 0.0f;
        } else {
            return (float) (FastMath.sin(x * Constants.PI) / (x * Constants.PI));
        }
    }

    private static void addGCPGrid(final int width, final int height, final int numPins,
                                   final ProductNodeGroup<Placemark> group,
                                   final GeoCoding targetGeoCoding) {

        final double ratio = width / (double) height;
        final double n = Math.sqrt(numPins / ratio);
        final double m = ratio * n;
        final double spacingX = width / m;
        final double spacingY = height / n;
        final GcpDescriptor gcpDescriptor = GcpDescriptor.getInstance();

        group.removeAll();
        int pinNumber = group.getNodeCount() + 1;

        for (double y = spacingY / 2f; y < height; y += spacingY) {

            for (double x = spacingX / 2f; x < width; x += spacingX) {

                final String name = PlacemarkNameFactory.createName(gcpDescriptor, pinNumber);
                final String label = PlacemarkNameFactory.createLabel(gcpDescriptor, pinNumber, true);

                final Placemark newPin = Placemark.createPointPlacemark(gcpDescriptor,
                        name, label, "",
                        new PixelPos((int) x, (int) y), null,
                        targetGeoCoding);
                group.add(newPin);
                ++pinNumber;
            }
        }
    }
}
