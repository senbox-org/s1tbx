package org.esa.snap.binning.support;

import org.esa.snap.binning.PlanetaryGrid;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author Marco Peters
 * @author Ralf Quast
 */
public class RegularGaussianGridTest {

    private final PlanetaryGrid grid = new RegularGaussianGrid(64);

    @Test
    public void testGetNumRows() {
        assertEquals(64, grid.getNumRows());
    }

    @Test
    public void testGetNumCols() {
        assertEquals(128, grid.getNumCols(0));
        assertEquals(128, grid.getNumCols(32));
        assertEquals(128, grid.getNumCols(63));
    }

    @Test
    public void testGetRowIndex() {
        assertEquals(2, grid.getRowIndex(270));
    }

    @Test
    public void testGetNumBins() {
        assertEquals(64 * 128, grid.getNumBins());
    }

    @Test
    public void testGetFirstBinIndex() {
        assertEquals(0, grid.getFirstBinIndex(0));
        assertEquals(10 * 128, grid.getFirstBinIndex(10));
        assertEquals(50 * 128, grid.getFirstBinIndex(50));
    }

    @Test
    public void testGetCenterLat() {
        assertEquals(-51.62573, grid.getCenterLat(50), 0.0);
        assertEquals(-87.86379, grid.getCenterLat(63), 0.0);
    }

    @Test
    public void testGetCenterLatLon() {
        // bin=300 -> row=2, col=44
        final double[] center = grid.getCenterLatLon(300);

        final double centerLat = center[0];
        assertEquals(82.31291, centerLat, 0.0);

        final double centerLon = center[1];
        assertEquals(123.75, centerLon, 0.0);
    }

    @Test
    public void testGetBinIndex() {
        // lat=45.0, lon=90.0 -> row=15, col=32 -> bin=1952
        assertEquals(1952, grid.getBinIndex(45.0, 90.0));
    }

}
