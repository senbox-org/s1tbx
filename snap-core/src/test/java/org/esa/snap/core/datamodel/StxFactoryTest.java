package org.esa.snap.core.datamodel;

import com.bc.ceres.core.ProgressMonitor;
import org.junit.Test;

import java.awt.Color;

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
    public void testMinAndMaxAreTheSameVeryLargePositiveValue() throws Exception {
        double aLargePositiveValue = Double.MAX_VALUE;
        double aSlightlySmallerValue = Math.nextAfter(aLargePositiveValue, Double.NEGATIVE_INFINITY);
        StxFactory factory = new StxFactory();
        factory
                .withMinimum(aLargePositiveValue)
                .withMaximum(aLargePositiveValue)
                .withHistogramBins(new int[]{1000, 0, 0, 0, 0, 0, 0, 0});
        Stx stx = factory.create();

        assertEquals(aLargePositiveValue, stx.getMinimum(), 1e-10);
        assertEquals(aLargePositiveValue, stx.getMaximum(), 1e-10);
        assertEquals(aLargePositiveValue, stx.getMean(), 1e-10);
        assertEquals(aLargePositiveValue, stx.getMedian(), 1e-10);

        assertNotNull(stx.getHistogram());
        assertEquals(1, stx.getHistogram().getNumBands());
        assertEquals(8, stx.getHistogram().getNumBins()[0]);
        assertEquals(aSlightlySmallerValue, stx.getHistogram().getLowValue(0), 1E-10);
        assertEquals(aLargePositiveValue, stx.getHistogram().getHighValue(0), 1E-10);
        assertTrue(stx.getHistogram().getLowValue(0) < stx.getHistogram().getHighValue(0));
        assertArrayEquals(new int[]{1000, 0, 0, 0, 0, 0, 0, 0}, stx.getHistogramBins());
    }

    @Test
    public void testMinAndMaxAreTheSameVeryLargeNegativeValue() throws Exception {
        double aLargeNegativeValue = -Double.MAX_VALUE;
        double aSlightlyLargerValue = Math.nextUp(aLargeNegativeValue);
        StxFactory factory = new StxFactory();
        factory
                .withMinimum(aLargeNegativeValue)
                .withMaximum(aLargeNegativeValue)
                .withHistogramBins(new int[]{1000, 0, 0, 0, 0, 0, 0, 0});
        Stx stx = factory.create();

        assertEquals(aLargeNegativeValue, stx.getMinimum(), 1e-10);
        assertEquals(aLargeNegativeValue, stx.getMaximum(), 1e-10);
        assertEquals(aLargeNegativeValue, stx.getMean(), 1e-10);
        assertEquals(aLargeNegativeValue, stx.getMedian(), 1e-10);

        assertNotNull(stx.getHistogram());
        assertEquals(1, stx.getHistogram().getNumBands());
        assertEquals(8, stx.getHistogram().getNumBins()[0]);
        assertEquals(aLargeNegativeValue, stx.getHistogram().getLowValue(0), 1E-10);
        assertEquals(aSlightlyLargerValue, stx.getHistogram().getHighValue(0), 1E-10);
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

    @Test
    public void testCreateStxForMultipleBands() throws Exception {
        final Band testBand1 = createTestBand(ProductData.TYPE_FLOAT64, 10, 10, -100);
        final Band testBand2 = createTestBand(ProductData.TYPE_FLOAT64, 10, 10, -200);
        Stx stx = new StxFactory().withHistogramBinCount(2097152).create(null, new RasterDataNode[]{testBand1, testBand2}, ProgressMonitor.NULL);
        assertEquals(100, stx.getMinimum(), 1E-3);
        assertEquals(299, stx.getMaximum(), 1E-3);
        assertEquals(199.5, stx.getMean(), 1E-3);
        assertEquals(199.5, stx.getMedian(), 1E-3);
        assertEquals(279, stx.getHistogram().getPTileThreshold(0.9)[0], 1E-3);
        assertEquals(199, stx.getHistogram().getPTileThreshold(0.5)[0], 1E-3);
        assertEquals(119, stx.getHistogram().getPTileThreshold(0.1)[0], 1E-3);
    }

    @Test
    public void testCreateStxForNullBands() throws Exception {
        final Band testBand1 = createTestBand(ProductData.TYPE_FLOAT64, 10, 10, -100);
        final Band testBand2 = null;
        Stx stx = new StxFactory().withHistogramBinCount(524288).create(null, new RasterDataNode[]{testBand1, testBand2}, ProgressMonitor.NULL);
        assertEquals(100, stx.getMinimum(), 1E-3);
        assertEquals(199, stx.getMaximum(), 1E-3);
        assertEquals(149.5, stx.getMean(), 1E-3);
        assertEquals(149.5, stx.getMedian(), 1E-3);
        assertEquals(189, stx.getHistogram().getPTileThreshold(0.9)[0], 1E-3);
        assertEquals(149, stx.getHistogram().getPTileThreshold(0.5)[0], 1E-3);
        assertEquals(109, stx.getHistogram().getPTileThreshold(0.1)[0], 1E-3);
    }

    @Test
    public void testCreateStxForMultipleBandsAndRoiMask() throws Exception {
        final Band testBand1 = createTestBand(ProductData.TYPE_FLOAT64, 10, 10, -100);
        final Band testBand2 = createTestBand(ProductData.TYPE_FLOAT64, 10, 10, -200);
        final Mask roiMask = testBand1.getProduct().addMask("validMask", "X < 5", "testValidMask", Color.gray, Double.NaN);
        final Mask roiMask2 = testBand2.getProduct().addMask("validMask", "X < 5", "testValidMask", Color.gray, Double.NaN);
        Stx stx = new StxFactory()
                .withHistogramBinCount(2097152)
                .create(new Mask[]{roiMask, roiMask2}, new RasterDataNode[]{testBand1, testBand2}, ProgressMonitor.NULL);
        assertEquals(100, stx.getMinimum(), 1E-3);
        assertEquals(294, stx.getMaximum(), 1E-3);
        assertEquals(197, stx.getMean(), 1E-3);
        assertEquals(274, stx.getHistogram().getPTileThreshold(0.9)[0], 1E-3);
        assertEquals(194, stx.getHistogram().getPTileThreshold(0.5)[0], 1E-3);
        assertEquals(114, stx.getHistogram().getPTileThreshold(0.1)[0], 1E-3);
    }

    @Test
    public void testThatAccumulateWithSummaryStxOpGetsTheRightMinMax() {
        //preparation
        final Band testBand = createFloatTestBand(10, 10, 20, 60);
        final SummaryStxOp stxOp = new SummaryStxOp();

        //execution
        StxFactory.accumulate(testBand, 0, null, null, stxOp, ProgressMonitor.NULL);

        //verification
        assertEquals(20, stxOp.getMinimum(), 1e-44);
        assertEquals(60, stxOp.getMaximum(), 1e-44);
        assertEquals(40, stxOp.getMean(), 1e-7);
        final double variance = 137.4009443244;
        assertEquals(variance, stxOp.getVariance(), 1e-7);
        assertEquals(Math.sqrt(variance), stxOp.getStandardDeviation(), 1e-7);

    }

    private Band createFloatTestBand(int w, int h, float min, float max) {
        final Product product = createTestProduct(w, h);
        final Band band = product.addBand("float", ProductData.TYPE_FLOAT32);
        final float[] values = new float[w * h];
        for (int i = 0; i < values.length; i++) {
            values[i] = min + i * (max - min) / (values.length - 1);
        }
        band.setData(new ProductData.Float(values));
        return band;
    }

    private Band createTestBand(int type, int w, int h) {
        final double mean = (w * h - 1.0) / 2.0;
        return createTestBand(type, w, h, mean);
    }

    private Band createTestBand(int type, int w, int h, double offset) {
        final Product product = createTestProduct(w, h);
        final Band band = new VirtualBand("V", type, w, h, "(Y-0.5) * " + w + " + (X-0.5) - " + offset);
        product.addBand(band);
        return band;
    }

    private Product createTestProduct(int w, int h) {
        return new Product("F", "F", w, h);
    }

}
