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

package org.esa.beam.csv.productio;

import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.ProductData;
import org.junit.Test;

import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * @author Olaf Danne
 * @author Thomas Storm
 */
public class CsvProductFileTest {

    @Test
    public void testParseProperties() throws Exception {
        final String simpleFormatExample = getClass().getResource("simple_format_example.txt").getFile();
        final CsvProductSourceParser parser = new CsvProductFile(simpleFormatExample);
        parser.parseProperties();

        final CsvProductSource productSource = parser.getCsvProductSource();
        final Map<String,String> properties = productSource.getProperties();
        assertNotNull(properties);
        assertEquals(2, properties.size());
        assertEquals("POLYGON(0.0, 1.0, 1.1)", properties.get("geometry1"));
        assertEquals("POLYGON(2.0, 1.0, 1.1)", properties.get("geometry2"));
    }

    @Test(expected = CsvProductFile.ParseException.class)
    public void testParseProperties_Fail() throws Exception {
        final CsvProductSourceParser parser = new CsvProductFile("invalid_path");
        parser.parseProperties();
    }

    @Test
    public void testParseRecords() throws Exception {
        final String simpleFormatExample = getClass().getResource("simple_format_example.txt").getFile();
        final CsvProductSourceParser parser = new CsvProductFile(simpleFormatExample);
        parser.parseHeader();
        parser.parseRecords();

        final CsvProductSource csvProductSource = parser.getCsvProductSource();
        final List<Record> records = csvProductSource.getRecords();
        
        assertEquals(3, records.size());
        
        assertEquals("AMRU1", records.get(0).getLocationName());
        assertEquals("AMRU1", records.get(1).getLocationName());
        assertEquals("AMRU2", records.get(2).getLocationName());

        assertEquals(new GeoPos(30.0f, 50.0f), records.get(0).getLocation());
        assertEquals(new GeoPos(30.0f, 50.0f), records.get(1).getLocation());
        assertEquals(new GeoPos(40.0f, 120.0f), records.get(2).getLocation());

        assertEquals(ProductData.UTC.parse("2010-06-01 12:45:00", "yyyy-MM-dd HH:mm:ss").getAsDate().getTime(), records.get(0).getTime().getAsDate().getTime());
        assertEquals(ProductData.UTC.parse("2010-06-01 12:48:00", "yyyy-MM-dd HH:mm:ss").getAsDate().getTime(), records.get(1).getTime().getAsDate().getTime());
        assertEquals(null, records.get(2).getTime());

        for (Record record : records) {
            assertEquals(3, record.getAttributeValues().length);
            
            assertEquals(Double.class, record.getAttributeValues()[0].getClass());
            assertEquals(Double.class, record.getAttributeValues()[1].getClass());
        }
        assertEquals(ProductData.UTC.class, records.get(0).getAttributeValues()[2].getClass());
        assertEquals(ProductData.UTC.class, records.get(2).getAttributeValues()[2].getClass());

        assertEquals(Double.NaN, records.get(0).getAttributeValues()[0]);
        assertEquals(13.4, records.get(0).getAttributeValues()[1]);

        assertEquals(18.3, records.get(1).getAttributeValues()[0]);
        assertEquals(2.4, records.get(1).getAttributeValues()[1]);

        assertEquals(10.5, records.get(2).getAttributeValues()[0]);
        assertEquals(10.6, records.get(2).getAttributeValues()[1]);
        
        assertEquals(ProductData.UTC.parse("2011-06-01 10:45:00", "yyyy-MM-dd HH:mm:ss").getAsDate().getTime(),
                     ((ProductData.UTC)records.get(0).getAttributeValues()[2]).getAsDate().getTime());
        assertEquals(null, records.get(1).getAttributeValues()[2]);
        assertEquals(ProductData.UTC.parse("2011-06-01 12:45:00", "yyyy-MM-dd HH:mm:ss").getAsDate().getTime(),
                     ((ProductData.UTC)records.get(2).getAttributeValues()[2]).getAsDate().getTime());
    }

    @Test
    public void testParseHeader() throws Exception {
        final String simpleFormatExample = getClass().getResource("simple_format_example.txt").getFile();
        final CsvProductSourceParser parser = new CsvProductFile(simpleFormatExample);
        parser.parseHeader();

        final CsvProductSource csvProductSource = parser.getCsvProductSource();
        final Header header = csvProductSource.getHeader();
        assertNotNull(header);
        assertTrue(header.hasLocationName());
        assertTrue(header.hasLocation());
        assertTrue(header.hasTime());

        final HeaderImpl.AttributeHeader[] attributeHeaders = header.getMeasurementAttributeHeaders();
        assertEquals(3, attributeHeaders.length);
        assertEquals("radiance_1", attributeHeaders[0].name);
        assertEquals("float", attributeHeaders[0].type);
        assertEquals("radiance_2", attributeHeaders[1].name);
        assertEquals("float", attributeHeaders[1].type);
        assertEquals("testTime", attributeHeaders[2].name);
        assertEquals("time", attributeHeaders[2].type);

        assertEquals(7, header.getColumnCount());

        HeaderImpl.AttributeHeader attributeHeader = header.getAttributeHeader(0);
        assertEquals("station", attributeHeader.name);
        assertEquals("string", attributeHeader.type);

        attributeHeader = header.getAttributeHeader(3);
        assertEquals("date_time", attributeHeader.name);
        assertEquals("time", attributeHeader.type);

        attributeHeader = header.getAttributeHeader(5);
        assertEquals("radiance_2", attributeHeader.name);
        assertEquals("float", attributeHeader.type);
    }
}
