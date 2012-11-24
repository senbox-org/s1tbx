package org.esa.beam.statistics.percentile.interpolated;

import org.junit.*;

import static org.junit.Assert.*;

public class PercentileComputerTest {

    @Before
    public void setUp() throws Exception {
    }

    @Test
    public void testSomething() {
        float[] values = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10};

        assertEquals(1, PercentileComputer.compute(0, values), 1e-7);
        assertEquals(2, PercentileComputer.compute(10, values), 1e-7);
        assertEquals(3, PercentileComputer.compute(20, values), 1e-7);
        assertEquals(4, PercentileComputer.compute(30, values), 1e-7);
        assertEquals(5, PercentileComputer.compute(40, values), 1e-7);
        assertEquals(6, PercentileComputer.compute(50, values), 1e-7);
        assertEquals(7, PercentileComputer.compute(60, values), 1e-7);
        assertEquals(8, PercentileComputer.compute(70, values), 1e-7);
        assertEquals(9, PercentileComputer.compute(80, values), 1e-7);
        assertEquals(10, PercentileComputer.compute(90, values), 1e-7);
        assertEquals(10, PercentileComputer.compute(99, values), 1e-7);
    }
}
