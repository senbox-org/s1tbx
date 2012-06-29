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
import org.esa.beam.framework.datamodel.Mask;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.gpf.GPF;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.statistics.calculators.PercentileStatisticsCalculator;
import org.esa.beam.statistics.calculators.StatisticsCalculatorDescriptor;
import org.junit.Test;

import java.text.ParseException;

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
    public void testUtcConverter() throws Exception {
        final StatisticsOp.UtcConverter utcConverter = new StatisticsOp.UtcConverter();
        assertEquals(ProductData.UTC.class, utcConverter.getValueType());

        final ProductData.UTC actual = utcConverter.parse("2010-01-31 14:46:22");
        final long expected = ProductData.UTC.parse("2010-01-31 14:46:22", "yyyy-MM-dd hh:mm:ss").getAsDate().getTime();

        assertEquals(expected, actual.getAsDate().getTime());

        expectException(utcConverter, "2010-01-31'T'14:46:22.1234");
        expectException(utcConverter, "2010-31-01'T'14:46:22.123");
        expectException(utcConverter, "2010-01-31T14:46:22.123");
        expectException(utcConverter, "2010-01-31'T'14.46.22.123");

        expectNotImplementedException(utcConverter, ProductData.UTC.parse("2010-JAN-01 10:37:22"));
    }

    @Test
    public void testStatisticsCalculatorConverter() throws Exception {
        final StatisticsOp.StatisticsCalculatorDescriptorConverter converter = new StatisticsOp.StatisticsCalculatorDescriptorConverter();
        assertEquals(StatisticsCalculatorDescriptor.class, converter.getValueType());

        StatisticsCalculatorDescriptor calculator = converter.parse("PERCENTILE");
        assertTrue(calculator instanceof PercentileStatisticsCalculator.Descriptor);

        calculator = converter.parse("percentile");
        assertTrue(calculator instanceof PercentileStatisticsCalculator.Descriptor);

        calculator = converter.parse("Percentile");
        assertTrue(calculator instanceof PercentileStatisticsCalculator.Descriptor);

        expectException(converter, "Perzentil");

        expectNotImplementedException(converter, new PercentileStatisticsCalculator.Descriptor());
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


    }

    @Test
    public void testCreateMaskFromRegion() throws Exception {
        final StatisticsOp statisticsOp = new StatisticsOp();
        final GeometryFactory factory = new GeometryFactory();
        final Mask mask = statisticsOp.createMaskFromRegion(
                ProductIO.readProduct(getClass().getResource("testProduct1.dim").getFile()),
                new Polygon(new LinearRing(new CoordinateArraySequence(new Coordinate[]{
                        new Coordinate(13.8, 38),
                        new Coordinate(14.5, 38.1),
                        new Coordinate(14.24, 36.9),
                        new Coordinate(13.24, 37.17),
                        new Coordinate(13.8, 38)
                }), factory), new LinearRing[0], factory)
        );
    }

    @Test
    public void testGetPixelValues_Triangle() throws Exception {
        final StatisticsOp statisticsOp = new StatisticsOp();
        final GeometryFactory factory = new GeometryFactory();
        final Polygon region = new Polygon(new LinearRing(new CoordinateArraySequence(new Coordinate[]{
                new Coordinate(13.56552, 38.366566),
                new Coordinate(13.58868, 38.36225),
                new Coordinate(13.582469, 38.34155),
                new Coordinate(13.56552, 38.366566)
        }), factory), new LinearRing[0], factory);
        final double[] pixelValues = statisticsOp.getPixelValues(
                ProductIO.readProduct(getClass().getResource("testProduct1.dim").getFile()),
                "algal_2", region);

        final double[] expected =
                {0.83418011, 0.83418011, 0.695856511,
                             0.69585651, 0.775825023,
                                         0.721552133};
        assertArrayEquals(expected, pixelValues, 1E-6);
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
        final double[] pixelValues = statisticsOp.getPixelValues(
                ProductIO.readProduct(getClass().getResource("testProduct1.dim").getFile()),
                "algal_2", region);

        final double[] expected = {
                             0.83418011,
                 0.69585651, 0.69585651, 0.775825023,
                             0.80447363
        };
        assertArrayEquals(expected, pixelValues, 1E-6);
    }

    private static void expectException(Converter converter, String text) {
        try {
            converter.parse(text);
            fail();
        } catch (ConversionException e) {}
    }

    private static void expectNotImplementedException(Converter converter, Object value) throws ParseException {
        try {
            converter.format(value);
            fail();
        } catch (IllegalStateException e) {
            assertTrue(e.getMessage().contains("Not implemented"));
        }
    }
}
