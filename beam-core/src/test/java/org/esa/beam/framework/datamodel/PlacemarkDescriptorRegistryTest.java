package org.esa.beam.framework.datamodel;

import com.vividsolutions.jts.geom.Point;
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
        assertNotNull(registry.getPlacemarkDescriptor("org.esa.beam.framework.datamodel.DefaultPlacemarkDescriptor"));

        Set<PlacemarkDescriptor> descriptors = registry.getPlacemarkDescriptors();
        assertNotNull(descriptors);
        assertTrue("expected placemarkDescriptors.length > 4, but was " + descriptors.size(),
                   descriptors.size() >= 4);
        assertTrue(descriptors.contains(registry.getPlacemarkDescriptor("org.esa.beam.framework.datamodel.PinDescriptor")));
        assertTrue(descriptors.contains(registry.getPlacemarkDescriptor("org.esa.beam.framework.datamodel.GcpDescriptor")));
        assertTrue(descriptors.contains(registry.getPlacemarkDescriptor("org.esa.beam.framework.datamodel.GeometryDescriptor")));
        assertTrue(descriptors.contains(registry.getPlacemarkDescriptor("org.esa.beam.framework.datamodel.DefaultPlacemarkDescriptor")));
    }

    @Test
    public void testThatPlacemarkDescriptorsAreFoundForPinFeatureType() throws Exception {
        hahaha("org.esa.beam.Pin", "org.esa.beam.framework.datamodel.PinDescriptor");
    }

    @Test
    public void testThatPlacemarkDescriptorsAreFoundForGcpFeatureType() throws Exception {
        hahaha("org.esa.beam.GroundControlPoint", "org.esa.beam.framework.datamodel.GcpDescriptor");
    }

    @Test
    public void testThatPlacemarkDescriptorsAreFoundForGeometryFeatureType() throws Exception {
        hahaha("org.esa.beam.Geometry", "org.esa.beam.framework.datamodel.GeometryDescriptor");
    }

    @Test
    public void testThatPlacemarkDescriptorsAreFoundForYetUnknownFeatureType() throws Exception {
        PlacemarkDescriptorRegistry registry = new PlacemarkDescriptorRegistry();

        SimpleFeatureType ft = createYetUnknownFeatureType();

        List<PlacemarkDescriptor> descriptors = registry.getPlacemarkDescriptors(ft);
        assertNotNull(descriptors);
        assertEquals(1, descriptors.size());

        assertSame(registry.getPlacemarkDescriptor("org.esa.beam.framework.datamodel.DefaultPlacemarkDescriptor"),
                   descriptors.get(0));
    }

    private void hahaha(String featureTypeName, String className) {
        PlacemarkDescriptorRegistry registry = new PlacemarkDescriptorRegistry();

        SimpleFeatureType ft = Placemark.createPointFeatureType(featureTypeName);

        List<PlacemarkDescriptor> descriptors = registry.getPlacemarkDescriptors(ft);
        assertNotNull(descriptors);
        assertEquals(2, descriptors.size());

        assertSame(registry.getPlacemarkDescriptor(className),
                   descriptors.get(0));
        assertSame(registry.getPlacemarkDescriptor("org.esa.beam.framework.datamodel.DefaultPlacemarkDescriptor"),
                   descriptors.get(1));
    }

    public static SimpleFeatureType createYetUnknownFeatureType() {
        SimpleFeatureTypeBuilder sftb = new SimpleFeatureTypeBuilder();
        AttributeTypeBuilder atb = new AttributeTypeBuilder();

        atb.setBinding(Point.class);
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
