package org.esa.snap.core.dataio.geocoding.forward;

import org.esa.snap.core.dataio.geocoding.GeoRaster;
import org.esa.snap.core.dataio.geocoding.TestData;
import org.esa.snap.core.datamodel.GeoPos;
import org.esa.snap.core.datamodel.PixelPos;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class TiePointSplineForwardTest {

    private TiePointSplineForward coding;

    @Before
    public void setUp() {
        coding = new TiePointSplineForward();
    }

    @Test
    public void testDispose() {
        coding.dispose();
    }

    @Test
    public void testGetGeoPos_MER_RR_outside_raster() {
        final GeoRaster geoRaster = TestData.get_MER_RR();
        coding.initialize(geoRaster, false, new PixelPos[0]);

        GeoPos geoPos = coding.getGeoPos(new PixelPos(-0.01f, 1.5f), null);
        assertEquals(Float.NaN, geoPos.lat, 1e-8);
        assertEquals(Float.NaN, geoPos.lon, 1e-8);

        geoPos = coding.getGeoPos(new PixelPos(66.01f, 1.f), null);
        assertEquals(Float.NaN, geoPos.lat, 1e-8);
        assertEquals(Float.NaN, geoPos.lon, 1e-8);

        geoPos = coding.getGeoPos(new PixelPos(3.6f, -0.01f), null);
        assertEquals(Float.NaN, geoPos.lat, 1e-8);
        assertEquals(Float.NaN, geoPos.lon, 1e-8);

        geoPos = coding.getGeoPos(new PixelPos(3.7f, 66.01f), null);
        assertEquals(Float.NaN, geoPos.lat, 1e-8);
        assertEquals(Float.NaN, geoPos.lon, 1e-8);
    }

    @Test
    public void testGetGeoPos_MER_RR() {
        final GeoRaster geoRaster = TestData.get_MER_RR();
        coding.initialize(geoRaster, false, new PixelPos[0]);

        // upper left spline segment
        GeoPos geoPos = coding.getGeoPos(new PixelPos(0.5, 0.5), null);
        assertEquals(17.934608, geoPos.lon, 1e-8);
        assertEquals(72.23347, geoPos.lat, 1e-8);

        geoPos = coding.getGeoPos(new PixelPos(1.5, 0.5), null);
        assertEquals(17.96354510998535, geoPos.lon, 1e-8);
        assertEquals(72.23050283752441, geoPos.lat, 1e-8);

        geoPos = coding.getGeoPos(new PixelPos(0.5, 1.5), null);
        assertEquals(17.92368691809082, geoPos.lon, 1e-8);
        assertEquals(72.22353811035157, geoPos.lat, 1e-8);

        geoPos = coding.getGeoPos(new PixelPos(16.5, 0.5), null);
        assertEquals(18.396393, geoPos.lon, 1e-8);
        assertEquals(72.18546, geoPos.lat, 1e-8);

        // transition to next spline segment - x-direction
        geoPos = coding.getGeoPos(new PixelPos(32.5, 0.5), null);
        assertEquals(18.855751, geoPos.lon, 1e-8);
        assertEquals(72.136375, geoPos.lat, 1e-8);

        geoPos = coding.getGeoPos(new PixelPos(33.5, 0.5), null);
        assertEquals(18.884384726928715, geoPos.lon, 1e-8);
        assertEquals(72.13327440002442, geoPos.lat, 1e-8);

        // transition to next spline segment - x- and y-direction
        geoPos = coding.getGeoPos(new PixelPos(32.5, 32.5), null);
        assertEquals(18.497456, geoPos.lon, 1e-8);
        assertEquals(71.81966, geoPos.lat, 1e-8);

        geoPos = coding.getGeoPos(new PixelPos(33.5, 33.5), null);
        assertEquals(18.514617705687066, geoPos.lon, 1e-8);
        assertEquals(71.80669527228457, geoPos.lat, 1e-8);

        // border-pixel x-direction
        geoPos = coding.getGeoPos(new PixelPos(64.5, 0.5), null);
        assertEquals(19.767054, geoPos.lon, 1e-8);
        assertEquals(72.03501, geoPos.lat, 1e-8);

        // border-pixel x- and y-direction
        geoPos = coding.getGeoPos(new PixelPos(64.5, 64.5), null);
        assertEquals(19.034265518188477, geoPos.lon, 1e-8);
        assertEquals(71.40375518798828, geoPos.lat, 1e-8);
    }

    @Test
    public void testGetGeoPos_AMSUB_anti_meridian() {
        final GeoRaster geoRaster = TestData.get_AMSUB_subs_3_anti_meridian();
        coding.initialize(geoRaster, true, new PixelPos[0]);

        GeoPos geoPos = coding.getGeoPos(new PixelPos(21.5, 1.5), null);
        assertEquals(179.54856296296296, geoPos.lon, 1e-8);
        assertEquals(-72.51221111111111, geoPos.lat, 1e-8);

        geoPos = coding.getGeoPos(new PixelPos(22.5, 1.5), null);
        assertEquals(179.95886831275723, geoPos.lon, 1e-8);
        assertEquals(-72.42456611796983, geoPos.lat, 1e-8);

        geoPos = coding.getGeoPos(new PixelPos(23.5, 1.5), null);
        assertEquals(-179.63089053497941, geoPos.lon, 1e-8);
        assertEquals(-72.33357215363513, geoPos.lat, 1e-8);

        geoPos = coding.getGeoPos(new PixelPos(24.5, 1.5), null);
        assertEquals(-179.22068148148145, geoPos.lon, 1e-8);
        assertEquals(-72.24090370370371, geoPos.lat, 1e-8);
    }
}
