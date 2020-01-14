package org.esa.snap.core.dataio.geocoding.inverse;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class ResultTest {

    private Result result;

    @Before
    public void setUp() {
        result = new Result();
    }

    @Test
    public void testConstruction() {
        assertEquals(0.0, result.x, 1e-8);
        assertEquals(0.0, result.y, 1e-8);
        assertEquals(Double.MAX_VALUE, result.delta, 1e-8);
    }

    @Test
    public void testUpdate() {
        boolean update = result.update(12, 13, 14);
        assertTrue(update);
        assertEquals(12.0, result.x, 1e-8);
        assertEquals(13.0, result.y, 1e-8);
        assertEquals(14.0, result.delta, 1e-8);

        update = result.update(13, 14, 12);
        assertTrue(update);
        assertEquals(13.0, result.x, 1e-8);
        assertEquals(14.0, result.y, 1e-8);
        assertEquals(12.0, result.delta, 1e-8);

        update = result.update(14, 15, 16);  // delta too large - will be skipped tb 2019-12-16
        assertFalse(update);
        assertEquals(13.0, result.x, 1e-8);
        assertEquals(14.0, result.y, 1e-8);
        assertEquals(12.0, result.delta, 1e-8);
    }
}
