package org.esa.snap.binning.support;

import org.esa.snap.core.util.grid.isin.IsinPoint;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class IsinPlanetaryGridTest {

    @Test
    public void testConstruct_incorrectNumberOfRows() {

        try {
            new IsinPlanetaryGrid(11);
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException expected) {
        }

        try {
            new IsinPlanetaryGrid(-4);
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException expected) {
        }

        try {
            new IsinPlanetaryGrid(18 * 1200 - 1);
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException expected) {
        }

        try {
            new IsinPlanetaryGrid(18 * 2400 + 1);
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException expected) {
        }
    }

    @Test
    public void testGetBinIndex_1km() {
        final IsinPlanetaryGrid grid = new IsinPlanetaryGrid(18 * 1200);

        long binIndex = grid.getBinIndex(0.0, 0.0);
        assertEquals(91800000000L, binIndex);

        // Hamburg
        binIndex = grid.getBinIndex(53.551086, 9.993682);
        assertEquals(31807730712L, binIndex);

        // Cape of Good Hope
        binIndex = grid.getBinIndex(-34.357203, -18.49755);
        assertEquals(121605220567L, binIndex);

        // Dome C
        binIndex = grid.getBinIndex(-75, -125);
        assertEquals(161406000918L, binIndex);
    }

    @Test
    public void testGetBinIndex_500m() {
        final IsinPlanetaryGrid grid = new IsinPlanetaryGrid(18 * 2400);

        long binIndex = grid.getBinIndex(0.0, 0.0);
        assertEquals(91800000000L, binIndex);

        // Hamburg
        binIndex = grid.getBinIndex(53.551086, 9.993682);
        assertEquals(31815471424L, binIndex);

        // Cape of Good Hope
        binIndex = grid.getBinIndex(-34.357203, -18.49755);
        assertEquals(121610451134L, binIndex);

        // Dome C
        binIndex = grid.getBinIndex(-75, -125);
        assertEquals(161412001835L, binIndex);
    }

    @Test
    public void testGetBinIndex_250m() {
        final IsinPlanetaryGrid grid = new IsinPlanetaryGrid(18 * 4800);

        long binIndex = grid.getBinIndex(0.0, 0.0);
        assertEquals(91800000000L, binIndex);

        // Hamburg
        binIndex = grid.getBinIndex(53.551086, 9.993682);
        assertEquals(31830952849L, binIndex);

        // Cape of Good Hope
        binIndex = grid.getBinIndex(-34.357203, -18.49755);
        assertEquals(121620912270L, binIndex);

        // Dome C
        binIndex = grid.getBinIndex(-75, -125);
        assertEquals(161424003671L, binIndex);
    }

    @Test
    public void testToBinIndex() {
        IsinPoint isinPoint = new IsinPoint(0, 0, 0, 0);
        assertEquals(0, IsinPlanetaryGrid.toBinIndex(isinPoint));

        isinPoint = new IsinPoint(2599, 0, 0, 0);
        assertEquals(2599, IsinPlanetaryGrid.toBinIndex(isinPoint));

        isinPoint = new IsinPoint(0, 1111, 0, 0);
        assertEquals(11110000, IsinPlanetaryGrid.toBinIndex(isinPoint));

        isinPoint = new IsinPoint(2222, 1111, 0, 0);
        assertEquals(11112222, IsinPlanetaryGrid.toBinIndex(isinPoint));

        isinPoint = new IsinPoint(2222, 1111, 33, 0);
        assertEquals(3311112222L, IsinPlanetaryGrid.toBinIndex(isinPoint));

        isinPoint = new IsinPoint(2222, 1111, 33, 44);
        assertEquals(443311112222L, IsinPlanetaryGrid.toBinIndex(isinPoint));

        isinPoint = new IsinPoint(0, 0, 0, 1);
        assertEquals(10000000000L, IsinPlanetaryGrid.toBinIndex(isinPoint));

        isinPoint = new IsinPoint(0, 0, 0, 17);
        assertEquals(170000000000L, IsinPlanetaryGrid.toBinIndex(isinPoint));

        isinPoint = new IsinPoint(0, 0, 1, 0);
        assertEquals(100000000L, IsinPlanetaryGrid.toBinIndex(isinPoint));

        isinPoint = new IsinPoint(0, 0, 16, 0);
        assertEquals(1600000000L, IsinPlanetaryGrid.toBinIndex(isinPoint));

        isinPoint = new IsinPoint(0, 0, 16, 5);
        assertEquals(51600000000L, IsinPlanetaryGrid.toBinIndex(isinPoint));

        isinPoint = new IsinPoint(0, 156, 0, 0);
        assertEquals(1560000, IsinPlanetaryGrid.toBinIndex(isinPoint));

        isinPoint = new IsinPoint(0, 157, 17, 6);
        assertEquals(61701570000L, IsinPlanetaryGrid.toBinIndex(isinPoint));

        isinPoint = new IsinPoint(158, 0, 0, 0);
        assertEquals(158L, IsinPlanetaryGrid.toBinIndex(isinPoint));

        isinPoint = new IsinPoint(4799, 4799, 35, 17);
        assertEquals(173547994799L, IsinPlanetaryGrid.toBinIndex(isinPoint));
    }

    @Test
    public void testToIsinPoint() {
        IsinPoint point = IsinPlanetaryGrid.toIsinPoint(0L);
        assertEquals(0.0, point.getX(), 1e-8);
        assertEquals(0.0, point.getY(), 1e-8);
        assertEquals(0, point.getTile_col());
        assertEquals(0, point.getTile_line());

        point = IsinPlanetaryGrid.toIsinPoint(2599L);
        assertEquals(2599.0, point.getX(), 1e-8);
        assertEquals(0.0, point.getY(), 1e-8);
        assertEquals(0, point.getTile_col());
        assertEquals(0, point.getTile_line());

        point = IsinPlanetaryGrid.toIsinPoint(11110000L);
        assertEquals(0.0, point.getX(), 1e-8);
        assertEquals(1111.0, point.getY(), 1e-8);
        assertEquals(0, point.getTile_col());
        assertEquals(0, point.getTile_line());

        point = IsinPlanetaryGrid.toIsinPoint(11112222L);
        assertEquals(2222.0, point.getX(), 1e-8);
        assertEquals(1111.0, point.getY(), 1e-8);
        assertEquals(0, point.getTile_col());
        assertEquals(0, point.getTile_line());

        point = IsinPlanetaryGrid.toIsinPoint(3311112222L);
        assertEquals(2222.0, point.getX(), 1e-8);
        assertEquals(1111.0, point.getY(), 1e-8);
        assertEquals(33, point.getTile_col());
        assertEquals(0, point.getTile_line());

        point = IsinPlanetaryGrid.toIsinPoint(443311112222L);
        assertEquals(2222.0, point.getX(), 1e-8);
        assertEquals(1111.0, point.getY(), 1e-8);
        assertEquals(33, point.getTile_col());
        assertEquals(44, point.getTile_line());

        point = IsinPlanetaryGrid.toIsinPoint(1600L);
        assertEquals(1600.0, point.getX(), 1e-8);
        assertEquals(0.0, point.getY(), 1e-8);
        assertEquals(0, point.getTile_col());
        assertEquals(0, point.getTile_line());

        point = IsinPlanetaryGrid.toIsinPoint(1560000L);
        assertEquals(0.0, point.getX(), 1e-8);
        assertEquals(156.0, point.getY(), 1e-8);
        assertEquals(0, point.getTile_col());
        assertEquals(0, point.getTile_line());

        point = IsinPlanetaryGrid.toIsinPoint(1571807L);
        assertEquals(1807.0, point.getX(), 1e-8);
        assertEquals(157.0, point.getY(), 1e-8);
        assertEquals(0, point.getTile_col());
        assertEquals(0, point.getTile_line());

        point = IsinPlanetaryGrid.toIsinPoint(15800000000L);
        assertEquals(0.0, point.getX(), 1e-8);
        assertEquals(0.0, point.getY(), 1e-8);
        assertEquals(58, point.getTile_col());
        assertEquals(1, point.getTile_line());

        point = IsinPlanetaryGrid.toIsinPoint(173547994799L);
        assertEquals(4799.0, point.getX(), 1e-8);
        assertEquals(4799.0, point.getY(), 1e-8);
        assertEquals(35, point.getTile_col());
        assertEquals(17, point.getTile_line());
    }

    @Test
    public void testGetRowIndex_1km() {
        final IsinPlanetaryGrid grid = new IsinPlanetaryGrid(18 * 1200);

        assertEquals(0, grid.getRowIndex(0));
        assertEquals(20400, grid.getRowIndex(170000000000L));
        assertEquals(7200, grid.getRowIndex(61600000000L));
        assertEquals(156, grid.getRowIndex(1560001));
        assertEquals(157, grid.getRowIndex(1571808));
        assertEquals(21599, grid.getRowIndex(173511991199L));
    }

    @Test
    public void testGetRowIndex_500m() {
        final IsinPlanetaryGrid grid = new IsinPlanetaryGrid(18 * 2400);

        assertEquals(0, grid.getRowIndex(0));
        assertEquals(40800, grid.getRowIndex(170000000000L));
        assertEquals(14400, grid.getRowIndex(61600000000L));
        assertEquals(156, grid.getRowIndex(1560001));
        assertEquals(157, grid.getRowIndex(1571808));
        assertEquals(43199, grid.getRowIndex(173523992399L));
    }

    @Test
    public void testGetRowIndex_250m() {
        final IsinPlanetaryGrid grid = new IsinPlanetaryGrid(18 * 4800);

        assertEquals(0, grid.getRowIndex(0));
        assertEquals(81600, grid.getRowIndex(170000000000L));
        assertEquals(28800, grid.getRowIndex(61600000000L));
        assertEquals(156, grid.getRowIndex(1560001));
        assertEquals(157, grid.getRowIndex(1571808));
        assertEquals(86399, grid.getRowIndex(173547994799L));
    }

    @Test
    public void testGetNumBins_1km() {
        final IsinPlanetaryGrid grid = new IsinPlanetaryGrid(18 * 1200);

        assertEquals(933120000, grid.getNumBins());
    }

    @Test
    public void testGetNumBins_500m() {
        final IsinPlanetaryGrid grid = new IsinPlanetaryGrid(18 * 2400);

        assertEquals(3732480000L, grid.getNumBins());
    }

    @Test
    public void testGetNumBins_250m() {
        final IsinPlanetaryGrid grid = new IsinPlanetaryGrid(18 * 4800);

        assertEquals(14929920000L, grid.getNumBins());
    }

    @Test
    public void testGetNumRows_1km() {
        final IsinPlanetaryGrid grid = new IsinPlanetaryGrid(18 * 1200);

        assertEquals(21600, grid.getNumRows());
    }

    @Test
    public void testGetNumRows_500m() {
        final IsinPlanetaryGrid grid = new IsinPlanetaryGrid(18 * 2400);

        assertEquals(43200, grid.getNumRows());
    }

    @Test
    public void testGetNumRows_250m() {
        final IsinPlanetaryGrid grid = new IsinPlanetaryGrid(18 * 4800);

        assertEquals(86400, grid.getNumRows());
    }

    @Test
    public void testGetNumCols_1km() {
        final IsinPlanetaryGrid grid = new IsinPlanetaryGrid(18 * 1200);

        assertEquals(43200, grid.getNumCols(12));
    }

    @Test
    public void testGetNumCols_500m() {
        final IsinPlanetaryGrid grid = new IsinPlanetaryGrid(18 * 2400);

        assertEquals(86400, grid.getNumCols(13));
    }

    @Test
    public void testGetNumCols_250m() {
        final IsinPlanetaryGrid grid = new IsinPlanetaryGrid(18 * 4800);

        assertEquals(172800, grid.getNumCols(14));
    }

    @Test
    public void testGetFirstBinIndex_1km() {
        final IsinPlanetaryGrid grid = new IsinPlanetaryGrid(18 * 1200);

        assertEquals(0, grid.getFirstBinIndex(0));
        assertEquals(140000, grid.getFirstBinIndex(14));
        assertEquals(10006760000L, grid.getFirstBinIndex(1876));
        assertEquals(170011990000L, grid.getFirstBinIndex(21599));
    }

    @Test
    public void testGetFirstBinIndex_500m() {
        final IsinPlanetaryGrid grid = new IsinPlanetaryGrid(18 * 2400);

        assertEquals(0, grid.getFirstBinIndex(0));
        assertEquals(150000, grid.getFirstBinIndex(15));
        assertEquals(18770000, grid.getFirstBinIndex(1877));
        assertEquals(170023990000L, grid.getFirstBinIndex(43199));
    }

    @Test
    public void testGetFirstBinIndex_250m() {
        final IsinPlanetaryGrid grid = new IsinPlanetaryGrid(18 * 4800);

        assertEquals(0, grid.getFirstBinIndex(0));
        assertEquals(160000, grid.getFirstBinIndex(16));
        assertEquals(18780000, grid.getFirstBinIndex(1878));
        assertEquals(170047990000L, grid.getFirstBinIndex(86399));
    }
}
