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

public class VectorDataNodeReaderTest extends TestCase {
    static final String CONTENTS_1 =
            "FT1\tname:String\tgeom:Geometry\tpixel:Integer\tdescription:String\n"
                    + "ID65\tmark1\tPOINT (12.3 45.6)\t0\tThis is mark1.\n"
                    + "ID66\tmark2\tPOINT (78.9 10.1)\t1\t[null]\n"
                    + "ID67\tmark3\tPOINT (23.4 56.7)\t2\tThis is mark3.\n";

    static final String CONTENTS_2 =
            "FT2\tname:String\tgeom:Point\tweight:Float\n"
                    + "ID65\tmark1\tPOINT (12.3 45.6)\t0.4\n";

    public void testContents1() throws IOException {
        StringReader reader = new StringReader(CONTENTS_1);

        VectorDataNodeReader vectorDataNodeReader = new VectorDataNodeReader(null);
        FeatureCollection<SimpleFeatureType, SimpleFeature> fc = vectorDataNodeReader.readFeatures(reader);
        SimpleFeatureType simpleFeatureType = fc.getSchema();

        assertNotNull(simpleFeatureType);
        assertNull(simpleFeatureType.getCoordinateReferenceSystem());
        assertEquals("FT1", simpleFeatureType.getTypeName());
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

    public void testContents2() throws IOException {
        StringReader reader = new StringReader(CONTENTS_2);

        VectorDataNodeReader vectorDataNodeReader = new VectorDataNodeReader(null);
        FeatureCollection<SimpleFeatureType, SimpleFeature> fc = vectorDataNodeReader.readFeatures(reader);
        SimpleFeatureType simpleFeatureType = fc.getSchema();

        assertNotNull(simpleFeatureType);
        assertEquals("FT2", simpleFeatureType.getTypeName());
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
        StringReader reader = new StringReader(CONTENTS_2);

        VectorDataNodeReader vectorDataNodeReader = new VectorDataNodeReader(DefaultGeographicCRS.WGS84);
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
            expectException("FT\ta:Integer\n" +
                    "ID1\tX\n");
        } catch (IOException e) {
            // ok
        }

        try {
            expectException("FT\ta:Integer\n" +
                    "ID1\t\n");
        } catch (IOException e) {
            // ok
        }

        try {
            expectException("FT\ta:Integer\n" +
                    "ID1\t1234\tABC\n");
        } catch (IOException e) {
            // ok
        }
    }

    private void expectException(String contents) throws IOException {
        new VectorDataNodeReader(null).readFeatures(new StringReader(contents));
        fail("IOException expected");
    }
}
