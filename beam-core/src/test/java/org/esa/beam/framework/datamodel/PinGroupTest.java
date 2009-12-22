package org.esa.beam.framework.datamodel;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.impl.CoordinateArraySequence;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.junit.Before;
import org.junit.Test;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

import static org.junit.Assert.*;

public class PinGroupTest {

    private SimpleFeatureBuilder pinBuilder;
    private PinGroup pinGroup;
    private FeatureCollection<SimpleFeatureType, SimpleFeature> pinFeatureCollection;

    @Before()
    public void setup() {
        Product product = new Product("PinGroup Test", "TestType", 10, 10);
        VectorDataNode pinVectorDataNode = new VectorDataNode("pins", Pin.getPinFeatureType());
        product.getVectorDataGroup().add(pinVectorDataNode);
        pinBuilder = new SimpleFeatureBuilder(Pin.getPinFeatureType());
        pinGroup = new PinGroup(product, "pinGroup", pinVectorDataNode);
        pinFeatureCollection = pinVectorDataNode.getFeatureCollection();

    }

    @Test
    public void testManipulatingPinGroup() {
        assertGroupCollectionAreEqual(pinGroup, pinFeatureCollection);

        pinGroup.add(createPin("p1", new PixelPos(3, 1), new GeoPos(12, 34)));
        assertGroupCollectionAreEqual(pinGroup, pinFeatureCollection);
        final Pin pin2 = createPin("p2", new PixelPos(5, 4), new GeoPos(16, 48));
        pinGroup.add(pin2);
        assertGroupCollectionAreEqual(pinGroup, pinFeatureCollection);
        pinGroup.add(createPin("p3", new PixelPos(6, 2), new GeoPos(-45, 80)));
        assertGroupCollectionAreEqual(pinGroup, pinFeatureCollection);

        pinGroup.remove(pin2);
        assertGroupCollectionAreEqual(pinGroup, pinFeatureCollection);

        pinGroup.add(1, createPin("p4", new PixelPos(6, 3), new GeoPos(-60, 47)));
        assertGroupCollectionAreEqual(pinGroup, pinFeatureCollection);
    }

    @Test
    public void testManipulatingVectorDataNode() {
        assertGroupCollectionAreEqual(pinGroup, pinFeatureCollection);

        pinFeatureCollection.add(createPinFeature());
        assertGroupCollectionAreEqual(pinGroup, pinFeatureCollection);
        final SimpleFeature simpleFeature = createPinFeature();
        pinFeatureCollection.add(simpleFeature);
        assertGroupCollectionAreEqual(pinGroup, pinFeatureCollection);
        pinFeatureCollection.add(createPinFeature());
        assertGroupCollectionAreEqual(pinGroup, pinFeatureCollection);

        pinFeatureCollection.remove(simpleFeature);
        assertGroupCollectionAreEqual(pinGroup, pinFeatureCollection);

        pinFeatureCollection.add(createPinFeature());
        assertGroupCollectionAreEqual(pinGroup, pinFeatureCollection);
    }

    @Test
    public void testChangingFeature() {
        pinGroup.add(createPin("p1", new PixelPos(3, 1), new GeoPos(12, 34)));
        pinGroup.add(createPin("p2", new PixelPos(5, 4), new GeoPos(16, 48)));
        pinGroup.add(createPin("p3", new PixelPos(6, 2), new GeoPos(-45, 80)));
        assertGroupCollectionAreEqual(pinGroup, pinFeatureCollection);
        final CoordinateArraySequence coordinates = new CoordinateArraySequence(new Coordinate[]{new Coordinate(-30, 70)});
        final SimpleFeature changedFeature = pinGroup.get(2).getFeature();
        changedFeature.setDefaultGeometry(new Point(coordinates, new GeometryFactory()));
        assertGroupCollectionAreEqual(pinGroup, pinFeatureCollection);

        final SimpleFeature[] features = pinFeatureCollection.toArray(new SimpleFeature[pinFeatureCollection.size()]);
        assertEquals(changedFeature.getDefaultGeometry(), features[2].getDefaultGeometry());
    }

    private SimpleFeature createPinFeature() {
        return pinBuilder.buildFeature(String.valueOf(System.currentTimeMillis()));
    }

    private static void assertGroupCollectionAreEqual(PinGroup pinGroup,
                                                      FeatureCollection<SimpleFeatureType, SimpleFeature> pinFeatureCollection) {
        final SimpleFeature[] features = pinFeatureCollection.toArray(new SimpleFeature[pinFeatureCollection.size()]);
        assertEquals(pinGroup.getNodeCount(), features.length);
        for (int i = 0; i < pinGroup.getNodeCount(); i++) {
            SimpleFeature pinGroupFeature = pinGroup.get(i).getFeature();
            assertTrue("Feature of pin group is not contained in feature collection\n",
                       pinFeatureCollection.contains(pinGroupFeature));

        }
    }

    private Pin createPin(String name, PixelPos pixelPos, GeoPos geoPos) {
        return new Pin(name, "", "", pixelPos, geoPos, PinDescriptor.INSTANCE.createDefaultSymbol());
    }
}
