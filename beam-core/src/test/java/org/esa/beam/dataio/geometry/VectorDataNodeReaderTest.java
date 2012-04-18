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

package org.esa.beam.dataio.geometry;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import junit.framework.TestCase;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.feature.type.GeometryDescriptor;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import java.io.IOException;
import java.io.StringReader;
import java.util.List;
import java.util.Map;

public class VectorDataNodeReaderTest extends TestCase {
    static final String INPUT_1 =
            "# This is a test comment\n" +
                    "# separator=TAB\n" +
                    "# styleCss=color:0,0,255\n" +
                    "\n" +
                    "org.esa.beam.FT1\tname:String\tgeom:Geometry\tpixel:Integer\tdescription:String\n"
                    + "ID65\tmark1\tPOINT (12.3 45.6)\t0\tThis is mark1.\n"
                    + "ID66\tmark2\tPOINT (78.9 10.1)\t1\t[null]\n"
                    + "ID67\tmark3\tPOINT (23.4 56.7)\t2\tThis is mark3.\n";

    static final String INPUT_2 =
            "org.esa.beam.FT2\tname:String\tgeom:Point\tweight:Float\n"
                    + "ID65\tmark1\tPOINT (12.3 45.6)\t0.4\n";

    public void testReadPropertiesInInput1() throws IOException {
        StringReader reader = new StringReader(INPUT_1);

        VectorDataNodeReader vectorDataNodeReader = new VectorDataNodeReader("mem", null);
        Map<String,String> properties = vectorDataNodeReader.readProperties(reader);

        assertNotNull(properties);
        assertEquals(2, properties.size());
        assertEquals("color:0,0,255", properties.get("styleCss"));
        assertEquals("TAB", properties.get("separator"));
    }

    public void testReadFeaturesInInput1() throws IOException {
        StringReader reader = new StringReader(INPUT_1);

        VectorDataNodeReader vectorDataNodeReader = new VectorDataNodeReader("mem", null);
        FeatureCollection<SimpleFeatureType, SimpleFeature> fc = vectorDataNodeReader.readFeatures(reader);
        SimpleFeatureType simpleFeatureType = fc.getSchema();

        assertNotNull(simpleFeatureType);
        assertNull(simpleFeatureType.getCoordinateReferenceSystem());
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

        GeometryFactory gf = new GeometryFactory();

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

    public void testReadFeaturesInInput2() throws IOException {
        StringReader reader = new StringReader(INPUT_2);

        VectorDataNodeReader vectorDataNodeReader = new VectorDataNodeReader("mem", null);
        FeatureCollection<SimpleFeatureType, SimpleFeature> fc = vectorDataNodeReader.readFeatures(reader);
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

    public void testCRS() throws Exception {
        StringReader reader = new StringReader(INPUT_2);

        VectorDataNodeReader vectorDataNodeReader = new VectorDataNodeReader("mem", DefaultGeographicCRS.WGS84);
        FeatureCollection<SimpleFeatureType, SimpleFeature> fc = vectorDataNodeReader.readFeatures(reader);
        SimpleFeatureType simpleFeatureType = fc.getSchema();

        assertNotNull(simpleFeatureType);
        CoordinateReferenceSystem crs = simpleFeatureType.getCoordinateReferenceSystem();
        assertNotNull(crs);
        assertSame(DefaultGeographicCRS.WGS84, crs);
    }

    public void testFailures() {

        try {
            expectException("");
        } catch (IOException e) {
            // ok
        }

        try {
            expectException("FT\ta:\n");
        } catch (IOException e) {
            // ok
        }

        try {
            expectException("FT\t:Integer\n");
        } catch (IOException e) {
            // ok
        }

        try {
            expectException("FT\ta\n");
        } catch (IOException e) {
            // ok
        }

        try {
            String source = "FT\ta:Integer\n" +
                    "ID1\tX\n";
            FeatureCollection<SimpleFeatureType, SimpleFeature> fc = new VectorDataNodeReader("mem", null).readFeatures(new StringReader(source));
            assertEquals(1, fc.size());
            assertEquals(null, fc.features().next().getAttribute("a"));
        } catch (IOException e) {
            fail("Not expected: IOException: " + e.getMessage());
        }

        try {
            String source = "FT\ta:Integer\n" +
                    "ID1\t1234\tABC\n";
            FeatureCollection<SimpleFeatureType, SimpleFeature> fc = new VectorDataNodeReader("mem", null).readFeatures(new StringReader(source));
            assertEquals(0, fc.size());
        } catch (IOException e) {
            fail("Not expected: IOException: " + e.getMessage());
        }
    }

    private void expectException(String contents) throws IOException {
        new VectorDataNodeReader("mem", null).readFeatures(new StringReader(contents));
        fail("IOException expected");
    }
}
