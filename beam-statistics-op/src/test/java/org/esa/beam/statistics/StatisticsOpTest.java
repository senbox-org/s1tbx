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
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.geom.impl.CoordinateArraySequence;
import org.esa.beam.framework.dataio.ProductIO;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.gpf.GPF;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.statistics.calculators.StatisticsCalculatorDescriptor;
import org.esa.beam.statistics.calculators.StatisticsCalculatorPercentile;
import org.junit.Test;

import java.io.IOException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.ParseException;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * @author Thomas Storm
 */
public class StatisticsOpTest {

    static {
        GPF.getDefaultInstance().getOperatorSpiRegistry().loadOperatorSpis();
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
        bandConfiguration.statisticsCalculatorDescriptor = new StatisticsCalculatorPercentile.Descriptor();
        bandConfiguration.percentile = 50;
        statisticsOp.bandConfigurations = new StatisticsOp.BandConfiguration[]{bandConfiguration};
        statisticsOp.sourceProducts = new Product[]{getTestProduct()};
        statisticsOp.shapefile = getClass().getResource("9_pixels.shp");
        final StringBuilder builder = new StringBuilder();

        statisticsOp.outputStrategy = new StatisticsOp.OutputStrategy() {
            @Override
            public void addToOutput(StatisticsOp.BandConfiguration configuration, String regionId, Map<String, Double> statistics) {
                builder.append(regionId)
                        .append("\n")
                        .append(configuration.sourceBandName)
                        .append(":\n");
                for (Map.Entry<String, Double> entry : statistics.entrySet()) {
                    final DecimalFormatSymbols decimalFormatSymbols = DecimalFormatSymbols.getInstance();
                    decimalFormatSymbols.setDecimalSeparator('.');
                    builder.append(entry.getKey())
                            .append(": ")
                            .append(new DecimalFormat("0.000000", decimalFormatSymbols).format(entry.getValue()));
                }
            }

            @Override
            public void initialiseOutput(Product[] sourceProducts, String[] algorithmNames, ProductData.UTC startDate, ProductData.UTC endDate, String[] regionIds) {
            }

            @Override
            public void finaliseOutput() throws IOException {
            }

        };

        statisticsOp.initialize();

        final String result = builder.toString();
        assertEquals("9_pixels.1\n" +
                     "algal_2:\n" +
                     "p50: 0.775825",
                     result);
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

        assertNotImplementedException(utcConverter, ProductData.UTC.parse("2010-JAN-01 10:37:22"));
    }

    @Test
    public void testStatisticsCalculatorConverter() throws Exception {
        final StatisticsOp.StatisticsCalculatorDescriptorConverter converter = new StatisticsOp.StatisticsCalculatorDescriptorConverter();
        assertEquals(StatisticsCalculatorDescriptor.class, converter.getValueType());

        StatisticsCalculatorDescriptor calculator = converter.parse("PERCENTILE");
        assertTrue(calculator instanceof StatisticsCalculatorPercentile.Descriptor);

        calculator = converter.parse("percentile");
        assertTrue(calculator instanceof StatisticsCalculatorPercentile.Descriptor);

        calculator = converter.parse("Percentile");
        assertTrue(calculator instanceof StatisticsCalculatorPercentile.Descriptor);

        assertConversionException(converter, "Perzentil");

        assertNotImplementedException(converter, new StatisticsCalculatorPercentile.Descriptor());
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
    public void testExtractRegions() throws Exception {
        final StatisticsOp statisticsOp = new StatisticsOp();
        statisticsOp.shapefile = getClass().getResource("polygons.shp");

        statisticsOp.extractRegions();

        assertEquals(3, statisticsOp.regions.length);
        assertEquals(3, statisticsOp.regionIds.length);

        for (Geometry region : statisticsOp.regions) {
            assertNotNull(region);
        }

        final Geometry firstRegion = statisticsOp.regions[0];
        assertEquals(5, firstRegion.getCoordinates().length);
        assertEquals(firstRegion.getCoordinates()[4], firstRegion.getCoordinates()[0]);

        final Geometry secondRegion = statisticsOp.regions[1];
        assertEquals(13, secondRegion.getCoordinates().length);
        assertEquals(secondRegion.getCoordinates()[12], secondRegion.getCoordinates()[0]);

        final Geometry thirdRegion = statisticsOp.regions[2];
        assertEquals(7, thirdRegion.getCoordinates().length);
        assertEquals(thirdRegion.getCoordinates()[6], thirdRegion.getCoordinates()[0]);

        assertEquals("polygons.1", statisticsOp.regionIds[0]);
        assertEquals("polygons.2", statisticsOp.regionIds[1]);
        assertEquals("polygons.3", statisticsOp.regionIds[2]);
    }

    @Test
    public void testGetPixelValues_Triangle() throws Exception {
        final StatisticsOp statisticsOp = new StatisticsOp();
        final GeometryFactory factory = new GeometryFactory();
        final Polygon region = new Polygon(new LinearRing(new CoordinateArraySequence(new Coordinate[]{
                new Coordinate(13.56552, 38.366566),
                new Coordinate(13.600261, 38.3601),
                new Coordinate(13.594047, 38.339397),
                new Coordinate(13.56552, 38.366566)
        }), factory), new LinearRing[0], factory);
        final StatisticsOp.BandConfiguration bandConfiguration = new StatisticsOp.BandConfiguration();
        bandConfiguration.sourceBandName = "algal_2";

        final double[] expectedWithoutExpression = {
                0.83418011, 0.83418011, 0.695856511, 0.6710759,
                0.775825023, 0.6241307,
                0.7215521
        };

        final double[] expectedWithExpression = {
                0.83418011, 0.83418011, 0.695856511, 0.6710759,
                0.775825023, 0.6241307
        };
        testThatValuesAreOk(statisticsOp, region, bandConfiguration, expectedWithoutExpression, null);
        testThatValuesAreOk(statisticsOp, region, bandConfiguration, expectedWithExpression, "algal_2 > 0.73 or algal_2 < 0.71");
    }

    private void testThatValuesAreOk(StatisticsOp statisticsOp, Polygon region, StatisticsOp.BandConfiguration bandConfiguration, double[] expected, String validPixelExpression) throws IOException {
        bandConfiguration.validPixelExpression = validPixelExpression;
        final double[] pixelValues = statisticsOp.getPixelValues(
                new Product[]{getTestProduct()},
                bandConfiguration, region);

        assertArrayEquals(expected, pixelValues, 1E-6);
    }

    private Product getTestProduct() throws IOException {
        return ProductIO.readProduct(getClass().getResource("testProduct1.dim").getFile());
    }

    @Test
    public void testGetPixelValues_Rhomb() throws Exception {
        final StatisticsOp statisticsOp = new StatisticsOp();
        final GeometryFactory factory = new GeometryFactory();
        final Polygon region = new Polygon(new LinearRing(new CoordinateArraySequence(new Coordinate[]{
                new Coordinate(13.577101, 38.3664407),
                new Coordinate(13.585574, 38.351902),
                new Coordinate(13.570892, 38.343708),
                new Coordinate(13.562417, 38.356213),
                new Coordinate(13.577101, 38.3664407)
        }), factory), new LinearRing[0], factory);
        final StatisticsOp.BandConfiguration bandConfiguration = new StatisticsOp.BandConfiguration();
        bandConfiguration.sourceBandName = "algal_2";

        final double[] expectedWithoutExpression = {
                0.83418011,
                0.69585651, 0.69585651, 0.775825023,
                0.80447363
        };

        final double[] expectedWithExpression = {
                0.83418011,
                0.775825023,
                0.80447363
        };

        testThatValuesAreOk(statisticsOp, region, bandConfiguration, expectedWithoutExpression, null);
        testThatValuesAreOk(statisticsOp, region, bandConfiguration, expectedWithExpression, "algal_2 > 0.7");
    }

    private static void assertConversionException(Converter converter, String text) {
        try {
            converter.parse(text);
            fail();
        } catch (ConversionException e) {
        }
    }

    @SuppressWarnings("unchecked")
    private static void assertNotImplementedException(Converter converter, Object value) throws ParseException {
        try {
            converter.format(value);
            fail();
        } catch (IllegalStateException e) {
            assertTrue(e.getMessage().contains("Not implemented"));
        }
    }
}
