package org.esa.beam.framework.datamodel;

import org.junit.Test;
import org.opengis.feature.simple.SimpleFeatureType;

import java.util.Set;

import static org.junit.Assert.*;

/**
 * @author Norman Fomferra
 * @since BEAM 4.10
 */
public class PlacemarkDescriptorRegistryTest {

    @Test
    public void testThatRegistryHostsKnownPlacemarkDescriptors() throws Exception {
        PlacemarkDescriptorRegistry registry = new PlacemarkDescriptorRegistry();
        assertNotNull(registry);
        assertNotNull(registry.getPlacemarkDescriptor("org.esa.beam.framework.datamodel.PinDescriptor"));
        assertNotNull(registry.getPlacemarkDescriptor("org.esa.beam.framework.datamodel.GcpDescriptor"));
        assertNotNull(registry.getPlacemarkDescriptor("org.esa.beam.framework.datamodel.GeometryDescriptor"));

        Set<PlacemarkDescriptor> descriptors = registry.getPlacemarkDescriptors();
        assertNotNull(descriptors);
        assertTrue("expected placemarkDescriptors.length > 3, but was " + descriptors.size(),
                   descriptors.size() >= 3);
        assertTrue(descriptors.contains(registry.getPlacemarkDescriptor("org.esa.beam.framework.datamodel.PinDescriptor")));
        assertTrue(descriptors.contains(registry.getPlacemarkDescriptor("org.esa.beam.framework.datamodel.GcpDescriptor")));
        assertTrue(descriptors.contains(registry.getPlacemarkDescriptor("org.esa.beam.framework.datamodel.GeometryDescriptor")));
    }

    @Test
    public void testThatPlacemarkDescriptorsAreFoundForPinFeatureType() throws Exception {
        PlacemarkDescriptorRegistry registry = new PlacemarkDescriptorRegistry();

        SimpleFeatureType ft = Placemark.createFeatureType("org.esa.beam.Pin");

        Set<PlacemarkDescriptor> descriptors = registry.getPlacemarkDescriptors(ft);
        assertNotNull(descriptors);
        // Note: for time being, we only expect one matching descriptor (since VectorDataNode can only handle one)
        assertEquals(1, descriptors.size());

        assertSame(registry.getPlacemarkDescriptor("org.esa.beam.framework.datamodel.PinDescriptor"),
                   descriptors.iterator().next());
    }

    @Test
    public void testThatPlacemarkDescriptorsAreFoundForGcpFeatureType() throws Exception {
        PlacemarkDescriptorRegistry registry = new PlacemarkDescriptorRegistry();

        SimpleFeatureType ft = Placemark.createFeatureType("org.esa.beam.GroundControlPoint");

        Set<PlacemarkDescriptor> descriptors = registry.getPlacemarkDescriptors(ft);
        assertNotNull(descriptors);
        // Note: for time being, we only expect one matching descriptor (since VectorDataNode can only handle one)
        assertEquals(1, descriptors.size());

        assertSame(registry.getPlacemarkDescriptor("org.esa.beam.framework.datamodel.GcpDescriptor"),
                   descriptors.iterator().next());
    }

    @Test
    public void testThatPlacemarkDescriptorsAreFoundForGeometryFeatureType() throws Exception {
        PlacemarkDescriptorRegistry registry = new PlacemarkDescriptorRegistry();

        SimpleFeatureType ft = PlainFeatureFactory.createDefaultFeatureType();

        Set<PlacemarkDescriptor> descriptors = registry.getPlacemarkDescriptors(ft);
        assertNotNull(descriptors);
        // Note: for time being, we only expect one matching descriptor (since VectorDataNode can only handle one)
        assertEquals(1, descriptors.size());

        assertSame(registry.getPlacemarkDescriptor("org.esa.beam.framework.datamodel.GeometryDescriptor"),
                   descriptors.iterator().next());
    }
}
