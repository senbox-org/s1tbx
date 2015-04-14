package org.esa.snap.binning.support;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author Marco Peters
 */
public class GaussianGridConfigTest {

    @Test
    public void testN128() throws Exception {
        GaussianGridConfig gridConfig = GaussianGridConfig.load(128);
        assertEquals(512, gridConfig.getRegularColumnCount());
        assertEquals(25, gridConfig.getReducedColumnCount(1));
        assertEquals(450, gridConfig.getReducedColumnCount(80));
        assertEquals(25, gridConfig.getReducedColumnCount(254));
        assertEquals(18, gridConfig.getReducedColumnCount(255));
        assertEquals(71.929491, gridConfig.getLatitude(25), 1.0e-6);
        assertEquals(-0.350877, gridConfig.getLatitude(128), 1.0e-6);
        assertEquals(512, gridConfig.getRegularLongitudePoints().length);
        assertEquals(18, gridConfig.getReducedLongitudePoints(0).length);
        assertEquals(450, gridConfig.getReducedLongitudePoints(80).length);
        assertEquals(3973, gridConfig.getReducedFirstBinIndex(33));
    }

    @Test
    public void testN400() throws Exception {
        GaussianGridConfig gridConfig = GaussianGridConfig.load(400);
        assertEquals(1600, gridConfig.getRegularColumnCount());
        assertEquals(25, gridConfig.getReducedColumnCount(1));
        assertEquals(540, gridConfig.getReducedColumnCount(80));
        assertEquals(640, gridConfig.getReducedColumnCount(700));
        assertEquals(18, gridConfig.getReducedColumnCount(799));
        assertEquals(84.209759, gridConfig.getLatitude(25), 1.0e-6);
        assertEquals(-8.207369, gridConfig.getLatitude(436), 1.0e-6);
        assertEquals(1600, gridConfig.getRegularLongitudePoints().length);
        assertEquals(18, gridConfig.getReducedLongitudePoints(0).length);
        assertEquals(540, gridConfig.getReducedLongitudePoints(80).length);
        assertEquals(1517, gridConfig.getReducedFirstBinIndex(20));
    }

    @Test
    public void testLongitudeComputation() throws Exception {
        double[] lons;

        lons = GaussianGridConfig.computeLongitudePoints(2);
        assertEquals(2, lons.length);
        assertEquals(0.0, lons[0], 0.0);
        assertEquals(180.0, lons[1], 0.0);

        lons = GaussianGridConfig.computeLongitudePoints(360);
        assertEquals(360, lons.length);
        assertEquals(0.0, lons[0], 0.0);
        assertEquals(179.0, lons[179], 0.0);
        assertEquals(180.0, lons[180], 0.0);
        assertEquals(359.0, lons[359], 0.0);

        lons = GaussianGridConfig.computeLongitudePoints(9);
        assertEquals(9, lons.length);
        assertEquals(0.0, lons[0], 0.0);
        assertEquals(160.0, lons[4], 0.0);
        assertEquals(320.0, lons[8], 0.0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testException() throws Exception {
        GaussianGridConfig.load(100);
    }

}
