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

package org.esa.beam.csv.dataio;

import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.feature.simple.SimpleFeatureImpl;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.filter.identity.FeatureIdImpl;
import org.junit.Test;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.PropertyDescriptor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * @author Olaf Danne
 * @author Thomas Storm
 */
public class ConceptTest {

    @Test
    public void testConcept() throws Exception {
        final CsvProductSource source = new MyCsvProductSource("band3", Integer.class);
        final String productName =
                source.getProperties().get("productName") != null ? source.getProperties().get("productName") :
                "defaultProductName";
        final String productType=
                source.getProperties().get("productType") != null ? source.getProperties().get("productType") :
                "defaultProductType";
        final int sceneRasterWidth = source.getRecordCount();
        final Collection<PropertyDescriptor> descriptors = source.getFeatureType().getDescriptors();

        final Product product = new Product(productName, productType, sceneRasterWidth, 1);

        List<Band> bands = new ArrayList<Band>(descriptors.size());

        for (final PropertyDescriptor propertyDescriptor : descriptors) {
            int dataType = getDataType(propertyDescriptor.getType().getBinding());
            bands.add(product.addBand(propertyDescriptor.getName().toString(), dataType));
        }

        assertEquals("testProductName", product.getName());
        assertEquals("defaultProductType", product.getProductType());
        assertEquals(3, product.getSceneRasterWidth());
        assertEquals(1, product.getSceneRasterHeight());

        assertEquals(3, product.getBands().length);
        assertEquals("band1", product.getBands()[0].getName());
        assertEquals("band2", product.getBands()[1].getName());
        assertEquals("band3", product.getBands()[2].getName());

        assignBandData(source, bands);

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
        if (source.getFeatureType().getDescriptors().size() != bands.size()) {
            throw new IllegalArgumentException("number of bands not equal to number of measurement attributes");
        }

        final FeatureCollection<SimpleFeatureType, SimpleFeature> features = source.getFeatureCollection();
        for (int bandIndex = 0; bandIndex < bands.size(); bandIndex++) {
            final Band band = bands.get(bandIndex);
            final Object[] elems = new Object[features.size()];
            final FeatureIterator<SimpleFeature> featureIterator = features.features();
            int recordIndex = 0;
            while (featureIterator.hasNext()) {
                final SimpleFeature simpleFeature = featureIterator.next();
                Object value = simpleFeature.getAttribute(bandIndex);
                elems[recordIndex++] = value;
            }

            final ProductData data = getProductData(elems, band.getDataType());
            band.setData(data);
        }
    }

    @Test
    public void testInvalidInput() throws Exception {
        try {
            getProductData(null, getDataType(String.class));
            fail();
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().matches("Unsupported type.*"));
        }
    }

    @Test
    public void testRecordCountUnequalToBandCount() throws Exception {
        final CsvProductSource source = new MyCsvProductSource("band3", Integer.class);
        List<Band> bands = new ArrayList<Band>(1);

        for (int i = 0; i < 4; i++) {
            bands.add(new Band("band" + i, ProductData.TYPE_FLOAT32, 3, 1));
        }

        try {
            assignBandData(source, bands);
            fail();
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().equals("number of bands not equal to number of measurement attributes"));
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
        assertEquals(ProductData.TYPE_ASCII, getDataType(String.class));
        assertEquals(ProductData.TYPE_FLOAT32, getDataType(Float.class));
        assertEquals(ProductData.TYPE_FLOAT64, getDataType(Double.class));
        assertEquals(ProductData.TYPE_INT8, getDataType(Byte.class));
    }

    private int getDataType(Class<?> type) {
        if (type.getSimpleName().toLowerCase().equals("string")) {
            return ProductData.TYPE_ASCII;
        } else if (type.getSimpleName().toLowerCase().equals("float")) {
            return ProductData.TYPE_FLOAT32;
        } else if (type.getSimpleName().toLowerCase().equals("double")) {
            return ProductData.TYPE_FLOAT64;
        } else if (type.getSimpleName().toLowerCase().equals("byte")) {
            return ProductData.TYPE_INT8;
        } else if (type.getSimpleName().toLowerCase().equals("short")) {
            return ProductData.TYPE_INT16;
        } else if (type.getSimpleName().toLowerCase().equals("integer")) {
            return ProductData.TYPE_INT32;
        } else if(type.getSimpleName().toLowerCase().equals("utc")) {
            return ProductData.TYPE_UTC;
        }
        throw new IllegalArgumentException("Unsupported type '" + type + "'.");
    }

    private static class MyCsvProductSource implements CsvProductSource {

        private SimpleFeatureType simpleFeatureType;
        private final String name;
        private final Class type;

        private MyCsvProductSource(String name, Class type) {
            this.name = name;
            this.type = type;
        }

        @Override
        public int getRecordCount() {
            return 3;
        }

        @Override
        public FeatureCollection<SimpleFeatureType, SimpleFeature> getFeatureCollection() {
            final FeatureCollection<SimpleFeatureType, SimpleFeature> collection = new DefaultFeatureCollection("id", getFeatureType());

            collection.add(new SimpleFeatureImpl(new Object[]{1.0f, 1.1f, 5},
                                                 getFeatureType(),
                                                 new FeatureIdImpl("0"),
                                                 true));
            collection.add(new SimpleFeatureImpl(new Object[]{2.0f, 2.1f, 7},
                                                 getFeatureType(),
                                                 new FeatureIdImpl("1"),
                                                 true));
            collection.add(new SimpleFeatureImpl(new Object[]{3.0f, 3.1f, 123},
                                                 getFeatureType(),
                                                 new FeatureIdImpl("2"),
                                                 true));

            return collection;
        }

        @Override
        public SimpleFeatureType getFeatureType() {
            if (simpleFeatureType == null) {
                final SimpleFeatureTypeBuilder builder = new SimpleFeatureTypeBuilder();

                builder.setName("header");

                builder.add("band1", Float.class);
                builder.add("band2", Float.class);
                builder.add(name, type);

                simpleFeatureType = builder.buildFeatureType();
            }
            return simpleFeatureType;
        }

        @Override
        public Map<String, String> getProperties() {
            final HashMap<String,String> properties = new HashMap<String,String>();
            properties.put("productName", "testProductName");
            return properties;
        }

    }
}