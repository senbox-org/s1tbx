package org.esa.beam.framework.datamodel;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Norman Fomferra
 */
public class StxFactoryTest {
    @Test
    public void testSimpleCase() throws Exception {
        final StxFactory factory = new StxFactory();
        final Stx stx = factory
                .withMinimum(0.0)
                .withMaximum(1.0)
                .withHistogramBins(new int[]{1, 2, 3, 6, 3, 2, 1})
                .create();

        assertEquals(0.0, stx.getMinimum(), 1e-10);
        assertEquals(1.0, stx.getMaximum(), 1e-10);
        assertEquals(0.42857142857142855, stx.getMean(), 1e-10);
        assertEquals(0.42857142857142855, stx.getMedian(), 1e-10);
        assertEquals(7, stx.getHistogramBinCount());
    }
}
