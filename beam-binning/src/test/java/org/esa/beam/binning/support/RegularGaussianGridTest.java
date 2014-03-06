package org.esa.beam.binning.support;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author Marco Peters
 */
public class RegularGaussianGridTest {

    @Test
    public void testRegularGird() throws Exception {
        RegularGaussianGrid gaussianGrid = new RegularGaussianGrid(64);
        assertEquals(64, gaussianGrid.getNumRows());
        assertEquals(128, gaussianGrid.getNumCols(0));
        assertEquals(128, gaussianGrid.getNumCols(32));
        assertEquals(128, gaussianGrid.getNumCols(63));
        assertEquals(2, gaussianGrid.getRowIndex(270));
        assertEquals(64 * 128, gaussianGrid.getNumBins());
        assertEquals(0, gaussianGrid.getFirstBinIndex(0));
        assertEquals(10 * 128, gaussianGrid.getFirstBinIndex(10));
        assertEquals(50 * 128, gaussianGrid.getFirstBinIndex(50));

        assertEquals(-51.62573, gaussianGrid.getCenterLat(50), 1.0e-6);
        assertEquals(-87.86379, gaussianGrid.getCenterLat(63), 1.0e-6);

        // binIndex=300 -> row=2,col=44
        assertEquals(82.31291, gaussianGrid.getCenterLatLon(300)[0], 1.0e-6);
        assertEquals(-54.84375, gaussianGrid.getCenterLatLon(300)[1], 1.0e-6);

        // lat=45.0,lon=90.0 -> row=15,col=96 -> 2016
        assertEquals(2016, gaussianGrid.getBinIndex(45.0, 90.0));
    }

    @Test
    public void testFindClosestInArray() throws Exception {
        double[] values = {-175, -50, 50, 175};
        assertEquals(0, RegularGaussianGrid.findClosestInArray(values, -178));
        assertEquals(1, RegularGaussianGrid.findClosestInArray(values, -60));
        assertEquals(2, RegularGaussianGrid.findClosestInArray(values, 30));
        assertEquals(3, RegularGaussianGrid.findClosestInArray(values, 160));
        assertEquals(3, RegularGaussianGrid.findClosestInArray(values, 179));
    }
}
