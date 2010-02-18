package org.esa.beam.framework.datamodel;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import static org.junit.Assert.*;
import org.junit.Test;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.operation.TransformException;


public class VectorDataNodeTest {
    @Test
    public void testVectorData() throws TransformException, FactoryException {
        SimpleFeatureType pinType = createPlacemarkFeatureType("PinType", "pixelPoint");
        SimpleFeatureType gcpType = createPlacemarkFeatureType("GcpType", "geoPoint");
        testVectorData(new VectorDataNode("Pins", pinType), "Pins", pinType);
        testVectorData(new VectorDataNode("GCPs", gcpType), "GCPs", gcpType);
    }

    @Test
    public void testVectorDataGroup() throws TransformException, FactoryException {
        Product p = new Product("p", "pt", 512, 512);
        assertEquals(2, p.getVectorDataGroup().getNodeCount());

        SimpleFeatureType pinType = createPlacemarkFeatureType("PinType", "pixelPoint");
        SimpleFeatureType gcpType = createPlacemarkFeatureType("GcpType", "geoPoint");

        p.getVectorDataGroup().add(new VectorDataNode("Pins2", pinType));
        p.getVectorDataGroup().add(new VectorDataNode("GCPs2", gcpType));
        assertEquals(4, p.getVectorDataGroup().getNodeCount());

        testVectorData(p, "Pins2", pinType);
        testVectorData(p, "GCPs2", gcpType);
    }

    private static void testVectorData(Product p, String expectedName, SimpleFeatureType expectedType) {
        VectorDataNode pins = p.getVectorDataGroup().get(expectedName);
        assertNotNull(pins);
        testVectorData(pins, expectedName, expectedType);
    }

    private static void testVectorData(VectorDataNode vectorDataNode, String expectedName, SimpleFeatureType expectedType) {
        assertEquals(expectedName, vectorDataNode.getName());
        assertNotNull(vectorDataNode.getFeatureCollection());
        assertSame(expectedType, vectorDataNode.getFeatureType());
        assertSame(expectedType, vectorDataNode.getFeatureCollection().getSchema());
    }

    private static SimpleFeatureType createPlacemarkFeatureType(String typeName, String defaultGeometryName) {
        SimpleFeatureTypeBuilder ftb = new SimpleFeatureTypeBuilder();
        DefaultGeographicCRS crs = DefaultGeographicCRS.WGS84;
        ftb.setCRS(crs);
        ftb.setName(typeName);
        ftb.add("label", String.class);
        ftb.add("geoPoint", Point.class, crs);
        ftb.add("pixelPoint", Point.class, crs);
        ftb.setDefaultGeometry(defaultGeometryName);
        return ftb.buildFeatureType();
    }

    private static SimpleFeature createFeature(SimpleFeatureType type, GeoPos geoPos, PixelPos pixelPos) {
        SimpleFeatureBuilder fb = new SimpleFeatureBuilder(type);
        GeometryFactory gf = new GeometryFactory();
        String id = "P_" + System.nanoTime();
        fb.add(id);
        fb.add(gf.createPoint(new Coordinate(geoPos.lon, geoPos.lat)));
        fb.add(gf.createPoint(new Coordinate(pixelPos.x, pixelPos.y)));
        return fb.buildFeature(id);
    }
}
