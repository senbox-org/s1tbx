package org.esa.snap.core.dataio.geocoding.forward;

import org.esa.snap.core.dataio.geocoding.ForwardCoding;
import org.esa.snap.core.dataio.geocoding.GeoRaster;
import org.esa.snap.core.dataio.geocoding.TestData;
import org.esa.snap.core.datamodel.GeoPos;
import org.esa.snap.core.datamodel.PixelPos;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class PixelInterpolatingForwardTest {

    private ForwardCoding pixelForward;

    @Before
    public void setUp() {
        pixelForward = new PixelInterpolatingForward();
    }

    @Test
    public void testGetGeoPos_SLSTR_OL() {
        final GeoRaster geoRaster = TestData.get_SLSTR_OL();

        pixelForward.initialize(geoRaster, false, new PixelPos[0]);

        GeoPos geoPos = pixelForward.getGeoPos(new PixelPos(0.5, 0.5), null);
        assertEquals(-130.350693, geoPos.lon, 1e-8);
        assertEquals(45.855048, geoPos.lat, 1e-8);

        geoPos = pixelForward.getGeoPos(new PixelPos(0.4, 0.4), null);
        assertEquals(-130.35102159, geoPos.lon, 1e-8);
        assertEquals(45.85536669999999, geoPos.lat, 1e-8);

        geoPos = pixelForward.getGeoPos(new PixelPos(0.6, 0.6), null);
        assertEquals(-130.35036439, geoPos.lon, 1e-8);
        assertEquals(45.8547293, geoPos.lat, 1e-8);

        geoPos = pixelForward.getGeoPos(new PixelPos(0.5, 0.1), null);
        assertEquals(-130.35037780000002, geoPos.lon, 1e-8);
        assertEquals(45.856078, geoPos.lat, 1e-8);

        geoPos = pixelForward.getGeoPos(new PixelPos(0.5, 0.9), null);
        assertEquals(-130.3510082, geoPos.lon, 1e-8);
        assertEquals(45.854017999999996, geoPos.lat, 1e-8);

        geoPos = pixelForward.getGeoPos(new PixelPos(31.5, 0.5), null);
        assertEquals(-130.245281, geoPos.lon, 1e-8);
        assertEquals(45.839166999999996, geoPos.lat, 1e-8);

        geoPos = pixelForward.getGeoPos(new PixelPos(31.9, 0.5), null);
        assertEquals(-130.24366540000003, geoPos.lon, 1e-8);
        assertEquals(45.838922999999994, geoPos.lat, 1e-8);

        geoPos = pixelForward.getGeoPos(new PixelPos(31.9, 0.95), null);
        assertEquals(-130.2440218, geoPos.lon, 1e-8);
        assertEquals(45.837764699999994, geoPos.lat, 1e-8);

        geoPos = pixelForward.getGeoPos(new PixelPos(30.5, 25.5), null);
        assertEquals(-130.269123, geoPos.lon, 1e-8);
        assertEquals(45.775419, geoPos.lat, 1e-8);

        geoPos = pixelForward.getGeoPos(new PixelPos(0.5, 25.5), null);
        assertEquals(-130.37037999999998, geoPos.lon, 1e-8);
        assertEquals(45.790684, geoPos.lat, 1e-8);
    }

    @Test
    public void testGetGeoPos_AMSUB_anti_meridian() {
        final GeoRaster geoRaster = TestData.get_AMSUB();

        pixelForward.initialize(geoRaster, true, new PixelPos[0]);

        GeoPos geoPos = pixelForward.getGeoPos(new PixelPos(24.5, 4.5), null);
        assertEquals(179.8498, geoPos.lon, 1e-8);
        assertEquals(-71.8596, geoPos.lat, 1e-8);

        geoPos = pixelForward.getGeoPos(new PixelPos(24.7, 4.5), null);
        assertEquals(179.9314, geoPos.lon, 1e-8);
        assertEquals(-71.84114, geoPos.lat, 1e-8);

        geoPos = pixelForward.getGeoPos(new PixelPos(24.9, 4.5), null);
        assertEquals(-179.987, geoPos.lon, 1e-8);
        assertEquals(-71.82268, geoPos.lat, 1e-8);

        geoPos = pixelForward.getGeoPos(new PixelPos(25.1, 4.5), null);
        assertEquals(-179.9054, geoPos.lon, 1e-8);
        assertEquals(-71.80422, geoPos.lat, 1e-8);
    }

    @Test
    public void testGetGeoPos_SLSTR_OL_outside() {
        final GeoRaster geoRaster = TestData.get_SLSTR_OL();

        pixelForward.initialize(geoRaster, false, new PixelPos[0]);

        GeoPos geoPos = pixelForward.getGeoPos(new PixelPos(-0.1, 0.45), null);
        assertEquals(Double.NaN, geoPos.lon, 1e-8);
        assertEquals(Double.NaN, geoPos.lat, 1e-8);

        geoPos = pixelForward.getGeoPos(new PixelPos(1.75, -0.01), null);
        assertEquals(Double.NaN, geoPos.lon, 1e-8);
        assertEquals(Double.NaN, geoPos.lat, 1e-8);

        geoPos = pixelForward.getGeoPos(new PixelPos(32.001, 1.6), null);
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

        GeoPos geoPos = pixelForward.getGeoPos(new PixelPos(Double.NaN, 1.3), null);
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
