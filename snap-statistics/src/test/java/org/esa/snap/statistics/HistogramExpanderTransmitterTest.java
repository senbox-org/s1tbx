package org.esa.snap.statistics;

import org.esa.snap.core.datamodel.HistogramStxOp;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.media.jai.Histogram;

import static org.junit.Assert.*;

public class HistogramExpanderTransmitterTest {

    @Before
    public void before() throws Exception {
    }

    @After
    public void after() throws Exception {
    }

    @Test
    public void testCreateExpandedHistogramOp_withPositiveValues() throws Exception {
        Histogram oldHistogram = new Histogram(10, 2.5, 7.5, 1);
        int[] oldHistogramBins = oldHistogram.getBins(0);
        for (int i = 0; i < oldHistogramBins.length; i++) {
            oldHistogramBins[i] = i + 20;
        }
        HistogramStxOp expandedHistogramOp = HistogramExpanderTransmitter.createExpandedHistogramOp(oldHistogram, 0, 10, false, 10);

        Histogram histogram = expandedHistogramOp.getHistogram();
        assertEquals(20, histogram.getNumBins(0));
        int[] expecteds = new int[]{0, 0, 0, 0, 0, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 0, 0, 0, 0, 0};
        assertArrayEquals(expecteds, histogram.getBins(0));
    }

    @Test
    public void testCreateExpandedHistogramOp_withZeroCrossingValues() throws Exception {
        Histogram oldHistogram = new Histogram(10, -2.5, 2.5, 1);
        int[] oldHistogramBins = oldHistogram.getBins(0);
        for (int i = 0; i < oldHistogramBins.length; i++) {
            oldHistogramBins[i] = i + 20;
        }
        HistogramStxOp expandedHistogramOp = HistogramExpanderTransmitter.createExpandedHistogramOp(oldHistogram, -2.5, 7.5, false, 10);

        Histogram histogram = expandedHistogramOp.getHistogram();
        assertEquals(20, histogram.getNumBins(0));
        int[] expecteds = new int[]{20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
        assertArrayEquals(expecteds, histogram.getBins(0));
    }

    @Test
    public void testCreateExpandedHistogramOp_withNegativeValues() throws Exception {
        Histogram oldHistogram = new Histogram(10, -12.5, -7.5, 1);
        int[] oldHistogramBins = oldHistogram.getBins(0);
        for (int i = 0; i < oldHistogramBins.length; i++) {
            oldHistogramBins[i] = i + 20;
        }
        HistogramStxOp expandedHistogramOp = HistogramExpanderTransmitter.createExpandedHistogramOp(oldHistogram, -15, -5, false, 10);

        Histogram histogram = expandedHistogramOp.getHistogram();
        assertEquals(20, histogram.getNumBins(0));
        int[] expecteds = new int[]{0, 0, 0, 0, 0, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 0, 0, 0, 0, 0};
        assertArrayEquals(expecteds, histogram.getBins(0));
    }

    @Test
    public void testCreateExpandedHistogramOp_withLowStartRangeAndExpandToABiggerValueRange() throws Exception {
        Histogram oldHistogram = new Histogram(1000, 0, 1e-20, 1);
        int[] oldHistogramBins = oldHistogram.getBins(0);
        for (int i = 0; i < oldHistogramBins.length; i++) {
            oldHistogramBins[i] = 1;
        }

        double minimum = 1;
        double maximum = 15;
        HistogramStxOp expandedHistogramOp = HistogramExpanderTransmitter.createExpandedHistogramOp(oldHistogram, minimum, maximum, false, 1000);

        Histogram histogram = expandedHistogramOp.getHistogram();
        assertEquals(0, histogram.getLowValue(0), 1e-12);
        assertEquals(15, histogram.getHighValue(0), 1e-12);
        assertEquals(1000, histogram.getNumBins(0));
        assertEquals(0, histogram.getBinLowValue(0, 0), 1e-12);
        assertEquals(0.015, histogram.getBinLowValue(0, 1), 1e-12);
        int[] expecteds = new int[1000];
        expecteds[0] = 1000;
        assertArrayEquals(expecteds, histogram.getBins(0));
    }

    @Test
    public void testCreateExpandedHistogramOp_withVeryLowStartRangeAndExpandToABiggerValueRange() throws Exception {
        Histogram oldHistogram = new Histogram(1000, 0, Double.MIN_VALUE, 1);
        int[] oldHistogramBins = oldHistogram.getBins(0);
        for (int i = 0; i < oldHistogramBins.length; i++) {
            oldHistogramBins[i] = 1;
        }

        double minimum = 1;
        double maximum = 15;
        HistogramStxOp expandedHistogramOp = HistogramExpanderTransmitter.createExpandedHistogramOp(oldHistogram, minimum, maximum, false, 1000);

        Histogram histogram = expandedHistogramOp.getHistogram();
        assertEquals(0, histogram.getLowValue(0), 1e-12);
        assertEquals(15, histogram.getHighValue(0), 1e-12);
        assertEquals(1000, histogram.getNumBins(0));
        assertEquals(0, histogram.getBinLowValue(0, 0), 1e-12);
        assertEquals(0.015, histogram.getBinLowValue(0, 1), 1e-12);
        int[] expecteds = new int[1000];
        expecteds[0] = 1000;
        assertArrayEquals(expecteds, histogram.getBins(0));
    }


//    @Test
//    public void testComputeBinWidth() throws Exception {
        /*
        try {
           Method method = HistogramExpanderTransmitter.getClass().getMethod("computeBinWidth", double.class, double.class, int.class);
           method.setAccessible(true);
           method.invoke(<Object>, <Parameters>);
        } catch(NoSuchMethodException e) {
        } catch(IllegalAccessException e) {
        } catch(InvocationTargetException e) {
        }
        */
//    }
}
