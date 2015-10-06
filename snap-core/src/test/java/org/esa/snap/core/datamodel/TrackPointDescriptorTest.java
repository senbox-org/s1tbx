package org.esa.snap.core.datamodel;

import com.vividsolutions.jts.geom.Point;
import org.esa.snap.core.dataio.DecodeQualification;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.junit.Test;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

import static org.junit.Assert.*;

/**
 * @author Norman Fomferra
 */
public class TrackPointDescriptorTest {

    private PlacemarkDescriptor instance = TrackPointDescriptor.getInstance();

    @Test
    public void testGetInstance() throws Exception {
        assertNotNull(instance);
        assertSame(instance, TrackPointDescriptor.getInstance());
        assertSame(TrackPointDescriptor.class, instance.getClass());
    }

    @Test
    public void testGetBaseFeatureType() throws Exception {
        SimpleFeatureType ft = instance.getBaseFeatureType();
        assertNotNull(ft);
        assertEquals("org.esa.snap.TrackPoint", ft.getTypeName());
        assertEquals(7, ft.getAttributeCount());
        assertEquals("geometry", ft.getGeometryDescriptor().getLocalName());
    }

    @Test
    public void testGetQualification() throws Exception {
        SimpleFeatureType ft = instance.getBaseFeatureType();
        assertEquals(DecodeQualification.SUITABLE, instance.getCompatibilityFor(ft));

        ft.getUserData().put("trackPoints", "true");
        assertEquals(DecodeQualification.INTENDED, instance.getCompatibilityFor(ft));

        final SimpleFeatureType ft2 = createCompatibleFT("org.esa.snap.TrackPoint");
        assertEquals(DecodeQualification.SUITABLE, instance.getCompatibilityFor(ft2));

        ft2.getUserData().put("trackPoints", "true");
        assertEquals(DecodeQualification.INTENDED, instance.getCompatibilityFor(ft2));

        // no geometry
        SimpleFeatureTypeBuilder ftb = new SimpleFeatureTypeBuilder();
        ftb.setName("org.esa.snap.Pineapple");
        assertEquals(DecodeQualification.UNABLE, instance.getCompatibilityFor(ftb.buildFeatureType()));
    }

    @Test
    public void testSetUserData() throws Exception {
        SimpleFeatureType ft1 = createCompatibleFT("org.esa.snap.Pineapple");
        assertEquals(0, ft1.getUserData().size());
        instance.setUserDataOf(ft1);
        assertEquals(3, ft1.getUserData().size());
        assertEquals("geometry", ft1.getUserData().get("defaultGeometry"));
        assertEquals("true", ft1.getUserData().get("trackPoints"));
        assertEquals(TrackPointDescriptor.class.getName(), ft1.getUserData().get("placemarkDescriptor"));

        SimpleFeatureType ft2 = createCompatibleFT("org.esa.snap.Pin");
        assertEquals(0, ft2.getUserData().size());
        instance.setUserDataOf(ft2);
        assertEquals(3, ft2.getUserData().size());
        assertEquals("geometry", ft2.getUserData().get("defaultGeometry"));
        assertEquals("true", ft2.getUserData().get("trackPoints"));
        assertEquals(TrackPointDescriptor.class.getName(), ft2.getUserData().get("placemarkDescriptor"));
    }

    @Test
    public void testCreatePlacemark() throws Exception {
        final SimpleFeatureType ft = instance.getBaseFeatureType();
        SimpleFeatureBuilder fb = new SimpleFeatureBuilder(ft);
        final SimpleFeature f = fb.buildFeature("id1", new Object[ft.getAttributeCount()]);
        final Placemark placemark = instance.createPlacemark(f);

        assertNotNull(placemark);
        assertSame(f, placemark.getFeature());
        assertSame(instance, placemark.getDescriptor());
    }

    @Test
    public void testDeprecatedProperties() throws Exception {
        final Product product = new Product("n", "t", 1, 1);
        assertNull(instance.getPlacemarkGroup(product));
        assertEquals("track_point", instance.getRoleName());
        assertEquals("track point", instance.getRoleLabel());
        assertNull(instance.getCursorImage());
        assertNotNull(instance.getCursorHotSpot());
        assertEquals(null, instance.getShowLayerCommandId());
    }

    @Test
    public void testUpdatePixelPos() throws Exception {

    }

    @Test
    public void testUpdateGeoPos() throws Exception {

    }

    SimpleFeatureType createCompatibleFT(String name) {
        SimpleFeatureTypeBuilder ftb = new SimpleFeatureTypeBuilder();
        ftb.setName(name);
        ftb.add("geometry", Point.class);
        ftb.setDefaultGeometry("geometry");
        return ftb.buildFeatureType();
    }
}
