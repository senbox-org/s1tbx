/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.snap.core.util.converters;

import com.bc.ceres.binding.ConversionException;
import com.bc.ceres.binding.Converter;
import com.bc.ceres.binding.ConverterRegistry;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKTReader;
import com.vividsolutions.jts.io.WKTWriter;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.*;

public class JtsGeometryConverterTest {

    private static JtsGeometryConverter converter;
    private static final GeometryFactory factory = new GeometryFactory();

    @BeforeClass
    public static void setUp() throws Exception {
        converter = new JtsGeometryConverter();
        JtsGeometryConverter.registerConverter();
    }

    @Test
    public void testRetrievingConverterFromRegistry() {
        final ConverterRegistry registry = ConverterRegistry.getInstance();
        final Converter<Geometry> geomConverter = registry.getConverter(Geometry.class);
        assertNotNull(geomConverter);
    }

    @Test
    public void testType() {
        assertEquals(Geometry.class, converter.getValueType());
    }

    @Test
    public void testParse() throws ConversionException {
        testParsing(factory.createPoint(new Coordinate(12.4567890, 0.00000001)));
        final Coordinate[] coordinates = new Coordinate[]{
                new Coordinate(2, 0), new Coordinate(6, 0),
                new Coordinate(8, 3), new Coordinate(8, 9),
                new Coordinate(6, 11), new Coordinate(2, 9),
                new Coordinate(2, 0),
        };
        testParsing(factory.createLineString(coordinates));
        final LinearRing linearRing = factory.createLinearRing(coordinates);
        testParsing(linearRing);
        final Coordinate[] hole = new Coordinate[]{
                new Coordinate(4, 4), new Coordinate(5, 4),
                new Coordinate(5, 6), new Coordinate(4, 6),
                new Coordinate(4, 4),
        };
        final Polygon polygon = factory.createPolygon(linearRing, new LinearRing[]{factory.createLinearRing(hole)});
        testParsing(polygon);
    }

    @Test
    public void testFormat() throws ConversionException, ParseException {
        testFormatting(factory.createPoint(new Coordinate(12.4567890, 0.00000001)));
        final Coordinate[] coordinates = new Coordinate[]{
                new Coordinate(2, 0), new Coordinate(6, 0),
                new Coordinate(8, 3), new Coordinate(8, 9),
                new Coordinate(6, 11), new Coordinate(2, 9),
                new Coordinate(2, 0),
        };
        final LinearRing linearRing = factory.createLinearRing(coordinates);
        testFormatting(linearRing);
        final Coordinate[] hole = new Coordinate[]{
                new Coordinate(4, 4), new Coordinate(5, 4),
                new Coordinate(5, 6), new Coordinate(4, 6),
                new Coordinate(4, 4),
        };
        final Polygon polygon = factory.createPolygon(linearRing, new LinearRing[]{factory.createLinearRing(hole)});
        testFormatting(polygon);

        assertEquals("", converter.format(null));
    }

    private void testFormatting(Geometry expectedGeometry) throws ParseException {
        final WKTReader wktReader = new WKTReader();
        final String geometryWkt = converter.format(expectedGeometry);
        final Geometry geometry = wktReader.read(geometryWkt);
        assertTrue(expectedGeometry.equalsExact(geometry));
    }

    private void testParsing(Geometry expectedGeometry) throws ConversionException {
        final WKTWriter wktWriter = new WKTWriter();
        final String geometryWkt = wktWriter.write(expectedGeometry);
        final Geometry geometry = converter.parse(geometryWkt);
        assertTrue(expectedGeometry.equalsExact(geometry));

        assertEquals(null, converter.parse(""));
    }
}
