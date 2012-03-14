package org.esa.beam.framework.datamodel;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

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
        assertEquals(0.0, stx.getHistogram().getPTileThreshold(0.5)[0] + stx.getHistogramBinWidth(), 1e-10);
        assertNotNull(stx.getHistogram());
        assertEquals(1, stx.getHistogram().getNumBands());
        assertEquals(8, stx.getHistogram().getNumBins()[0]);
        assertEquals(-1.0, stx.getHistogram().getLowValue(0), 1e-10);
        assertEquals(1.0, stx.getHistogram().getHighValue(0), 1e-10);
    }

    @Test
    public void testMinMaxBinsIntHistogram() throws Exception {
        StxFactory factory = new StxFactory();
        factory
                .withMinimum(0)
                .withMaximum(10)
                .withIntHistogram(true)
                .withHistogramBins(new int[]{1, 2, 3, 6, 3, 2, 1});
        Stx stx = factory.create();

        assertEquals(0, stx.getMinimum(), 1e-10);
        assertEquals(10, stx.getMaximum(), 1e-10);
        assertEquals(4.7142857142857135, stx.getMean(), 1e-10);
        assertEquals(4.714285714285714, stx.getMedian(), 1e-10);
        assertEquals(4.714285714285714, stx.getHistogram().getPTileThreshold(0.5)[0], 1e-10);

        assertNotNull(stx.getHistogram());
        assertEquals(1, stx.getHistogram().getNumBands());
        assertEquals(7, stx.getHistogram().getNumBins()[0]);
        assertEquals(0, stx.getHistogram().getLowValue(0), 1e-10);
        assertEquals(11, stx.getHistogram().getHighValue(0), 1e-10);
    }
}
