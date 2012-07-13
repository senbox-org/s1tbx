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

package org.esa.beam.statistics;

import com.bc.ceres.binding.ConversionException;
import com.bc.ceres.binding.Converter;
import org.esa.beam.framework.dataio.ProductIO;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.datamodel.VirtualBand;
import org.esa.beam.framework.gpf.GPF;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.util.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import static org.junit.Assert.*;

/**
 * @author Thomas Storm
 */
public class StatisticsOpTest {

    static final File TESTDATA_DIR = new File("target/statistics-test-io");

    static {
        GPF.getDefaultInstance().getOperatorSpiRegistry().loadOperatorSpis();
    }

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
        final StatisticsOp statisticsOp = new StatisticsOp();
        final StatisticsOp.BandConfiguration bandConfiguration = new StatisticsOp.BandConfiguration();
        bandConfiguration.sourceBandName = "algal_2";
        statisticsOp.bandConfigurations = new StatisticsOp.BandConfiguration[]{bandConfiguration};
        statisticsOp.sourceProducts = new Product[]{getTestProducts()[0]};
        statisticsOp.shapefile = new File(getClass().getResource("4_pixels.shp").getFile());
        statisticsOp.doOutputAsciiFile = false;

        final MyOutputter outputter = new MyOutputter();
        statisticsOp.outputters.add(outputter);

        statisticsOp.initialize();

        assertEquals("4_pixels.1", outputter.region);
        assertEquals("algal_2", outputter.bandName);
        assertEquals(4, outputter.pixels);
        assertEquals(0.804474, outputter.maximum, 1E-4);
        assertEquals(0.695857, outputter.minimum, 1E-4);
        assertEquals(0.749427, outputter.average, 1E-4);
        assertEquals(0.721552, outputter.median, 1E-4);
        assertEquals(0.049578, outputter.sigma, 1E-4);
        assertEquals(0.804474, outputter.p90, 1E-4);
        assertEquals(0.804474, outputter.p95, 1E-4);
    }

    @Test
    public void testStatisticsOp_WithExpression() throws Exception {
        final StatisticsOp statisticsOp = new StatisticsOp();
        final StatisticsOp.BandConfiguration bandConfiguration = new StatisticsOp.BandConfiguration();
        bandConfiguration.expression = "algal_2 * PI";
        statisticsOp.bandConfigurations = new StatisticsOp.BandConfiguration[]{bandConfiguration};
        statisticsOp.sourceProducts = new Product[]{getTestProducts()[0]};
        statisticsOp.shapefile = new File(getClass().getResource("4_pixels.shp").getFile());
        statisticsOp.doOutputAsciiFile = false;

        final MyOutputter outputter = new MyOutputter();
        statisticsOp.outputters.add(outputter);

        statisticsOp.initialize();

        assertEquals("4_pixels.1", outputter.region);
        assertEquals("algal_2_*_PI", outputter.bandName);
        assertEquals(4, outputter.pixels);
        assertEquals(2.527328, outputter.maximum, 1E-4);
        assertEquals(2.186098, outputter.minimum, 1E-4);
        assertEquals(2.354394, outputter.average, 1E-4);
        assertEquals(2.266823, outputter.median, 1E-4);
        assertEquals(0.155752, outputter.sigma, 1E-4);
        assertEquals(2.527328, outputter.p90, 1E-4);
        assertEquals(2.527328, outputter.p95, 1E-4);
    }

    @Test
    public void testStatisticsOp_WithValidExpression() throws Exception {
        final StatisticsOp statisticsOp = new StatisticsOp();
        final StatisticsOp.BandConfiguration bandConfiguration = new StatisticsOp.BandConfiguration();
        bandConfiguration.sourceBandName = "algal_2";
        bandConfiguration.validPixelExpression = "algal_2 > 0.7";
        statisticsOp.bandConfigurations = new StatisticsOp.BandConfiguration[]{bandConfiguration};
        statisticsOp.sourceProducts = new Product[]{getTestProducts()[0]};
        statisticsOp.shapefile = new File(getClass().getResource("4_pixels.shp").getFile());
        statisticsOp.doOutputAsciiFile = false;

        final MyOutputter outputter = new MyOutputter();
        statisticsOp.outputters.add(outputter);

        statisticsOp.initialize();

        assertEquals("4_pixels.1", outputter.region);
        assertEquals("algal_2", outputter.bandName);
        assertEquals(3, outputter.pixels);
        assertEquals(0.8045, outputter.maximum, 1E-4);
        assertEquals(0.7216, outputter.minimum, 1E-4);
        assertEquals(0.7672, outputter.average, 1E-4);
        assertEquals(0.7758, outputter.median, 1E-4);
        assertEquals(0.04211, outputter.sigma, 1E-4);
        assertEquals(0.8044, outputter.p90, 1E-4);
        assertEquals(0.8044, outputter.p95, 1E-4);
    }

    @Test
    public void testGetBand() throws Exception {
        final StatisticsOp.BandConfiguration configuration = new StatisticsOp.BandConfiguration();

        final Product testProduct = getTestProducts()[0];
        try {
            StatisticsOp.getBand(configuration, testProduct);
            fail();
        } catch (OperatorException expected) {
            assertTrue(expected.getMessage().contains("must contain either a source band name or an expression"));
        }

        configuration.sourceBandName = "algal_2";
        final Band band = StatisticsOp.getBand(configuration, testProduct);
        assertEquals("algal_2", band.getName());

        configuration.expression = "algal_2 * PI";
        try {
            StatisticsOp.getBand(configuration, testProduct);
            fail();
        } catch (OperatorException expected) {
            assertTrue(expected.getMessage().contains("must contain either a source band name or an expression"));
        }

        configuration.sourceBandName = null;
        final Band virtualBand = StatisticsOp.getBand(configuration, testProduct);
        assertEquals("algal_2_*_PI", virtualBand.getName());
        assertTrue(virtualBand instanceof VirtualBand);
        assertEquals("algal_2 * PI", ((VirtualBand) virtualBand).getExpression());
    }

    @Test
    public void testStatisticsOp_WithGPF() throws Exception {
        final StatisticsOp.BandConfiguration bandConfiguration_1 = new StatisticsOp.BandConfiguration();
        bandConfiguration_1.sourceBandName = "algal_2";

        final HashMap<String, Object> parameters = new HashMap<String, Object>();
        parameters.put("outputAsciiFile", getTestFile("statisticsOutput.out"));
        parameters.put("outputShapefile", getTestFile("statisticsShapefile.shp"));
        parameters.put("doOutputAsciiFile", true);
        parameters.put("doOutputShapefile", true);
        parameters.put("shapefile", new File(getClass().getResource("4_pixels.shp").toURI()));
        parameters.put("bandConfigurations", new StatisticsOp.BandConfiguration[]{
                bandConfiguration_1,
        });
        GPF.createProduct("StatisticsOp", parameters, getTestProducts()[0]);

        assertFalse(getTestFile("statisticsOutput.put").exists());
        assertTrue(getTestFile("statisticsOutput.out").exists());
        assertTrue(getTestFile("statisticsOutput_metadata.txt").exists());
        assertTrue(getTestFile("statisticsShapefile.shp").exists());
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
    public void testValidateInput() throws Exception {
        final StatisticsOp statisticsOp = new StatisticsOp();
        statisticsOp.startDate = ProductData.UTC.parse("2010-01-31 14:46:23", "yyyy-MM-ss hh:mm:ss");
        statisticsOp.endDate = ProductData.UTC.parse("2010-01-31 14:45:23", "yyyy-MM-ss hh:mm:ss");

        try {
            statisticsOp.validateInput();
            fail();
        } catch (OperatorException expected) {
            assertTrue(expected.getMessage().contains("before start date"));
        }

        statisticsOp.endDate = ProductData.UTC.parse("2010-01-31 14:47:23", "yyyy-MM-ss hh:mm:ss");

        try {
            statisticsOp.validateInput();
            fail();
        } catch (OperatorException expected) {
            assertTrue(expected.getMessage().contains("must be given"));
        }
    }

    @Test
    public void testComputeOutput() throws Exception {
        StatisticsOp statisticsOp = new StatisticsOp();
        StatisticsOp.BandConfiguration bandConfiguration = new StatisticsOp.BandConfiguration();
        bandConfiguration.sourceBandName = "algal_2";
        statisticsOp.bandConfigurations = new StatisticsOp.BandConfiguration[] {bandConfiguration};
        MyOutputter outputter = new MyOutputter();
        statisticsOp.outputters.add(outputter);
        statisticsOp.shapefile = new File(getClass().getResource("south_of_sicily.shp").getFile());

        Product[] testProducts = getTestProducts();

        statisticsOp.initializeVectorDataNodes(testProducts);
        statisticsOp.computeOutput(testProducts);

        // todo - validate output
    }

    private Product[] getTestProducts() throws IOException {
        return new Product[]{
                ProductIO.readProduct(getClass().getResource("testProduct1.dim").getFile()),
                ProductIO.readProduct(getClass().getResource("testProduct2.dim").getFile())
        };
    }

    private static void assertConversionException(Converter converter, String text) {
        try {
            converter.parse(text);
            fail();
        } catch (ConversionException e) {
        }
    }

    private static class MyOutputter implements StatisticsOp.Outputter {

        int pixels;
        double minimum;
        double maximum;
        double average;
        double median;
        double sigma;
        double p90;
        double p95;
        String region;
        String bandName;

        public MyOutputter() {
        }

        @Override
        public void initialiseOutput(Product[] sourceProducts, String[] bandNames, String[] algorithmNames, ProductData.UTC startDate, ProductData.UTC endDate, String[] regionIds) {
        }

        @Override
        public void addToOutput(String bandName, String regionId, Map<String, Double> statistics) {
            final TreeMap<String, Double> map = new TreeMap<String, Double>();
            map.putAll(statistics);
            region = regionId;
            this.bandName = bandName;
            for (Map.Entry<String, Double> entry : map.entrySet()) {
                final String key = entry.getKey();
                if(key.equalsIgnoreCase("total")) {
                    pixels = entry.getValue().intValue();
                } else if(key.equalsIgnoreCase("minimum")) {
                    minimum = entry.getValue();
                } else if(key.equalsIgnoreCase("maximum")) {
                    maximum = entry.getValue();
                } else if(key.equalsIgnoreCase("average")) {
                    average = entry.getValue();
                } else if(key.equalsIgnoreCase("median")) {
                    median = entry.getValue();
                } else if(key.equalsIgnoreCase("sigma")) {
                    sigma = entry.getValue();
                } else if(key.equalsIgnoreCase("p90")) {
                    p90 = entry.getValue();
                } else if(key.equalsIgnoreCase("p95")) {
                    p95 = entry.getValue();
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
