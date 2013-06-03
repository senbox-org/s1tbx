/*
 * Copyright (C) 2013 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.nest.gpf;

import com.bc.ceres.core.ProgressMonitor;
import junit.framework.TestCase;
import org.esa.beam.framework.datamodel.*;
import org.esa.beam.framework.gpf.GPF;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.nest.datamodel.Unit;

/**
 * Unit test for GCPSelectionOp.
 */
public class TestGCPSelectionOp extends TestCase {

    private OperatorSpi spi;

    @Override
    protected void setUp() throws Exception {
        spi = new GCPSelectionOp.Spi();
        GPF.getDefaultInstance().getOperatorSpiRegistry().addOperatorSpi(spi);
    }

    @Override
    protected void tearDown() throws Exception {
        GPF.getDefaultInstance().getOperatorSpiRegistry().removeOperatorSpi(spi);
    }

    public void testOperator() throws Exception {

        final Product product = createTestMasterProduct(40,40);

        final ProductNodeGroup<Placemark> masterGcpGroup = product.getGcpGroup(product.getBandAt(0));
        assertTrue(masterGcpGroup.getNodeCount() == 1);

        final GCPSelectionOp op = (GCPSelectionOp)spi.createOperator();
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

        final ProductNodeGroup<Placemark> targetGcpGroup = targetProduct.getGcpGroup(targetProduct.getBandAt(1));
        //assertTrue(targetGcpGroup.getNodeCount() == 1);

        final Placemark pin = targetGcpGroup.get(0);
        final PixelPos pixelPos = pin.getPixelPos();
        assertTrue(Double.compare(pixelPos.x, 16.0f) == 0);
        assertTrue(Double.compare(pixelPos.y, 21.0f) == 0);
    }

    private static Product createTestMasterProduct(int w, int h) {

        final Product product = new Product("p", "ASA_IMP_1P", w, h);

        // create a band: sinc function centre is at (19, 19)
        final Band band = product.addBand("amplitude_mst", ProductData.TYPE_FLOAT32);
        band.setUnit(Unit.AMPLITUDE);
        final float[] floatValues = new float[w * h];
        int i;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                i = y*w + x;
                floatValues[i] = sinc((float)(x - w/2 + 1) / 4.0f)*sinc((float)(y - h/2 + 1) / 4.0f);
            }
        }
        band.setData(ProductData.createInstance(floatValues));

        final Band slvBand = createTestSlaveBand(w, h);
        product.addBand(slvBand);

        // create lat/lon tie point grids
        final float[] lat = new float[w*h];
        final float[] lon = new float[w*h];
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                i = y*w + x;
                lon[i] = 13.20f;
                lat[i] = 51.60f;
            }
        }
        final TiePointGrid latGrid = new TiePointGrid(OperatorUtils.TPG_LATITUDE, w, h, 0, 0, 1, 1, lat);
        final TiePointGrid lonGrid = new TiePointGrid(OperatorUtils.TPG_LONGITUDE, w, h, 0, 0, 1, 1, lon);
        product.addTiePointGrid(latGrid);
        product.addTiePointGrid(lonGrid);

        // create Geo coding
        product.setGeoCoding(new TiePointGeoCoding(latGrid, lonGrid));

        // create GCP
        final ProductNodeGroup<Placemark> masterGcpGroup = product.getGcpGroup(band);
        final Placemark pin1 = Placemark.createPointPlacemark(
                           GcpDescriptor.getInstance(),
                           "gcp_1",
                           "GCP 1",
                           "",
                           new PixelPos(19.0f, 19.0f),
                           new GeoPos(lat[w*h/2], lon[w*h/2]),
                           product.getGeoCoding());

        masterGcpGroup.add(pin1);

        return product;
    }

    private static Band createTestSlaveBand(int w, int h) {

        // create a band: sinc function centre is at (16, 21)
        final Band band = new Band("amplitude_slv", ProductData.TYPE_FLOAT32, w, h);
        band.setUnit(Unit.AMPLITUDE);
        float[] floatValues = new float[w * h];
        int i;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                i = y*w + x;
                floatValues[i] = sinc((float)(x - w/2 + 4) / 4.0f)*sinc((float)(y - h/2 - 1) / 4.0f);
            }
        }
        band.setData(ProductData.createInstance(floatValues));

        return band;
    }

    private static float sinc(float x) {

        if (Float.compare(x, 0.0f) == 0) {
            return 0.0f;
        } else {
            return (float)(Math.sin(x*Math.PI) / (x*Math.PI));
        }
    }
}
