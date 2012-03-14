package org.esa.beam.framework.datamodel;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author Norman Fomferra
 */
public class StxFactoryTest {
    @Test
    public void testMinMaxBins() throws Exception {
        StxFactory factory = new StxFactory();
        factory
                .withMinimum(-1.0)
                .withMaximum(1.0)
                .withHistogramBins(new int[]{1, 2, 3, 6, 6, 3, 2, 1});
        Stx stx = factory.create();

        assertEquals(-1.0, stx.getMinimum(), 1e-10);
        assertEquals(1.0, stx.getMaximum(), 1e-10);
        assertEquals(-0.125, stx.getMean(), 1e-10);
        assertEquals(0.0, stx.getMedian(), 1e-10);
        assertNotNull(stx.getHistogram());
        assertEquals(1, stx.getHistogram().getNumBands());
        assertEquals(8, stx.getHistogram().getNumBins()[0]);
        assertEquals(-1.0, stx.getHistogram().getLowValue(0), 1e-10);
        assertEquals(1.0, stx.getHistogram().getHighValue(0), 1e-10);
        assertArrayEquals(new int[]{1, 2, 3, 6, 6, 3, 2, 1}, stx.getHistogramBins());
    }

    @Test
    public void testMinMaxBinsIntHistogram() throws Exception {
        StxFactory factory = new StxFactory();
        factory
                .withMinimum(1)
                .withMaximum(100)
                .withIntHistogram(true)
                .withHistogramBins(new int[]{1, 2, 3, 6, 6, 3, 2, 1});
        Stx stx = factory.create();

        assertEquals(1, stx.getMinimum(), 1e-10);
        assertEquals(100, stx.getMaximum(), 1e-10);
        assertEquals(44.75, stx.getMean(), 1e-10);
        assertEquals(51.0, stx.getMedian(), 1e-10);

        assertNotNull(stx.getHistogram());
        assertEquals(1, stx.getHistogram().getNumBands());
        assertEquals(8, stx.getHistogram().getNumBins()[0]);
        assertEquals(1, stx.getHistogram().getLowValue(0), 1e-10);
        assertEquals(101, stx.getHistogram().getHighValue(0), 1e-10);
        assertArrayEquals(new int[]{1, 2, 3, 6, 6, 3, 2, 1}, stx.getHistogramBins());
    }

    @Test
    public void testMinMaxBinsLogHistogram() throws Exception {
        StxFactory factory = new StxFactory();
        factory
                .withMinimum(0)
                .withMaximum(100)
                .withLogHistogram(true)
                .withHistogramBins(new int[]{1, 2, 3, 6, 6, 3, 2, 1});
        Stx stx = factory.create();

        assertEquals(0, stx.getMinimum(), 1e-10);
        assertEquals(100, stx.getMaximum(), 1e-10);
        assertEquals(6.5316581703709, stx.getMean(), 1e-10);
        assertEquals(9.0498756211209, stx.getMedian(), 1e-10);

        assertNotNull(stx.getHistogram());
        assertEquals(1, stx.getHistogram().getNumBands());
        assertEquals(8, stx.getHistogram().getNumBins()[0]);
        assertEquals(0, stx.getHistogram().getLowValue(0), 1e-10);
        assertEquals(2.0043213737826, stx.getHistogram().getHighValue(0), 1e-10);
        assertArrayEquals(new int[]{1, 2, 3, 6, 6, 3, 2, 1}, stx.getHistogramBins());
    }

    @Test
    public void testMinIsMax() throws Exception {
        StxFactory factory = new StxFactory();
        factory
                .withMinimum(0)
                .withMaximum(0)
                .withHistogramBins(new int[]{1000, 0, 0, 0, 0, 0, 0, 0});
        Stx stx = factory.create();

        assertEquals(0, stx.getMinimum(), 1e-10);
        assertEquals(0, stx.getMaximum(), 1e-10);
        assertEquals(0, stx.getMean(), 1e-10);
        assertEquals(0, stx.getMedian(), 1e-10);

        assertNotNull(stx.getHistogram());
        assertEquals(1, stx.getHistogram().getNumBands());
        assertEquals(8, stx.getHistogram().getNumBins()[0]);
        assertEquals(0, stx.getHistogram().getLowValue(0), 1E-10);
        assertEquals(1.0E-10, stx.getHistogram().getHighValue(0), 1E-10);
        assertTrue(stx.getHistogram().getLowValue(0) < stx.getHistogram().getHighValue(0));
        assertArrayEquals(new int[]{1000, 0, 0, 0, 0, 0, 0, 0}, stx.getHistogramBins());
    }
}
