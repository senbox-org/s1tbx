package org.esa.snap.core.datamodel;

import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.junit.Test;
import org.opengis.feature.simple.SimpleFeatureType;

import static junit.framework.Assert.*;

public class ProductMaskTest {

    @Test
    public void testMaskGroupIsInitiallyEmpty() throws Exception {
        final Product p = new Product("P", "T", 1, 1);

        assertEquals(0, p.getMaskGroup().getNodeCount());
    }

    @Test
    public void testMaskIsAddedWhenPinIsAddedForTheFirstTime() throws Exception {
        final Product p = new Product("P", "T", 1, 1);
        p.getPinGroup().add(Placemark.createPointPlacemark(PinDescriptor.getInstance(), "a", "a", "a", new PixelPos(), new GeoPos(), null));

        assertEquals(1, p.getMaskGroup().getNodeCount());
        assertNotNull(p.getMaskGroup().get("pins"));
    }

    @Test
    public void testMaskIsNotAddedWhenPinIsAddedForTheSecondTime() throws Exception {
        final Product p = new Product("P", "T", 1, 1);
        p.getPinGroup().add(Placemark.createPointPlacemark(PinDescriptor.getInstance(), "a", "a", "a", new PixelPos(), new GeoPos(), null));
        p.getPinGroup().add(Placemark.createPointPlacemark(PinDescriptor.getInstance(), "b", "b", "b", new PixelPos(), new GeoPos(), null));

        assertEquals(1, p.getMaskGroup().getNodeCount());
        assertNotNull(p.getMaskGroup().get("pins"));
    }

    @Test
    public void testMaskIsRemovedWhenAllPinsAreRemoved() throws Exception {
        final Product p = new Product("P", "T", 1, 1);
        p.getPinGroup().add(Placemark.createPointPlacemark(PinDescriptor.getInstance(), "a", "a", "a", new PixelPos(), new GeoPos(), null));
        p.getPinGroup().removeAll();

        assertEquals(0, p.getMaskGroup().getNodeCount());
    }
    
    @Test
    public void testMaskIsAddedWhenGcpIsAddedForTheFirstTime() throws Exception {
        final Product p = new Product("P", "T", 1, 1);
        p.getGcpGroup().add(Placemark.createPointPlacemark(GcpDescriptor.getInstance(), "a", "a", "a", new PixelPos(), new GeoPos(), null));

        assertEquals(1, p.getMaskGroup().getNodeCount());
        assertNotNull(p.getMaskGroup().get("ground_control_points"));
    }

    @Test
    public void testMaskIsNotAddedWhenGcpIsAddedForTheSecondTime() throws Exception {
        final Product p = new Product("P", "T", 1, 1);
        p.getGcpGroup().add(Placemark.createPointPlacemark(GcpDescriptor.getInstance(), "a", "a", "a", new PixelPos(), new GeoPos(), null));
        p.getGcpGroup().add(Placemark.createPointPlacemark(GcpDescriptor.getInstance(), "b", "b", "b", new PixelPos(), new GeoPos(), null));

        assertEquals(1, p.getMaskGroup().getNodeCount());
        assertNotNull(p.getMaskGroup().get("ground_control_points"));
    }

    @Test
    public void testMaskIsRemovedWhenAllGcpsAreRemoved() throws Exception {
        final Product p = new Product("P", "T", 1, 1);
        p.getGcpGroup().add(Placemark.createPointPlacemark(GcpDescriptor.getInstance(), "a", "a", "a", new PixelPos(), new GeoPos(), null));
        p.getGcpGroup().removeAll();

        assertEquals(0, p.getMaskGroup().getNodeCount());
    }

    @Test
    public void testMaskIsNotAddedWhenEmptyVdnIsAdded() throws Exception {
        final Product p = new Product("P", "T", 1, 1);
        p.getVectorDataGroup().add(new VectorDataNode("V", new GeometryDescriptor().getBaseFeatureType()));

        assertEquals(0, p.getMaskGroup().getNodeCount());
    }

    @Test
    public void testMaskIsAddedWhenNonEmptyVdnIsAdded() throws Exception {
        final Product p = new Product("P", "T", 1, 1);
        final SimpleFeatureType featureType = new GeometryDescriptor().getBaseFeatureType();
        final VectorDataNode node = new VectorDataNode("V", featureType);
        node.getFeatureCollection().add(new SimpleFeatureBuilder(featureType).buildFeature("id"));
        p.getVectorDataGroup().add(node);

        assertEquals(1, p.getMaskGroup().getNodeCount());
        assertNotNull(p.getMaskGroup().get("V"));
    }

    @Test
    public void testMaskIsAddedWhenFeatureIsAddedForTheFirstTime() throws Exception {
        final Product p = new Product("P", "T", 1, 1);
        final SimpleFeatureType featureType = new GeometryDescriptor().getBaseFeatureType();
        final VectorDataNode node = new VectorDataNode("V", featureType);
        p.getVectorDataGroup().add(node);
        node.getFeatureCollection().add(new SimpleFeatureBuilder(featureType).buildFeature("id"));

        assertEquals(1, p.getMaskGroup().getNodeCount());
        assertNotNull(p.getMaskGroup().get("V"));
    }

    @Test
    public void testMaskIsNotAddedWhenFeatureIsAddedForTheSecondTime() throws Exception {
        final Product p = new Product("P", "T", 1, 1);
        final SimpleFeatureType featureType = new GeometryDescriptor().getBaseFeatureType();
        final VectorDataNode node = new VectorDataNode("V", featureType);
        p.getVectorDataGroup().add(node);
        node.getFeatureCollection().add(new SimpleFeatureBuilder(featureType).buildFeature("id1"));
        node.getFeatureCollection().add(new SimpleFeatureBuilder(featureType).buildFeature("id2"));

        assertEquals(1, p.getMaskGroup().getNodeCount());
        assertNotNull(p.getMaskGroup().get("V"));
    }

    @Test
    public void testMaskIsRemovedWhenAllFeaturesAreRemoved() throws Exception {
        final Product p = new Product("P", "T", 1, 1);
        final SimpleFeatureType featureType = new GeometryDescriptor().getBaseFeatureType();
        final VectorDataNode node = new VectorDataNode("V", featureType);
        p.getVectorDataGroup().add(node);
        node.getFeatureCollection().add(new SimpleFeatureBuilder(featureType).buildFeature("id1"));
        node.getFeatureCollection().add(new SimpleFeatureBuilder(featureType).buildFeature("id2"));
        node.getFeatureCollection().clear();

        assertEquals(0, p.getMaskGroup().getNodeCount());
    }

    @Test
    public void testMaskIsRemovedWhenVdnIsRemoved() throws Exception {
        final Product p = new Product("P", "T", 1, 1);
        final SimpleFeatureType featureType = new GeometryDescriptor().getBaseFeatureType();
        final VectorDataNode node = new VectorDataNode("V", featureType);
        p.getVectorDataGroup().add(node);
        node.getFeatureCollection().add(new SimpleFeatureBuilder(featureType).buildFeature("id1"));

        assertEquals(1, p.getMaskGroup().getNodeCount());

        p.getVectorDataGroup().remove(node);

        assertEquals(0, p.getMaskGroup().getNodeCount());
    }
}
