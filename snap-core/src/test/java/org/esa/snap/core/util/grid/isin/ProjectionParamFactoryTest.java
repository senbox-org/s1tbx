package org.esa.snap.core.util.grid.isin;

import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class ProjectionParamFactoryTest {

    @Test
    public void testGetISIN_K() {
        final ProjectionParam param = ProjectionParamFactory.get(ProjectionType.ISIN_K);
        assertNotNull(param);

        assertEquals(ProjectionType.ISIN_K, param.projection);
        assertISINParams(param);
    }

    @Test
    public void testGetISIN_H() {
        final ProjectionParam param = ProjectionParamFactory.get(ProjectionType.ISIN_H);
        assertNotNull(param);

        assertEquals(ProjectionType.ISIN_H, param.projection);
        assertISINParams(param);
    }

    @Test
    public void testGetISIN_Q() {
        final ProjectionParam param = ProjectionParamFactory.get(ProjectionType.ISIN_Q);
        assertNotNull(param);

        assertEquals(ProjectionType.ISIN_Q, param.projection);
        assertISINParams(param);
    }

    private void assertISINParams(ProjectionParam param) {
        assertEquals(-20015109.354, param.ul_xul, 1e-8);
        assertEquals(10007554.677, param.ul_yul, 1e-8);
        assertEquals(926.62543305, param.pixel_size, 1e-8);
        assertEquals(-1, param.sphere_code);
        assertEquals(6371007.181, param.sphere, 1e-8);
        assertEquals(1200, param.nl_tile);
        assertEquals(1200, param.ns_tile);
        assertEquals(18, param.ntile_line);
        assertEquals(36, param.ntile_samp);
        assertEquals(18 * 1200, param.nl_grid);
        assertEquals(36 * 1200, param.ns_grid);
        assertArrayEquals(new int[] {90, 540, 1080, 4320}, param.nl_global);
        assertArrayEquals(new int[] {180, 1080, 2160, 8640}, param.ns_global);
        assertArrayEquals(new int[] {0, 0, 0, 0}, param.nl_offset);
        assertArrayEquals(new int[] {0, 0, 0, 0}, param.ns_offset);
    }
}
