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
        final StxFactory factory = new StxFactory();
        final Stx stx = factory
                .withMinimum(-1.0)
                .withMaximum(1.0)
                .withHistogramBins(new int[]{1, 2, 3, 6, 6, 3, 2, 1})
                .create();

        assertEquals(-1.0, stx.getMinimum(), 1e-10);
        assertEquals(1.0, stx.getMaximum(), 1e-10);
        assertEquals(-0.125, stx.getMean(), 1e-10);
        assertEquals(-0.125, stx.getMedian(), 1e-10);
        assertNotNull(stx.getHistogram());
        assertEquals(1, stx.getHistogram().getNumBands());
        assertEquals(7, stx.getHistogram().getNumBins()[0]);
        assertEquals(-1.0, stx.getHistogram().getLowValue(0), 1e-10);
        assertEquals(1.0, stx.getHistogram().getHighValue(0), 1e-10);
    }

    @Test
    public void testMinMaxBinsIntHistogram() throws Exception {
        final StxFactory factory = new StxFactory();
        final Stx stx = factory
                .withMinimum(0)
                .withMaximum(10)
                .withIntHistogram(true)
                .withHistogramBins(new int[]{1, 2, 3, 6, 3, 2, 1})
                .create();

        assertEquals(0, stx.getMinimum(), 1e-10);
        assertEquals(10, stx.getMaximum(), 1e-10);
        assertEquals(0.42857142857142855, stx.getMean(), 1e-10);
        assertEquals(0.42857142857142855, stx.getMedian(), 1e-10);
        assertNotNull(stx.getHistogram());
        assertEquals(1, stx.getHistogram().getNumBands());
        assertEquals(7, stx.getHistogram().getNumBins()[0]);
        assertEquals(0, stx.getHistogram().getLowValue(0), 1e-10);
        assertEquals(11, stx.getHistogram().getHighValue(0), 1e-10);
    }
}
