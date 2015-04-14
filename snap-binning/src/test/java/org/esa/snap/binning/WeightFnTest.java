package org.esa.snap.binning;

import org.junit.Test;

import static org.junit.Assert.*;

public class WeightFnTest {

    @Test
    public void testCreatePow() {
        WeightFn f;

        f = WeightFn.createPow(0.0);
        assertNotNull(f);
        assertEquals(1.0f, f.eval(0), 1e-10f);
        assertEquals(1.0f, f.eval(1), 1e-10f);
        assertEquals(1.0f, f.eval(2), 1e-10f);
        assertEquals(1.0f, f.eval(4), 1e-10f);

        f = WeightFn.createPow(1.0);
        assertNotNull(f);
        assertEquals(0.0f, f.eval(0), 1e-10f);
        assertEquals(1.0f, f.eval(1), 1e-10f);
        assertEquals(2.0f, f.eval(2), 1e-10f);
        assertEquals(4.0f, f.eval(4), 1e-10f);

        f = WeightFn.createPow(0.5);
        assertNotNull(f);
        assertEquals(0.0f, f.eval(0), 1e-10f);
        assertEquals(1.0f, f.eval(1), 1e-10f);
        assertEquals((float) Math.sqrt(2), f.eval(2), 1e-10f);
        assertEquals(2.0f, f.eval(4), 1e-10f);

        f = WeightFn.createPow(0.42);
        assertNotNull(f);
        assertEquals((float) Math.pow(0, 0.42), f.eval(0), 1e-10f);
        assertEquals((float) Math.pow(1, 0.42), f.eval(1), 1e-10f);
        assertEquals((float) Math.pow(2, 0.42), f.eval(2), 1e-10f);
        assertEquals((float) Math.pow(4, 0.42), f.eval(4), 1e-10f);
    }
}
