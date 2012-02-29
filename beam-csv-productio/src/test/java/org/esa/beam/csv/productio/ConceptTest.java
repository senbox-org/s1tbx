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

import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.junit.Test;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import static org.junit.Assert.*;

/**
 * @author Olaf Danne
 * @author Thomas Storm
 */
public class ConceptTest {

    @Test
    public void testConcept() throws Exception {
        final CsvProductSource source = new MyCsvProductSource("band3", "int");
        final String productName = source.getProperties().getProperty("productName", "defaultProductName");
        final String productType = source.getProperties().getProperty("productType", "defaultProductType");
        final int sceneRasterWidth = source.getRecordCount();
        final HeaderImpl.AttributeHeader[] measurementAttributeHeaders = source.getHeader().getMeasurementAttributeHeaders();
        final Product product = new Product(productName, productType, sceneRasterWidth, 1);

        List<Band> bands = new ArrayList<Band>(measurementAttributeHeaders.length);

        for (final HeaderImpl.AttributeHeader measurementAttributeHeader : measurementAttributeHeaders) {
            int dataType = getDataType(measurementAttributeHeader.type);
            bands.add(product.addBand(measurementAttributeHeader.name, dataType));
        }

        assignBandData(source, bands);

        assertEquals("testProductName", product.getName());
        assertEquals("defaultProductType", product.getProductType());
        assertEquals(3, product.getSceneRasterWidth());
        assertEquals(1, product.getSceneRasterHeight());

        assertEquals(3, product.getBands().length);
        assertEquals("band1", product.getBands()[0].getName());
        assertEquals("band2", product.getBands()[1].getName());
        assertEquals("band3", product.getBands()[2].getName());

        final ProductData band0Data = bands.get(0).getData();
        final ProductData band1Data = bands.get(1).getData();
        final ProductData band2Data = bands.get(2).getData();

        assertEquals(3, band0Data.getNumElems());
        assertEquals(3, band1Data.getNumElems());
        assertEquals(3, band2Data.getNumElems());

        assertEquals(1.0f, band0Data.getElemFloatAt(0), 1.0E-6);
        assertEquals(2.0f, band0Data.getElemFloatAt(1), 1.0E-6);
        assertEquals(3.0f, band0Data.getElemFloatAt(2), 1.0E-6);

        assertEquals(1.1f, band1Data.getElemFloatAt(0), 1.0E-6);
        assertEquals(2.1f, band1Data.getElemFloatAt(1), 1.0E-6);
        assertEquals(3.1f, band1Data.getElemFloatAt(2), 1.0E-6);

        assertEquals(5, band2Data.getElemIntAt(0));
        assertEquals(7, band2Data.getElemIntAt(1));
        assertEquals(123, band2Data.getElemIntAt(2));
    }

    private void assignBandData(CsvProductSource source, List<Band> bands) {
        final List<Record> records = source.getRecords();
        if(records.size() != bands.size()) {
            throw new IllegalArgumentException("record count != band count");
        }
        for (int bandIndex = 0; bandIndex < bands.size(); bandIndex++) {
            final Band band = bands.get(bandIndex);
            final Object[] elems = new Object[records.size()];
            for (int recordIndex = 0; recordIndex < records.size(); recordIndex++) {
                final Record record = records.get(recordIndex);
                Object value = record.getAttributeValues()[bandIndex];
                elems[recordIndex] = value;
            }

            final ProductData data = getProductData(elems, band.getDataType());
            band.setData(data);
        }
    }

    @Test
    public void testInvalidInput() throws Exception {
        final CsvProductSource source = new MyCsvProductSource("band3", "UINT");
        List<Band> bands = new ArrayList<Band>(1);

        for (HeaderImpl.AttributeHeader attributeHeader : source.getHeader().getMeasurementAttributeHeaders()) {
            int dataType = getDataType(attributeHeader.type);
            bands.add(new Band(attributeHeader.name, dataType, 3, 1));
        }

        try {
            assignBandData(source, bands);
            fail();
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().matches("Unsupported type.*"));
        }
    }

    @Test
    public void testRecordCountUnequalToBandCount() throws Exception {
        final CsvProductSource source = new MyCsvProductSource("band3", "UINT");
        List<Band> bands = new ArrayList<Band>(1);

        for (HeaderImpl.AttributeHeader attributeHeader : source.getHeader().getMeasurementAttributeHeaders()) {
            int dataType = getDataType(attributeHeader.type);
            bands.add(new Band(attributeHeader.name, dataType, 3, 1));
        }
        bands.add(new Band("test", ProductData.TYPE_FLOAT32, 1, 1));

        try {
            assignBandData(source, bands);
            fail();
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().equals("record count != band count"));
        }
    }

    private ProductData getProductData(Object[] elems, int type) {
        ProductData data;
        switch (type) {
            case ProductData.TYPE_FLOAT32: {
                data = new ProductData.Float(elems.length);
                for (int i = 0; i < elems.length; i++) {
                    final Object elem = elems[i];
                    data.setElemFloatAt(i, (Float) elem);
                }
                break;
            }
            case ProductData.TYPE_FLOAT64: {
                data = new ProductData.Double(elems.length);
                for (int i = 0; i < elems.length; i++) {
                    final Object elem = elems[i];
                    data.setElemDoubleAt(i, (Double) elem);
                }
                break;
            }
            case ProductData.TYPE_INT8: {
                data = new ProductData.Byte(elems.length);
                for (int i = 0; i < elems.length; i++) {
                    final Object elem = elems[i];
                    data.setElemIntAt(i, (Byte) elem);
                }
                break;
            }
            case ProductData.TYPE_INT16: {
                data = new ProductData.Short(elems.length);
                for (int i = 0; i < elems.length; i++) {
                    final Object elem = elems[i];
                    data.setElemIntAt(i, (Short) elem);
                }
                break;
            }
            case ProductData.TYPE_INT32: {
                data = new ProductData.Int(elems.length);
                for (int i = 0; i < elems.length; i++) {
                    final Object elem = elems[i];
                    data.setElemIntAt(i, (Integer) elem);
                }
                break;
            }
            default: {
                throw new IllegalArgumentException("Unsupported type '" + ProductData.getTypeString(type) + "'.");
            }
        }

        return data;
    }

    @Test
    public void testGetDataType() throws Exception {
        assertEquals(ProductData.TYPE_ASCII, getDataType("string"));
        assertEquals(ProductData.TYPE_FLOAT32, getDataType("Float"));
        assertEquals(ProductData.TYPE_FLOAT64, getDataType("DOUBLE"));
        assertEquals(ProductData.TYPE_UINT8, getDataType("uint8"));
        assertEquals(ProductData.TYPE_INT8, getDataType("iNT8"));
    }

    private int getDataType(String type) {
        if ("string".equals(type.toLowerCase())) {
            return ProductData.TYPE_ASCII;
        } else if ("float".equals(type.toLowerCase())) {
            return ProductData.TYPE_FLOAT32;
        } else if ("double".equals(type.toLowerCase())) {
            return ProductData.TYPE_FLOAT64;
        } else if ("byte".equals(type.toLowerCase()) || "int8".equals(type.toLowerCase())) {
            return ProductData.TYPE_INT8;
        } else if ("ubyte".equals(type.toLowerCase()) || "uint8".equals(type.toLowerCase())) {
            return ProductData.TYPE_UINT8;
        } else if ("short".equals(type.toLowerCase()) || "int16".equals(type.toLowerCase())) {
            return ProductData.TYPE_INT16;
        } else if ("ushort".equals(type.toLowerCase()) || "uint16".equals(type.toLowerCase())) {
            return ProductData.TYPE_UINT16;
        } else if ("int".equals(type.toLowerCase()) || "int32".equals(type.toLowerCase())) {
            return ProductData.TYPE_INT32;
        } else if ("uint".equals(type.toLowerCase()) || "uint32".equals(type.toLowerCase())) {
            return ProductData.TYPE_UINT32;
        }
        throw new IllegalArgumentException("Unknown type '" + type + "'.");
    }

    private static class MyCsvProductSource implements CsvProductSource {

        private MyHeader header;
        private final String name;
        private final String type;

        private MyCsvProductSource(String name, String type) {
            this.name = name;
            this.type = type;
        }

        @Override
        public int getRecordCount() {
            return 3;
        }

        @Override
        public List<Record> getRecords() {
            final ArrayList<Record> records = new ArrayList<Record>(3);
            try {
                records.add(new MyRecord(new Object[]{
                        1.0f,
                        1.1f,
                        5
                }, new GeoPos(10.0f, 10.0f), "location1", ProductData.UTC.parse("2010-01-01", "yyyy-MM-dd")));
                records.add(new MyRecord(new Object[]{
                        2.0f,
                        2.1f,
                        7
                }, new GeoPos(20.0f, 20.0f), "location2", ProductData.UTC.parse("2020-01-01", "yyyy-MM-dd")));
                records.add(new MyRecord(new Object[]{
                        3.0f,
                        3.1f,
                        123
                }, new GeoPos(30.0f, 30.0f), "location3", ProductData.UTC.parse("2030-01-01", "yyyy-MM-dd")));
            } catch (ParseException e) {
                e.printStackTrace();
            }
            return records;
        }

        @Override
        public Header getHeader() {
            if (header == null) {
                header = new MyHeader(name, type);
            }
            return header;
        }

        @Override
        public Properties getProperties() {
            final Properties properties = new Properties();
            properties.put("productName", "testProductName");
            return properties;
        }

        private static class MyRecord implements Record {

            private final Object[] values;
            private final GeoPos location;
            private final String locationName;
            private final ProductData.UTC time;

            private MyRecord(Object[] values, GeoPos location, String locationName, ProductData.UTC time) {
                this.values = values;
                this.location = location;
                this.locationName = locationName;
                this.time = time;
            }

            @Override
            public GeoPos getLocation() {
                return location;
            }

            @Override
            public ProductData.UTC getTime() {
                return time;
            }

            @Override
            public Object[] getAttributeValues() {
                final Object[] objects = new Object[values.length];
                for (int i = 0; i < values.length; i++) {
                    objects[i] = values[i];
                }
                return objects;
            }

            @Override
            public String getLocationName() {
                return locationName;
            }
        }
    }

    private static class MyHeader implements Header {

        private final HeaderImpl.AttributeHeader attributeHeader;

        private MyHeader(String name, String type) {
            attributeHeader = new HeaderImpl.AttributeHeader(name, type);
        }

        @Override
        public boolean hasLocation() {
            return false;
        }

        @Override
        public boolean hasTime() {
            return false;
        }

        @Override
        public boolean hasLocationName() {
            return false;
        }

        @Override
        public HeaderImpl.AttributeHeader[] getMeasurementAttributeHeaders() {
            final HeaderImpl.AttributeHeader[] attributeHeaders = new HeaderImpl.AttributeHeader[3];
            attributeHeaders[0] = new HeaderImpl.AttributeHeader("band1", "float");
            attributeHeaders[1] = new HeaderImpl.AttributeHeader("band2", "float");
            attributeHeaders[2] = attributeHeader;
            return attributeHeaders;
        }

        @Override
        public int getColumnCount() {
            return 0;
        }

        @Override
        public HeaderImpl.AttributeHeader getAttributeHeader(int columnIndex) {
            return null;
        }
    }
}
