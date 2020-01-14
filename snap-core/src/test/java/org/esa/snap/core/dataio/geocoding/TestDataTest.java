package org.esa.snap.core.dataio.geocoding;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class TestDataTest {

    @Test
    public void testGetSubSampled_2() {
        final double[] data = {0., 1., 2., 3., 4.,
                5., 6., 7., 8., 9.,
                10., 11., 12., 13., 14.f};

        final double[] subsampled = TestData.getSubSampled(2, data, 5);
        assertEquals(6, subsampled.length);
        assertEquals(0.0, subsampled[0], 1e-8);
        assertEquals(2.0, subsampled[1], 1e-8);
        assertEquals(4.0, subsampled[2], 1e-8);
        assertEquals(10.0, subsampled[3], 1e-8);
        assertEquals(12.0, subsampled[4], 1e-8);
        assertEquals(14.0, subsampled[5], 1e-8);
    }

    @Test
    public void testGetSubSampled_3() {
        final double[] data = {0., 1., 2., 3.,
                4., 5., 6., 7.,
                8., 9., 10., 11.,
                12., 13., 14., 15f};

        final double[] subsampled = TestData.getSubSampled(3, data, 4);
        assertEquals(4, subsampled.length);
        assertEquals(0.0, subsampled[0], 1e-8);
        assertEquals(3.0, subsampled[1], 1e-8);
        assertEquals(12.0, subsampled[2], 1e-8);
        assertEquals(15.0, subsampled[3], 1e-8);
    }

    @Test
    public void testGetSubSampled_4() {
        final double[] subsampled = TestData.getSubSampled(4, AMSRE.AMSRE_HIGH_RES_LAT, 25);
        assertEquals(49, subsampled.length);
        // first line
        assertEquals(-0.9204868, subsampled[0], 1e-8);
        assertEquals(-0.79991394, subsampled[1], 1e-8);
        assertEquals(-0.6817513, subsampled[2], 1e-8);
        assertEquals(-0.56605387, subsampled[3], 1e-8);
        assertEquals(-0.45287573, subsampled[4], 1e-8);
        assertEquals(-0.3422698, subsampled[5], 1e-8);
        assertEquals(-0.23428795, subsampled[6], 1e-8);

        // third line
        assertEquals(-0.20022038, subsampled[14], 1e-8);
        assertEquals(-0.079506345, subsampled[15], 1e-8);
        assertEquals(0.038798757, subsampled[16], 1e-8);
        assertEquals(0.15463954, subsampled[17], 1e-8);
        assertEquals(0.2679617, subsampled[18], 1e-8);
        assertEquals(0.37871206, subsampled[19], 1e-8);
        assertEquals(0.4868385, subsampled[20], 1e-8);
    }

    @Test
    public void testGet_MER_RR() {
        final GeoRaster geoRaster = TestData.get_MER_RR();

        assertEquals(17.934608, geoRaster.getLongitudes()[0], 1e-8);
        assertEquals(72.23347, geoRaster.getLatitudes()[0], 1e-8);
        assertEquals(65, geoRaster.getSceneHeight());
        assertEquals(65, geoRaster.getSceneWidth());
        assertEquals(5, geoRaster.getRasterWidth());
        assertEquals(5, geoRaster.getRasterHeight());
        assertEquals(0.5, geoRaster.getOffsetX(), 1e-8);
        assertEquals(0.5, geoRaster.getOffsetY(), 1e-8);
        assertEquals(16.0, geoRaster.getSubsamplingX(), 1e-8);
        assertEquals(16.0, geoRaster.getSubsamplingY(), 1e-8);
    }
}
