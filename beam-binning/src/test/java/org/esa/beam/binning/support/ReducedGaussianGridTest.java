package org.esa.beam.binning.support;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author Marco Peters
 */
public class ReducedGaussianGridTest {

    @Test
    public void testReducedGird() throws Exception {
        ReducedGaussianGrid gaussianGrid = new ReducedGaussianGrid(64);
        assertEquals(64, gaussianGrid.getNumRows());
        assertEquals(20, gaussianGrid.getNumCols(0));
        assertEquals(80, gaussianGrid.getNumCols(10));
        assertEquals(128, gaussianGrid.getNumCols(32));
        assertEquals(20, gaussianGrid.getNumCols(63));
        assertEquals(1, gaussianGrid.getRowIndex(20));
        assertEquals(6, gaussianGrid.getRowIndex(270));
        assertEquals(6114, gaussianGrid.getNumBins());
        assertEquals(0, gaussianGrid.getFirstBinIndex(0));
        assertEquals(489, gaussianGrid.getFirstBinIndex(10));
        assertEquals(5269, gaussianGrid.getFirstBinIndex(50));

        assertEquals(-51.62573, gaussianGrid.getCenterLat(50), 1.0e-6);
        assertEquals(-87.86379, gaussianGrid.getCenterLat(63), 1.0e-6);

        // binIndex=300 -> row=7,col=22 -> lat=68.36775,lon=-53.4375
        assertEquals(68.36775, gaussianGrid.getCenterLatLon(300)[0], 1.0e-6);
        assertEquals(-53.4375, gaussianGrid.getCenterLatLon(300)[1], 1.0e-6);

        // lat=45.0,lon=90.0 -> row=15,col=91 -> 1026
        assertEquals(1026, gaussianGrid.getBinIndex(45.0, 90.0));
    }

    @Test
    public void testFindClosestInArray() throws Exception {
        double[] values = {-175, -50, 50, 175};
        assertEquals(0, ReducedGaussianGrid.findClosestInArray(values, -178));
        assertEquals(1, ReducedGaussianGrid.findClosestInArray(values, -60));
        assertEquals(2, ReducedGaussianGrid.findClosestInArray(values, 30));
        assertEquals(3, ReducedGaussianGrid.findClosestInArray(values, 160));
        assertEquals(3, ReducedGaussianGrid.findClosestInArray(values, 179));
    }

}
