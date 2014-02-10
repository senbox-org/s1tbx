package org.esa.pfa.fe;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Norman Fomferra
 */
public class ConnectivityMetricsTest {
    @Test
    public void testCompute2x3_noneSet() throws Exception {
        ConnectivityMetrics cm = ConnectivityMetrics.compute(3, 2, new byte[]{
                0, 0, 0,
                0, 0, 0,
        });

        assertEquals(24, cm.connectionCountMax);
        assertEquals(0, cm.connectionCount);
        assertEquals(0.0, cm.connectionRatio, 1e-5);

        assertEquals(0, cm.insideCount);
        assertEquals(0, cm.borderCount);
        assertEquals(2.0, cm.fractalIndex, 1e-5);
    }

    @Test
    public void testCompute2x3_allSet() throws Exception {
        ConnectivityMetrics cm = ConnectivityMetrics.compute(3, 2, new byte[]{
                1, 1, 1,
                1, 1, 1,
        });

        assertEquals(24, cm.connectionCountMax);
        assertEquals(24, cm.connectionCount);
        assertEquals(1.0, cm.connectionRatio, 1e-5);

        assertEquals(6, cm.insideCount);
        assertEquals(0, cm.borderCount);
        assertEquals(1.0, cm.fractalIndex, 1e-5);
    }

    @Test
    public void testCompute2x3_noConn() throws Exception {
        ConnectivityMetrics cm = ConnectivityMetrics.compute(3, 2, new byte[]{
                1, 0, 1,
                0, 1, 0,
        });

        assertEquals(24, cm.connectionCountMax);
        assertEquals(5, cm.connectionCount);
        assertEquals(5.0/24, cm.connectionRatio, 1e-5);

        assertEquals(0, cm.insideCount);
        assertEquals(3, cm.borderCount);
        assertEquals(2.0, cm.fractalIndex, 1e-5);
    }

    @Test
    public void testCompute2x3_someConn() throws Exception {
        ConnectivityMetrics cm = ConnectivityMetrics.compute(3, 2, new byte[]{
                1, 1, 1,
                0, 1, 0,
        });

        assertEquals(24, cm.connectionCountMax);
        assertEquals(12, cm.connectionCount);
        assertEquals(0.5, cm.connectionRatio, 1e-5);

        assertEquals(1, cm.insideCount);
        assertEquals(3, cm.borderCount);
        assertEquals(1.75, cm.fractalIndex, 1e-5);
    }

    @Test
    public void testCompute4x4() throws Exception {
        ConnectivityMetrics cm = ConnectivityMetrics.compute(4, 4, new byte[]{
                1, 1, 1, 1,
                1, 1, 1, 1,
                1, 1, 0, 0,
                1, 1, 0, 0,
        });

        assertEquals(64, cm.connectionCountMax);
        assertEquals(44, cm.connectionCount);
        assertEquals(0.6875, cm.connectionRatio, 1e-5);

        assertEquals(8, cm.insideCount);
        assertEquals(4, cm.borderCount);
        assertEquals(1.0 + 1.0/3.0, cm.fractalIndex, 1e-5);
    }

    @Test
    public void testCompute5x5() throws Exception {
        ConnectivityMetrics cm = ConnectivityMetrics.compute(5, 5, new byte[]{
                1, 0, 1, 1, 1,
                1, 1, 0, 1, 1,
                1, 0, 1, 0, 1,
                0, 1, 0, 1, 0,
                0, 1, 0, 1, 0,
        });

        assertEquals(100, cm.connectionCountMax);
        assertEquals(34, cm.connectionCount);
        assertEquals(0.34, cm.connectionRatio, 1e-5);
    }

    @Test
    public void testCompute5x5_2() throws Exception {
        ConnectivityMetrics cm = ConnectivityMetrics.compute(5, 5, new byte[]{
                0, 1, 1, 1, 0,
                1, 1, 1, 1, 1,
                1, 1, 1, 1, 1,
                1, 1, 1, 1, 1,
                0, 1, 1, 1, 0,
        });

        assertEquals(100, cm.connectionCountMax);
        assertEquals(76, cm.connectionCount);
        assertEquals(0.76, cm.connectionRatio, 1e-5);
    }

    @Test
    public void testCompute5x5_3() throws Exception {
        ConnectivityMetrics cm = ConnectivityMetrics.compute(5, 5, new byte[]{
                1, 1, 0, 0, 0,
                1, 1, 1, 1, 0,
                0, 1, 1, 1, 0,
                0, 1, 1, 1, 1,
                0, 0, 0, 1, 1,
        });

        assertEquals(100, cm.connectionCountMax);
        assertEquals(48, cm.connectionCount);
        assertEquals(0.48, cm.connectionRatio, 1e-5);
    }
}
