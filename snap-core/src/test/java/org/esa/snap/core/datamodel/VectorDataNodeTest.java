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

package org.esa.snap.core.datamodel;

import org.esa.snap.core.util.ObservableFeatureCollection;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.junit.Test;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.operation.TransformException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;


public class VectorDataNodeTest {
    @Test
    public void testVectorData() throws TransformException, FactoryException {
        SimpleFeatureType pinType = Placemark.createPinFeatureType();
        SimpleFeatureType gcpType = Placemark.createGcpFeatureType();
        SimpleFeatureType unknownType = PlacemarkDescriptorRegistryTest.createYetUnknownFeatureType();
        testVectorData(new VectorDataNode("Pins", pinType), "Pins", pinType);
        testVectorData(new VectorDataNode("GCPs", gcpType), "GCPs", gcpType);
        testVectorData(new VectorDataNode("Imported", unknownType), "Imported", unknownType);
    }

    @Test
    public void testVectorDataNodeAndPlacemarkGroup() {
        Product p = new Product("p", "pt", 512, 512);
        ProductNodeGroup<VectorDataNode> vectorDataGroup = p.getVectorDataGroup();
        Placemark placemark = Placemark.createPointPlacemark(PointDescriptor.getInstance(), "placemark_1", null, null,
                                                             new PixelPos(10, 10), null, null);

        VectorDataNode vectorDataNode = new VectorDataNode("Features", Placemark.createPointFeatureType("feature"));
        DefaultFeatureCollection featureCollection = vectorDataNode.getFeatureCollection();
        vectorDataGroup.add(vectorDataNode);        //Also: Sets the owner of the vectorDataNode
        vectorDataNode.getPlacemarkGroup();         //Also: Creates the PlacemarkGroup (owner has to be set!)

        featureCollection.add(placemark.getFeature());

        assertEquals(1, vectorDataNode.getFeatureCollection().size());
        assertEquals(vectorDataNode.getFeatureCollection().size(), vectorDataNode.getPlacemarkGroup().getNodeCount());
    }

    @Test
    public void testVectorDataGroup() throws TransformException, FactoryException {
        Product p = new Product("p", "pt", 512, 512);
        assertEquals(2, p.getVectorDataGroup().getNodeCount());

        SimpleFeatureType pinType = Placemark.createPinFeatureType();
        SimpleFeatureType gcpType = Placemark.createGcpFeatureType();

        p.getVectorDataGroup().add(new VectorDataNode("My Pins", pinType));
        p.getVectorDataGroup().add(new VectorDataNode("My GCPs", gcpType));
        assertEquals(4, p.getVectorDataGroup().getNodeCount());

        testVectorData(p, "My Pins", pinType);
        testVectorData(p, "My GCPs", gcpType);
    }

    private static void testVectorData(Product p, String expectedName, SimpleFeatureType expectedType) {
        VectorDataNode pins = p.getVectorDataGroup().get(expectedName);
        assertNotNull(pins);
        testVectorData(pins, expectedName, expectedType);
    }

    private static void testVectorData(VectorDataNode vectorDataNode, String expectedName, SimpleFeatureType expectedType) {
        assertNotNull(vectorDataNode.getPlacemarkDescriptor());
        assertEquals(expectedName, vectorDataNode.getName());
        assertNotNull(vectorDataNode.getFeatureCollection());
        assertSame(expectedType, vectorDataNode.getFeatureType());
        assertSame(expectedType, vectorDataNode.getFeatureCollection().getSchema());
    }


    @Test
    public void testDefaultStyle() {
        SimpleFeatureType unknownType = PlacemarkDescriptorRegistryTest.createYetUnknownFeatureType();
        Product p = new Product("p", "pt", 4, 4);
        VectorDataNode vdn = new VectorDataNode("vdn", unknownType);
        p.getVectorDataGroup().add(vdn);

        MyProductNodeListenerAdapter pnl = new MyProductNodeListenerAdapter();
        p.addProductNodeListener(pnl);

        String styleCss = vdn.getStyleCss();

        assertNotNull(vdn.getDefaultStyleCss());
        vdn.setDefaultStyleCss("fill:#aabbcc");
        assertEquals("fill:#aabbcc", vdn.getDefaultStyleCss());
        assertNotNull(pnl.event);
        assertEquals("defaultStyleCss", pnl.event.getPropertyName());
        assertEquals("fill:#aabbcc", pnl.event.getNewValue());
        pnl.event = null;
        vdn.setDefaultStyleCss("fill:#aabbcc");
        assertNull(pnl.event);
        vdn.setDefaultStyleCss("fill:#000000");
        assertNotNull(pnl.event);
        assertEquals("defaultStyleCss", pnl.event.getPropertyName());
        assertEquals("fill:#000000", pnl.event.getNewValue());

        // test that styleCss is not affected
        assertEquals(styleCss, vdn.getStyleCss());
    }

    @Test
    public void testStyle() {
        SimpleFeatureType unknownType = PlacemarkDescriptorRegistryTest.createYetUnknownFeatureType();
        Product p = new Product("p", "pt", 4, 4);
        VectorDataNode vdn = new VectorDataNode("vdn", unknownType);
        p.getVectorDataGroup().add(vdn);

        MyProductNodeListenerAdapter pnl = new MyProductNodeListenerAdapter();
        p.addProductNodeListener(pnl);

        String defaultStyleCss = vdn.getDefaultStyleCss();

        assertNull(vdn.getStyleCss());

        vdn.setStyleCss("fill:#aabbcc");
        assertEquals("fill:#aabbcc", vdn.getStyleCss());
        assertNotNull(pnl.event);
        assertEquals("styleCss", pnl.event.getPropertyName());
        assertEquals("fill:#aabbcc", pnl.event.getNewValue());
        pnl.event = null;
        vdn.setStyleCss("fill:#aabbcc");
        assertNull(pnl.event);
        vdn.setStyleCss("fill:#000000");
        assertNotNull(pnl.event);
        assertEquals("styleCss", pnl.event.getPropertyName());
        assertEquals("fill:#000000", pnl.event.getNewValue());

        // test that defaultStyleCss is not affected
        assertEquals(defaultStyleCss, vdn.getDefaultStyleCss());
    }

    @Test
    public void testFireFeatureChanged() throws Exception {
        SimpleFeatureType type = createFeatureType();
        SimpleFeatureBuilder fb = new SimpleFeatureBuilder(type);

        VectorDataNode vdn = new VectorDataNode("test", type);
        Product p = new Product("dummy", "type", 10, 10);
        p.getVectorDataGroup().add(vdn);
        MyProductNodeListenerAdapter listener = new MyProductNodeListenerAdapter();
        p.addProductNodeListener(listener);

        SimpleFeature aFeature = fb.buildFeature("1");
        SimpleFeature toBeRemoved = fb.buildFeature("2");
        vdn.getFeatureCollection().add(aFeature);
        assertEquals(VectorDataNode.PROPERTY_NAME_FEATURE_COLLECTION, listener.event.getPropertyName());
        assertEquals(aFeature, ((SimpleFeature[]) listener.event.getNewValue())[0]);
        vdn.getFeatureCollection().add(toBeRemoved);
        assertEquals(VectorDataNode.PROPERTY_NAME_FEATURE_COLLECTION, listener.event.getPropertyName());
        assertEquals(toBeRemoved, ((SimpleFeature[]) listener.event.getNewValue())[0]);
        vdn.getFeatureCollection().remove(toBeRemoved);
        assertEquals(VectorDataNode.PROPERTY_NAME_FEATURE_COLLECTION, listener.event.getPropertyName());
        assertEquals(toBeRemoved, ((SimpleFeature[]) listener.event.getOldValue())[0]);
    }

    private static class MyProductNodeListenerAdapter extends ProductNodeListenerAdapter {
        ProductNodeEvent event;

        @Override
        public void nodeChanged(ProductNodeEvent event) {
            this.event = event;
        }
    }

    private static SimpleFeatureType createFeatureType() throws IOException {
        SimpleFeatureTypeBuilder ftb = new SimpleFeatureTypeBuilder();
        ftb.add("height", Integer.class);
        ftb.setName("my.special.typename");
        final SimpleFeatureType ft = ftb.buildFeatureType();
        ft.getUserData().put("trackPoints", "true");
        return ft;
    }

    private static class MyCollectionListener implements ObservableFeatureCollection.Listener {

        List<Event> eventList = new ArrayList<>();

        @Override
        public void changed(ObservableFeatureCollection.EVENT_TYPE type, SimpleFeature... features) {
            eventList.add(new Event(type, features));
        }
    }

    private static class Event  {
        ObservableFeatureCollection.EVENT_TYPE type;
        SimpleFeature[] features;

        public Event(ObservableFeatureCollection.EVENT_TYPE type, SimpleFeature[] features) {
            this.type = type;
            this.features = features;
        }
    }
}
