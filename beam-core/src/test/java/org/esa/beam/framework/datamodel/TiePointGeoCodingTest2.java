package org.esa.beam.framework.datamodel;

import junit.framework.TestCase;
import org.esa.beam.framework.dataop.maptransf.Datum;
import org.esa.beam.util.Debug;

import java.awt.geom.AffineTransform;

public class TiePointGeoCodingTest2 extends TestCase {

    // high error expected due to approximation
    public static final float PP_ERROR = 1e-3f;
    public static final float GP_ERROR = 1e-4f;

    private boolean _lastDebugState;

    protected void setUp() throws Exception {
        _lastDebugState = Debug.setEnabled(true);
    }

    protected void tearDown() throws Exception {
        Debug.setEnabled(_lastDebugState);
    }

    public void testMerisRRPositions() {
        testMerisRRPositions(0, 0, 0, false);
        testMerisRRPositions(0, -180, 0, true);
        testMerisRRPositions(0, -179, 0, true);
        testMerisRRPositions(0, -170.1, 0, true);
        testMerisRRPositions(0, -100, 0, false);
        testMerisRRPositions(0, +100, 0, false);
        testMerisRRPositions(0, +170.1, 0, true);
        testMerisRRPositions(0, +179, 0, true);
        testMerisRRPositions(0, +180, 0, true);

        testMerisRRPositions(-7, 0, 0, false);
        testMerisRRPositions(-7, -180, 0, true);
        testMerisRRPositions(-7, -179, 0, true);
        testMerisRRPositions(-7, -100, 0, false);
        testMerisRRPositions(-7, +100, 0, false);
        testMerisRRPositions(-7, +179, 0, true);
        testMerisRRPositions(-7, +180, 0, true);

        ////////////////////////////////////////////////////////////////
        // 90 degree rotation, check against east-most meridian (+180)

        // Let UL, UC & UR curtly miss the +180 degree meridian
        testMerisRRPositions(+90, +180 - 0.5 * 60 - 1, 0, false);
        // Let UL, UC & UR curtly pass the +180 degree meridian
        testMerisRRPositions(+90, +180 - 0.5 * 60 + 1, 0, true);
        // Let LL, LC & LR curtly miss the +180 degree meridian
        testMerisRRPositions(-90, +180 - 0.5 * 60 - 1, 0, false);
        // Let LL, LC & LR curtly pass the +180 degree meridian
        testMerisRRPositions(-90, +180 - 0.5 * 60 + 1, 0, true);

        ////////////////////////////////////////////////////////////////
        // 90 degree rotation, check against west-most meridian (-180)

        // Let UL, UC & UR curtly miss the -180 degree meridian
        testMerisRRPositions(+90, -180 + 0.5 * 60 + 1, 0, false);
        // Let UL, UC & UR curtly pass the -180 degree meridian
        testMerisRRPositions(+90, -180 + 0.5 * 60 - 1, 0, true);
        // Let LL, LC & LR curtly miss the -180 degree meridian
        testMerisRRPositions(-90, -180 + 0.5 * 60 + 1, 0, false);
        // Let LL, LC & LR curtly pass the -180 degree meridian
        testMerisRRPositions(-90, -180 + 0.5 * 60 - 1, 0, true);

        ////////////////////////////////////////////////////////////////
        // 45 degree rotation, check against east-most meridian (+180)

        // Let LR curtly miss the +180 degree meridian
        testMerisRRPositions(+45, +180 - 0.5 * (60 + 20) / Math.sqrt(2) - 1, 0, false);
        // Let LR curtly pass the +180 degree meridian
        testMerisRRPositions(+45, +180 - 0.5 * (60 + 20) / Math.sqrt(2) + 1, 0, true);
        // Let UR curtly miss the +180 degree meridian
        testMerisRRPositions(-45, +180 - 0.5 * (60 + 20) / Math.sqrt(2) - 1, 0, false);
        // Let UR curtly pass the +180 degree meridian
        testMerisRRPositions(-45, +180 - 0.5 * (60 + 20) / Math.sqrt(2) + 1, 0, true);

        ////////////////////////////////////////////////////////////////
        // 45 degree rotation, check against west-most meridian (-180)

        // Let UL curtly miss the -180 degree meridian
        testMerisRRPositions(+45, -180 + 0.5 * (60 + 20) / Math.sqrt(2) + 1, 0, false);
        // Let UL curtly pass the -180 degree meridian
        testMerisRRPositions(+45, -180 + 0.5 * (60 + 20) / Math.sqrt(2) - 1, 0, true);
        // Let LL curtly miss the -180 degree meridian
        testMerisRRPositions(-45, -180 + 0.5 * (60 + 20) / Math.sqrt(2) + 1, 0, false);
        // Let LL curtly pass the -180 degree meridian
        testMerisRRPositions(-45, -180 + 0.5 * (60 + 20) / Math.sqrt(2) - 1, 0, true);
    }

    public void testSelf() {
        TestSet ts = createMerisRRTestSet(0, +180, 0, true);
        assertEquals(+170, ts.gp[TestSet.UL].lon, 1.e-5f);
        assertEquals(+180, ts.gp[TestSet.UC].lon, 1.e-5f);
        assertEquals(-170, ts.gp[TestSet.UR].lon, 1.e-5f);
    }

    private TestSet createMerisRRTestSet(double rotationAngle,
                                         double lonOffset,
                                         double latOffset,
                                         boolean crossingMeridianAt180) {
        return new TestSet(
                1120 + 1, 3 * 1120 + 1,
                16, 16,
                20.0f, 3 * 20.0f,
                rotationAngle,
                lonOffset,
                latOffset, crossingMeridianAt180);
    }

    private void testMerisRRPositions(double rotationAngle,
                                      double lonOffset,
                                      double latOffset,
                                      boolean crossingMeridianAt180) {
        TestSet ts = createMerisRRTestSet(rotationAngle, lonOffset, latOffset, crossingMeridianAt180);
        testPositions(ts);
        testNormalization(ts);
    }

    private void testPositions(TestSet ts) {
        assertEquals(true, ts.gc.canGetGeoPos());
        assertEquals(true, ts.gc.canGetPixelPos());

        testGeoPos(ts, TestSet.UL);
        testGeoPos(ts, TestSet.UC);
        testGeoPos(ts, TestSet.UR);
        testGeoPos(ts, TestSet.CL);
        testGeoPos(ts, TestSet.CC);
        testGeoPos(ts, TestSet.CR);
        testGeoPos(ts, TestSet.LL);
        testGeoPos(ts, TestSet.LC);
        testGeoPos(ts, TestSet.LR);

        testPixelPos(ts, TestSet.UL);
        testPixelPos(ts, TestSet.UC);
        testPixelPos(ts, TestSet.UR);
        testPixelPos(ts, TestSet.CL);
        testPixelPos(ts, TestSet.CC);
        testPixelPos(ts, TestSet.CR);
        testPixelPos(ts, TestSet.LL);
        testPixelPos(ts, TestSet.LC);
        testPixelPos(ts, TestSet.LR);

        assertEquals(ts.crossingMeridianAt180, ts.gc.isCrossingMeridianAt180());
    }

    private void testNormalization(TestSet ts) {
        float delta = 0.1f;

        // Test latitude out of bounds
        assertTrue(Float.isNaN(ts.gc.normalizeLat(-90 - delta)));
        assertTrue(Float.isNaN(ts.gc.normalizeLat(+90 + delta)));
        // Test latitude within bounds
        assertFalse(Float.isNaN(ts.gc.normalizeLat(-90)));
        assertFalse(Float.isNaN(ts.gc.normalizeLat(+90)));

        // Test longitude out of bounds
        assertTrue(Float.isNaN(ts.gc.normalizeLon(-180 - delta)));
        assertTrue(Float.isNaN(ts.gc.normalizeLon(+180 + delta)));

        // Test around lonMin. Care here! lonMin can be > +180
        float lonMin = Math.max(-180, ts.gc.getNormalizedLonMin());
        assertTrue(Float.isNaN(ts.gc.normalizeLon(lonMin - delta)));
        assertFalse(Float.isNaN(ts.gc.normalizeLon(lonMin)));
        assertFalse(Float.isNaN(ts.gc.normalizeLon(lonMin + delta)));
    }


    private void testGeoPos(TestSet ts, int i) {
        PixelPos pp = ts.pp[i];
        GeoPos gp = ts.gc.getGeoPos(pp, null);
        assertNotNull(gp);
        assertEquals(ts.getPositionName(i) + " GeoPos.lat for " + pp, ts.gp[i].lat, gp.lat, GP_ERROR);
        assertEquals(ts.getPositionName(i) + " GeoPos.lon for " + pp, ts.gp[i].lon, gp.lon, GP_ERROR);
    }

    private void testPixelPos(TestSet ts, int i) {
        GeoPos gp = ts.gp[i];
        PixelPos pp = ts.gc.getPixelPos(gp, null);
        assertNotNull(pp);
        assertEquals(ts.getPositionName(i) + " PixelPos.x for " + gp, ts.pp[i].x, pp.x, PP_ERROR);
        assertEquals(ts.getPositionName(i) + " PixelPos.y for " + gp, ts.pp[i].y, pp.y, PP_ERROR);
    }

    private static class TestSet {

        float latRange, lonRange;
        int sceneW, sceneH;
        int stepX, stepY;
        int gridW, gridH;
        double rotationAngle;
        double lonOffset, latOffset;

        TiePointGeoCoding gc;
        PixelPos[] pp;
        GeoPos[] gp;
        boolean crossingMeridianAt180;

        public TestSet(int sceneW, int sceneH,
                       int stepX, int stepY,
                       float lonRange, float latRange,
                       double rotationAngle,
                       double lonOffset,
                       double latOffset,
                       boolean crossingMeridianAt180) {
            this.latRange = latRange;
            this.lonRange = lonRange;
            this.sceneW = sceneW;
            this.sceneH = sceneH;
            this.stepX = stepX;
            this.stepY = stepY;
            this.gridW = sceneW / stepX + 1;
            this.gridH = sceneH / stepY + 1;
            this.rotationAngle = rotationAngle;
            this.lonOffset = lonOffset;
            this.latOffset = latOffset;
            this.gc = createTestGeoCoding();
            this.pp = createTestPixelPositions();
            this.gp = createTestGeoPositions();
            this.crossingMeridianAt180 = crossingMeridianAt180;
        }

        private String getPositionName(int i) {
            return POSITION_NAMES[i];
        }

        private TiePointGeoCoding createTestGeoCoding() {
            GeoPos[] targetCoords = createCoords();

            int numCoords = targetCoords.length;
            float[] lats = new float[numCoords];
            float[] lons = new float[numCoords];
            for (int i = 0; i < numCoords; i++) {
                lats[i] = targetCoords[i].lat;
                lons[i] = targetCoords[i].lon;
            }

            TiePointGrid latGrid = new TiePointGrid("lat", gridW, gridH, 0.0f, 0.0f, stepX, stepY, lats);
            TiePointGrid lonGrid = new TiePointGrid("lon", gridW, gridH, 0.0f, 0.0f, stepX, stepY, lons,
                                                    TiePointGrid.DISCONT_AT_180);
            return new TiePointGeoCoding(latGrid, lonGrid, Datum.WGS_84);
        }

        private GeoPos[] createCoords() {
            int numCoords = gridW * gridH;
            GeoPos[] sourceGpArray = new GeoPos[numCoords];
            float lon0 = -0.5f * lonRange;
            float lat0 = +0.5f * latRange;
            for (int i = 0, y = 0; y < gridH; y++) {
                for (int x = 0; x < gridW; x++) {
                    float lat = lat0 - latRange * y / (gridH - 1.0f);
                    float lon = lon0 + lonRange * x / (gridW - 1.0f);
                    sourceGpArray[i++] = new GeoPos(lat, lon);
                }
            }

            return transformGeoPositions(sourceGpArray);
        }

        private GeoPos[] transformGeoPositions(GeoPos[] sourceGpArray) {
            int numCoords = sourceGpArray.length;
            float[] sourceCoords = new float[2 * numCoords];
            for (int i = 0; i < numCoords; i++) {
                GeoPos geoPos = sourceGpArray[i];
                sourceCoords[2 * i] = geoPos.lon;
                sourceCoords[2 * i + 1] = geoPos.lat;
            }

//            AffineTransform t = AffineTransform.getRotateInstance(Math.toRadians(rotationAngle));
//            t.concatenate(AffineTransform.getTranslateInstance(lonOffset, latOffset));

            AffineTransform t = AffineTransform.getTranslateInstance(lonOffset, latOffset);
            t.concatenate(AffineTransform.getRotateInstance(Math.toRadians(rotationAngle)));
            float[] targetCoords = new float[2 * numCoords];
            t.transform(sourceCoords, 0, targetCoords, 0, numCoords);

            GeoPos[] targetGpArray = new GeoPos[numCoords];
            for (int i = 0; i < numCoords; i++) {
                float lon = targetCoords[2 * i];
                float lat = targetCoords[2 * i + 1];
                if (lat < -90 || lat > 90) {
                    throw new IllegalArgumentException("lat < -90 || lat > 90");
                }
                if (lon > 180) {
                    lon -= 360;
                }
                if (lon < -180) {
                    lon += 360;
                }
                if (lon < -180 || lon > 180) {
                    throw new IllegalArgumentException("lon < -180 || lon > 180");
                }
                targetGpArray[i] = new GeoPos(lat, lon);
            }

            return targetGpArray;
        }

        final static int UL = 0;
        final static int UC = 1;
        final static int UR = 2;
        final static int CL = 3;
        final static int CC = 4;
        final static int CR = 5;
        final static int LL = 6;
        final static int LC = 7;
        final static int LR = 8;

        final static String[] POSITION_NAMES = {
                /*UL*/ "Upper-Left",
                /*UC*/ "Upper-Center",
                /*UR*/ "Upper-Right",
                /*CL*/ "Center-Left",
                /*CC*/ "Center",
                /*CR*/ "Center-Right",
                /*LL*/ "Lower-Left",
                /*LC*/ "Lower-Center",
                /*LR*/ "Lower-Right",
        };

        private PixelPos[] createTestPixelPositions() {
            return new PixelPos[]{
                    /*UL*/ new PixelPos(0.0f, 0.0f),
                    /*UC*/ new PixelPos(sceneW / 2, 0),
                    /*UR*/ new PixelPos(sceneW - 1, 0),
                    /*CL*/ new PixelPos(0, sceneH / 2),
                    /*CC*/ new PixelPos(sceneW / 2, sceneH / 2),
                    /*CR*/ new PixelPos(sceneW - 1, sceneH / 2),
                    /*LL*/ new PixelPos(0, sceneH - 1),
                    /*LC*/ new PixelPos(sceneW / 2, sceneH - 1),
                    /*LR*/ new PixelPos(sceneW - 1, sceneH - 1),
            };
        }

        private GeoPos[] createTestGeoPositions() {
            GeoPos[] geoPositions = new GeoPos[]{
                    /*UL*/ new GeoPos(+0.5f * latRange, -0.5f * lonRange),
                    /*UC*/ new GeoPos(+0.5f * latRange, 0),
                    /*UR*/ new GeoPos(+0.5f * latRange, +0.5f * lonRange),
                    /*CL*/ new GeoPos(0, -0.5f * lonRange),
                    /*CC*/ new GeoPos(0, 0),
                    /*CR*/ new GeoPos(0, +0.5f * lonRange),
                    /*LL*/ new GeoPos(-0.5f * latRange, -0.5f * lonRange),
                    /*LC*/ new GeoPos(-0.5f * latRange, 0),
                    /*LR*/ new GeoPos(-0.5f * latRange, +0.5f * lonRange),
            };
            return transformGeoPositions(geoPositions);
        }
    }
}
