package org.esa.snap.core.dataio.geocoding.forward;

import org.esa.snap.core.dataio.geocoding.GeoRaster;
import org.esa.snap.core.dataio.geocoding.TestData;
import org.esa.snap.core.datamodel.GeoPos;
import org.esa.snap.core.datamodel.PixelPos;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class TiePointBilinearForwardTest {

    private TiePointBilinearForward coding;

    @Before
    public void setUp() {
        coding = new TiePointBilinearForward();
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

        geoPos = coding.getGeoPos(new PixelPos(65.01f, 1.f), null);
        assertEquals(Float.NaN, geoPos.lat, 1e-8);
        assertEquals(Float.NaN, geoPos.lon, 1e-8);

        geoPos = coding.getGeoPos(new PixelPos(3.6f, -0.01f), null);
        assertEquals(Float.NaN, geoPos.lat, 1e-8);
        assertEquals(Float.NaN, geoPos.lon, 1e-8);

        geoPos = coding.getGeoPos(new PixelPos(3.7f, 65.01f), null);
        assertEquals(Float.NaN, geoPos.lat, 1e-8);
        assertEquals(Float.NaN, geoPos.lon, 1e-8);
    }

    @Test
    public void testGetGeoPos_MER_RR() {
        final GeoRaster geoRaster = TestData.get_MER_RR();
        coding.initialize(geoRaster, false, new PixelPos[0]);

        // on upper left point
        GeoPos geoPos = coding.getGeoPos(new PixelPos(0.5, 0.5), null);
        assertEquals(17.934608f, geoPos.lon, 1e-8);
        assertEquals(72.23347f, geoPos.lat, 1e-8);

        // on lower right point
        geoPos = coding.getGeoPos(new PixelPos(64.5, 64.5), null);
        assertEquals(19.034266f, geoPos.lon, 1e-8);
        assertEquals(71.403755f, geoPos.lat, 1e-8);

        // cross cell 2/3
        geoPos = coding.getGeoPos(new PixelPos(32.5, 48.5), null);
        assertEquals(18.322376251220703, geoPos.lon, 1e-8);
        assertEquals(71.66108703613281, geoPos.lat, 1e-8);

        geoPos = coding.getGeoPos(new PixelPos(33.5, 49.5), null);
        assertEquals(18.339499466121197, geoPos.lon, 1e-8);
        assertEquals(71.64809927344322, geoPos.lat, 1e-8);

        geoPos = coding.getGeoPos(new PixelPos(34.5, 50.5), null);
        assertEquals(18.356596678495407, geoPos.lon, 1e-8);
        assertEquals(71.63511407375336, geoPos.lat, 1e-8);

        geoPos = coding.getGeoPos(new PixelPos(35.5, 51.5), null);
        assertEquals(18.373667888343334, geoPos.lon, 1e-8);
        assertEquals(71.62213143706322, geoPos.lat, 1e-8);

        geoPos = coding.getGeoPos(new PixelPos(36.5, 52.5), null);
        assertEquals(18.390713095664978, geoPos.lon, 1e-8);
        assertEquals(71.6091513633728, geoPos.lat, 1e-8);
    }

    @Test
    public void testGetGeoPos_AMSUB_anti_meridian() {
        final GeoRaster geoRaster = TestData.get_AMSUB_subs_3_anti_meridian();

        coding.initialize(geoRaster, true, new PixelPos[0]);

        // lon-data has not the exact values at the sampling points, interpolates over the tie point tb 2019-11-14
        // on upper left point
        GeoPos geoPos = coding.getGeoPos(new PixelPos(0.5, 0.5), null);
        assertEquals(170.64950566121803, geoPos.lon, 1e-8);
        assertEquals(-74.2444f, geoPos.lat, 1e-8);

        // on lower right point
        geoPos = coding.getGeoPos(new PixelPos(30.5, 30.5), null);
        assertEquals(175.38380436960162, geoPos.lon, 1e-8);
        assertEquals(-67.9106f, geoPos.lat, 1e-8);

        // crossing the anti-meridian
        geoPos = coding.getGeoPos(new PixelPos(23.5, 3.5), null);
        assertEquals(179.74847591386276, geoPos.lon, 1e-8);
        assertEquals(-72.0763651529948, geoPos.lat, 1e-8);

        geoPos = coding.getGeoPos(new PixelPos(24.0, 3.5), null);
        assertEquals(179.95224604222287, geoPos.lon, 1e-8);
        assertEquals(-72.03178151448567, geoPos.lat, 1e-8);

        geoPos = coding.getGeoPos(new PixelPos(24.5, 3.5), null);
        assertEquals(-179.8439941395608, geoPos.lon, 1e-8);
        assertEquals(-71.98719787597656, geoPos.lat, 1e-8);
    }
}
