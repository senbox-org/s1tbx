package org.esa.snap.core.dataio.geocoding;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

public class GeoRasterTest {

    @Test
    public void testConstructionAndGetter() {
        double[] longitudes = new double[]{1, 2, 3, 4, 5, 6};
        double[] latitudes = new double[]{7, 8, 9, 10, 11, 12};

        final GeoRaster geoRaster = new GeoRaster(longitudes, latitudes, "fritz", "franz",
                2, 3, 18.7);

        assertSame(longitudes, geoRaster.getLongitudes());
        assertSame(latitudes, geoRaster.getLatitudes());

        assertEquals("fritz", geoRaster.getLonVariableName());
        assertEquals("franz", geoRaster.getLatVariableName());

        assertEquals(2, geoRaster.getRasterWidth());
        assertEquals(3, geoRaster.getRasterHeight());
        assertEquals(2, geoRaster.getSceneWidth());
        assertEquals(3, geoRaster.getSceneHeight());
        assertEquals(18.7, geoRaster.getRasterResolutionInKm(), 1e-8);

        assertEquals(0.5, geoRaster.getOffsetX(), 1e-8);
        assertEquals(0.5, geoRaster.getOffsetY(), 1e-8);
        assertEquals(1.0, geoRaster.getSubsamplingX(), 1e-8);
        assertEquals(1.0, geoRaster.getSubsamplingY(), 1e-8);
    }

    @Test
    public void testFullConstructionAndGetter() {
        double[] longitudes = new double[]{1, 2, 3, 4, 5, 6};
        double[] latitudes = new double[]{7, 8, 9, 10, 11, 12};

        final GeoRaster geoRaster = new GeoRaster(longitudes, latitudes,
                "lon", "lati",
                2, 3, 4, 5, 18.7,
                6.0, 7.0, 8.0, 9.0);

        assertSame(longitudes, geoRaster.getLongitudes());
        assertSame(latitudes, geoRaster.getLatitudes());

        assertEquals("lon", geoRaster.getLonVariableName());
        assertEquals("lati", geoRaster.getLatVariableName());

        assertEquals(2, geoRaster.getRasterWidth());
        assertEquals(3, geoRaster.getRasterHeight());
        assertEquals(4, geoRaster.getSceneWidth());
        assertEquals(5, geoRaster.getSceneHeight());
        assertEquals(18.7, geoRaster.getRasterResolutionInKm(), 1e-8);

        assertEquals(6.0, geoRaster.getOffsetX(), 1e-8);
        assertEquals(7.0, geoRaster.getOffsetY(), 1e-8);
        assertEquals(8.0, geoRaster.getSubsamplingX(), 1e-8);
        assertEquals(9.0, geoRaster.getSubsamplingY(), 1e-8);
    }
}
