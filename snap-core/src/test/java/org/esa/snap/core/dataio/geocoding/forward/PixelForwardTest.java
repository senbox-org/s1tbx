package org.esa.snap.core.dataio.geocoding.forward;

import org.esa.snap.core.dataio.geocoding.GeoRaster;
import org.esa.snap.core.dataio.geocoding.TestData;
import org.esa.snap.core.datamodel.GeoPos;
import org.esa.snap.core.datamodel.PixelPos;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class PixelForwardTest {

    private PixelForward pixelForward;

    @Before
    public void setUp() {
        pixelForward = new PixelForward();
    }

    @Test
    public void testGetGeoPos_SLSTR_OL() {
        final GeoRaster geoRaster = TestData.get_SLSTR_OL();

        pixelForward.initialize(geoRaster, false, new PixelPos[0]);

        GeoPos geoPos = pixelForward.getGeoPos(new PixelPos(0.5, 0.5), null);
        assertEquals(-130.350693, geoPos.lon, 1e-8);
        assertEquals(45.855048, geoPos.lat, 1e-8);

        geoPos = pixelForward.getGeoPos(new PixelPos(31.5, 0.5), null);
        assertEquals(-130.245281, geoPos.lon, 1e-8);
        assertEquals(45.839166999999996, geoPos.lat, 1e-8);

        geoPos = pixelForward.getGeoPos(new PixelPos(30.5, 25.5), null);
        assertEquals(-130.269123, geoPos.lon, 1e-8);
        assertEquals(45.775419, geoPos.lat, 1e-8);

        geoPos = pixelForward.getGeoPos(new PixelPos(0.5, 25.5), null);
        assertEquals(-130.37037999999998, geoPos.lon, 1e-8);
        assertEquals(45.790684, geoPos.lat, 1e-8);
    }

    @Test
    public void testGetGeoPos_SLSTR_OL_outside() {
        final GeoRaster geoRaster = TestData.get_SLSTR_OL();

        pixelForward.initialize(geoRaster, false, new PixelPos[0]);

        GeoPos geoPos = pixelForward.getGeoPos(new PixelPos(-0.1, 0.5), null);
        assertEquals(Double.NaN, geoPos.lon, 1e-8);
        assertEquals(Double.NaN, geoPos.lat, 1e-8);

        geoPos = pixelForward.getGeoPos(new PixelPos(1.5, -0.01), null);
        assertEquals(Double.NaN, geoPos.lon, 1e-8);
        assertEquals(Double.NaN, geoPos.lat, 1e-8);

        geoPos = pixelForward.getGeoPos(new PixelPos(33.5, 1.6), null);
        assertEquals(Double.NaN, geoPos.lon, 1e-8);
        assertEquals(Double.NaN, geoPos.lat, 1e-8);

        geoPos = pixelForward.getGeoPos(new PixelPos(1.5, 27.01), null);
        assertEquals(Double.NaN, geoPos.lon, 1e-8);
        assertEquals(Double.NaN, geoPos.lat, 1e-8);
    }

    @Test
    public void testGetGeoPos_SLSTR_OL_invalid_pixelpos() {
        final GeoRaster geoRaster = TestData.get_SLSTR_OL();

        pixelForward.initialize(geoRaster, false, new PixelPos[0]);

        GeoPos geoPos = pixelForward.getGeoPos(new PixelPos(Double.NaN, 0.5), null);
        assertEquals(Double.NaN, geoPos.lon, 1e-8);
        assertEquals(Double.NaN, geoPos.lat, 1e-8);
    }

    @Test
    public void testDispose() {
        pixelForward.dispose();

        final GeoRaster geoRaster = TestData.get_SLSTR_OL();
        pixelForward.initialize(geoRaster, false, new PixelPos[0]);
        pixelForward.dispose();
    }
}
