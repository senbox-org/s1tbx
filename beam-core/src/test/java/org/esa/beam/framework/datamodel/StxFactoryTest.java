package org.esa.beam.framework.datamodel;

import com.bc.ceres.core.ProgressMonitor;
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
                .withMinimum(0.1)
                .withMaximum(10)
                .withLogHistogram(true)
                .withHistogramBins(new int[]{1, 2, 3, 6, 6, 3, 2, 1});
        Stx stx = factory.create();

        assertNotNull(stx.getHistogram());
        assertEquals(1, stx.getHistogram().getNumBands());
        assertEquals(8, stx.getHistogram().getNumBins()[0]);

        assertEquals(0.1, stx.getMinimum(), 1e-10);
        assertEquals(10, stx.getMaximum(), 1e-10);
        assertEquals(Math.pow(10.0, stx.getHistogram().getMean()[0]), stx.getMean(), 1e-3);
        assertEquals(Math.pow(10.0, 0), stx.getMedian(), 1e-3);

        assertEquals(-1.0, stx.getHistogram().getLowValue(0), 1e-10);
        assertEquals(1.0, stx.getHistogram().getHighValue(0), 1e-10);
        assertArrayEquals(new int[]{1, 2, 3, 6, 6, 3, 2, 1}, stx.getHistogramBins());
    }

    @Test
    public void testMinMaxBinsLogHistogramWithNegativeMinimum() throws Exception {
        StxFactory factory = new StxFactory();
        factory
                .withMinimum(-10)
                .withMaximum(+10)
                .withLogHistogram(true)
                .withHistogramBins(new int[]{1, 2, 3, 6, 6, 3, 2, 1});
        Stx stx = factory.create();

        assertEquals(-10, stx.getMinimum(), 1e-10);
        assertEquals(+10, stx.getMaximum(), 1e-10);
        assertEquals(0.0, stx.getMean(), 1e-3);
        assertEquals(0.0, stx.getMedian(), 1e-3);

        assertNotNull(stx.getHistogram());
        assertEquals(1, stx.getHistogram().getNumBands());
        assertEquals(8, stx.getHistogram().getNumBins()[0]);
        assertEquals(-9.0, stx.getHistogram().getLowValue(0), 1e-10); // 1E-9 is the max value we handle
        assertEquals(1.0, stx.getHistogram().getHighValue(0), 1e-10);
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


    @Test
    public void testSignedByteBandStatistics() throws Exception {
        final Band band = createTestBand(ProductData.TYPE_INT8, 11, 13);
        final Stx stx = new StxFactory().create(band, ProgressMonitor.NULL);
        assertEquals(0.0, stx.getMedian(), 1.0e-1);
        assertEquals(0.0, stx.getMean(), 0.0);
        assertEquals(41.4, stx.getStandardDeviation(), 1.0e-1);
    }

    @Test
    public void testFloatBandStatistics() throws Exception {
        final Band band = createTestBand(ProductData.TYPE_FLOAT32, 100, 120);
        final Stx stx = new StxFactory().create(band, ProgressMonitor.NULL);
        assertEquals(0.0, stx.getMedian(), 0.0);
        assertEquals(0.0, stx.getMean(), 0.0);
        assertEquals(3464.2, stx.getStandardDeviation(), 1.0e-1);
    }

    @Test
    public void testFloatBandStatisticsWithGapsInHistogram() throws Exception {
        final Band band = createTestBand(ProductData.TYPE_FLOAT32, 10, 12);
        Stx stx = new StxFactory().create(band, ProgressMonitor.NULL);
        assertEquals(0.0, stx.getMedian(), 0.0);
        assertEquals(0.0, stx.getMean(), 0.0);
        assertEquals(34.8, stx.getStandardDeviation(), 1.0e-1);
    }

    @Test
    public void testFloatBandStatisticsWithNoDataValueSet() throws Exception {
        final Band band = createTestBand(ProductData.TYPE_FLOAT32, 100, 120);
        band.setNoDataValueUsed(true);
        band.setNoDataValue(-0.5);

        Stx stx = new StxFactory().withRoiImage(band.getValidMaskImage()).create(band, ProgressMonitor.NULL);
        assertEquals(5.0e-1, stx.getMedian(), 0.1e-1);
        assertEquals(4.1e-5, stx.getMean(), 0.1e-5);
        assertEquals(3464.4, stx.getStandardDeviation(), 1.0e-1);

        band.setNoDataValue(0.5);
        stx = new StxFactory().withRoiImage(band.getValidMaskImage()).create(band, ProgressMonitor.NULL);
        assertEquals(-5.0e-1, stx.getMedian(), 0.1e-1);
        assertEquals(-4.1e-5, stx.getMean(), 0.1e-5);
        assertEquals(3464.4, stx.getStandardDeviation(), 1.0e-1);
    }

    private Band createTestBand(int type, int w, int h) {
        final Product product = new Product("F", "F", w, h);
        final double mean = (w * h - 1.0) / 2.0;
        final Band band = new VirtualBand("V", type, w, h, "(Y-0.5) * " + w + " + (X-0.5) - " + mean);
        product.addBand(band);

        return band;
    }
}
