package org.esa.snap.core.util;

import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.GeometryFactory;
import org.esa.snap.core.datamodel.Placemark;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureImpl;
import org.geotools.filter.identity.FeatureIdImpl;
import org.junit.Test;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Marco Peters
 */
public class ObservableFeatureCollectionTest {

    private GeometryFactory gf = new GeometryFactory();
    private SimpleFeatureType featureType = Placemark.createGeometryFeatureType();

    @Test
    public void testAddEvents() throws Exception {
        DefaultFeatureCollection sourceCollection = createTestCollection();
        ObservableFeatureCollection featureCollection = new ObservableFeatureCollection(sourceCollection);
        MyListener listener = new MyListener();
        featureCollection.addListener(listener);

        SimpleFeature singleFeature = createFeature("T1", new Envelope(1, 2, 3, 4));
        List<SimpleFeature> featureList = Arrays.asList(createFeature("T2", new Envelope(1, 2, 3, 4)),
                                                        createFeature("T3", new Envelope(1, 2, 3, 4)));
        featureCollection.add(singleFeature);
        featureCollection.addAll(featureList);

        assertEquals(6, featureCollection.size());
        assertEquals(3, sourceCollection.size());

        assertEquals(ObservableFeatureCollection.EVENT_TYPE.ADDED, listener.eventTypeList.get(0));
        assertEquals(1, listener.eventFeatureList.get(0).length);
        assertEquals(singleFeature, listener.eventFeatureList.get(0)[0]);

        assertEquals(ObservableFeatureCollection.EVENT_TYPE.ADDED, listener.eventTypeList.get(1));
        assertEquals(2, listener.eventFeatureList.get(1).length);
        assertEquals(featureList.get(0), listener.eventFeatureList.get(1)[0]);
        assertEquals(featureList.get(1), listener.eventFeatureList.get(1)[1]);

    }

    @Test
    public void testRemoveEvents() throws Exception {
        DefaultFeatureCollection sourceCollection = createTestCollection();
        SimpleFeature[] origFeatures = sourceCollection.toArray(new SimpleFeature[0]);
        ObservableFeatureCollection featureCollection = new ObservableFeatureCollection(sourceCollection);
        MyListener listener = new MyListener();

        SimpleFeature toBeRemoved = createFeature("T1", new Envelope(1, 2, 3, 4));
        List<SimpleFeature> retainFeatureList = Arrays.asList(createFeature("T2", new Envelope(1, 2, 3, 4)),
                                                              createFeature("T3", new Envelope(1, 2, 3, 4)));
        List<SimpleFeature> otherFeatureList = Arrays.asList(createFeature("T4", new Envelope(1, 2, 3, 4)),
                                                        createFeature("T5", new Envelope(1, 2, 3, 4)));
        featureCollection.add(toBeRemoved);
        featureCollection.addAll(retainFeatureList);
        featureCollection.addAll(otherFeatureList);

        featureCollection.addListener(listener);

        featureCollection.remove(toBeRemoved);
        featureCollection.removeAll(Arrays.asList(origFeatures));
        featureCollection.retainAll(retainFeatureList);

        for (SimpleFeature retainFeature : retainFeatureList) {
            assertTrue(featureCollection.contains(retainFeature));
        }

        for (SimpleFeature origFeature : origFeatures) {
            assertTrue(sourceCollection.contains(origFeature));
        }

        assertEquals(ObservableFeatureCollection.EVENT_TYPE.REMOVED, listener.eventTypeList.get(0));
        assertEquals(1, listener.eventFeatureList.get(0).length);
        assertEquals(toBeRemoved, listener.eventFeatureList.get(0)[0]);

        assertEquals(ObservableFeatureCollection.EVENT_TYPE.REMOVED, listener.eventTypeList.get(1));
        assertEquals(3, listener.eventFeatureList.get(1).length);
        assertEquals(origFeatures[0], listener.eventFeatureList.get(1)[0]);
        assertEquals(origFeatures[1], listener.eventFeatureList.get(1)[1]);
        assertEquals(origFeatures[2], listener.eventFeatureList.get(1)[2]);

        assertEquals(ObservableFeatureCollection.EVENT_TYPE.REMOVED, listener.eventTypeList.get(2));
        assertEquals(2, listener.eventFeatureList.get(2).length);
        assertTrue(otherFeatureList.contains(listener.eventFeatureList.get(2)[0]));
        assertTrue(otherFeatureList.contains(listener.eventFeatureList.get(2)[1]));

    }

    @Test
    public void testBounds() throws Exception {
        DefaultFeatureCollection collection = createTestCollection();
        ObservableFeatureCollection featureCollection = new ObservableFeatureCollection(collection);
        SimpleFeature feature = createFeature("T1", new Envelope(70, 730, -5, 20));

        assertEquals(new Envelope(0, 50, 0, 10), featureCollection.getBounds());
        featureCollection.add(feature);
        assertEquals(new Envelope(0, 730, -5, 20), featureCollection.getBounds());
        featureCollection.remove(feature);
        assertEquals(new Envelope(0, 50, 0, 10), featureCollection.getBounds());
    }

    private DefaultFeatureCollection createTestCollection() {
        final DefaultFeatureCollection collection = new DefaultFeatureCollection("testID", featureType);
        collection.add(createFeature("F1", new Envelope(0, 10, 0, 10)));
        collection.add(createFeature("F2", new Envelope(20, 30, 0, 10)));
        collection.add(createFeature("F3", new Envelope(40, 50, 0, 10)));
        return collection;
    }

    private SimpleFeature createFeature(String name, Envelope envelope) {
        Object[] data1 = {gf.toGeometry(envelope), "Area " + name};
        return new SimpleFeatureImpl(data1, featureType, new FeatureIdImpl(name), true);
    }


    private static class MyListener implements ObservableFeatureCollection.Listener {
        private List<ObservableFeatureCollection.EVENT_TYPE> eventTypeList;
        private List<SimpleFeature[]> eventFeatureList;

        private MyListener() {
            eventTypeList = new ArrayList<>();
            eventFeatureList = new ArrayList<>();
        }

        @Override
        public void changed(ObservableFeatureCollection.EVENT_TYPE type, SimpleFeature... features) {
            eventTypeList.add(type);
            eventFeatureList.add(features);
        }
    }
}