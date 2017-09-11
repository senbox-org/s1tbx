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
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.datamodel.VirtualBand;
import org.esa.snap.core.gpf.GPF;
import org.esa.snap.core.util.io.FileUtils;
import org.esa.snap.statistics.output.StatisticsOutputContext;
import org.esa.snap.statistics.output.StatisticsOutputter;
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

    static final File TESTDATA_DIR = new File("target/statistics-test-io");

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
        statisticsOp.statisticsOutputters.add(outputter);

        statisticsOp.initialize();

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
        statisticsOp.statisticsOutputters.add(outputter);

        statisticsOp.initialize();

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
        statisticsOp.statisticsOutputters.add(outputter);

        statisticsOp.initialize();
        

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
                          outputter.algorithmNames);
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
        statisticsOp.statisticsOutputters.add(outputter);

        statisticsOp.initialize();

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
        statisticsOp.statisticsOutputters.add(outputter);

        statisticsOp.initialize();

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
        GPF.createProduct("StatisticsOp", parameters, TestUtil.getTestProduct());

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
        statisticsOp.statisticsOutputters.add(outputter);

        statisticsOp.initialize();

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
        private String[] algorithmNames;

        public MyOutputter() {
            percentiles = new double[2];
        }

        @Override
        public void initialiseOutput(StatisticsOutputContext statisticsOutputContext) {
            int numPercentiles = 0;
            algorithmNames = statisticsOutputContext.algorithmNames;
            for (String algorithmName : algorithmNames) {
                if (algorithmName.matches("p\\d\\d_threshold")) {
                    numPercentiles++;
                }
            }
            percentiles = new double[numPercentiles];
        }

        @Override
        public void addToOutput(String bandName, String regionId, Map<String, Number> statistics) {
            final TreeMap<String, Number> map = new TreeMap<>();
            map.putAll(statistics);
            region = regionId;
            this.bandName = bandName;
            int percentileIndex = 0;
            for (Map.Entry<String, Number> entry : map.entrySet()) {
                final String key = entry.getKey();
                if (key.equalsIgnoreCase("total")) {
                    pixels = entry.getValue().intValue();
                } else if (key.equalsIgnoreCase("minimum")) {
                    minimum = entry.getValue().doubleValue();
                } else if (key.equalsIgnoreCase("maximum")) {
                    maximum = entry.getValue().doubleValue();
                } else if (key.equalsIgnoreCase("average")) {
                    average = entry.getValue().doubleValue();
                } else if (key.equalsIgnoreCase("median")) {
                    median = entry.getValue().doubleValue();
                } else if (key.equalsIgnoreCase("sigma")) {
                    sigma = entry.getValue().doubleValue();
                } else if (key.startsWith("p") && key.endsWith("threshold")) {
                    percentiles[percentileIndex++] = entry.getValue().doubleValue();
                }
            }
        }

        @Override
        public void finaliseOutput() throws IOException {
        }
    }

    static File getTestFile(String fileName) {
        return new File(TESTDATA_DIR, fileName);
    }
}
