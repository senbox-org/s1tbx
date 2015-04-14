package org.esa.snap.binning.support;

import org.junit.Test;

import static org.junit.Assert.*;

public class SEAGridTest {
    @Test
    public void testConstructors() {
        SEAGrid grid = new SEAGrid();
        assertEquals(2160, grid.getNumRows());

        grid = new SEAGrid(2);
        assertEquals(2, grid.getNumRows());

        try {
            new SEAGrid(1);
            fail("IAE expected");
        } catch (IllegalArgumentException e) {
        }

        try {
            new SEAGrid(0);
            fail("IAE expected");
        } catch (IllegalArgumentException e) {
        }

        try {
            new SEAGrid(-1);
            fail("IAE expected");
        } catch (IllegalArgumentException e) {
        }

        try {
            new SEAGrid(9);
            fail("IAE expected");
        } catch (IllegalArgumentException e) {
        }
    }

    @Test
    public void testDefaultGrid() {
        int NUM_ROWS = 2160;
        final int NUM_BINS = 5940422;
        SEAGrid grid = new SEAGrid();

        testGrid(grid, NUM_ROWS, NUM_BINS);
    }

    @Test
    public void testMerisFRGrid() {
        //300m resolution
        final int NUM_ROWS = 66792;
        final long NUM_BINS = 5680139712L;
        SEAGrid grid = new SEAGrid(NUM_ROWS);

        testGrid(grid, NUM_ROWS, NUM_BINS);
    }

    private static void testGrid(SEAGrid grid, int numRows, long numBins) {
        assertEquals(numRows, grid.getNumRows());
        assertEquals(numBins, grid.getNumBins());

        assertEquals(3, grid.getNumCols(0));
        assertEquals(2 * numRows, grid.getNumCols(numRows / 2));
        assertEquals(2 * numRows, grid.getNumCols(numRows / 2 + 1));
        assertEquals(3, grid.getNumCols(numRows - 1));

        final double W = 360.0 / (2 * numRows);
        final double H = 180.0 / numRows;

        assertEquals(0, grid.getBinIndex(+90.0, -180.0));
        assertEquals(1, grid.getBinIndex(+90.0, 0.0));
        assertEquals(2, grid.getBinIndex(+90.0, +180.0));
        assertEquals(numBins - 3, grid.getBinIndex(-90.0, -180.0));
        assertEquals(numBins - 2, grid.getBinIndex(-90.0, 0.0));
        assertEquals(numBins - 1, grid.getBinIndex(-90.0, +180.0));
        assertEquals(numBins / 2 - 1, grid.getBinIndex(+H / 2, +180.0));
        assertEquals(numBins / 2, grid.getBinIndex(-H / 2, -180.0));
    }

    @Test
    public void testPerformance() {

        long t0 = System.nanoTime();
        int N = 1000;
        for (int i = 0; i < N; i++) {
            new SEAGrid();
        }
        long t1 = System.nanoTime();

        final double seconds = (t1 - t0) / 1.0E9;
        assertTrue("Bad performance in " + SEAGrid.class + " detected, took " + seconds + " seconds",
                   seconds < 1.0);
    }


    @Test
    public void testGetRowIndex() {
        // 3, 8, 12, 12, 8, 3
        SEAGrid grid = new SEAGrid(6);
        try {
            grid.getRowIndex(-1);
            fail("ArrayIndexOutOfBoundsException?");
        } catch (ArrayIndexOutOfBoundsException e) {
        }
        assertEquals(0, grid.getRowIndex(0));
        assertEquals(0, grid.getRowIndex(1));
        assertEquals(0, grid.getRowIndex(2));
        assertEquals(1, grid.getRowIndex(3));
        assertEquals(1, grid.getRowIndex(4));
        assertEquals(1, grid.getRowIndex(5));
        assertEquals(1, grid.getRowIndex(6));
        assertEquals(1, grid.getRowIndex(7));
        assertEquals(1, grid.getRowIndex(8));
        assertEquals(1, grid.getRowIndex(9));
        assertEquals(1, grid.getRowIndex(10));
        assertEquals(2, grid.getRowIndex(11));
        assertEquals(2, grid.getRowIndex(12));
        assertEquals(2, grid.getRowIndex(13));
        assertEquals(2, grid.getRowIndex(14));
        assertEquals(2, grid.getRowIndex(15));
        assertEquals(2, grid.getRowIndex(16));
        assertEquals(2, grid.getRowIndex(17));
        assertEquals(2, grid.getRowIndex(18));
        assertEquals(2, grid.getRowIndex(19));
        assertEquals(2, grid.getRowIndex(20));
        assertEquals(2, grid.getRowIndex(21));
        assertEquals(2, grid.getRowIndex(22));
        assertEquals(3, grid.getRowIndex(23));
        assertEquals(3, grid.getRowIndex(24));
        assertEquals(3, grid.getRowIndex(25));
        assertEquals(3, grid.getRowIndex(26));
        assertEquals(3, grid.getRowIndex(27));
        assertEquals(3, grid.getRowIndex(28));
        assertEquals(3, grid.getRowIndex(29));
        assertEquals(3, grid.getRowIndex(30));
        assertEquals(3, grid.getRowIndex(31));
        assertEquals(3, grid.getRowIndex(32));
        assertEquals(3, grid.getRowIndex(33));
        assertEquals(3, grid.getRowIndex(34));
        assertEquals(4, grid.getRowIndex(35));
        assertEquals(4, grid.getRowIndex(36));
        assertEquals(4, grid.getRowIndex(37));
        assertEquals(4, grid.getRowIndex(38));
        assertEquals(4, grid.getRowIndex(39));
        assertEquals(4, grid.getRowIndex(40));
        assertEquals(4, grid.getRowIndex(41));
        assertEquals(4, grid.getRowIndex(42));
        assertEquals(5, grid.getRowIndex(43));
        assertEquals(5, grid.getRowIndex(44));
        assertEquals(5, grid.getRowIndex(45));
        try {
            grid.getRowIndex(46);
//            fail("ArrayIndexOutOfBoundsException?");
        } catch (ArrayIndexOutOfBoundsException e) {
        }
    }

    @Test
    public void testGetFirstBinIndex() {
        // 3, 8, 12, 12, 8, 3
        SEAGrid grid = new SEAGrid(6);
        try {
            grid.getFirstBinIndex(-1);
            fail("ArrayIndexOutOfBoundsException?");
        } catch (ArrayIndexOutOfBoundsException e) {
        }
        assertEquals(0, grid.getFirstBinIndex(0));
        assertEquals(3, grid.getFirstBinIndex(1));
        assertEquals(3 + 8, grid.getFirstBinIndex(2));
        assertEquals(3 + 8 + 12, grid.getFirstBinIndex(3));
        assertEquals(3 + 8 + 12 + 12, grid.getFirstBinIndex(4));
        assertEquals(3 + 8 + 12 + 12 + 8, grid.getFirstBinIndex(5));
        try {
            grid.getFirstBinIndex(6);
            fail("ArrayIndexOutOfBoundsException?");
        } catch (ArrayIndexOutOfBoundsException e) {
        }
    }

    @Test
    public void testGetCenterLat() {
        // 3, 8, 12, 12, 8, 3
        SEAGrid grid = new SEAGrid(6);
        try {
            grid.getCenterLat(-1);
            fail("ArrayIndexOutOfBoundsException?");
        } catch (ArrayIndexOutOfBoundsException e) {
        }
        assertEquals(+75.0, grid.getCenterLat(0), 1E-10);
        assertEquals(+45.0, grid.getCenterLat(1), 1E-10);
        assertEquals(+15.0, grid.getCenterLat(2), 1E-10);
        assertEquals(-15.0, grid.getCenterLat(3), 1E-10);
        assertEquals(-45.0, grid.getCenterLat(4), 1E-10);
        assertEquals(-75.0, grid.getCenterLat(5), 1E-10);
        try {
            grid.getCenterLat(6);
            fail("ArrayIndexOutOfBoundsException?");
        } catch (ArrayIndexOutOfBoundsException e) {
        }
    }

    @Test
    public void testGetBinIndex() {
        // 3, 8, 12, 12, 8, 3
        SEAGrid grid = new SEAGrid(6);
        assertEquals(0, grid.getBinIndex(+75.0, -500.0));
        assertEquals(0, grid.getBinIndex(+100, -120.0));

        assertEquals(0, grid.getBinIndex(+75.0, -120.0));
        assertEquals(2, grid.getBinIndex(+75.0, +120.0));
        assertEquals(3, grid.getBinIndex(+45.0, -157.5));
        assertEquals(3 + 7, grid.getBinIndex(+45.0, +157.5));
        assertEquals(3 + 8, grid.getBinIndex(+15.0, -165.0));
        assertEquals(3 + 8 + 11, grid.getBinIndex(+15.0, +165.0));
        assertEquals(3 + 8 + 12, grid.getBinIndex(-15.0, -165.0));
        assertEquals(3 + 8 + 12 + 11, grid.getBinIndex(-15.0, +165.0));
        assertEquals(3 + 8 + 12 + 12, grid.getBinIndex(-45.0, -157.5));
        assertEquals(3 + 8 + 12 + 12 + 7, grid.getBinIndex(-45.0, +157.5));
        assertEquals(3 + 8 + 12 + 12 + 8, grid.getBinIndex(-75.0, -120.0));
        assertEquals(3 + 8 + 12 + 12 + 8 + 2, grid.getBinIndex(-75.0, +120.0));

        assertEquals(3 + 8 + 12 + 12 + 8 + 2, grid.getBinIndex(-75.0, +500.0));
        assertEquals(3 + 8 + 12 + 12 + 8 + 2, grid.getBinIndex(-100.0, +120.0));
    }

    @Test
    public void testGetCenterLatLon() {
        // 3, 8, 12, 12, 8, 3
        SEAGrid grid = new SEAGrid(6);
        try {
            grid.getCenterLatLon(-1);
            fail("ArrayIndexOutOfBoundsException?");
        } catch (ArrayIndexOutOfBoundsException e) {
        }
        assertArrayEquals(new double[]{+75.0, -120.0}, grid.getCenterLatLon(0), 1E-10);
        assertArrayEquals(new double[]{+75.0, +120.0}, grid.getCenterLatLon(2), 1E-10);
        assertArrayEquals(new double[]{+45.0, -157.5}, grid.getCenterLatLon(3), 1E-10);
        assertArrayEquals(new double[]{+45.0, +157.5}, grid.getCenterLatLon(3 + 7), 1E-10);
        assertArrayEquals(new double[]{+15.0, -165.0}, grid.getCenterLatLon(3 + 8), 1E-10);
        assertArrayEquals(new double[]{+15.0, +165.0}, grid.getCenterLatLon(3 + 8 + 11), 1E-10);
        assertArrayEquals(new double[]{-15.0, -165.0}, grid.getCenterLatLon(3 + 8 + 12), 1E-10);
        assertArrayEquals(new double[]{-15.0, +165.0}, grid.getCenterLatLon(3 + 8 + 12 + 11), 1E-10);
        assertArrayEquals(new double[]{-45.0, -157.5}, grid.getCenterLatLon(3 + 8 + 12 + 12), 1E-10);
        assertArrayEquals(new double[]{-45.0, +157.5}, grid.getCenterLatLon(3 + 8 + 12 + 12 + 7), 1E-10);
        assertArrayEquals(new double[]{-75.0, -120.0}, grid.getCenterLatLon(3 + 8 + 12 + 12 + 8), 1E-10);
        assertArrayEquals(new double[]{-75.0, +120.0}, grid.getCenterLatLon(3 + 8 + 12 + 12 + 8 + 2), 1E-10);
        try {
            grid.getCenterLatLon(grid.getNumBins());
//            fail("ArrayIndexOutOfBoundsException?");
        } catch (ArrayIndexOutOfBoundsException e) {
        }
    }


    @Test
    public void testComputeRowCount() {
        // Test "standard" 9.28km grid
        assertEquals(2160, SEAGrid.computeRowCount(9.28));
        assertEquals(2160 * 2, SEAGrid.computeRowCount(9.28 / 2));
        assertEquals(2160 / 2, SEAGrid.computeRowCount(9.28 * 2));

        // Test MERIS FR equivalent at 300m
        assertEquals(66792, SEAGrid.computeRowCount(0.300));

        // And at 400m
        assertEquals(50094, SEAGrid.computeRowCount(0.400));
    }
}
