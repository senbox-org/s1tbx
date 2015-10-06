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

package org.esa.snap.core.datamodel;

import com.bc.ceres.core.ProgressMonitor;
import junit.framework.TestCase;
import org.esa.snap.core.dataio.AbstractProductReader;
import org.esa.snap.core.dataio.DecodeQualification;
import org.esa.snap.core.dataio.ProductReader;
import org.esa.snap.core.dataio.ProductReaderPlugIn;
import org.esa.snap.core.util.io.SnapFileFilter;

import java.awt.Rectangle;
import java.io.IOException;
import java.util.Locale;

public class RasterDataNodeIOTest extends TestCase {

    public final static TestProductReaderPlugIn TPRPI = new TestProductReaderPlugIn();

    private static final int SW = 5; // scene width
    private static final int SH = 5; // scene height
    private static final int GW = 3; // grid width
    private static final int GH = 3; // grid height

    private static final double SFAC = 2.5;
    private static final double SOFF = -1.25;

    private static final float DELTA = 1e-5f;

    private static final String FLOAT_BAND_NAME = "float_band";
    private static final String SCALED_USHORT_BAND_NAME = "scaled_ushort_band";
    private static final String BUFFERED_FLOAT_BAND_NAME = "buffered_float_band";
    private static final String TIE_POINT_GRID_NAME = "tie_point_grid";
    private static final String VIRT_BAND_NAME = "virt_band";
    private static final String VIRT_BAND_EXPR = "float_band + 1.0";

    private Product p;
    private TestProductReader pr;
    private Rectangle rectangle;

    @Override
    protected void setUp() {
        p = createTestProduct();
        pr = (TestProductReader) p.getProductReader();
        rectangle = new Rectangle(0, 0, SW, SH);
    }

    public void testThatSourceImagesGeneratedForBandsAreCached() throws IOException {
        final ProductData bpd = ProductData.createInstance(ProductData.TYPE_FLOAT32, SW * SH);
        final ProductData gpd = ProductData.createInstance(ProductData.TYPE_FLOAT32, GW * GH);

        assertEquals(0, pr.getNumReads());

        p.getTiePointGrid(TIE_POINT_GRID_NAME).readRasterData(0, 0, GW, GH, gpd, ProgressMonitor.NULL);
        assertEquals(0, pr.getNumReads());

        p.getBand(FLOAT_BAND_NAME).readRasterData(0, 0, SW, SH, bpd, ProgressMonitor.NULL);
        assertEquals(1, pr.getNumReads());

        p.getBand(FLOAT_BAND_NAME).ensureRasterData();
        assertEquals(1, pr.getNumReads());

        p.getBand(FLOAT_BAND_NAME).readRasterData(0, 0, SW, SH, bpd, ProgressMonitor.NULL);
        assertEquals(2, pr.getNumReads());

        p.getBand(VIRT_BAND_NAME).readRasterData(0, 0, SW, SH, bpd, ProgressMonitor.NULL);
        assertEquals(2, pr.getNumReads());

        p.getBand(FLOAT_BAND_NAME).unloadRasterData();
        p.getBand(VIRT_BAND_NAME).readRasterData(0, 0, SW, SH, bpd, ProgressMonitor.NULL);
        assertEquals(2, pr.getNumReads());

        p.getBand(BUFFERED_FLOAT_BAND_NAME).readRasterData(0, 0, SW, SH, bpd, ProgressMonitor.NULL);
        assertEquals(2, pr.getNumReads());

        p.getBand(FLOAT_BAND_NAME).readRasterData(0, 0, SW, SH, bpd, ProgressMonitor.NULL);
        assertEquals(3, pr.getNumReads());
    }

    public void testReadPixelsFromFloatBand() throws IOException {
        Band b = p.getBand(FLOAT_BAND_NAME);
        float[] floatElems = new float[SW * SH];
        b.readRasterData(rectangle.x, rectangle.y, rectangle.width, rectangle.height, ProductData.createInstance(floatElems), ProgressMonitor.NULL);
        int x, y;
        x = 0;
        y = 0;
        assertEquals(getRawPixelValue(x, y), floatElems[i(y, x)], DELTA);
        x = SW / 2;
        y = SH / 2;
        assertEquals(getRawPixelValue(x, y), floatElems[i(y, x)], DELTA);
        x = SW - 1;
        y = SH - 1;
        assertEquals(getRawPixelValue(x, y), floatElems[i(y, x)], DELTA);
    }

    private int i(int y, int x) {
        return y * SW + x;
    }

    public void testReadPixelsFromScaledIntBand() throws IOException {
        Band b = p.getBand(SCALED_USHORT_BAND_NAME);
        short[] intElems = new short[SW * SH];
        b.readRasterData(rectangle.x, rectangle.y, rectangle.width, rectangle.height, ProductData.createInstance(intElems), ProgressMonitor.NULL);
        int x, y;
        x = 0;
        y = 0;
        assertEquals(getRawPixelValue(x, y), intElems[i(y, x)], DELTA);
        x = SW / 2;
        y = SH / 2;
        assertEquals(getRawPixelValue(x, y), intElems[i(y, x)], DELTA);
        x = SW - 1;
        y = SH - 1;
        assertEquals(getRawPixelValue(x, y), intElems[i(y, x)], DELTA);
    }

    public void testReadPixelsFromTiePointGrid() throws IOException {
        TiePointGrid g = p.getTiePointGrid(TIE_POINT_GRID_NAME);
        float[] floatElems = new float[SW * SH];
        ProductData rasterData = ProductData.createInstance(floatElems);
        if (rasterData.getType() == ProductData.TYPE_FLOAT32) {
            g.readPixels(rectangle.x, rectangle.y, rectangle.width, rectangle.height, (float[]) rasterData.getElems(), ProgressMonitor.NULL);
        } else {
            float[] pixels = g.readPixels(rectangle.x, rectangle.y, rectangle.width, rectangle.height, (float[])null, ProgressMonitor.NULL);
            for (int i = 0; i < pixels.length; i++) {
                rasterData.setElemFloatAt(i, pixels[i]);
            }
        }
        int x, y;
        x = 0;
        y = 0;
        assertEquals(getTiePointValue(x / 2, y / 2), floatElems[i(y, x)], DELTA);
        x = SW / 2;
        y = SH / 2;
        assertEquals(getTiePointValue(x / 2, y / 2), floatElems[i(y, x)], DELTA);
        x = SW - 1;
        y = SH - 1;
        assertEquals(getTiePointValue(x / 2, y / 2), floatElems[i(y, x)], DELTA);

        // test interpolated values
        x = 1;
        y = 1;
        assertEquals(0.5f + getTiePointValue(x / 2, y / 2), floatElems[i(y, x)], DELTA);
        x = SW / 2 + 1;
        y = SH / 2 + 1;
        assertEquals(0.5f + getTiePointValue(x / 2, y / 2), floatElems[i(y, x)], DELTA);
        x = SW - 1 - 1;
        y = SH - 1 - 1;
        assertEquals(0.5f + getTiePointValue(x / 2, y / 2), floatElems[i(y, x)], DELTA);
    }

    private static int getRawPixelValue(int x, int y) {
        return 1 + x + y;
    }

    private static float getTiePointValue(int i, int j) {
        return 1.0f + 0.5f * (i + j);
    }

    private Product createTestProduct() {
        Product p = new Product("p", "t", SW, SH, TPRPI.createReaderInstance());

        final float[] tiePoints = new float[GW * GH];
        for (int j = 0; j < GH; j++) {
            for (int i = 0; i < GW; i++) {
                tiePoints[j * GW + i] = getTiePointValue(i, j);
            }
        }
        TiePointGrid tpg = new TiePointGrid(TIE_POINT_GRID_NAME,
                                            GW, GH, 0.5f, 0.5f,
                                            (SW - 1) / (float) (GW - 1), (SH - 1) / (float) (GH - 1),
                                            tiePoints);
        p.addTiePointGrid(tpg);

        Band floatBand = new Band(FLOAT_BAND_NAME, ProductData.TYPE_FLOAT32, SW, SH);
        p.addBand(floatBand);

        Band scaledIntBand = new Band(SCALED_USHORT_BAND_NAME, ProductData.TYPE_UINT16, SW, SH);
        scaledIntBand.setScalingFactor(SFAC);
        scaledIntBand.setScalingOffset(SOFF);
        p.addBand(scaledIntBand);

        Band syntFloatBand = new Band(BUFFERED_FLOAT_BAND_NAME, ProductData.TYPE_FLOAT32, SW, SH);
        syntFloatBand.ensureRasterData();
        p.addBand(syntFloatBand);

        VirtualBand virtBand = new VirtualBand(VIRT_BAND_NAME, ProductData.TYPE_FLOAT32, SW, SH, VIRT_BAND_EXPR);
        p.addBand(virtBand);

        return p;
    }

    private static class TestProductReaderPlugIn implements ProductReaderPlugIn {

        public DecodeQualification getDecodeQualification(Object input) {
            return DecodeQualification.UNABLE;
        }

        public Class[] getInputTypes() {
            return new Class[0];
        }

        public ProductReader createReaderInstance() {
            return new TestProductReader(this);
        }

        public SnapFileFilter getProductFileFilter() {
            return new SnapFileFilter(getFormatNames()[0], getDefaultFileExtensions(), getDescription(null));
        }

        public String[] getFormatNames() {
            return new String[0];
        }

        public String[] getDefaultFileExtensions() {
            return new String[0];
        }

        public String getDescription(Locale locale) {
            return null;
        }
    }

    private static class TestProductReader extends AbstractProductReader {

        private int _numReads;

        public TestProductReader(ProductReaderPlugIn readerPlugIn) {
            super(readerPlugIn);
        }

        public int getNumReads() {
            return _numReads;
        }

        @Override
        protected Product readProductNodesImpl() throws IOException{
            return null;
        }

        @Override
        protected void readBandRasterDataImpl(int sourceOffsetX, int sourceOffsetY, int sourceWidth, int sourceHeight,
                                              int sourceStepX, int sourceStepY, Band destBand, int destOffsetX,
                                              int destOffsetY, int destWidth, int destHeight,
                                              ProductData destBuffer,
                                              ProgressMonitor pm) throws IOException {
            _numReads++;
            pm.beginTask("Reading band raster data...", sourceHeight - sourceOffsetY);
            try {
                for (int sy = sourceOffsetY, j = 0; sy < sourceHeight; sy += sourceStepY, j++) {
                    int dy = destOffsetX + j;
                    for (int sx = sourceOffsetX, i = 0; sx < sourceWidth; sx += sourceStepX, i++) {
                        int dx = destOffsetX + i;
                        destBuffer.setElemFloatAt(dy * destWidth + dx, getRawPixelValue(sx, sy));
                    }
                    pm.worked(1);
                }
            } finally {
                pm.done();
            }
        }

    }
}
