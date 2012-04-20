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

package org.esa.beam.dataio.geometry;

import com.bc.ceres.core.ProgressMonitor;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;
import org.esa.beam.framework.dataio.ProductSubsetDef;
import org.esa.beam.framework.datamodel.*;
import org.esa.beam.framework.dataop.maptransf.Datum;
import org.esa.beam.util.FeatureUtils;
import org.esa.beam.util.io.CsvReader;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.junit.Before;
import org.junit.Test;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.feature.type.GeometryDescriptor;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * @author Olaf Danne
 * @author Thomas Storm
 */
public class VectorDataNodeReader2Test {

    private FeatureUtils.FeatureCrsProvider crsProvider;
    private VectorDataNodeReader2.PlacemarkDescriptorProvider placemarkDescriptorProvider;

    @Before
    public void setUp() throws Exception {
        crsProvider = new FeatureUtils.FeatureCrsProvider() {
            @Override
            public CoordinateReferenceSystem getFeatureCrs(Product product) {
                return DefaultGeographicCRS.WGS84;
            }
        };
        placemarkDescriptorProvider = new VectorDataNodeReader2.PlacemarkDescriptorProvider() {
            @Override
            public PlacemarkDescriptor getPlacemarkDescriptor(SimpleFeatureType simpleFeatureType) {
                return PlacemarkDescriptorRegistry.getInstance().getPlacemarkDescriptor(org.esa.beam.framework.datamodel.GeometryDescriptor.class);
            }
        };
    }

    @Test
    public void testFeatureTypeWithInput1() throws Exception {
        List<AttributeDescriptor> attributeDescriptors = getAttributeDescriptors(readStringFromFile("exported_pin_input1.csv"));

        assertEquals("Name", attributeDescriptors.get(0).getLocalName());
        assertEquals("X", attributeDescriptors.get(1).getLocalName());
        assertEquals("Y", attributeDescriptors.get(2).getLocalName());
        assertEquals("Lon", attributeDescriptors.get(3).getLocalName());
        assertEquals("Lat", attributeDescriptors.get(4).getLocalName());
        assertEquals("Label", attributeDescriptors.get(5).getLocalName());
        assertEquals("Desc", attributeDescriptors.get(6).getLocalName());
        assertEquals("radiance_14", attributeDescriptors.get(7).getLocalName());
        assertEquals("geoPos", attributeDescriptors.get(8).getLocalName());
        assertEquals("pixelPos", attributeDescriptors.get(9).getLocalName());

        assertEquals(String.class, attributeDescriptors.get(0).getType().getBinding());
        assertEquals(Double.class, attributeDescriptors.get(1).getType().getBinding());
        assertEquals(Double.class, attributeDescriptors.get(2).getType().getBinding());
        assertEquals(Double.class, attributeDescriptors.get(3).getType().getBinding());
        assertEquals(Double.class, attributeDescriptors.get(4).getType().getBinding());
        assertEquals(String.class, attributeDescriptors.get(5).getType().getBinding());
        assertEquals(String.class, attributeDescriptors.get(6).getType().getBinding());
        assertEquals(Double.class, attributeDescriptors.get(7).getType().getBinding());
        assertEquals(Point.class, attributeDescriptors.get(8).getType().getBinding());
        assertEquals(Point.class, attributeDescriptors.get(9).getType().getBinding());
    }

    private String readStringFromFile(String name) throws IOException {
        String file = getClass().getResource(name).getFile();
        BufferedReader bufferedReader = new BufferedReader(new FileReader(file));
        StringBuilder b = new StringBuilder();
        while (true) {
            String s = bufferedReader.readLine();
            if (s == null) {
                break;
            }
            b.append(s);
            b.append("\n");
        }
        bufferedReader.close();
        return b.toString();
    }

    @Test
    public void testFeatureTypeWithInputs2And3() throws Exception {
        testFeatureType(getAttributeDescriptors(readStringFromFile("exported_pin_input_wildly_sorted.csv")));
        testFeatureType(getAttributeDescriptors(readStringFromFile("exported_pin_input_without_feature_type_name.csv")));
    }

    private void testFeatureType(List<AttributeDescriptor> attributeDescriptors) {
        assertEquals("Name", attributeDescriptors.get(0).getLocalName());
        assertEquals("X", attributeDescriptors.get(1).getLocalName());
        assertEquals("Y", attributeDescriptors.get(2).getLocalName());
        assertEquals("Lat", attributeDescriptors.get(3).getLocalName());
        assertEquals("Label", attributeDescriptors.get(4).getLocalName());
        assertEquals("Desc", attributeDescriptors.get(5).getLocalName());
        assertEquals("Lon", attributeDescriptors.get(6).getLocalName());
        assertEquals("radiance_14", attributeDescriptors.get(7).getLocalName());
        assertEquals("geoPos", attributeDescriptors.get(8).getLocalName());
        assertEquals("pixelPos", attributeDescriptors.get(9).getLocalName());

        assertEquals(String.class, attributeDescriptors.get(0).getType().getBinding());
        assertEquals(Double.class, attributeDescriptors.get(1).getType().getBinding());
        assertEquals(Double.class, attributeDescriptors.get(2).getType().getBinding());
        assertEquals(Double.class, attributeDescriptors.get(3).getType().getBinding());
        assertEquals(String.class, attributeDescriptors.get(4).getType().getBinding());
        assertEquals(String.class, attributeDescriptors.get(5).getType().getBinding());
        assertEquals(Double.class, attributeDescriptors.get(6).getType().getBinding());
        assertEquals(Double.class, attributeDescriptors.get(7).getType().getBinding());
        assertEquals(Point.class, attributeDescriptors.get(8).getType().getBinding());
        assertEquals(Point.class, attributeDescriptors.get(9).getType().getBinding());
    }

    @Test
    public void testTrackFeatureClassesWithMultipleInputs() throws Exception {
        testTrackFeatureClasses(readStringFromFile("exported_pin_input1.csv"));
        testTrackFeatureClasses(readStringFromFile("exported_pin_input_wildly_sorted.csv"));
        testTrackFeatureClasses(readStringFromFile("exported_pin_input_without_feature_type_name.csv"));
    }

    @Test(expected = IOException.class)
    public void testMissingGeometry() throws IOException {
        InputStreamReader reader = new InputStreamReader(getClass().getResourceAsStream("exported_pin_input_without_geometry.csv"));
        Product dummyProduct = createDummyProduct();
        VectorDataNodeReader2.read("blah", reader, dummyProduct, crsProvider, placemarkDescriptorProvider, DefaultGeographicCRS.WGS84, ProgressMonitor.NULL);
    }

    @Test
    public void testReadPropertiesInInput1() throws IOException {
        InputStreamReader reader = new InputStreamReader(getClass().getResourceAsStream("input_with_properties.csv"));
        final VectorDataNode dataNode = VectorDataNodeReader2.read("non-empty", reader, createDummyProduct(), crsProvider, placemarkDescriptorProvider, DefaultGeographicCRS.WGS84, ProgressMonitor.NULL);
        final Map<Object, Object> properties = dataNode.getFeatureType().getUserData();

        assertNotNull(properties);
        assertEquals(4, properties.size());
        assertEquals("color:0,0,255", properties.get("styleCss"));
        assertEquals("TAB", properties.get("separator"));
        assertEquals("GeometryDescriptor", properties.get("beam.placemarkDescriptor.class"));
        assertEquals("geom", properties.get("defaultGeometry"));
    }

    @Test
    public void testReadFeaturesInInput1() throws IOException {
        InputStreamReader reader = new InputStreamReader(getClass().getResourceAsStream("input_with_properties.csv"));

        final VectorDataNode dataNode = VectorDataNodeReader2.read("blah", reader, createDummyProduct(), crsProvider, placemarkDescriptorProvider, DefaultGeographicCRS.WGS84, ProgressMonitor.NULL);
        FeatureCollection<SimpleFeatureType, SimpleFeature> fc = dataNode.getFeatureCollection();
        SimpleFeatureType simpleFeatureType = fc.getSchema();

        assertNotNull(simpleFeatureType);
        assertNotNull(simpleFeatureType.getCoordinateReferenceSystem());
        assertEquals("org.esa.beam.FT1", simpleFeatureType.getTypeName());
        assertEquals(4, simpleFeatureType.getAttributeCount());

        List<AttributeDescriptor> list = simpleFeatureType.getAttributeDescriptors();
        AttributeDescriptor ad0 = list.get(0);
        AttributeDescriptor ad1 = list.get(1);
        AttributeDescriptor ad2 = list.get(2);
        AttributeDescriptor ad3 = list.get(3);

        assertEquals("name", ad0.getType().getName().getLocalPart());
        assertEquals(String.class, ad0.getType().getBinding());

        assertEquals("geom", ad1.getType().getName().getLocalPart());
        assertEquals(Geometry.class, ad1.getType().getBinding());

        assertEquals("pixel", ad2.getType().getName().getLocalPart());
        assertEquals(Integer.class, ad2.getType().getBinding());

        assertEquals("description", ad3.getType().getName().getLocalPart());
        assertEquals(String.class, ad3.getType().getBinding());

        GeometryDescriptor geometryDescriptor = simpleFeatureType.getGeometryDescriptor();
        assertEquals("geom", geometryDescriptor.getType().getName().getLocalPart());

        assertEquals(3, fc.size());
        FeatureIterator<SimpleFeature> featureIterator = fc.features();

        SimpleFeature f1 = featureIterator.next();
        assertEquals("ID65", f1.getID());
        assertEquals("mark1", f1.getAttribute(0));
        //assertEquals(gf.createPoint(new Coordinate(12.3, 45.6)), f1.getAttribute(1));
        assertEquals(0, f1.getAttribute(2));
        assertEquals("This is mark1.", f1.getAttribute(3));

        SimpleFeature f2 = featureIterator.next();
        assertEquals("ID66", f2.getID());
        assertEquals("mark2", f2.getAttribute(0));
        //assertEquals(gf.createPoint(new Coordinate(78.9,  10.1)), f2.getAttribute(1));
        assertEquals(1, f2.getAttribute(2));
        assertEquals(null, f2.getAttribute(3));

        SimpleFeature f3 = featureIterator.next();
        assertEquals("ID67", f3.getID());
        assertEquals("mark3", f3.getAttribute(0));
        //assertEquals(gf.createPoint(new Coordinate(23.4, 56.7)), f3.getAttribute(1));
        assertEquals(2, f3.getAttribute(2));
        assertEquals("This is mark3.", f3.getAttribute(3));
    }

    @Test
    public void testReadFeaturesInInput2() throws IOException {
        InputStreamReader reader = new InputStreamReader(getClass().getResourceAsStream("input_with_only_one_point.csv"));

        final VectorDataNode dataNode = VectorDataNodeReader2.read("blah", reader, createDummyProduct(), crsProvider, placemarkDescriptorProvider, DefaultGeographicCRS.WGS84, ProgressMonitor.NULL);
        FeatureCollection<SimpleFeatureType, SimpleFeature> fc = dataNode.getFeatureCollection();
        SimpleFeatureType simpleFeatureType = fc.getSchema();

        assertNotNull(simpleFeatureType);
        assertEquals("org.esa.beam.FT2", simpleFeatureType.getTypeName());
        assertEquals(3, simpleFeatureType.getAttributeCount());

        List<AttributeDescriptor> list = simpleFeatureType.getAttributeDescriptors();
        AttributeDescriptor ad0 = list.get(0);
        AttributeDescriptor ad1 = list.get(1);
        AttributeDescriptor ad2 = list.get(2);

        assertEquals("name", ad0.getType().getName().getLocalPart());
        assertEquals(String.class, ad0.getType().getBinding());

        assertEquals("geom", ad1.getType().getName().getLocalPart());
        assertEquals(Point.class, ad1.getType().getBinding());

        assertEquals("weight", ad2.getType().getName().getLocalPart());
        assertEquals(Float.class, ad2.getType().getBinding());
    }

    @Test
    public void testCRS() throws Exception {
        InputStreamReader reader = new InputStreamReader(getClass().getResourceAsStream("input_with_only_one_point.csv"));
        final VectorDataNode dataNode = VectorDataNodeReader2.read("blah", reader, createDummyProduct(), crsProvider, placemarkDescriptorProvider, DefaultGeographicCRS.WGS84, ProgressMonitor.NULL);
        FeatureCollection<SimpleFeatureType, SimpleFeature> fc = dataNode.getFeatureCollection();
        SimpleFeatureType simpleFeatureType = fc.getSchema();

        assertNotNull(simpleFeatureType);
        CoordinateReferenceSystem crs = simpleFeatureType.getCoordinateReferenceSystem();
        assertNotNull(crs);
    }

    @Test
    public void testWrongUsages() {

        try {
            expectException("");
        } catch (IOException expected) {
            // ok
        }

        try {
            expectException("org.esa.beam.FT\ta:\n");
        } catch (IOException expected) {
            // ok
        }

        try {
            expectException("org.esa.beam.FT\t:Integer\n");
        } catch (IOException expected) {
            // ok
        }

        try {
            expectException("org.esa.beam.FT\ta:Integer\tlat\tlon\n" +
                            "ID1\t1234\tABC\n");
        } catch (IOException expected) {
            // ok
        }

        try {
            VectorDataNodeReader2.read("blah", new StringReader("org.esa.beam.FT\ta:Integer\tno_lat\tlon\n" +
                                                                "ID1\t1234\tABC\n"), new Product("name", "type", 360, 90), crsProvider, placemarkDescriptorProvider, DefaultGeographicCRS.WGS84, ProgressMonitor.NULL).getFeatureCollection();
        } catch (IOException expected) {
            // ok
        }

        try {
            String source = "org.esa.beam.FT\ta\tlat\tlon\n" +
                            "ID1\t10\t5.0\t50.0\n";
            FeatureCollection<SimpleFeatureType, SimpleFeature> fc = VectorDataNodeReader2.read("blah", new StringReader(source), createDummyProduct(), crsProvider, placemarkDescriptorProvider, DefaultGeographicCRS.WGS84, ProgressMonitor.NULL).getFeatureCollection();
            assertEquals(1, fc.size());
            assertEquals(Double.class, fc.getSchema().getType(0).getBinding());
            assertEquals(10.0, fc.features().next().getAttribute("a"));
        } catch (IOException e) {
            fail("Not expected: IOException: " + e.getMessage());
        }
    }

    private void expectException(String contents) throws IOException {
        VectorDataNodeReader2.read("blah", new StringReader(contents), createDummyProduct(), crsProvider, placemarkDescriptorProvider, DefaultGeographicCRS.WGS84, ProgressMonitor.NULL);
        fail("IOException expected");
    }

    private void testTrackFeatureClasses(String input) throws IOException {
        final VectorDataNode node = VectorDataNodeReader2.read("blah", new StringReader(input), createDummyProduct(), crsProvider, placemarkDescriptorProvider, DefaultGeographicCRS.WGS84, ProgressMonitor.NULL);
        FeatureCollection<SimpleFeatureType, SimpleFeature> featureCollection = node.getFeatureCollection();

        FeatureIterator<SimpleFeature> features = featureCollection.features();
        assertEquals(6, featureCollection.size());

        List<SimpleFeature> simpleFeatures = new ArrayList<SimpleFeature>();

        while (features.hasNext()) {
            simpleFeatures.add(features.next());
        }

        for (SimpleFeature simpleFeature : simpleFeatures) {
            assertTrue(simpleFeature.getDefaultGeometry() instanceof Point);
        }

        assertEquals("pin_1", simpleFeatures.get(0).getAttribute("Name").toString());
        assertEquals("pin_6", simpleFeatures.get(5).getAttribute("Name").toString());

        assertEquals("151.5", simpleFeatures.get(0).getAttribute("Y").toString());
        assertEquals("313.5", simpleFeatures.get(5).getAttribute("Y").toString());

        assertEquals(7.777766, ((Point) simpleFeatures.get(0).getAttribute("geoPos")).getX(), 1E-3);
        assertEquals(47.96903, ((Point) simpleFeatures.get(0).getAttribute("geoPos")).getY(), 1E-3);

        assertEquals(5.0080223, ((Point) simpleFeatures.get(5).getAttribute("geoPos")).getX(), 1E-3);
        assertEquals(46.710358, ((Point) simpleFeatures.get(5).getAttribute("geoPos")).getY(), 1E-3);

        assertEquals(7.77766, ((Point) simpleFeatures.get(0).getAttribute("pixelPos")).getX(), 1E-3);
        assertEquals(47.96903, ((Point) simpleFeatures.get(0).getAttribute("pixelPos")).getY(), 1E-3);

        assertEquals(5.0080223, ((Point) simpleFeatures.get(5).getAttribute("pixelPos")).getX(), 1E-3);
        assertEquals(46.710358, ((Point) simpleFeatures.get(5).getAttribute("pixelPos")).getY(), 1E-3);

    }

    private List<AttributeDescriptor> getAttributeDescriptors(String input) throws IOException {
        CsvReader csvReader = new CsvReader(new StringReader(input), new char[]{'\t'}, true, "#");
        SimpleFeatureType simpleFeatureType = VectorDataNodeReader2.read("blah", csvReader, createDummyProduct(), crsProvider, placemarkDescriptorProvider, DefaultGeographicCRS.WGS84, ProgressMonitor.NULL).getFeatureType();

        assertNotNull(simpleFeatureType);
        assertEquals(10, simpleFeatureType.getAttributeCount());

        return simpleFeatureType.getAttributeDescriptors();
    }

    private static Product createDummyProduct() {
        Product dummyProduct = new Product("blah", "blahType", 360, 180);
        dummyProduct.setGeoCoding(new DummyGeoCoding());
        return dummyProduct;
    }


    static class DummyGeoCoding extends AbstractGeoCoding {

        @Override
        public boolean transferGeoCoding(Scene srcScene, Scene destScene, ProductSubsetDef subsetDef) {
            return false;
        }

        @Override
        public boolean isCrossingMeridianAt180() {
            return false;
        }

        @Override
        public boolean canGetPixelPos() {
            return false;
        }

        @Override
        public boolean canGetGeoPos() {
            return false;
        }

        @Override
        public PixelPos getPixelPos(GeoPos geoPos, PixelPos pixelPos) {
            if (pixelPos == null) {
                pixelPos = new PixelPos();
            }
            pixelPos.x = geoPos.lon;
            pixelPos.y = geoPos.lat;
            return pixelPos;
        }

        @Override
        public GeoPos getGeoPos(PixelPos pixelPos, GeoPos geoPos) {
            if (geoPos == null) {
                geoPos = new GeoPos();
            }
            geoPos.lon = pixelPos.x;
            geoPos.lat = pixelPos.y;
            return geoPos;
        }

        @Override
        public Datum getDatum() {
            return null;
        }

        @Override
        public void dispose() {
        }
    }
}
