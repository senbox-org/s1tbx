package org.esa.snap.core.util.grid.isin;


import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class TileTest {

    private Tile tile;

    @Before
    public void setUp() {
        tile = new Tile();
    }

    @Test
    public void testSetGetNl() {
        tile.setNl(123);
        assertEquals(123, tile.getNl());
    }

    @Test
    public void testSetGetNs() {
        tile.setNs(124);
        assertEquals(124, tile.getNs());
    }

    @Test
    public void testSetGetNl_tile() {
        tile.setNl_tile(125);
        assertEquals(125, tile.getNl_tile());
    }

    @Test
    public void testSetGetNs_tile() {
        tile.setNs_tile(126);
        assertEquals(126, tile.getNs_tile());
    }

    @Test
    public void testSetGetNl_offset() {
        tile.setNl_offset(127);
        assertEquals(127, tile.getNl_offset());
    }

    @Test
    public void testSetGetNs_offset() {
        tile.setNs_offset(128);
        assertEquals(128, tile.getNs_offset());
    }

    @Test
    public void testSetGetNl_p() {
        tile.setNl_p(129);
        assertEquals(129, tile.getNl_p());
    }

    @Test
    public void testSetGetNs_p() {
        tile.setNs_p(130);
        assertEquals(130, tile.getNs_p());
    }

    @Test
    public void testSetGetSiz_x() {
        tile.setSiz_x(3.98);
        assertEquals(3.98, tile.getSiz_x(), 1e-8);
    }

    @Test
    public void testSetGetSiz_y() {
        tile.setSiz_y(4.09);
        assertEquals(4.09, tile.getSiz_y(), 1e-8);
    }

    @Test
    public void testSetGetUl_x() {
        tile.setUl_x(5.1);
        assertEquals(5.1, tile.getUl_x(), 1e-8);
    }

    @Test
    public void testSetGetUl_y() {
        tile.setUl_y(6.21);
        assertEquals(6.21, tile.getUl_y(), 1e-8);
    }

    @Test
    public void testInit_ISIN_K() {
        final ProjectionParam params = ProjectionParamFactory.get(ProjectionType.ISIN_K);

        tile.init(params);

        assertEquals(21600, tile.getNl());
        assertEquals(43200, tile.getNs());

        assertEquals(1200, tile.getNl_tile());
        assertEquals(1200, tile.getNs_tile());

        assertEquals(0, tile.getNl_offset());
        assertEquals(0, tile.getNs_offset());

        assertEquals(21600, tile.getNl_p());
        assertEquals(43200, tile.getNs_p());

        assertEquals(926.62543305, tile.getSiz_x(), 1e-8);
        assertEquals(926.62543305, tile.getSiz_y(), 1e-8);

        assertEquals(-2.0014646041283473E7, tile.getUl_x(), 1e-8);
        assertEquals(1.0007091364283474E7, tile.getUl_y(), 1e-8);
    }

    @Test
    public void testInit_ISIN_H() {
        final ProjectionParam params = ProjectionParamFactory.get(ProjectionType.ISIN_H);

        tile.init(params);

        assertEquals(43200, tile.getNl());
        assertEquals(86400, tile.getNs());

        assertEquals(2400, tile.getNl_tile());
        assertEquals(2400, tile.getNs_tile());

        assertEquals(0, tile.getNl_offset());
        assertEquals(0, tile.getNs_offset());

        assertEquals(43200, tile.getNl_p());
        assertEquals(86400, tile.getNs_p());

        assertEquals(463.312716525, tile.getSiz_x(), 1e-8);
        assertEquals(463.312716525, tile.getSiz_y(), 1e-8);

        assertEquals(-2.0014877697641738E7, tile.getUl_x(), 1e-8);
        assertEquals(1.0007323020641737E7, tile.getUl_y(), 1e-8);
    }

    @Test
    public void testInit_ISIN_Q() {
        final ProjectionParam params = ProjectionParamFactory.get(ProjectionType.ISIN_Q);

        tile.init(params);

        assertEquals(86400, tile.getNl());
        assertEquals(172800, tile.getNs());

        assertEquals(4800, tile.getNl_tile());
        assertEquals(4800, tile.getNs_tile());

        assertEquals(0, tile.getNl_offset());
        assertEquals(0, tile.getNs_offset());

        assertEquals(86400, tile.getNl_p());
        assertEquals(172800, tile.getNs_p());

        assertEquals(231.6563582625, tile.getSiz_x(), 1e-8);
        assertEquals(231.6563582625, tile.getSiz_y(), 1e-8);

        assertEquals(-2.0014993525820866E7, tile.getUl_x(), 1e-8);
        assertEquals(1.0007438848820869E7, tile.getUl_y(), 1e-8);
    }

    @Test
    public void testGetIsinDef() {
        final IsinDef isinDef = Tile.getIsinDef(5640, 6371007.181);

        assertNotNull(isinDef);
        assertEquals(5640, isinDef.nrow_half);
        assertEquals(11280, isinDef.nrow);
        assertEquals(1.5696105365918595E-7, isinDef.sphere_inv, 1e-8);
        assertEquals(3590.535516153159, isinDef.ang_size_inv, 1e-8);

        assertEquals(5640, isinDef.icol_cen.length);
        assertEquals(2, isinDef.icol_cen[0]);
        assertEquals(36, isinDef.icol_cen[11]);
        assertEquals(404, isinDef.icol_cen[128]);
        assertEquals(4336, isinDef.icol_cen[1416]);
        assertEquals(7978, isinDef.icol_cen[2820]);
        assertEquals(9472, isinDef.icol_cen[3578]);
        assertEquals(11280, isinDef.icol_cen[5639]);

        assertEquals(11280, isinDef.ncol_inv.length);
        assertEquals(0.3333333333333333, isinDef.ncol_inv[0], 1e-8);
        assertEquals(0.02857142857142857, isinDef.ncol_inv[5], 1e-8);
        assertEquals(0.003215434083601286, isinDef.ncol_inv[49], 1e-8);
        assertEquals(5.78368999421631E-4, isinDef.ncol_inv[275], 1e-8);
        assertEquals(1.1410314924691921E-4, isinDef.ncol_inv[1432], 1e-8);
        assertEquals(5.1129972389814906E-5, isinDef.ncol_inv[3766], 1e-8);
        assertEquals(0.0, isinDef.ncol_inv[9234], 1e-8);    // @todo 2 tb/tb why the hell do they allocate double the values when they're not filled with data 2018-03-21

        assertEquals(5.635742378161289E-4, isinDef.col_dist_inv, 1e-8);
    }
}
