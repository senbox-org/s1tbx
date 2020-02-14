package org.esa.snap.core.dataio.geocoding.util;

import org.esa.snap.core.dataio.geocoding.*;
import org.esa.snap.core.datamodel.PixelPos;
import org.junit.Test;

import static org.junit.Assert.*;

public class RasterUtilsTest {

    @Test
    public void testCalculateDiscontinuity() {
        float[] longitudes = new float[]{-180.f, -120.f, 0.f, 120.f, 180.f};

        Discontinuity discontinuity = RasterUtils.calculateDiscontinuity(longitudes);
        assertEquals(Discontinuity.AT_180, discontinuity);

        longitudes = new float[]{-170.f, -110.f, 10.f, 130.f, 190.f};
        discontinuity = RasterUtils.calculateDiscontinuity(longitudes);
        assertEquals(Discontinuity.AT_360, discontinuity);
    }

    @Test
    public void testContainsAntiMeridian() {
        double[] longitudes = new double[]{-179.f, -120.f, 0.f,
                -178.f, -118.f, -11.f};
        assertFalse(RasterUtils.containsAntiMeridian(longitudes, 3));

        longitudes = new double[]{120.f, 130f, 140f,
                150f, 160f, 168.f,
                179.f, -171f, -160f};
        assertTrue(RasterUtils.containsAntiMeridian(longitudes, 3));
    }

    @Test
    public void testConstainsAntiMeridian_realData() {
        assertFalse(RasterUtils.containsAntiMeridian(MERIS.MER_RR_LON, 5));
        assertFalse(RasterUtils.containsAntiMeridian(AMSRE.AMSRE_HIGH_RES_LON, 25));

        assertTrue(RasterUtils.containsAntiMeridian(AMSUB.AMSUB_POLE_LON, 25));
        assertTrue(RasterUtils.containsAntiMeridian(AMSUB.AMSUB_ANTI_MERID_LON, 30));
    }

    @Test
    public void testGetPoleLocations_no_pole() {
        double[] longitudes = new double[]{54., 55., 56., 57., 58., 59f};
        double[] latitudes = new double[]{-11., -10.5, -10., -9.5, -9., 8.5f};
        final GeoRaster geoRaster = new GeoRaster(longitudes, latitudes, null, null, 3, 2, 15.0);

        final PixelPos[] poleLocations = RasterUtils.getPoleLocations(geoRaster);
        assertEquals(0, poleLocations.length);
    }

    @Test
    public void testGetPoleLocations_northPole() {
        double[] longitudes = new double[]{14.37, 32.99, 97.55, 144.09,
                2.05, 11.87, 115.3, 160.25,
                -11.13, -15.76, -151.63, 178.72,
                -23.9, -40.01, -115.36, -163.88f
        };
        double[] latitudes = new double[]{89.28, 89.59, 89.73, 89.46,
                89.32, 89.67, 89.87, 89.51,
                89.32, 89.68, 89.92, 89.52,
                89.3, 89.62, 89.79, 89.48f
        };
        final GeoRaster geoRaster = new GeoRaster(longitudes, latitudes, null, null, 4, 4, 12.0);

        final PixelPos[] poleLocations = RasterUtils.getPoleLocations(geoRaster);
        assertEquals(1, poleLocations.length);

        assertEquals(2, (int) poleLocations[0].x);
        assertEquals(2, (int) poleLocations[0].y);
    }

    @Test
    public void testGetPoleLocations_northPole_twoLocations() {
        double[] longitudes = new double[]{14.37, 32.99, 97.55, 144.09,
                2.05, 11.87, 115.3, 160.25,
                -11.13, -15.76, -151.63, 178.72,
                -23.9, -40.01, -115.36, -163.88f
        };
        double[] latitudes = new double[]{89.28, 89.59, 89.73, 89.46,
                89.32, 89.67, 89.87, 89.51,
                89.32, 89.68, 89.92, 89.52,
                89.3, 89.62, 89.79, 89.48f
        };
        final GeoRaster geoRaster = new GeoRaster(longitudes, latitudes, null, null, 4, 4, 16.0);

        final PixelPos[] poleLocations = RasterUtils.getPoleLocations(geoRaster);
        assertEquals(2, poleLocations.length);

        assertEquals(2, (int) poleLocations[0].x);
        assertEquals(1, (int) poleLocations[0].y);

        assertEquals(2, (int) poleLocations[1].x);
        assertEquals(2, (int) poleLocations[1].y);
    }

    @Test
    public void testGetPoleLocations_AMSRE_noPole() {
        final GeoRaster geoRaster = TestData.get_AMSRE();

        final PixelPos[] poleLocations = RasterUtils.getPoleLocations(geoRaster);
        assertEquals(0, poleLocations.length);
    }

    @Test
    public void testGetPoleLocations_AMSUB_northPole() {
        final GeoRaster geoRaster = new GeoRaster(AMSUB.AMSUB_POLE_LON, AMSUB.AMSUB_POLE_LAT, null, null, 25, 25, 16.0);

        final PixelPos[] poleLocations = RasterUtils.getPoleLocations(geoRaster);
        assertEquals(2, poleLocations.length);

        assertEquals(22, (int) poleLocations[0].x);
        assertEquals(8, (int) poleLocations[0].y);

        assertEquals(22, (int) poleLocations[1].x);
        assertEquals(9, (int) poleLocations[1].y);
    }

    @Test
    public void testGetLatDeltaToPole() {
        double deltaLat = RasterUtils.getLatDeltaToPole(24.0);
        assertEquals(0.2158372849225998, deltaLat, 1e-8);

        deltaLat = RasterUtils.getLatDeltaToPole(111.0);
        assertEquals(0.9982474446296692, deltaLat, 1e-8);

        deltaLat = RasterUtils.getLatDeltaToPole(0.3);
        assertEquals(0.002697966294363141, deltaLat, 1e-8);
    }

    @Test
    public void testToFloat() {
        final double[] doubles = new double[]{1.0, 2.0, 3.0, 5.0};

        final float[] floats = RasterUtils.toFloat(doubles);
        assertEquals(doubles.length, floats.length);
        assertEquals(1.f, floats[0], 1e-8);
        assertEquals(3.f, floats[2], 1e-8);
    }

    @Test
    public void testToDouble() {
        final float[] floats = new float[]{6.f, 7.f, 8.f, 9.f};

        final double[] doubles = RasterUtils.toDouble(floats);
        assertEquals(floats.length, doubles.length);
        assertEquals(7.0, doubles[1], 1e-8);
        assertEquals(9.0, doubles[3], 1e-8);
    }
}
