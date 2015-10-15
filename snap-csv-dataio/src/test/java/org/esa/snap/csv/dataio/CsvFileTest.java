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

package org.esa.snap.csv.dataio;

import org.esa.snap.core.datamodel.GeoPos;
import org.esa.snap.core.datamodel.ProductData;
import org.junit.Test;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.type.FeatureType;
import org.opengis.feature.type.PropertyDescriptor;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * @author Olaf Danne
 * @author Thomas Storm
 */
public class CsvFileTest {

    @Test
    public void testParseProperties() throws Exception {
        final String simpleFormatExample = getClass().getResource("reader/simple_format_example.txt").getFile();
        final CsvSourceParser parser = CsvFile.createCsvSourceParser(simpleFormatExample);
        parser.parseMetadata();

        final CsvSource source = parser.parseMetadata();
        final Map<String, String> properties = source.getProperties();
        assertNotNull(properties);
        assertEquals(5, properties.size());
        assertEquals("POLYGON(0.0, 1.0, 1.1)", properties.get("geometry1"));
        assertEquals("POLYGON(2.0, 1.0, 1.1)", properties.get("geometry2"));
        assertEquals(",", properties.get("separator"));
        assertEquals("3", properties.get("sceneRasterWidth"));
    }

    @Test(expected = IOException.class)
    public void testParseProperties_Fail() throws Exception {
        final CsvSourceParser parser = CsvFile.createCsvSourceParser("invalid_path");
        parser.parseMetadata();
    }

    @Test
    public void testParseRecords() throws Exception {
        final String simpleFormatExample = getClass().getResource("reader/simple_format_example.txt").getFile();
        final CsvSourceParser parser = CsvFile.createCsvSourceParser(simpleFormatExample);
        final CsvSource csvSource = parser.parseMetadata();
        parser.parseRecords(0, 3);

        SimpleFeature[] features = csvSource.getSimpleFeatures();

        assertEquals(4, csvSource.getRecordCount());
        assertEquals(3, features.length);

        SimpleFeature feature = features[0];

        assertEquals(7, feature.getAttributeCount());
        assertEquals("" + 26, feature.getID());
        assertEquals(String.class, feature.getAttribute(0).getClass());
        assertEquals(Float.class, feature.getAttribute(1).getClass());
        assertEquals(Float.class, feature.getAttribute(2).getClass());
        assertEquals(ProductData.UTC.class, feature.getAttribute(3).getClass());
        assertEquals(Float.class, feature.getAttribute(4).getClass());
        assertEquals(Float.class, feature.getAttribute(5).getClass());
        assertEquals(ProductData.UTC.class, feature.getAttribute(6).getClass());

        feature = features[1];

        assertEquals(7, feature.getAttributeCount());
        assertEquals("" + 28, feature.getID());
        assertEquals(String.class, feature.getAttribute(0).getClass());
        assertEquals(Float.class, feature.getAttribute(1).getClass());
        assertEquals(Float.class, feature.getAttribute(2).getClass());
        assertEquals(ProductData.UTC.class, feature.getAttribute(3).getClass());
        assertEquals(Float.class, feature.getAttribute(4).getClass());
        assertEquals(Float.class, feature.getAttribute(5).getClass());
        assertEquals(null, feature.getAttribute(6));

        feature = features[2];

        assertEquals(7, feature.getAttributeCount());
        assertEquals("" + 371, feature.getID());
        assertEquals(String.class, feature.getAttribute(0).getClass());
        assertEquals(Float.class, feature.getAttribute(1).getClass());
        assertEquals(Float.class, feature.getAttribute(2).getClass());
        assertEquals(null, feature.getAttribute(3));
        assertEquals(Float.class, feature.getAttribute(4).getClass());
        assertEquals(Float.class, feature.getAttribute(5).getClass());
        assertEquals(ProductData.UTC.class, feature.getAttribute(6).getClass());

        assertEquals("AMRU1", features[0].getAttribute(0));
        assertEquals("AMRU1", features[1].getAttribute(0));
        assertEquals("AMRU2", features[2].getAttribute(0));

        assertEquals(new GeoPos(30.0f, 50.0f), new GeoPos((Float) features[0].getAttribute(1), (Float) features[0].getAttribute(2)));
        assertEquals(new GeoPos(30.0f, 50.0f), new GeoPos((Float) features[1].getAttribute(1), (Float) features[1].getAttribute(2)));
        assertEquals(new GeoPos(40.0f, 120.0f), new GeoPos((Float) features[2].getAttribute(1), (Float) features[2].getAttribute(2)));

        assertEquals(ProductData.UTC.parse("2010-06-01 12:45:00", "yyyy-MM-dd HH:mm:ss").getAsDate().getTime(), ((ProductData.UTC) features[0].getAttribute(3)).getAsDate().getTime());
        assertEquals(ProductData.UTC.parse("2010-06-01 12:48:00", "yyyy-MM-dd HH:mm:ss").getAsDate().getTime(), ((ProductData.UTC) features[1].getAttribute(3)).getAsDate().getTime());
        assertEquals(null, features[2].getAttribute(3));

        assertEquals(Float.NaN, features[0].getAttribute(4));
        assertEquals(18.3f, features[1].getAttribute(4));
        assertEquals(10.6f, features[2].getAttribute(5));

        assertEquals(ProductData.UTC.parse("2011-06-01 10:45:00", "yyyy-MM-dd HH:mm:ss").getAsDate().getTime(),
                     ((ProductData.UTC) features[0].getAttribute(6)).getAsDate().getTime());

    }


    @Test
    public void testParseRecords_NoFeatureId() throws Exception {
        final String simpleFormatExample = getClass().getResource("reader/simple_format_no_feature_id.txt").getFile();
        final CsvSourceParser parser = CsvFile.createCsvSourceParser(simpleFormatExample);
        final CsvSource csvSource = parser.parseMetadata();
        parser.parseRecords(0, 3);

        SimpleFeature[] features = csvSource.getSimpleFeatures();

        assertEquals(3, csvSource.getRecordCount());
        assertEquals(3, features.length);

        SimpleFeature feature = features[0];

        assertEquals(7, feature.getAttributeCount());

        assertEquals("" + 0, feature.getID());
        assertEquals(String.class, feature.getAttribute(0).getClass());
        assertEquals(Float.class, feature.getAttribute(1).getClass());
        assertEquals(Float.class, feature.getAttribute(2).getClass());
        assertEquals(ProductData.UTC.class, feature.getAttribute(3).getClass());
        assertEquals(Float.class, feature.getAttribute(4).getClass());
        assertEquals(Float.class, feature.getAttribute(5).getClass());
        assertEquals(ProductData.UTC.class, feature.getAttribute(6).getClass());

        feature = features[1];

        assertEquals(7, feature.getAttributeCount());

        assertEquals("" + 1, feature.getID());
        assertEquals(String.class, feature.getAttribute(0).getClass());
        assertEquals(Float.class, feature.getAttribute(1).getClass());
        assertEquals(Float.class, feature.getAttribute(2).getClass());
        assertEquals(ProductData.UTC.class, feature.getAttribute(3).getClass());
        assertEquals(Float.class, feature.getAttribute(4).getClass());
        assertEquals(Float.class, feature.getAttribute(5).getClass());
        assertEquals(null, feature.getAttribute(6));

        feature = features[2];

        assertEquals(7, feature.getAttributeCount());
        assertEquals("" + 2, feature.getID());
        assertEquals(String.class, feature.getAttribute(0).getClass());
        assertEquals(Float.class, feature.getAttribute(1).getClass());
        assertEquals(Float.class, feature.getAttribute(2).getClass());
        assertEquals(null, feature.getAttribute(3));
        assertEquals(Float.class, feature.getAttribute(4).getClass());
        assertEquals(Float.class, feature.getAttribute(5).getClass());
        assertEquals(ProductData.UTC.class, feature.getAttribute(6).getClass());

        assertEquals("AMRU1", features[0].getAttribute(0));
        assertEquals("AMRU1", features[1].getAttribute(0));
        assertEquals("AMRU2", features[2].getAttribute(0));

        assertEquals(new GeoPos(30.0f, 50.0f), new GeoPos((Float) features[0].getAttribute(1), (Float) features[0].getAttribute(2)));
        assertEquals(new GeoPos(30.0f, 50.0f), new GeoPos((Float) features[1].getAttribute(1), (Float) features[1].getAttribute(2)));
        assertEquals(new GeoPos(40.0f, 120.0f), new GeoPos((Float) features[2].getAttribute(1), (Float) features[2].getAttribute(2)));

        assertEquals(ProductData.UTC.parse("2010-06-01 12:45:00", "yyyy-MM-dd HH:mm:ss").getAsDate().getTime(), ((ProductData.UTC) features[0].getAttribute(3)).getAsDate().getTime());
        assertEquals(ProductData.UTC.parse("2010-06-01 12:48:00", "yyyy-MM-dd HH:mm:ss").getAsDate().getTime(), ((ProductData.UTC) features[1].getAttribute(3)).getAsDate().getTime());
        assertEquals(null, features[2].getAttribute(3));

        assertEquals(Float.NaN, features[0].getAttribute(4));
        assertEquals(18.3f, features[1].getAttribute(4));
        assertEquals(10.6f, features[2].getAttribute(5));

        assertEquals(ProductData.UTC.parse("2011-06-01 10:45:00", "yyyy-MM-dd HH:mm:ss").getAsDate().getTime(),
                     ((ProductData.UTC) features[0].getAttribute(6)).getAsDate().getTime());

    }

    @Test
    public void testParseRecords_NotAllRecords() throws Exception {
        final String simpleFormatExample = getClass().getResource("reader/simple_format_example.txt").getFile();
        final CsvSourceParser parser = CsvFile.createCsvSourceParser(simpleFormatExample);
        final CsvSource csvSource = parser.parseMetadata();
        parser.parseRecords(1, 2);

        SimpleFeature[] features = csvSource.getSimpleFeatures();

        assertEquals(4, csvSource.getRecordCount());
        assertEquals(2, features.length);

        SimpleFeature feature = features[0];

        assertEquals(7, feature.getAttributeCount());

        assertEquals("" + 28, feature.getID());
        assertEquals(String.class, feature.getAttribute(0).getClass());
        assertEquals(Float.class, feature.getAttribute(1).getClass());
        assertEquals(Float.class, feature.getAttribute(2).getClass());
        assertEquals(ProductData.UTC.class, feature.getAttribute(3).getClass());
        assertEquals(Float.class, feature.getAttribute(4).getClass());
        assertEquals(Float.class, feature.getAttribute(5).getClass());
        assertEquals(null, feature.getAttribute(6));

        feature = features[1];

        assertEquals(7, feature.getAttributeCount());
        assertEquals("" + 371, feature.getID());
        assertEquals(String.class, feature.getAttribute(0).getClass());
        assertEquals(Float.class, feature.getAttribute(1).getClass());
        assertEquals(Float.class, feature.getAttribute(2).getClass());
        assertEquals(null, feature.getAttribute(3));
        assertEquals(Float.class, feature.getAttribute(4).getClass());
        assertEquals(Float.class, feature.getAttribute(5).getClass());
        assertEquals(ProductData.UTC.class, feature.getAttribute(6).getClass());

        assertEquals("AMRU1", features[0].getAttribute(0));
        assertEquals("AMRU2", features[1].getAttribute(0));

        assertEquals(new GeoPos(30.0f, 50.0f), new GeoPos((Float) features[0].getAttribute(1), (Float) features[0].getAttribute(2)));
        assertEquals(new GeoPos(40.0f, 120.0f), new GeoPos((Float) features[1].getAttribute(1), (Float) features[1].getAttribute(2)));

        assertEquals(ProductData.UTC.parse("2010-06-01 12:48:00", "yyyy-MM-dd HH:mm:ss").getAsDate().getTime(), ((ProductData.UTC) features[0].getAttribute(3)).getAsDate().getTime());
        assertEquals(null, features[1].getAttribute(3));

        assertEquals(18.3f, features[0].getAttribute(4));
        assertEquals(10.6f, features[1].getAttribute(5));
    }

    @Test
    public void testParseRecords_LessRecordsThanExpected() throws Exception {
        final String simpleFormatExample = getClass().getResource("reader/simple_format_example.txt").getFile();
        final CsvSourceParser parser = CsvFile.createCsvSourceParser(simpleFormatExample);
        final CsvSource csvSource = parser.parseMetadata();
        parser.parseRecords(0, 10);

        SimpleFeature[] features = csvSource.getSimpleFeatures();

        assertEquals(4, csvSource.getRecordCount());
        assertEquals(4, features.length);
    }

    @Test
    public void testParseHeader() throws Exception {
        final String simpleFormatExample = getClass().getResource("reader/simple_format_example.txt").getFile();
        final CsvSourceParser parser = CsvFile.createCsvSourceParser(simpleFormatExample);
        parser.parseMetadata();

        // we call parseMetadata twice to make sure that counters are properly reset
        final CsvSource csvSource = parser.parseMetadata();
        final FeatureType featureType = csvSource.getFeatureType();

        assertNotNull(featureType);

        final PropertyDescriptor[] propertyDescriptors = toPropertyDescriptorArray(featureType.getDescriptors());
        assertEquals(7, propertyDescriptors.length);
        assertEquals("station", propertyDescriptors[0].getName().toString());
        assertEquals("lat", propertyDescriptors[1].getName().toString());
        assertEquals("lon", propertyDescriptors[2].getName().toString());
        assertEquals("date_time", propertyDescriptors[3].getName().toString());
        assertEquals("radiance_1", propertyDescriptors[4].getName().toString());
        assertEquals("radiance_2", propertyDescriptors[5].getName().toString());
        assertEquals("testTime", propertyDescriptors[6].getName().toString());

        assertEquals(7, propertyDescriptors.length);
        assertTrue(propertyDescriptors[0].getType().getBinding().getSimpleName().matches(".*String"));
        assertTrue(propertyDescriptors[1].getType().getBinding().getSimpleName().matches(".*Float"));
        assertTrue(propertyDescriptors[2].getType().getBinding().getSimpleName().matches(".*Float"));
        assertTrue(propertyDescriptors[3].getType().getBinding().getSimpleName().matches(".*UTC"));
        assertTrue(propertyDescriptors[4].getType().getBinding().getSimpleName().matches(".*Float"));
        assertTrue(propertyDescriptors[5].getType().getBinding().getSimpleName().matches(".*Float"));
        assertTrue(propertyDescriptors[6].getType().getBinding().getSimpleName().matches(".*UTC"));
    }

    @Test
    public void testParseHeader_NoFeatureId() throws Exception {
        final String simpleFormatExample = getClass().getResource("reader/simple_format_no_feature_id.txt").getFile();
        final CsvSourceParser parser = CsvFile.createCsvSourceParser(simpleFormatExample);
        parser.parseMetadata();

        final CsvSource csvSource = parser.parseMetadata();
        final FeatureType featureType = csvSource.getFeatureType();

        assertNotNull(featureType);

        final PropertyDescriptor[] propertyDescriptors = toPropertyDescriptorArray(featureType.getDescriptors());
        assertEquals(7, propertyDescriptors.length);
        assertEquals("station", propertyDescriptors[0].getName().toString());
        assertEquals("lat", propertyDescriptors[1].getName().toString());
        assertEquals("lon", propertyDescriptors[2].getName().toString());
        assertEquals("date_time", propertyDescriptors[3].getName().toString());
        assertEquals("radiance_1", propertyDescriptors[4].getName().toString());
        assertEquals("radiance_2", propertyDescriptors[5].getName().toString());
        assertEquals("testTime", propertyDescriptors[6].getName().toString());

        assertEquals(7, propertyDescriptors.length);
        assertTrue(propertyDescriptors[0].getType().getBinding().getSimpleName().matches(".*String"));
        assertTrue(propertyDescriptors[1].getType().getBinding().getSimpleName().matches(".*Float"));
        assertTrue(propertyDescriptors[2].getType().getBinding().getSimpleName().matches(".*Float"));
        assertTrue(propertyDescriptors[3].getType().getBinding().getSimpleName().matches(".*UTC"));
        assertTrue(propertyDescriptors[4].getType().getBinding().getSimpleName().matches(".*Float"));
        assertTrue(propertyDescriptors[5].getType().getBinding().getSimpleName().matches(".*Float"));
        assertTrue(propertyDescriptors[6].getType().getBinding().getSimpleName().matches(".*UTC"));
    }

    private PropertyDescriptor[] toPropertyDescriptorArray(Collection<PropertyDescriptor> descriptors) {
        final Object[] objects = descriptors.toArray(new Object[descriptors.size()]);
        final PropertyDescriptor[] propertyDescriptors = new PropertyDescriptor[objects.length];
        for (int i = 0; i < propertyDescriptors.length; i++) {
            propertyDescriptors[i] = (PropertyDescriptor) objects[i];

        }
        return propertyDescriptors;
    }
}
