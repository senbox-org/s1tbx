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
        // @todo 1 tb/tb continue here 2018-05-29
    }
}
