/*
 * $Id: ProductUtilsTest.java,v 1.2 2006/12/08 13:48:37 marcop Exp $
 *
 * Copyright (C) 2002 by Brockmann Consult (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation. This program is distributed in the hope it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */

package org.esa.beam.util;

import com.bc.ceres.core.ProgressMonitor;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.esa.beam.framework.datamodel.*;
import org.esa.beam.framework.dataop.maptransf.Datum;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class ProductUtilsTest extends TestCase {

    private static final float EPS = 1.0e-6f;


    public ProductUtilsTest(String testName) {
        super(testName);
    }

    public static Test suite() {
        return new TestSuite(ProductUtilsTest.class);
    }

    public void testCreateBufferedImageRGB() {
        final VirtualBand redRaster = new VirtualBand("bn", ProductData.TYPE_FLOAT32, 2, 3, "0");
        final VirtualBand greenRaster = new VirtualBand("bn", ProductData.TYPE_FLOAT32, 2, 3, "0");
        final VirtualBand blueRaster = new VirtualBand("bn", ProductData.TYPE_FLOAT32, 2, 3, "0");
        final float[] floatsr = new float[]{1, 2, 3, 4, 5, 6};
        final float[] floatsg = new float[]{1, 2, 3, 5, 6, 7};
        final float[] floatsb = new float[]{1, 2, 3, 6, 7, 8};
        redRaster.ensureRasterData();
        redRaster.setDataElems(floatsr);
        greenRaster.ensureRasterData();
        greenRaster.setDataElems(floatsg);
        blueRaster.ensureRasterData();
        blueRaster.setDataElems(floatsb);
        final VirtualBand[] virtualBands = new VirtualBand[]{redRaster, greenRaster, blueRaster};
        BufferedImage bufferedImageRGB = null;
        try {
            bufferedImageRGB = ProductUtils.createRgbImage(virtualBands, ProgressMonitor.NULL);
        } catch (IOException e) {
            fail("unexpected IOException");
        }

        assertNotNull(bufferedImageRGB);

//        Expected RGB values
//                 R        G        B          HEX
//        0,0  00000000 00000000 00000000    00 00 00
//        1,0  00110011 00101010 00100100    33 2A 24
//        0,1  01100110 01010101 01001001    66 55 49
//        1,1  10011001 10101010 10110110    99 AA B6
//        0,2  11001100 11010101 11011011    CC D5 DB
//        1,2  11111111 11111111 11111111    FF FF FF

        int rgb;
        final int withoutAlpha = 0x00ffffff;

        rgb = bufferedImageRGB.getRGB(0, 0);
        rgb = rgb & withoutAlpha;
        assertEquals(0x000000, rgb);

        rgb = bufferedImageRGB.getRGB(1, 0);
        rgb = rgb & withoutAlpha;
        assertEquals(0x332A24, rgb);

        rgb = bufferedImageRGB.getRGB(0, 1);
        rgb = rgb & withoutAlpha;
        assertEquals(0x665549, rgb);

        rgb = bufferedImageRGB.getRGB(1, 1);
        rgb = rgb & withoutAlpha;
        assertEquals(0x99AAB6, rgb);

        rgb = bufferedImageRGB.getRGB(0, 2);
        rgb = rgb & withoutAlpha;
        assertEquals(0xCCD5DB, rgb);

        rgb = bufferedImageRGB.getRGB(1, 2);
        rgb = rgb & withoutAlpha;
        assertEquals(0xFFFFFF, rgb);
    }

    public void testGetAngleSum() {
        double angleSum;

        angleSum = ProductUtils.getAngleSum(createPositiveRotationGeoPolygon(0));
        assertEquals(Math.PI * 2, angleSum, 1e-6);

        angleSum = ProductUtils.getAngleSum(createNegativeRotationGeoPolygon(0));
        assertEquals(Math.PI * 2 * -1, angleSum, 1e-6);
    }

    public void testGetRotationDirection() {
        int rotationDirection;

        rotationDirection = ProductUtils.getRotationDirection(createPositiveRotationGeoPolygon(0));
        assertEquals(1, rotationDirection);

        rotationDirection = ProductUtils.getRotationDirection(createNegativeRotationGeoPolygon(0));
        assertEquals(-1, rotationDirection);
    }

    public void testNormalizeGeoBoundary() {
        GeoPos[] boundary;
        GeoPos[] expected;
        int expectedNormalizing;

        // Area does not intersect 180 degree meridian
        // --> no modification expected
        // --> longitude min/max of actual area are fully within [-180,+180] after normalizing
        boundary = createPositiveRotationGeoPolygon(0);
        expected = createPositiveRotationGeoPolygon(0);
        expectedNormalizing = 0;
        assertNormalizing(boundary, expectedNormalizing, expected);

        // Area does not intersect 180 degree meridian
        // --> no modification expected
        // --> longitude min/max of actual area are fully within [-180,+180] after normalizing
        boundary = createNegativeRotationGeoPolygon(0);
        expected = createNegativeRotationGeoPolygon(0);
        expectedNormalizing = 0;
        assertNormalizing(boundary, expectedNormalizing, expected);

        // Area does intersect 180 degree meridian at the upper side
        // --> modification expected
        // --> longitude min/max of actual area are fully within [-180,+360] after normalizing
        boundary = createPositiveRotationGeoPolygon(175);
        ProductUtils.denormalizeGeoPolygon(boundary);
        expected = createPositiveRotationGeoPolygon(175);
        expectedNormalizing = 1;
        assertNormalizing(boundary, expectedNormalizing, expected);

        // Area does intersect 180 degree meridian at the upper side
        // --> modification expected
        // --> longitude min/max of actual area are fully within [-180,+360] after normalizing
        boundary = createNegativeRotationGeoPolygon(175);
        ProductUtils.denormalizeGeoPolygon(boundary);
        expected = createNegativeRotationGeoPolygon(175);
        expectedNormalizing = 1;
        assertNormalizing(boundary, expectedNormalizing, expected);

        // Area does intersect 180 degree meridian at the lower side
        // --> modification expected
        // --> longitude min/max of actual area are fully within [-360,+180] after normalizing
        boundary = createPositiveRotationGeoPolygon(-135);
        ProductUtils.denormalizeGeoPolygon(boundary);
        expected = createPositiveRotationGeoPolygon(-135);
        shiftGeoPolygon(expected, 360);
        expectedNormalizing = -1;
        assertNormalizing(boundary, expectedNormalizing, expected);

        // Area does intersect 180 degree meridian at the lower side
        // --> modification expected
        // --> longitude min/max of actual area are fully within [-360,+180] after normalizing
        boundary = createNegativeRotationGeoPolygon(-135);
        ProductUtils.denormalizeGeoPolygon(boundary);
        expected = createNegativeRotationGeoPolygon(-135);
        shiftGeoPolygon(expected, 360);
        expectedNormalizing = -1;
        assertNormalizing(boundary, expectedNormalizing, expected);
    }

    public void testDenormalizeGeoPos() {
        final GeoPos geoPos = new GeoPos();

        geoPos.lon = -678.2f;
        ProductUtils.denormalizeGeoPos(geoPos);
        assertEquals(-678.2f + 720.f, geoPos.lon, 1e-8);

        geoPos.lon = -540.108f;
        ProductUtils.denormalizeGeoPos(geoPos);
        assertEquals(-540.108f + 720.f, geoPos.lon, 1e-8);

        geoPos.lon = -539.67f;
        ProductUtils.denormalizeGeoPos(geoPos);
        assertEquals(-539.67f + 360.f, geoPos.lon, 1e-8);

        geoPos.lon = -256.98f;
        ProductUtils.denormalizeGeoPos(geoPos);
        assertEquals(-256.98f + 360.f, geoPos.lon, 1e-8);

        geoPos.lon = -180.3f;
        ProductUtils.denormalizeGeoPos(geoPos);
        assertEquals(-180.3f + 360.f, geoPos.lon, 1e-8);

        geoPos.lon = -179.4f;
        ProductUtils.denormalizeGeoPos(geoPos);
        assertEquals(-179.4f, geoPos.lon, 1e-8);

        geoPos.lon = -34;
        ProductUtils.denormalizeGeoPos(geoPos);
        assertEquals(-34, geoPos.lon, 1e-8);

        geoPos.lon = 0.34f;
        ProductUtils.denormalizeGeoPos(geoPos);
        assertEquals(0.34f, geoPos.lon, 1e-8);

        geoPos.lon = 114.9f;
        ProductUtils.denormalizeGeoPos(geoPos);
        assertEquals(114.9f, geoPos.lon, 1e-8);

        geoPos.lon = 184.4f;
        ProductUtils.denormalizeGeoPos(geoPos);
        assertEquals(184.4f - 360.f, geoPos.lon, 1e-8);

        geoPos.lon = 245.7f;
        ProductUtils.denormalizeGeoPos(geoPos);
        assertEquals(245.7f - 360.f, geoPos.lon, 1e-8);

        geoPos.lon = 536.9f;
        ProductUtils.denormalizeGeoPos(geoPos);
        assertEquals(536.9f - 360.f, geoPos.lon, 1e-8);

        geoPos.lon = 541.5f;
        ProductUtils.denormalizeGeoPos(geoPos);
        assertEquals(541.5f - 720.f, geoPos.lon, 1e-8);

        geoPos.lon = 722.5f;
        ProductUtils.denormalizeGeoPos(geoPos);
        assertEquals(722.5f - 720.f, geoPos.lon, 1e-8);
    }

    public void testNormalizing_crossingMeridianTwice() {
        GeoPos[] expected = createPositiveRotationDualMeridianGeoPolygon();
        GeoPos[] converted = createPositiveRotationDualMeridianGeoPolygon();

        ProductUtils.normalizeGeoPolygon(converted);
        ProductUtils.denormalizeGeoPolygon(converted);

        for (int i = 0; i < expected.length; i++) {
            assertEquals("at index " + i, expected[i], converted[i]);
        }
    }

    private void assertNormalizing(final GeoPos[] boundary, final int expectedNormalizing,
                                   final GeoPos[] expected) {
        final int normalized = ProductUtils.normalizeGeoPolygon(boundary);
        assertEquals(expectedNormalizing, normalized);
        for (int i = 0; i < expected.length; i++) {
            assertEquals("at index " + i, expected[i], boundary[i]);
        }
    }

    private static GeoPos[] createPositiveRotationGeoPolygon(int lonOffset) {
        return new GeoPos[]{
                new GeoPos(-60, -40 + lonOffset),
                new GeoPos(-80, +20 + lonOffset),
                new GeoPos(-40, +60 + lonOffset),
                new GeoPos(+20, +120 + lonOffset),
                new GeoPos(+60, +40 + lonOffset),
                new GeoPos(+80, -20 + lonOffset),
                new GeoPos(+40, -60 + lonOffset),
                new GeoPos(-20, -120 + lonOffset)
        };
    }

    private static GeoPos[] createNegativeRotationGeoPolygon(int lonOffset) {
        return new GeoPos[]{
                new GeoPos(-60, -40 + lonOffset),
                new GeoPos(-20, -120 + lonOffset),
                new GeoPos(+40, -60 + lonOffset),
                new GeoPos(+80, -20 + lonOffset),
                new GeoPos(+60, +40 + lonOffset),
                new GeoPos(+20, +120 + lonOffset),
                new GeoPos(-40, +60 + lonOffset),
                new GeoPos(-80, +20 + lonOffset)
        };
    }

    private static GeoPos[] createPositiveRotationDualMeridianGeoPolygon() {
        return new GeoPos[]{
                new GeoPos(20, -160),
                new GeoPos(30, -180),
                new GeoPos(30, 178),
                new GeoPos(40, 10),
                new GeoPos(50, -70),
                new GeoPos(70, -170),
                new GeoPos(80, 150),
                new GeoPos(60, 150),
                new GeoPos(57, 170),
                new GeoPos(50, -140),
                new GeoPos(30, 30),
                new GeoPos(25, 170),
                new GeoPos(10, -165),
        };
    }

    private void shiftGeoPolygon(final GeoPos[] geoPositions, final int lonOffset) {
        for (int i = 0; i < geoPositions.length; i++) {
            GeoPos geoPosition = geoPositions[i];
            geoPosition.lon += lonOffset;
        }
    }

    public static boolean printVectors(GeoPos[] positions) {
        final int length = positions.length;
        double angleSum = 0;
        for (int i = 0; i < length; i++) {
            GeoPos p1 = positions[i];
            GeoPos p2 = positions[(i + 1) % length];
            GeoPos p3 = positions[(i + 2) % length];
            double ax = p2.lon - p1.lon;
            double ay = p2.lat - p1.lat;
            double bx = p3.lon - p2.lon;
            double by = p3.lat - p2.lat;
            double a = Math.sqrt(ax * ax + ay * ay);
            double b = Math.sqrt(bx * bx + by * by);
            double cosAB = (ax * bx + ay * by) / (a * b);  // Skalarproduct geteilt durch Betragsprodukt
            double sinAB = (ax * by - ay * bx) / (a * b);  // Vektorproduct geteilt durch Betragsprodukt
            final double angle = Math.round(180 * Math.atan2(sinAB, cosAB) / Math.PI * 100) / 100;
            angleSum += angle;
//            System.out.println(
//                    "  P" + (i + 1) + ": cosAB = " + cosAB + ", sinAB = " + sinAB + ", r = " + r + ", angle = " + angle);
        }
        System.out.println("  angleSum = " + angleSum);
        return false;  // todo - change body of created method
    }

    public void testCopyTiePointGrids() {
        final Product sourceProduct = new Product("p1n", "p1t", 20, 20);

        final float[] tpg1tp = new float[]{
                1, 2, 3, 4, 5,
                6, 7, 8, 9, 10,
                11, 12, 13, 14, 15,
                16, 17, 18, 19, 20
        };
        final TiePointGrid tiePointGrid1 = new TiePointGrid("tpg1n", 5, 4, 2, 3, 4, 5, tpg1tp);
        tiePointGrid1.setDescription("tpg1d");
        tiePointGrid1.setUnit("tpg1u");
        sourceProduct.addTiePointGrid(tiePointGrid1);

        final float[] tpg2tp = new float[]{
                1, 2, 3, 4,
                5, 6, 7, 8,
                9, 10, 11, 12,
                13, 14, 15, 16,
                17, 18, 19, 20
        };
        final TiePointGrid tiePointGrid2 = new TiePointGrid("tpg2n", 4, 5, 1.2f, 1.4f, 5, 4, tpg2tp);
        tiePointGrid2.setDescription("tpg2d");
        tiePointGrid2.setUnit("tpg2u");
        sourceProduct.addTiePointGrid(tiePointGrid2);

        final Product targetProduct = new Product("p2n", "p2t", 200, 200);
        ProductUtils.copyTiePointGrids(sourceProduct, targetProduct);

        assertEquals(2, sourceProduct.getNumTiePointGrids());
        assertEquals("tpg1n", sourceProduct.getTiePointGridAt(0).getName());
        assertEquals(5, sourceProduct.getTiePointGridAt(0).getRasterWidth());
        assertEquals(4, sourceProduct.getTiePointGridAt(0).getRasterHeight());
        assertEquals(2.0f, sourceProduct.getTiePointGridAt(0).getOffsetX(), 1e-5);
        assertEquals(3.0f, sourceProduct.getTiePointGridAt(0).getOffsetY(), 1e-5);
        assertEquals(4.0f, sourceProduct.getTiePointGridAt(0).getSubSamplingX(), 1e-5);
        assertEquals(5.0f, sourceProduct.getTiePointGridAt(0).getSubSamplingY(), 1e-5);
        assertEquals(tpg1tp, sourceProduct.getTiePointGridAt(0).getDataElems());
        assertEquals("tpg2n", sourceProduct.getTiePointGridAt(1).getName());
        assertEquals(4, sourceProduct.getTiePointGridAt(1).getRasterWidth());
        assertEquals(5, sourceProduct.getTiePointGridAt(1).getRasterHeight());
        assertEquals(1.2f, sourceProduct.getTiePointGridAt(1).getOffsetX(), 1e-5);
        assertEquals(1.4f, sourceProduct.getTiePointGridAt(1).getOffsetY(), 1e-5);
        assertEquals(5.0f, sourceProduct.getTiePointGridAt(1).getSubSamplingX(), 1e-5);
        assertEquals(4.0f, sourceProduct.getTiePointGridAt(1).getSubSamplingY(), 1e-5);
        assertEquals(tpg2tp, sourceProduct.getTiePointGridAt(1).getDataElems());

        assertEquals(2, targetProduct.getNumTiePointGrids());
        assertEquals("tpg1n", targetProduct.getTiePointGridAt(0).getName());
        assertEquals(5, targetProduct.getTiePointGridAt(0).getRasterWidth());
        assertEquals(4, targetProduct.getTiePointGridAt(0).getRasterHeight());
        assertEquals(2.0f, targetProduct.getTiePointGridAt(0).getOffsetX(), 1e-5);
        assertEquals(3.0f, targetProduct.getTiePointGridAt(0).getOffsetY(), 1e-5);
        assertEquals(4.0f, targetProduct.getTiePointGridAt(0).getSubSamplingX(), 1e-5);
        assertEquals(5.0f, targetProduct.getTiePointGridAt(0).getSubSamplingY(), 1e-5);
        assertTrue(Arrays.equals(tpg1tp, (float[]) targetProduct.getTiePointGridAt(0).getDataElems()));
        assertEquals("tpg2n", targetProduct.getTiePointGridAt(1).getName());
        assertEquals(4, targetProduct.getTiePointGridAt(1).getRasterWidth());
        assertEquals(5, targetProduct.getTiePointGridAt(1).getRasterHeight());
        assertEquals(1.2f, targetProduct.getTiePointGridAt(1).getOffsetX(), 1e-5);
        assertEquals(1.4f, targetProduct.getTiePointGridAt(1).getOffsetY(), 1e-5);
        assertEquals(5.0f, targetProduct.getTiePointGridAt(1).getSubSamplingX(), 1e-5);
        assertEquals(4.0f, targetProduct.getTiePointGridAt(1).getSubSamplingY(), 1e-5);
        assertTrue(Arrays.equals(tpg2tp, (float[]) targetProduct.getTiePointGridAt(1).getDataElems()));
    }

    public void testCopyBandsForGeomTransform() {
        final int sourceWidth = 100;
        final int sourceHeight = 200;
        final Product source = new Product("source", "test", sourceWidth, sourceHeight);
        final TiePointGrid t1 = new TiePointGrid("t1", 10, 20, 0, 0, 10, 10, new float[10 * 20]);
        final TiePointGrid t2 = new TiePointGrid("t2", 10, 20, 0, 0, 10, 10, new float[10 * 20]);
        final Band b1 = new Band("b1", ProductData.TYPE_INT8, sourceWidth, sourceHeight);
        final Band b2 = new Band("b2", ProductData.TYPE_UINT16, sourceWidth, sourceHeight);
        final Band b3 = new Band("b3", ProductData.TYPE_FLOAT32, sourceWidth, sourceHeight);
        source.addTiePointGrid(t1);
        source.addTiePointGrid(t2);
        source.addBand(b1);
        source.addBand(b2);
        source.addBand(b3);

        final Map bandMapping = new HashMap();

        // Should NOT copy any bands because source is NOT geo-coded
        final Product target1 = new Product("target1", "test", 300, 400);
        ProductUtils.copyBandsForGeomTransform(source, target1, 0, bandMapping);
        assertEquals(0, target1.getNumBands());
        assertEquals(0, bandMapping.size());

        // Geo-code source
        source.setGeoCoding(new DGeoCoding());

        // Should copy bands because source is now geo-coded
        final Product target2 = new Product("dest", "test", 300, 400);
        ProductUtils.copyBandsForGeomTransform(source, target2, 0, bandMapping);
        assertEquals(3, target2.getNumBands());
        assertNotNull(target2.getBand("b1"));
        assertNotNull(target2.getBand("b2"));
        assertNotNull(target2.getBand("b3"));
        assertEquals(3, bandMapping.size());
        assertSame(b1, bandMapping.get(target2.getBand("b1")));
        assertSame(b2, bandMapping.get(target2.getBand("b2")));
        assertSame(b3, bandMapping.get(target2.getBand("b3")));

        bandMapping.clear();

        // Should copy tie-point grids as well because flag has been set
        final Product target3 = new Product("dest", "test", 300, 400);
        ProductUtils.copyBandsForGeomTransform(source, target3, true, 0, bandMapping);
        assertEquals(5, target3.getNumBands());
        assertNotNull(target3.getBand("t1"));
        assertNotNull(target3.getBand("t2"));
        assertNotNull(target3.getBand("b1"));
        assertNotNull(target3.getBand("b2"));
        assertNotNull(target3.getBand("b3"));
        assertEquals(5, bandMapping.size());
        assertSame(t1, bandMapping.get(target3.getBand("t1")));
        assertSame(t2, bandMapping.get(target3.getBand("t2")));
        assertSame(b1, bandMapping.get(target3.getBand("b1")));
        assertSame(b2, bandMapping.get(target3.getBand("b2")));
        assertSame(b3, bandMapping.get(target3.getBand("b3")));
    }


    public void testComputeSourcePixelCoordinates() {
        final PixelPos[] pixelCoords = ProductUtils.computeSourcePixelCoordinates(new ProductUtilsTest.SGeoCoding(),
                                                                                  2, 2,
                                                                                  new ProductUtilsTest.DGeoCoding(),
                                                                                  new Rectangle(0, 0, 3, 2));

        assertEquals(3 * 2, pixelCoords.length);

        testCoord(pixelCoords, 0, 0.5f, 0.5f);
        testCoord(pixelCoords, 1, 1.5f, 0.5f);
        assertNull(pixelCoords[2]);
        testCoord(pixelCoords, 3, 0.5f, 1.5f);
        testCoord(pixelCoords, 4, 1.5f, 1.5f);
        assertNull(pixelCoords[5]);
    }

    private void testCoord(final PixelPos[] pixelCoords, final int i, final float x, final float y) {
        assertNotNull(pixelCoords[i]);
        assertEquals(x, pixelCoords[i].x, EPS);
        assertEquals(y, pixelCoords[i].y, EPS);
    }


    public void testComputeMinMaxY() {
        // call with null
        try {
            ProductUtils.computeMinMaxY(null);
            fail();
        } catch (IllegalArgumentException expected) {
        }

        // call with pixel positions array width null elements
        final PixelPos[] pixelPositions = new PixelPos[5];

        assertNull(ProductUtils.computeMinMaxY(pixelPositions));

        // call with pixel positions array width one element
        pixelPositions[0] = new PixelPos(2.23f, 3.87f);
        float[] minMaxEqual = ProductUtils.computeMinMaxY(pixelPositions);

        assertEquals(2, minMaxEqual.length);
        assertEquals(minMaxEqual[0], minMaxEqual[1], 1.0e-5f);

        // call with full pixel positions array
        pixelPositions[1] = new PixelPos(3, 3.34f);
        pixelPositions[2] = new PixelPos(4, 4.54f);
        pixelPositions[3] = null;
        pixelPositions[4] = new PixelPos(6, 6.36f);

        float[] minMax = ProductUtils.computeMinMaxY(pixelPositions);

        assertEquals(2, minMax.length);
        assertEquals(3.34f, minMax[0], 1.0e-5f);
        assertEquals(6.36f, minMax[1], 1.0e-5f);
    }

    public static class SGeoCoding implements GeoCoding {

        public boolean isCrossingMeridianAt180() {
            return false;
        }

        public Datum getDatum() {
            return Datum.WGS_84;
        }

        public boolean canGetPixelPos() {
            return true;
        }

        public boolean canGetGeoPos() {
            return false;
        }

        public PixelPos getPixelPos(GeoPos geoPos, PixelPos pixelPos) {
            if (pixelPos == null) {
                pixelPos = new PixelPos();
            }
            pixelPos.x = geoPos.lon;
            pixelPos.y = geoPos.lat;
            return pixelPos;
        }

        public GeoPos getGeoPos(PixelPos pixelPos, GeoPos geoPos) {
            return geoPos;
        }

        public void dispose() {
        }
    }

    public static class DGeoCoding implements GeoCoding {

        public boolean isCrossingMeridianAt180() {
            return true;
        }

        public Datum getDatum() {
            return Datum.WGS_84;
        }

        public boolean canGetPixelPos() {
            return false;
        }

        public boolean canGetGeoPos() {
            return true;
        }

        public PixelPos getPixelPos(GeoPos geoPos, PixelPos pixelPos) {
            return pixelPos;
        }

        public GeoPos getGeoPos(PixelPos pixelPos, GeoPos geoPos) {
            if (geoPos == null) {
                geoPos = new GeoPos();
            }
            geoPos.lon = pixelPos.x;
            geoPos.lat = pixelPos.y;
            return geoPos;
        }

        public void dispose() {
        }
    }

    public void testCreateRectBoundary_usePixelCenter_false() {
        final boolean notUsePixelCenter = false;
        final PixelPos[] rectBoundary = ProductUtils.createRectBoundary(new Rectangle(2, 3, 15, 20), 7,
                                                                        notUsePixelCenter);
        assertEquals(12, rectBoundary.length);
        assertEquals(new PixelPos(2, 3), rectBoundary[0]);
        assertEquals(new PixelPos(9, 3), rectBoundary[1]);
        assertEquals(new PixelPos(16, 3), rectBoundary[2]);
        assertEquals(new PixelPos(17, 3), rectBoundary[3]);
        assertEquals(new PixelPos(17, 10), rectBoundary[4]);
        assertEquals(new PixelPos(17, 17), rectBoundary[5]);
        assertEquals(new PixelPos(17, 23), rectBoundary[6]);
        assertEquals(new PixelPos(16, 23), rectBoundary[7]);
        assertEquals(new PixelPos(9, 23), rectBoundary[8]);
        assertEquals(new PixelPos(2, 23), rectBoundary[9]);
        assertEquals(new PixelPos(2, 17), rectBoundary[10]);
        assertEquals(new PixelPos(2, 10), rectBoundary[11]);
    }

    public void testCreateRectBoundary_usePixelCenter_true() {
        final boolean usePixelCenter = true;
        final PixelPos[] rectBoundary = ProductUtils.createRectBoundary(new Rectangle(2, 3, 15, 20), 7, usePixelCenter);
        assertEquals(10, rectBoundary.length);
        assertEquals(new PixelPos(2.5f, 3.5f), rectBoundary[0]);
        assertEquals(new PixelPos(9.5f, 3.5f), rectBoundary[1]);
        assertEquals(new PixelPos(16.5f, 3.5f), rectBoundary[2]);
        assertEquals(new PixelPos(16.5f, 10.5f), rectBoundary[3]);
        assertEquals(new PixelPos(16.5f, 17.5f), rectBoundary[4]);
        assertEquals(new PixelPos(16.5f, 22.5f), rectBoundary[5]);
        assertEquals(new PixelPos(9.5f, 22.5f), rectBoundary[6]);
        assertEquals(new PixelPos(2.5f, 22.5f), rectBoundary[7]);
        assertEquals(new PixelPos(2.5f, 17.5f), rectBoundary[8]);
        assertEquals(new PixelPos(2.5f, 10.5f), rectBoundary[9]);
    }

    public void testCreateRectBoundary_without_usePixelCenter_Parameter() {
        final PixelPos[] rectBoundary = ProductUtils.createRectBoundary(new Rectangle(2, 3, 15, 20), 7);
        assertEquals(10, rectBoundary.length);
        assertEquals(new PixelPos(2.5f, 3.5f), rectBoundary[0]);
        assertEquals(new PixelPos(9.5f, 3.5f), rectBoundary[1]);
        assertEquals(new PixelPos(16.5f, 3.5f), rectBoundary[2]);
        assertEquals(new PixelPos(16.5f, 10.5f), rectBoundary[3]);
        assertEquals(new PixelPos(16.5f, 17.5f), rectBoundary[4]);
        assertEquals(new PixelPos(16.5f, 22.5f), rectBoundary[5]);
        assertEquals(new PixelPos(9.5f, 22.5f), rectBoundary[6]);
        assertEquals(new PixelPos(2.5f, 22.5f), rectBoundary[7]);
        assertEquals(new PixelPos(2.5f, 17.5f), rectBoundary[8]);
        assertEquals(new PixelPos(2.5f, 10.5f), rectBoundary[9]);
    }

    public void testCopyMetadata() {
        try {
            ProductUtils.copyMetadata((Product) null, (Product) null);
            fail();
        } catch (NullPointerException expected) {
        }
        try {
            ProductUtils.copyMetadata((MetadataElement) null, (MetadataElement) null);
            fail();
        } catch (NullPointerException expected) {
        }
        try {
            ProductUtils.copyMetadata(new MetadataElement("source"), null);
            fail();
        } catch (NullPointerException expected) {
        }
        try {
            ProductUtils.copyMetadata(null, new MetadataElement("target"));
            fail();
        } catch (NullPointerException expected) {
        }

        final MetadataElement source = new MetadataElement("source");
        final FlagCoding sourceChild = new FlagCoding("child");
        sourceChild.addFlag("a", 1, "condition a is true");
        sourceChild.addFlag("b", 2, "condition b is true");
        source.addElement(sourceChild);

        final MetadataElement target = new MetadataElement("target");
        ProductUtils.copyMetadata(source, target);

        final MetadataElement targetChild = target.getElement("child");
        assertNotSame(sourceChild, targetChild);
        assertNotSame(sourceChild.getAttribute("a"), targetChild.getAttribute("a"));
        assertNotSame(sourceChild.getAttribute("b"), targetChild.getAttribute("b"));
        assertNotSame(sourceChild.getAttribute("a").getData(), targetChild.getAttribute("a").getData());
        assertNotSame(sourceChild.getAttribute("b").getData(), targetChild.getAttribute("b").getData());

        assertNotNull(targetChild.getAttribute("a"));
        assertNotNull(targetChild.getAttribute("b"));

        assertEquals("a", targetChild.getAttribute("a").getName());
        assertEquals("condition a is true", targetChild.getAttribute("a").getDescription());
        assertEquals(1, targetChild.getAttribute("a").getData().getElemInt());

        assertEquals("b", targetChild.getAttribute("b").getName());
        assertEquals("condition b is true", targetChild.getAttribute("b").getDescription());
        assertEquals(2, targetChild.getAttribute("b").getData().getElemInt());
    }
}

