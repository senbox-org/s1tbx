package org.esa.beam.dataio.merisl3;

import junit.framework.TestCase;

import java.awt.Point;

public class ISINGridTest extends TestCase {

    private ISINGrid grid = new ISINGrid(ISINGrid.DEFAULT_ROW_COUNT);

    public void testDefaults() {
        assertEquals(2160, ISINGrid.DEFAULT_ROW_COUNT);
        assertEquals(2160, grid.getRowCount());
        assertEquals(5940422, grid.getTotalBinCount());
        assertEquals(180.0 / grid.getRowCount(), grid.getDeltaLat(), 1e-10);
        assertEquals(Math.PI * ISINGrid.RE / grid.getRowCount(), grid.getBinSize(), 1e-10);
    }

    public void test_getRowLength() {
        assertEquals(3, grid.getRowLength(0));
        assertEquals(9, grid.getRowLength(1));
        assertEquals(16, grid.getRowLength(2));
        assertEquals(22, grid.getRowLength(3));

        assertEquals(4320, grid.getRowLength(2160 / 2 - 1));
        assertEquals(4320, grid.getRowLength(2160 / 2));

        assertEquals(3, grid.getRowLength(2160 - 1 - 0));
        assertEquals(9, grid.getRowLength(2160 - 1 - 1));
        assertEquals(16, grid.getRowLength(2160 - 1 - 2));
        assertEquals(22, grid.getRowLength(2160 - 1 - 3));
    }

    public void test_getRowOffset() {
        assertEquals(0, grid.getBinOffset(0));
        assertEquals(3, grid.getBinOffset(1));
        assertEquals(3 + 9, grid.getBinOffset(2));
        assertEquals(3 + 9 + 16, grid.getBinOffset(3));
        assertEquals(3 + 9 + 16 + 22, grid.getBinOffset(4));

        assertEquals(5940422 / 2 - 4320, grid.getBinOffset(2160 / 2 - 1));
        assertEquals(5940422 / 2, grid.getBinOffset(2160 / 2));

        assertEquals(5940422 - (3), grid.getBinOffset(2160 - 1 - 0));
        assertEquals(5940422 - (3 + 9), grid.getBinOffset(2160 - 1 - 1));
        assertEquals(5940422 - (3 + 9 + 16), grid.getBinOffset(2160 - 1 - 2));
        assertEquals(5940422 - (3 + 9 + 16 + 22), grid.getBinOffset(2160 - 1 - 3));
    }

    public void test_getRowIndex() {
        assertEquals(0, grid.getRowIndex(0));
        assertEquals(1, grid.getRowIndex(3));
        assertEquals(2, grid.getRowIndex(3 + 9));
        assertEquals(3, grid.getRowIndex(3 + 9 + 16));
        assertEquals(4, grid.getRowIndex(3 + 9 + 16 + 22));

        assertEquals(2160 / 2 - 1, grid.getRowIndex(5940422 / 2 - 4320));
        assertEquals(2160 / 2, grid.getRowIndex(5940422 / 2));

        assertEquals(2160 - 1 - 1, grid.getRowIndex(5940422 - (3) - 1));
        assertEquals(2160 - 1 - 0, grid.getRowIndex(5940422 - (3)));
        assertEquals(2160 - 1 - 0, grid.getRowIndex(5940422 - (3) + 1));
        assertEquals(2160 - 1 - 0, grid.getRowIndex(5940422 - (3) + 2));
        assertEquals(2160 - 1 - 1, grid.getRowIndex(5940422 - (3 + 9)));
        assertEquals(2160 - 1 - 1, grid.getRowIndex(5940422 - (3 + 9) + 1));
        assertEquals(2160 - 1 - 2, grid.getRowIndex(5940422 - (3 + 9 + 16)));
        assertEquals(2160 - 1 - 3, grid.getRowIndex(5940422 - (3 + 9 + 16 + 22)));

        // out of range
        assertEquals(-1, grid.getRowIndex(-1000));
        assertEquals(-1, grid.getRowIndex(-1));
        assertEquals(-1, grid.getRowIndex(5940422));
        assertEquals(-1, grid.getRowIndex(5940422 + 1000));
    }

    public void test_getGridPoint() {
        final Point point = new Point();

        assertEquals(new Point(2, 0), grid.getGridPoint((0) + 2, point));
        assertEquals(new Point(7, 1), grid.getGridPoint((3) + 7, point));
        assertEquals(new Point(11, 2), grid.getGridPoint((3 + 9) + 11, point));
        assertEquals(new Point(19, 3), grid.getGridPoint((3 + 9 + 16) + 19, point));
        assertEquals(new Point(25, 4), grid.getGridPoint((3 + 9 + 16 + 22) + 25, point));

        assertEquals(new Point(632, 2160 / 2 - 1), grid.getGridPoint((5940422 / 2 - 4320) + 632, point));
        assertEquals(new Point(1743, 2160 / 2), grid.getGridPoint((5940422 / 2) + 1743, point));

        assertEquals(new Point(1, 2160 - 1 - 0), grid.getGridPoint(5940422 - (3) + 1, point));
        assertEquals(new Point(6, 2160 - 1 - 1), grid.getGridPoint(5940422 - (3 + 9) + 6, point));
        assertEquals(new Point(15, 2160 - 1 - 2), grid.getGridPoint(5940422 - (3 + 9 + 16) + 15, point));
        assertEquals(new Point(20, 2160 - 1 - 3), grid.getGridPoint(5940422 - (3 + 9 + 16 + 22) + 20, point));

        // out of range
        assertEquals(new Point(-1, -1), grid.getGridPoint(-1000, point));
        assertEquals(new Point(-1, -1), grid.getGridPoint(-1, point));
        assertEquals(new Point(-1, -1), grid.getGridPoint(5940422, point));
        assertEquals(new Point(-1, -1), grid.getGridPoint(5940422 + 1000, point));
    }

    public void test_getBinIndex() {
        assertEquals((0) + 2, grid.getBinIndex(new Point(2, 0)));
        assertEquals((3) + 7, grid.getBinIndex(new Point(7, 1)));
        assertEquals((3 + 9) + 11, grid.getBinIndex(new Point(11, 2)));
        assertEquals((3 + 9 + 16) + 19, grid.getBinIndex(new Point(19, 3)));
        assertEquals((3 + 9 + 16 + 22) + 25, grid.getBinIndex(new Point(25, 4)));

        assertEquals((5940422 / 2 - 4320) + 632, grid.getBinIndex(new Point(632, 2160 / 2 - 1)));
        assertEquals((5940422 / 2) + 1743, grid.getBinIndex(new Point(1743, 2160 / 2)));

        assertEquals(5940422 - (3) + 1, grid.getBinIndex(new Point(1, 2160 - 1 - 0)));
        assertEquals(5940422 - (3 + 9) + 6, grid.getBinIndex(new Point(6, 2160 - 1 - 1)));
        assertEquals(5940422 - (3 + 9 + 16) + 15, grid.getBinIndex(new Point(15, 2160 - 1 - 2)));
        assertEquals(5940422 - (3 + 9 + 16 + 22) + 20, grid.getBinIndex(new Point(20, 2160 - 1 - 3)));

        // out of range
        assertEquals(-1, grid.getBinIndex(new Point(-1, -1)));
        assertEquals(-1, grid.getBinIndex(new Point(-1, 23)));
        assertEquals(-1, grid.getBinIndex(new Point(546, -1)));
        assertEquals(-1, grid.getBinIndex(new Point(4320 + 1, 46)));
        assertEquals(-1, grid.getBinIndex(new Point(343, 5940422 + 1)));
    }


    public void test_getColIndex() {

        // test valid rowIndex and lon
        assertEquals(0, grid.getColIndex(0, 0));
        assertEquals(0, grid.getColIndex(0, 0 + 45));
        assertEquals(1, grid.getColIndex(0, 120));
        assertEquals(1, grid.getColIndex(0, 120 + 45));
        assertEquals(2, grid.getColIndex(0, 2 * 120));
        assertEquals(2, grid.getColIndex(0, 2 * 120 + 45));
        assertEquals(3, grid.getColIndex(0, 3 * 120));
        assertEquals(3, grid.getColIndex(0, 3 * 120 + 45));

        // test longitude out of range 0...360
        try {
            grid.getColIndex(0, -2 * 120 - 45);
        } catch (Exception e) {
            fail();
        }
        try {
            grid.getColIndex(0, -120 - 45);
        } catch (Exception e) {
            fail();
        }
        try {
            grid.getColIndex(0, -45);
        } catch (Exception e) {
            fail();
        }
        try {
            grid.getColIndex(0, 3 * 120);
        } catch (Exception e) {
            fail();
        }
        try {
            grid.getColIndex(0, 3 * 120 + 45);
        } catch (Exception e) {
            fail();
        }

        // test rowIndex out of range
        try {
            grid.getColIndex(-1, 45.3);
            fail();
        } catch (ArrayIndexOutOfBoundsException e) {
        }

        try {
            grid.getColIndex(grid.getRowCount(), 45.3);
            fail();
        } catch (ArrayIndexOutOfBoundsException e) {
        }
    }

    public void test_getBinIndexWithLon() {

    }
    public void testDetectRowCount() throws Exception {
        final String name9277 = "L3_ENV_MER_A443_m__20030301_GLOB_SI_ACR_9277x9277_-90+90+-180+180_0000.nc";
        assertEquals(2160, ISINGrid.detectRowCount(name9277));
        
        final String name4320 = "L3_ENV_MER_CHL1_d__20070101_GLOB_SI_ESA_4638x4638_-90+90+-180+180_0000.nc";
        assertEquals(4320, ISINGrid.detectRowCount(name4320));
        
        // fallback
        final String nameFoo = "foo";
        assertEquals(2160, ISINGrid.detectRowCount(nameFoo));
    }
}

