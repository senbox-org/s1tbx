package org.esa.snap.core.datamodel;

import com.bc.ceres.core.DefaultServiceRegistry;
import com.vividsolutions.jts.geom.Polygon;
import org.esa.snap.core.dataio.DecodeQualification;
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
        PlacemarkDescriptorRegistry registry = PlacemarkDescriptorRegistry.getInstance();
        assertNotNull(registry);
        assertNotNull(registry.getPlacemarkDescriptor("org.esa.snap.core.datamodel.PinDescriptor"));
        assertNotNull(registry.getPlacemarkDescriptor("org.esa.snap.core.datamodel.GcpDescriptor"));
        assertNotNull(registry.getPlacemarkDescriptor("org.esa.snap.core.datamodel.GeometryDescriptor"));
        assertNull(registry.getPlacemarkDescriptor("org.esa.snap.core.datamodel.GenericPlacemarkDescriptor"));

        Set<PlacemarkDescriptor> descriptors = registry.getPlacemarkDescriptors();
        assertNotNull(descriptors);
        assertTrue("expected placemarkDescriptors.length >= 3, but was " + descriptors.size(),
                   descriptors.size() >= 3);
        assertTrue(descriptors.contains(registry.getPlacemarkDescriptor("org.esa.snap.core.datamodel.PinDescriptor")));
        assertTrue(descriptors.contains(registry.getPlacemarkDescriptor("org.esa.snap.core.datamodel.GcpDescriptor")));
        assertTrue(descriptors.contains(registry.getPlacemarkDescriptor("org.esa.snap.core.datamodel.GeometryDescriptor")));
        assertFalse(descriptors.contains(registry.getPlacemarkDescriptor("org.esa.snap.core.datamodel.GenericPlacemarkDescriptor")));
    }

    @Test
    public void testThatPlacemarkDescriptorIsFoundForPinFeatureType() throws Exception {
        testThatPlacemarkDescriptorIsFound("org.esa.snap.Pin", "org.esa.snap.core.datamodel.PinDescriptor", 3);
    }

    @Test
    public void testThatPlacemarkDescriptorIsFoundForGcpFeatureType() throws Exception {
        testThatPlacemarkDescriptorIsFound("org.esa.snap.GroundControlPoint", "org.esa.snap.core.datamodel.GcpDescriptor", 3);
    }

    @Test
    public void testThatPlacemarkDescriptorIsFoundForGeometryFeatureType() throws Exception {
        testThatPlacemarkDescriptorIsFound("org.esa.snap.Geometry", "org.esa.snap.core.datamodel.GeometryDescriptor", 2);
    }

    @Test
    public void testThatPlacemarkDescriptorIsNotFoundForYetUnknownFeatureType() throws Exception {
        PlacemarkDescriptorRegistry registry = PlacemarkDescriptorRegistry.getInstance();

        SimpleFeatureType ft = createYetUnknownFeatureType();

        List<PlacemarkDescriptor> descriptors = registry.getPlacemarkDescriptors(ft);
        assertNotNull(descriptors);
        assertEquals(1, descriptors.size());
    }

    @Test
    public void testOrderOfValidPlacemarkDescriptors() throws Exception {
        DefaultServiceRegistry<PlacemarkDescriptor> serviceRegistry = new DefaultServiceRegistry<PlacemarkDescriptor>(PlacemarkDescriptor.class);

        PlacemarkDescriptor first = new IntendedPlacemarkDescriptorWithPlacemarkDescriptorProperty();
        PlacemarkDescriptor second = new IntendedPlacemarkDescriptorWithoutProperty();
        PlacemarkDescriptor third = new SuitablePlacemarkDescriptor_1();
        PlacemarkDescriptor fourth = new SuitablePlacemarkDescriptor_2();

        serviceRegistry.addService(fourth);
        serviceRegistry.addService(second);
        serviceRegistry.addService(first);
        serviceRegistry.addService(third);

        PlacemarkDescriptorRegistry registry = new PlacemarkDescriptorRegistry(serviceRegistry);

        SimpleFeatureTypeBuilder builder = new SimpleFeatureTypeBuilder();
        builder.setName("n");
        SimpleFeatureType featureType = builder.buildFeatureType();

        List<PlacemarkDescriptor> descriptors = registry.getPlacemarkDescriptors(featureType);

        assertEquals(4, descriptors.size());

        // test that when two intended descriptors are registrated, the one having the property set is first in the list
        PlacemarkDescriptor descriptor = descriptors.get(0);
        assertSame(DecodeQualification.INTENDED, descriptor.getCompatibilityFor(null));
        assertSame(first, descriptor);

        // test that any other intended descriptors come before the suitable ones
        descriptor = descriptors.get(1);
        assertSame(DecodeQualification.INTENDED, descriptor.getCompatibilityFor(null));
        assertSame(second, descriptor);

        descriptor = descriptors.get(2);
        assertSame(DecodeQualification.SUITABLE, descriptor.getCompatibilityFor(null));
        assertTrue(descriptor == third || descriptor == fourth);

        descriptor = descriptors.get(3);
        assertSame(DecodeQualification.SUITABLE, descriptor.getCompatibilityFor(null));
        assertTrue(descriptor == third || descriptor == fourth);
    }

    @Test
    public void testGetBestPlacemarkDescriptor_Intended() throws Exception {
        DefaultServiceRegistry<PlacemarkDescriptor> serviceRegistry = new DefaultServiceRegistry<PlacemarkDescriptor>(PlacemarkDescriptor.class);

        PlacemarkDescriptor first = new IntendedPlacemarkDescriptorWithPlacemarkDescriptorProperty();
        PlacemarkDescriptor second = new IntendedPlacemarkDescriptorWithoutProperty();
        PlacemarkDescriptor third = new SuitablePlacemarkDescriptor_1();
        PlacemarkDescriptor fourth = new SuitablePlacemarkDescriptor_2();

        serviceRegistry.addService(fourth);
        serviceRegistry.addService(second);
        serviceRegistry.addService(first);
        serviceRegistry.addService(third);

        PlacemarkDescriptorRegistry registry = new PlacemarkDescriptorRegistry(serviceRegistry);


        PlacemarkDescriptor bestPlacemarkDescriptor = registry.getPlacemarkDescriptor((SimpleFeatureType) null);
        assertSame(first, bestPlacemarkDescriptor);
    }

    @Test
    public void testGetBestPlacemarkDescriptor_Suitable() throws Exception {
        DefaultServiceRegistry<PlacemarkDescriptor> serviceRegistry = new DefaultServiceRegistry<PlacemarkDescriptor>(PlacemarkDescriptor.class);

        PlacemarkDescriptor first = new SuitablePlacemarkDescriptor_1();
        PlacemarkDescriptor second = new SuitablePlacemarkDescriptor_2();

        serviceRegistry.addService(first);
        serviceRegistry.addService(second);

        PlacemarkDescriptorRegistry registry = new PlacemarkDescriptorRegistry(serviceRegistry);

        PlacemarkDescriptor bestPlacemarkDescriptor = registry.getPlacemarkDescriptor((SimpleFeatureType) null);
        assertTrue(bestPlacemarkDescriptor == first || bestPlacemarkDescriptor == second);
    }

    private void testThatPlacemarkDescriptorIsFound(String featureTypeName, String className, int expected) {
        PlacemarkDescriptorRegistry registry = PlacemarkDescriptorRegistry.getInstance();

        SimpleFeatureType ft = Placemark.createPointFeatureType(featureTypeName);

        List<PlacemarkDescriptor> descriptors = registry.getPlacemarkDescriptors(ft);
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

    private static class IntendedPlacemarkDescriptorWithPlacemarkDescriptorProperty extends AbstractPlacemarkDescriptor {

        @Override
        public DecodeQualification getCompatibilityFor(SimpleFeatureType featureType) {
            return DecodeQualification.INTENDED;
        }

        @Override
        public SimpleFeatureType getBaseFeatureType() {
            SimpleFeatureTypeBuilder builder = new SimpleFeatureTypeBuilder();
            builder.setName("kalle grabowski");
            SimpleFeatureType simpleFeatureType = builder.buildFeatureType();
            simpleFeatureType.getUserData().put(PlacemarkDescriptorRegistry.PROPERTY_NAME_PLACEMARK_DESCRIPTOR, "blub");
            return simpleFeatureType;
        }

        @Override
        public String getRoleName() {
            return null;
        }

        @Override
        public String getRoleLabel() {
            return null;
        }
    }

    private class IntendedPlacemarkDescriptorWithoutProperty extends AbstractPlacemarkDescriptor {
        @Override
        public DecodeQualification getCompatibilityFor(SimpleFeatureType featureType) {
            return DecodeQualification.INTENDED;
        }

        @Override
        public SimpleFeatureType getBaseFeatureType() {
            SimpleFeatureTypeBuilder builder = new SimpleFeatureTypeBuilder();
            builder.setName("werner kampmann");
            return builder.buildFeatureType();
        }

        @Override
        public String getRoleName() {
            return null;
        }

        @Override
        public String getRoleLabel() {
            return null;
        }
    }

    private class SuitablePlacemarkDescriptor_1 extends AbstractPlacemarkDescriptor {
        @Override
        public DecodeQualification getCompatibilityFor(SimpleFeatureType featureType) {
            return DecodeQualification.SUITABLE;
        }

        @Override
        public SimpleFeatureType getBaseFeatureType() {
            SimpleFeatureTypeBuilder builder = new SimpleFeatureTypeBuilder();
            builder.setName("keek");
            return builder.buildFeatureType();
        }

        @Override
        public String getRoleName() {
            return null;
        }

        @Override
        public String getRoleLabel() {
            return null;
        }
    }

    private class SuitablePlacemarkDescriptor_2 extends AbstractPlacemarkDescriptor {
        @Override
        public DecodeQualification getCompatibilityFor(SimpleFeatureType featureType) {
            return DecodeQualification.SUITABLE;
        }

        @Override
        public SimpleFeatureType getBaseFeatureType() {
            SimpleFeatureTypeBuilder builder = new SimpleFeatureTypeBuilder();
            builder.setName("schlucke");
            return builder.buildFeatureType();
        }

        @Override
        public String getRoleName() {
            return null;
        }

        @Override
        public String getRoleLabel() {
            return null;
        }
    }
}
