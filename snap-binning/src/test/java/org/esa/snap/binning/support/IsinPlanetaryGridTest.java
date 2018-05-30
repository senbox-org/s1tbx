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
        assertEquals(1809, binIndex);

        // Hamburg
        binIndex = grid.getBinIndex(53.551086, 9.993682);
        assertEquals(71207731803L, binIndex);

        // Cape of Good Hope
        binIndex = grid.getBinIndex(-34.357203, -18.49755);
        assertEquals(56705221612L, binIndex);

        // Dome C
        binIndex = grid.getBinIndex(-75, -125);
        assertEquals(91806001416L, binIndex);
    }

    @Test
    public void testGetBinIndex_500m() {
        final IsinPlanetaryGrid grid = new IsinPlanetaryGrid(18 * 2400);

        long binIndex = grid.getBinIndex(0.0, 0.0);
        assertEquals(1809, binIndex);

        // Hamburg
        binIndex = grid.getBinIndex(53.551086, 9.993682);
        assertEquals(142415471803L, binIndex);

        // Cape of Good Hope
        binIndex = grid.getBinIndex(-34.357203, -18.49755);
        assertEquals(113410451612L, binIndex);

        // Dome C
        binIndex = grid.getBinIndex(-75, -125);
        assertEquals(183512001416L, binIndex);
    }

    @Test
    public void testGetBinIndex_250m() {
        final IsinPlanetaryGrid grid = new IsinPlanetaryGrid(18 * 4800);

        long binIndex = grid.getBinIndex(0.0, 0.0);
        assertEquals(1809, binIndex);

        // Hamburg
        binIndex = grid.getBinIndex(53.551086, 9.993682);
        assertEquals(284930951803L, binIndex);

        // Cape of Good Hope
        binIndex = grid.getBinIndex(-34.357203, -18.49755);
        assertEquals(227020911612L, binIndex);

        // Dome C
        binIndex = grid.getBinIndex(-75, -125);
        assertEquals(367124001416L, binIndex);
    }

    @Test
    public void testToBinIndex() {
        IsinPoint isinPoint = new IsinPoint(0, 0, 0, 0);
        assertEquals(0, IsinPlanetaryGrid.toBinIndex(isinPoint));

        isinPoint = new IsinPoint(0, 0, 0, 1);
        assertEquals(1, IsinPlanetaryGrid.toBinIndex(isinPoint));

        isinPoint = new IsinPoint(0, 0, 0, 17);
        assertEquals(17, IsinPlanetaryGrid.toBinIndex(isinPoint));

        isinPoint = new IsinPoint(0, 0, 1, 0);
        assertEquals(100, IsinPlanetaryGrid.toBinIndex(isinPoint));

        isinPoint = new IsinPoint(0, 0, 16, 0);
        assertEquals(1600, IsinPlanetaryGrid.toBinIndex(isinPoint));

        isinPoint = new IsinPoint(0, 0, 16, 5);
        assertEquals(1605, IsinPlanetaryGrid.toBinIndex(isinPoint));

        isinPoint = new IsinPoint(0, 156, 0, 0);
        assertEquals(1560000, IsinPlanetaryGrid.toBinIndex(isinPoint));

        isinPoint = new IsinPoint(0, 157, 17, 6);
        assertEquals(1571706, IsinPlanetaryGrid.toBinIndex(isinPoint));

        isinPoint = new IsinPoint(158, 0, 0, 0);
        assertEquals(15800000000L, IsinPlanetaryGrid.toBinIndex(isinPoint));

        isinPoint = new IsinPoint(4799, 4799, 35, 17);
        assertEquals(479947993517L, IsinPlanetaryGrid.toBinIndex(isinPoint));
    }

    @Test
    public void testToIsinPoint() {
        IsinPoint point = IsinPlanetaryGrid.toIsinPoint(0L);
        assertEquals(0.0, point.getX(), 1e-8);
        assertEquals(0.0, point.getY(), 1e-8);
        assertEquals(0, point.getTile_col());
        assertEquals(0, point.getTile_line());

        point = IsinPlanetaryGrid.toIsinPoint(1L);
        assertEquals(0.0, point.getX(), 1e-8);
        assertEquals(0.0, point.getY(), 1e-8);
        assertEquals(0, point.getTile_col());
        assertEquals(1, point.getTile_line());

        point = IsinPlanetaryGrid.toIsinPoint(17L);
        assertEquals(0.0, point.getX(), 1e-8);
        assertEquals(0.0, point.getY(), 1e-8);
        assertEquals(0, point.getTile_col());
        assertEquals(17, point.getTile_line());

        point = IsinPlanetaryGrid.toIsinPoint(100L);
        assertEquals(0.0, point.getX(), 1e-8);
        assertEquals(0.0, point.getY(), 1e-8);
        assertEquals(1, point.getTile_col());
        assertEquals(0, point.getTile_line());

        point = IsinPlanetaryGrid.toIsinPoint(1600L);
        assertEquals(0.0, point.getX(), 1e-8);
        assertEquals(0.0, point.getY(), 1e-8);
        assertEquals(16, point.getTile_col());
        assertEquals(0, point.getTile_line());

        point = IsinPlanetaryGrid.toIsinPoint(1605L);
        assertEquals(0.0, point.getX(), 1e-8);
        assertEquals(0.0, point.getY(), 1e-8);
        assertEquals(16, point.getTile_col());
        assertEquals(5, point.getTile_line());

        point = IsinPlanetaryGrid.toIsinPoint(1560000L);
        assertEquals(0.0, point.getX(), 1e-8);
        assertEquals(156.0, point.getY(), 1e-8);
        assertEquals(0, point.getTile_col());
        assertEquals(0, point.getTile_line());

        point = IsinPlanetaryGrid.toIsinPoint(1571807L);
        assertEquals(0.0, point.getX(), 1e-8);
        assertEquals(157.0, point.getY(), 1e-8);
        assertEquals(18, point.getTile_col());
        assertEquals(7, point.getTile_line());

        point = IsinPlanetaryGrid.toIsinPoint(15800000000L);
        assertEquals(158.0, point.getX(), 1e-8);
        assertEquals(0.0, point.getY(), 1e-8);
        assertEquals(0, point.getTile_col());
        assertEquals(0, point.getTile_line());

        point = IsinPlanetaryGrid.toIsinPoint(479947993517L);
        assertEquals(4799.0, point.getX(), 1e-8);
        assertEquals(4799.0, point.getY(), 1e-8);
        assertEquals(35, point.getTile_col());
        assertEquals(17, point.getTile_line());
    }

    @Test
    public void testGetRowIndex_1km() {
        final IsinPlanetaryGrid grid = new IsinPlanetaryGrid(18 * 1200);

        assertEquals(0, grid.getRowIndex(0));
        assertEquals(20400, grid.getRowIndex(17));
        assertEquals(7200, grid.getRowIndex(1606));
        assertEquals(1356, grid.getRowIndex(1560001));
        assertEquals(9757, grid.getRowIndex(1571808));
        assertEquals(21599, grid.getRowIndex(119911993517L));
    }

    @Test
    public void testGetRowIndex_500m() {
        final IsinPlanetaryGrid grid = new IsinPlanetaryGrid(18 * 2400);

        assertEquals(0, grid.getRowIndex(0));
        assertEquals(40800, grid.getRowIndex(17));
        assertEquals(14400, grid.getRowIndex(1606));
        assertEquals(2556, grid.getRowIndex(1560001));
        assertEquals(19357, grid.getRowIndex(1571808));
        assertEquals(43199, grid.getRowIndex(239923993517L));
    }

    @Test
    public void testGetRowIndex_250m() {
        final IsinPlanetaryGrid grid = new IsinPlanetaryGrid(18 * 4800);

        assertEquals(0, grid.getRowIndex(0));
        assertEquals(81600, grid.getRowIndex(17));
        assertEquals(28800, grid.getRowIndex(1606));
        assertEquals(4956, grid.getRowIndex(1560001));
        assertEquals(38557, grid.getRowIndex(1571808));
        assertEquals(86399, grid.getRowIndex(479947993517L));
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
        assertEquals(6760001, grid.getFirstBinIndex(1876));
        assertEquals(11990017, grid.getFirstBinIndex(21599));
    }

    @Test
    public void testGetFirstBinIndex_500m() {
        final IsinPlanetaryGrid grid = new IsinPlanetaryGrid(18 * 2400);

        assertEquals(0, grid.getFirstBinIndex(0));
        assertEquals(150000, grid.getFirstBinIndex(15));
        assertEquals(18770000, grid.getFirstBinIndex(1877));
        assertEquals(23990017, grid.getFirstBinIndex(43199));
    }

    @Test
    public void testGetFirstBinIndex_250m() {
        final IsinPlanetaryGrid grid = new IsinPlanetaryGrid(18 * 4800);

        assertEquals(0, grid.getFirstBinIndex(0));
        assertEquals(160000, grid.getFirstBinIndex(16));
        assertEquals(18780000, grid.getFirstBinIndex(1878));
        assertEquals(47990017, grid.getFirstBinIndex(86399));
    }
}
