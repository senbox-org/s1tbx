/*
 * Copyright (C) 2011 Brockmann Consult GmbH (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */

package org.esa.snap.statistics;

import com.bc.ceres.binding.ConversionException;
import com.bc.ceres.binding.Converter;
import com.bc.ceres.core.ProgressMonitor;
import org.esa.snap.core.dataio.ProductIO;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.datamodel.VirtualBand;
import org.esa.snap.core.gpf.GPF;
import org.esa.snap.core.util.io.FileUtils;
import org.esa.snap.statistics.output.StatisticsOutputContext;
import org.esa.snap.statistics.output.StatisticsOutputter;
import org.esa.snap.statistics.tools.TimeInterval;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import static org.junit.Assert.*;

/**
 * @author Thomas Storm
 */
public class StatisticsOpTest {

    private static final File TESTDATA_DIR = new File("target/statistics-test-io");

    @Before
    public void setUp() throws Exception {
        TESTDATA_DIR.mkdirs();
        if (!TESTDATA_DIR.isDirectory()) {
            fail("Can't create test I/O directory: " + TESTDATA_DIR);
        }
    }

    @After
    public void tearDown() throws Exception {
        if (!FileUtils.deleteTree(TESTDATA_DIR)) {
            System.out.println("Warning: failed to completely delete test I/O directory:" + TESTDATA_DIR);
        }
    }

    @Test
    public void testThatStatisticsOpIsRegistered() throws Exception {
        assertNotNull(GPF.getDefaultInstance().getOperatorSpiRegistry().getOperatorSpi("StatisticsOp"));
    }

    @Test
    public void testStatisticsOp() throws Exception {
        final StatisticsOp statisticsOp = createStatisticsOp();
        final BandConfiguration bandConfiguration = new BandConfiguration();
        bandConfiguration.sourceBandName = "algal_2";
        statisticsOp.bandConfigurations = new BandConfiguration[]{bandConfiguration};
        statisticsOp.sourceProducts = new Product[]{TestUtil.getTestProduct()};
        statisticsOp.shapefile = new File(getClass().getResource("4_pixels.shp").getFile());
        final MyOutputter outputter = new MyOutputter();
        statisticsOp.allStatisticsOutputters.add(outputter);

        statisticsOp.initialize();
        statisticsOp.doExecute(ProgressMonitor.NULL);

        assertEquals("4_pixels.1", outputter.region);
        assertEquals("algal_2", outputter.bandName);
        assertEquals(4, outputter.pixels);
        assertEquals(0.804474, outputter.maximum, 1E-3);
        assertEquals(0.695857, outputter.minimum, 1E-3);
        assertEquals(0.749427, outputter.average, 1E-3);
        assertEquals(0.721552, outputter.median, 1E-3);
        assertEquals(0.049577, outputter.sigma, 1E-3);
        assertEquals(2, outputter.percentiles.length);
        assertEquals(0.804474, outputter.percentiles[0], 1E-3);
        assertEquals(0.804474, outputter.percentiles[1], 1E-3);
    }

    @Test
    public void testStatisticsOp_WithPrecisePercentiles() throws Exception {
        final StatisticsOp statisticsOp = createStatisticsOp();
        final BandConfiguration bandConfiguration = new BandConfiguration();
        bandConfiguration.sourceBandName = "algal_2";
        statisticsOp.bandConfigurations = new BandConfiguration[]{bandConfiguration};
        statisticsOp.sourceProducts = new Product[]{TestUtil.getTestProduct()};
        statisticsOp.shapefile = new File(getClass().getResource("4_pixels.shp").getFile());
        statisticsOp.accuracy = 6;

        final MyOutputter outputter = new MyOutputter();
        statisticsOp.allStatisticsOutputters.add(outputter);

        statisticsOp.initialize();
        statisticsOp.doExecute(ProgressMonitor.NULL);

        assertEquals("4_pixels.1", outputter.region);
        assertEquals("algal_2", outputter.bandName);
        assertEquals(4, outputter.pixels);
        assertEquals(0.804474, outputter.maximum, 1E-6);
        assertEquals(0.695857, outputter.minimum, 1E-6);
        assertEquals(0.749427, outputter.average, 1E-6);
        assertEquals(0.721552, outputter.median, 1E-6);
        assertEquals(0.049577, outputter.sigma, 1E-6);
        assertEquals(2, outputter.percentiles.length);
        assertEquals(0.80447364, outputter.percentiles[0], 1E-6);
        assertEquals(0.80447364, outputter.percentiles[1], 1E-6);
    }

    @Test
    public void testStatisticsOp_WithNoPercentiles() throws Exception {
        final StatisticsOp statisticsOp = createStatisticsOp();
        final BandConfiguration bandConfiguration = new BandConfiguration();
        bandConfiguration.sourceBandName = "algal_2";
        statisticsOp.bandConfigurations = new BandConfiguration[]{bandConfiguration};
        statisticsOp.sourceProducts = new Product[]{TestUtil.getTestProduct()};
        statisticsOp.shapefile = new File(getClass().getResource("4_pixels.shp").getFile());
        statisticsOp.accuracy = 6;
        statisticsOp.percentiles = null;

        final MyOutputter outputter = new MyOutputter();
        statisticsOp.allStatisticsOutputters.add(outputter);

        statisticsOp.initialize();
        statisticsOp.doExecute(ProgressMonitor.NULL);

        assertEquals("4_pixels.1", outputter.region);
        assertEquals("algal_2", outputter.bandName);
        assertEquals(4, outputter.pixels);
        assertEquals(0.804474, outputter.maximum, 1E-6);
        assertEquals(0.695857, outputter.minimum, 1E-6);
        assertEquals(0.749427, outputter.average, 1E-6);
        assertEquals(0.721552, outputter.median, 1E-6);
        assertEquals(0.049577, outputter.sigma, 1E-6);
        assertEquals(2, outputter.percentiles.length);
        assertEquals(0.80447364, outputter.percentiles[0], 1E-6);
        assertEquals(0.80447364, outputter.percentiles[1], 1E-6);
        assertArrayEquals(new String[]{"minimum","maximum", "median", "average", "sigma", "p90_threshold", "p95_threshold", "max_error", "total"},
                          outputter.measureNames);
    }

    @Test
    public void testStatisticsOp_WithExpression() throws Exception {
        final StatisticsOp statisticsOp = createStatisticsOp();
        final BandConfiguration bandConfiguration = new BandConfiguration();
        bandConfiguration.expression = "algal_2 * PI";
        statisticsOp.bandConfigurations = new BandConfiguration[]{bandConfiguration};
        statisticsOp.sourceProducts = new Product[]{TestUtil.getTestProduct()};
        statisticsOp.shapefile = new File(getClass().getResource("4_pixels.shp").getFile());

        final MyOutputter outputter = new MyOutputter();
        statisticsOp.allStatisticsOutputters.add(outputter);

        statisticsOp.initialize();
        statisticsOp.doExecute(ProgressMonitor.NULL);

        assertEquals("4_pixels.1", outputter.region);
        assertEquals("algal_2_*_PI", outputter.bandName);
        assertEquals(4, outputter.pixels);
        assertEquals(2.527328, outputter.maximum, 1E-3);
        assertEquals(2.186098, outputter.minimum, 1E-3);
        assertEquals(2.354394, outputter.average, 1E-3);
        assertEquals(2.266823, outputter.median, 1E-3);
        assertEquals(0.155752, outputter.sigma, 1E-3);
        assertEquals(2, outputter.percentiles.length);
        assertEquals(2.527328, outputter.percentiles[0], 1E-3);
        assertEquals(2.527328, outputter.percentiles[1], 1E-3);
    }

    @Test
    public void testStatisticsOp_WithValidExpression() throws Exception {
        final StatisticsOp statisticsOp = createStatisticsOp();
        final BandConfiguration bandConfiguration = new BandConfiguration();
        bandConfiguration.sourceBandName = "algal_2";
        bandConfiguration.validPixelExpression = "algal_2 > 0.7";
        statisticsOp.bandConfigurations = new BandConfiguration[]{bandConfiguration};
        statisticsOp.sourceProducts = new Product[]{TestUtil.getTestProduct()};
        statisticsOp.shapefile = new File(getClass().getResource("4_pixels.shp").getFile());

        final MyOutputter outputter = new MyOutputter();
        statisticsOp.allStatisticsOutputters.add(outputter);

        statisticsOp.initialize();
        statisticsOp.doExecute(ProgressMonitor.NULL);

        assertEquals("4_pixels.1", outputter.region);
        assertEquals("algal_2", outputter.bandName);
        assertEquals(3, outputter.pixels);
        assertEquals(0.8045, outputter.maximum, 1E-4);
        assertEquals(0.7216, outputter.minimum, 1E-4);
        assertEquals(0.7672, outputter.average, 1E-4);
        assertEquals(0.7758, outputter.median, 1E-4);
        assertEquals(0.0421, outputter.sigma, 1E-4);
        assertEquals(2, outputter.percentiles.length);
        assertEquals(0.8044, outputter.percentiles[0], 1E-4);
        assertEquals(0.8044, outputter.percentiles[1], 1E-4);
    }

    @Test
    public void testGetBand() throws Exception {
        final BandConfiguration configuration = new BandConfiguration();
        final Product testProduct = TestUtil.getTestProduct();

        configuration.expression = "algal_2 * PI";
        configuration.sourceBandName = null;
        final Band virtualBand = StatisticsOp.getBand(configuration, testProduct);
        assertEquals("algal_2_*_PI", virtualBand.getName());
        assertTrue(virtualBand instanceof VirtualBand);
        assertEquals("algal_2 * PI", ((VirtualBand) virtualBand).getExpression());
    }

    @Test
    public void testStatisticsOp_WithGPF() throws Exception {
        final BandConfiguration bandConfiguration_1 = new BandConfiguration();
        bandConfiguration_1.sourceBandName = "algal_2";

        final HashMap<String, Object> parameters = new HashMap<>();
        parameters.put("outputAsciiFile", getTestFile("statisticsOutput.out"));
        parameters.put("outputShapefile", getTestFile("statisticsShapefile.shp"));
        parameters.put("doOutputAsciiFile", true);
        parameters.put("doOutputShapefile", true);
        parameters.put("shapefile", new File(getClass().getResource("4_pixels.shp").toURI()));
        parameters.put("bandConfigurations", new BandConfiguration[]{
                bandConfiguration_1,
        });
        Product statisticsProduct = GPF.createProduct("StatisticsOp", parameters, TestUtil.getTestProduct());
        GPF.writeProduct(statisticsProduct, new File(TESTDATA_DIR, "test.dim"), "BEAM-DIMAP",
                true, true, ProgressMonitor.NULL);

        assertFalse(getTestFile("statisticsOutput.put").exists());
        assertTrue(getTestFile("statisticsOutput.out").exists());
        assertTrue(getTestFile("statisticsOutput_metadata.txt").exists());
        assertTrue(getTestFile("statisticsShapefile.shp").exists());
    }

    @Test
    public void testStatisticsOp_WithDifferentPercentiles() throws Exception {
        final StatisticsOp statisticsOp = createStatisticsOp();
        final BandConfiguration bandConfiguration = new BandConfiguration();
        bandConfiguration.sourceBandName = "algal_2";
        statisticsOp.bandConfigurations = new BandConfiguration[]{bandConfiguration};
        statisticsOp.sourceProducts = new Product[]{TestUtil.getTestProduct()};
        statisticsOp.shapefile = new File(getClass().getResource("4_pixels.shp").getFile());
        statisticsOp.percentiles = new int[]{20, 51, 90};

        final MyOutputter outputter = new MyOutputter();
        statisticsOp.allStatisticsOutputters.add(outputter);

        statisticsOp.initialize();
        statisticsOp.doExecute(ProgressMonitor.NULL);

        assertEquals("4_pixels.1", outputter.region);
        assertEquals("algal_2", outputter.bandName);
        assertEquals(4, outputter.pixels);
        assertEquals(0.804474, outputter.maximum, 1E-3);
        assertEquals(0.695857, outputter.minimum, 1E-3);
        assertEquals(0.749427, outputter.average, 1E-3);
        assertEquals(0.721552, outputter.median, 1E-3);
        assertEquals(0.049577, outputter.sigma, 1E-3);
        assertEquals(3, outputter.percentiles.length);
        assertEquals(0.6958565, outputter.percentiles[0], 1E-3);
        assertEquals(0.775825, outputter.percentiles[1], 1E-3);
        assertEquals(0.804474, outputter.percentiles[2], 1E-3);
    }

    @Test
    public void testUtcConverter() throws Exception {
        final StatisticsOp.UtcConverter utcConverter = new StatisticsOp.UtcConverter();
        assertEquals(ProductData.UTC.class, utcConverter.getValueType());

        final ProductData.UTC actual = utcConverter.parse("2010-01-31 14:46:22");
        final long expected = ProductData.UTC.parse("2010-01-31 14:46:22", "yyyy-MM-dd hh:mm:ss").getAsDate().getTime();

        assertEquals(expected, actual.getAsDate().getTime());

        assertConversionException(utcConverter, "2010-01-31'T'14:46:22.1234");
        assertConversionException(utcConverter, "2010-31-01'T'14:46:22.123");
        assertConversionException(utcConverter, "2010-01-31T14:46:22.123");
        assertConversionException(utcConverter, "2010-01-31'T'14.46.22.123");
    }

    @Test
    public void testProductAlreadyOpened() {
        final File file = new File("test.file");

        final Product product = new Product("name", "type", 20, 40);
        product.setFileLocation(file);

        final ArrayList<Product> products = new ArrayList<>();
        products.add(product);

        assertTrue(StatisticsOp.isProductAlreadyOpened(products, file));
        assertFalse(StatisticsOp.isProductAlreadyOpened(products, new File("other.path")));
    }

    @Test
    public void testGetTimeIntervals_no_time_info() {
        TimeInterval[] timeIntervals = StatisticsOp.getTimeIntervals(null, null, null);
        assertEquals(1, timeIntervals.length);
        assertEquals(0, timeIntervals[0].getId());
        assertEquals(new ProductData.UTC(0).getAsDate(), timeIntervals[0].getIntervalStart().getAsDate());
        assertEquals(new ProductData.UTC(1000000).getAsDate(), timeIntervals[0].getIntervalEnd().getAsDate());
    }

    @Test
    public void testGetTimeIntervals_no_time_interval_definition() {
        ProductData.UTC startDate = new ProductData.UTC(10, 10, 10);
        ProductData.UTC endDate = new ProductData.UTC(20, 10, 10);
        TimeInterval[] timeIntervals = StatisticsOp.getTimeIntervals(null, startDate, endDate);
        assertEquals(1, timeIntervals.length);
        assertEquals(0, timeIntervals[0].getId());
        assertEquals(startDate, timeIntervals[0].getIntervalStart());
        assertEquals(endDate, timeIntervals[0].getIntervalEnd());
    }

    @Test
    public void testGetTimeIntervals_day_increase() {
        ProductData.UTC startDate = new ProductData.UTC(10, 10, 10);
        ProductData.UTC endDate = new ProductData.UTC(20, 10, 10);
        TimeIntervalDefinition timeIntervalDefinition = new TimeIntervalDefinition();
        timeIntervalDefinition.amount = 3;
        timeIntervalDefinition.unit = "days";
        TimeInterval[] timeIntervals = StatisticsOp.getTimeIntervals(timeIntervalDefinition, startDate, endDate);
        assertEquals(4, timeIntervals.length);
        assertEquals(0, timeIntervals[0].getId());
        assertEquals(startDate.getAsDate(), timeIntervals[0].getIntervalStart().getAsDate());
        assertEquals(new ProductData.UTC(13, 10, 10).getAsDate(), timeIntervals[0].getIntervalEnd().getAsDate());
        assertEquals(1, timeIntervals[1].getId());
        assertEquals(new ProductData.UTC(13, 10, 10).getAsDate(), timeIntervals[1].getIntervalStart().getAsDate());
        assertEquals(new ProductData.UTC(16, 10, 10).getAsDate(), timeIntervals[1].getIntervalEnd().getAsDate());
        assertEquals(2, timeIntervals[2].getId());
        assertEquals(new ProductData.UTC(16, 10, 10).getAsDate(), timeIntervals[2].getIntervalStart().getAsDate());
        assertEquals(new ProductData.UTC(19, 10, 10).getAsDate(), timeIntervals[2].getIntervalEnd().getAsDate());
        assertEquals(3, timeIntervals[3].getId());
        assertEquals(new ProductData.UTC(19, 10, 10).getAsDate(), timeIntervals[3].getIntervalStart().getAsDate());
        assertEquals(new ProductData.UTC(20, 10, 10).getAsDate(), timeIntervals[3].getIntervalEnd().getAsDate());
    }

    @Test
    public void testGetTimeIntervals_week_increase() {
        ProductData.UTC startDate = new ProductData.UTC(1000, 10, 10);
        ProductData.UTC endDate = new ProductData.UTC(1050, 10, 10);
        TimeIntervalDefinition timeIntervalDefinition = new TimeIntervalDefinition();
        timeIntervalDefinition.amount = 3;
        timeIntervalDefinition.unit = "weeks";
        TimeInterval[] timeIntervals = StatisticsOp.getTimeIntervals(timeIntervalDefinition, startDate, endDate);
        assertEquals(3, timeIntervals.length);
        assertEquals(0, timeIntervals[0].getId());
        assertEquals(startDate.getAsDate(), timeIntervals[0].getIntervalStart().getAsDate());
        assertEquals(new ProductData.UTC(1021, 10, 10).getAsDate(), timeIntervals[0].getIntervalEnd().getAsDate());
        assertEquals(1, timeIntervals[1].getId());
        assertEquals(new ProductData.UTC(1021, 10, 10).getAsDate(), timeIntervals[1].getIntervalStart().getAsDate());
        assertEquals(new ProductData.UTC(1042, 10, 10).getAsDate(), timeIntervals[1].getIntervalEnd().getAsDate());
        assertEquals(2, timeIntervals[2].getId());
        assertEquals(new ProductData.UTC(1042, 10, 10).getAsDate(), timeIntervals[2].getIntervalStart().getAsDate());
        assertEquals(new ProductData.UTC(1050, 10, 10).getAsDate(), timeIntervals[2].getIntervalEnd().getAsDate());
    }

    @Test
    public void testGetTimeIntervals_month_increase() {
        ProductData.UTC startDate = new ProductData.UTC(1000, 10, 10);
        ProductData.UTC endDate = new ProductData.UTC(1150, 10, 10);
        TimeIntervalDefinition timeIntervalDefinition = new TimeIntervalDefinition();
        timeIntervalDefinition.amount = 2;
        timeIntervalDefinition.unit = "months";
        TimeInterval[] timeIntervals = StatisticsOp.getTimeIntervals(timeIntervalDefinition, startDate, endDate);
        assertEquals(3, timeIntervals.length);
        assertEquals(0, timeIntervals[0].getId());
        assertEquals(startDate.getAsDate(), timeIntervals[0].getIntervalStart().getAsDate());
        assertEquals(new ProductData.UTC(1061, 10, 10).getAsDate(), timeIntervals[0].getIntervalEnd().getAsDate());
        assertEquals(1, timeIntervals[1].getId());
        assertEquals(new ProductData.UTC(1061, 10, 10).getAsDate(), timeIntervals[1].getIntervalStart().getAsDate());
        assertEquals(new ProductData.UTC(1122, 10, 10).getAsDate(), timeIntervals[1].getIntervalEnd().getAsDate());
        assertEquals(2, timeIntervals[2].getId());
        assertEquals(new ProductData.UTC(1122, 10, 10).getAsDate(), timeIntervals[2].getIntervalStart().getAsDate());
        assertEquals(new ProductData.UTC(1150, 10, 10).getAsDate(), timeIntervals[2].getIntervalEnd().getAsDate());
    }

    @Test
    public void testGetTimeIntervals_year_increase() {
        ProductData.UTC startDate = new ProductData.UTC(1000, 10, 10);
        ProductData.UTC endDate = new ProductData.UTC(2750, 10, 10);
        TimeIntervalDefinition timeIntervalDefinition = new TimeIntervalDefinition();
        timeIntervalDefinition.amount = 2;
        timeIntervalDefinition.unit = "years";
        TimeInterval[] timeIntervals = StatisticsOp.getTimeIntervals(timeIntervalDefinition, startDate, endDate);
        assertEquals(3, timeIntervals.length);
        assertEquals(0, timeIntervals[0].getId());
        assertEquals(startDate.getAsDate(), timeIntervals[0].getIntervalStart().getAsDate());
        assertEquals(new ProductData.UTC(1731, 10, 10).getAsDate(), timeIntervals[0].getIntervalEnd().getAsDate());
        assertEquals(1, timeIntervals[1].getId());
        assertEquals(new ProductData.UTC(1731, 10, 10).getAsDate(), timeIntervals[1].getIntervalStart().getAsDate());
        assertEquals(new ProductData.UTC(2461, 10, 10).getAsDate(), timeIntervals[1].getIntervalEnd().getAsDate());
        assertEquals(2, timeIntervals[2].getId());
        assertEquals(new ProductData.UTC(2461, 10, 10).getAsDate(), timeIntervals[2].getIntervalStart().getAsDate());
        assertEquals(new ProductData.UTC(2750, 10, 10).getAsDate(), timeIntervals[2].getIntervalEnd().getAsDate());
    }

    @Test
    public void testGetOutputFile() {
        assertNull(StatisticsOp.getOutputFile(null, StatisticsOp.ALL_MEASURES));
        assertNull(StatisticsOp.getOutputFile(null, StatisticsOp.QUALITATIVE_MEASURES));
        assertNull(StatisticsOp.getOutputFile(null, StatisticsOp.QUANTITATIVE_MEASURES));

        File origFile = new File("origFile.txt");
        File expectedQualitativeFile = new File("origFile_categorical.txt");
        File expectedQuantitativeFile = new File("origFile_quantitative.txt");

        File allMeasures_outputFile = StatisticsOp.getOutputFile(origFile, StatisticsOp.ALL_MEASURES);
        assertEquals(origFile.getAbsolutePath(), allMeasures_outputFile.getAbsolutePath());

        File qualitativeMeasures_outputFile = StatisticsOp.getOutputFile(origFile, StatisticsOp.QUALITATIVE_MEASURES);
        assertEquals(expectedQualitativeFile.getAbsolutePath(), qualitativeMeasures_outputFile.getAbsolutePath());

        File quantitativeMeasures_outputFile = StatisticsOp.getOutputFile(origFile, StatisticsOp.QUANTITATIVE_MEASURES);
        assertEquals(expectedQuantitativeFile.getAbsolutePath(), quantitativeMeasures_outputFile.getAbsolutePath());
    }

    private StatisticsOp createStatisticsOp() {
        StatisticsOp statisticsOp = new StatisticsOp();
        statisticsOp.setParameterDefaultValues();
        return statisticsOp;
    }

    private static void assertConversionException(Converter converter, String text) {
        try {
            converter.parse(text);
            fail();
        } catch (ConversionException ignored) {
        }
    }

    private static class MyOutputter implements StatisticsOutputter {

        int pixels;
        double minimum;
        double maximum;
        double average;
        double median;
        double sigma;
        double[] percentiles;
        String region;
        String bandName;
        private String[] measureNames;

        public MyOutputter() {
            percentiles = new double[2];
        }

        @Override
        public void initialiseOutput(StatisticsOutputContext statisticsOutputContext) {
            int numPercentiles = 0;
            measureNames = statisticsOutputContext.measureNames;
            for (String algorithmName : measureNames) {
                if (algorithmName.matches("p\\d\\d_threshold")) {
                    numPercentiles++;
                }
            }
            percentiles = new double[numPercentiles];
        }

        @Override
        public void addToOutput(String bandName, String regionId, Map<String, Object> statistics) {
            final TreeMap<String, Object> map = new TreeMap<>();
            map.putAll(statistics);
            region = regionId;
            this.bandName = bandName;
            int percentileIndex = 0;
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                final String key = entry.getKey();
                if (key.equalsIgnoreCase("total")) {
                    pixels = ((Number) entry.getValue()).intValue();
                } else if (key.equalsIgnoreCase("minimum")) {
                    minimum = ((Number) entry.getValue()).doubleValue();
                } else if (key.equalsIgnoreCase("maximum")) {
                    maximum = ((Number) entry.getValue()).doubleValue();
                } else if (key.equalsIgnoreCase("average")) {
                    average = ((Number) entry.getValue()).doubleValue();
                } else if (key.equalsIgnoreCase("median")) {
                    median = ((Number) entry.getValue()).doubleValue();
                } else if (key.equalsIgnoreCase("sigma")) {
                    sigma = ((Number) entry.getValue()).doubleValue();
                } else if (key.startsWith("p") && key.endsWith("threshold")) {
                    percentiles[percentileIndex++] = ((Number) entry.getValue()).doubleValue();
                }
            }
        }

        @Override
        public void addToOutput(String bandName, TimeInterval interval, String regionId, Map<String, Object> statistics) {
            addToOutput(bandName, regionId, statistics);
        }

        @Override
        public void finaliseOutput() throws IOException {
        }
    }

    static File getTestFile(String fileName) {
        return new File(TESTDATA_DIR, fileName);
    }
}
