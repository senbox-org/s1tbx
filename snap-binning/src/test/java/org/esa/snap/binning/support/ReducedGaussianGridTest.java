package org.esa.snap.binning.support;

import org.esa.snap.binning.PlanetaryGrid;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author Marco Peters
 * @author Ralf Quast
 */
public class ReducedGaussianGridTest {

    private PlanetaryGrid grid = new ReducedGaussianGrid(64);

    @Test
    public void testGetNumRows() {
        assertEquals(64, grid.getNumRows());
    }

    @Test
    public void testGetNumCols() {
        assertEquals(20, grid.getNumCols(0));
        assertEquals(80, grid.getNumCols(10));
        assertEquals(128, grid.getNumCols(32));
        assertEquals(20, grid.getNumCols(63));
    }

    @Test
    public void testRowIndex() {
        assertEquals(1, grid.getRowIndex(20));
        assertEquals(6, grid.getRowIndex(270));
    }

    @Test
    public void testNumBins() {
        assertEquals(6114, grid.getNumBins());
    }

    @Test
    public void testGetFirstBinIndex() {
        assertEquals(0, grid.getFirstBinIndex(0));
        assertEquals(489, grid.getFirstBinIndex(10));
        assertEquals(5269, grid.getFirstBinIndex(50));
    }

    @Test
    public void testCenterLat() {
        assertEquals(-51.62573, grid.getCenterLat(50), 1.0e-6);
        assertEquals(-87.86379, grid.getCenterLat(63), 1.0e-6);
    }

    @Test
    public void testGetCenterLatLon() {
        // bin=300 -> row=7, col=22 -> lat=68.36775, lon=-123.75
        final double[] center = grid.getCenterLatLon(300);

        final double centerLat = center[0];
        assertEquals(68.36775, centerLat, 0.0);

        final double centerLon = center[1];
        assertEquals(123.75, centerLon, 0.0);
    }

    @Test
    public void testGetBinIndex() {
        // lat=45.0, lon=90.0 -> row=15, col=27 -> bin=945+27
        assertEquals(945 + 27, grid.getBinIndex(45.0, 90.0));
    }

}
