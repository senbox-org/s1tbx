package org.esa.beam.framework.datamodel;

import com.vividsolutions.jts.geom.Polygon;
import org.geotools.feature.AttributeTypeBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.junit.Test;
import org.opengis.feature.simple.SimpleFeatureType;

import java.util.List;
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
        assertNull(registry.getPlacemarkDescriptor("org.esa.beam.framework.datamodel.GenericPlacemarkDescriptor"));

        Set<PlacemarkDescriptor> descriptors = registry.getPlacemarkDescriptors();
        assertNotNull(descriptors);
        assertTrue("expected placemarkDescriptors.length >= 3, but was " + descriptors.size(),
                   descriptors.size() >= 3);
        assertTrue(descriptors.contains(registry.getPlacemarkDescriptor("org.esa.beam.framework.datamodel.PinDescriptor")));
        assertTrue(descriptors.contains(registry.getPlacemarkDescriptor("org.esa.beam.framework.datamodel.GcpDescriptor")));
        assertTrue(descriptors.contains(registry.getPlacemarkDescriptor("org.esa.beam.framework.datamodel.GeometryDescriptor")));
        assertFalse(descriptors.contains(registry.getPlacemarkDescriptor("org.esa.beam.framework.datamodel.GenericPlacemarkDescriptor")));
    }

    @Test
    public void testThatPlacemarkDescriptorIsFoundForPinFeatureType() throws Exception {
        testThatPlacemarkDescriptorIsFound("org.esa.beam.Pin", "org.esa.beam.framework.datamodel.PinDescriptor", 3);
    }

    @Test
    public void testThatPlacemarkDescriptorIsFoundForGcpFeatureType() throws Exception {
        testThatPlacemarkDescriptorIsFound("org.esa.beam.GroundControlPoint", "org.esa.beam.framework.datamodel.GcpDescriptor", 3);
    }

    @Test
    public void testThatPlacemarkDescriptorIsFoundForGeometryFeatureType() throws Exception {
        testThatPlacemarkDescriptorIsFound("org.esa.beam.Geometry", "org.esa.beam.framework.datamodel.GeometryDescriptor", 2);
    }

    @Test
    public void testThatPlacemarkDescriptorIsNotFoundForYetUnknownFeatureType() throws Exception {
        PlacemarkDescriptorRegistry registry = new PlacemarkDescriptorRegistry();

        SimpleFeatureType ft = createYetUnknownFeatureType();

        List<PlacemarkDescriptor> descriptors = registry.getValidPlacemarkDescriptors(ft);
        assertNotNull(descriptors);
        assertEquals(1, descriptors.size());
    }

    private void testThatPlacemarkDescriptorIsFound(String featureTypeName, String className, int expected) {
        PlacemarkDescriptorRegistry registry = new PlacemarkDescriptorRegistry();

        SimpleFeatureType ft = Placemark.createPointFeatureType(featureTypeName);

        List<PlacemarkDescriptor> descriptors = registry.getValidPlacemarkDescriptors(ft);
        assertNotNull(descriptors);
        assertEquals(expected, descriptors.size());

        assertTrue(descriptors.contains(registry.getPlacemarkDescriptor(className)));
    }

    public static SimpleFeatureType createYetUnknownFeatureType() {
        SimpleFeatureTypeBuilder sftb = new SimpleFeatureTypeBuilder();
        AttributeTypeBuilder atb = new AttributeTypeBuilder();

        atb.setBinding(Polygon.class);
        atb.nillable(false);
        sftb.add(atb.buildDescriptor("PT"));
        sftb.setDefaultGeometry("PT");

        atb.setBinding(String.class);
        sftb.add(atb.buildDescriptor("TXT"));

        atb.setBinding(String.class);
        sftb.add(atb.buildDescriptor("LAB"));

        sftb.setName("FT_" + System.nanoTime());
        return sftb.buildFeatureType();
    }

}
