package org.esa.beam.framework.datamodel;

import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;
import org.opengis.feature.simple.SimpleFeatureType;

public class VectorDataGroupTest {

    private ProductNodeGroup<VectorDataNode> vectorDataGroup;
    private ProductNodeGroup<Mask> maskGroup;

    @Before
    public void setup() {
        final Product product = new Product("P", "T", 10, 10);
        vectorDataGroup = product.getVectorDataGroup();
        maskGroup = product.getMaskGroup();
    }

    @Test
    public void initialState() {
        assertEquals(2, vectorDataGroup.getNodeCount());
        assertEquals(2, maskGroup.getNodeCount());
    }

    @Test
    public void maskGroupIsCoupledWithVectorDataGroup() {
        final SimpleFeatureType featureType = getFeatureType();
        final VectorDataNode v = new VectorDataNode("V", featureType);

        vectorDataGroup.add(v);
        assertEquals(3, vectorDataGroup.getNodeCount());
        assertEquals(3, maskGroup.getNodeCount());
        assertSame(v, vectorDataGroup.get(2));
        assertTrue(maskGroup.contains(v.getName()));

        final VectorDataNode u = new VectorDataNode("U", featureType);
        vectorDataGroup.add(2, u);
        assertEquals(4, vectorDataGroup.getNodeCount());
        assertEquals(4, maskGroup.getNodeCount());
        assertSame(u, vectorDataGroup.get(2));
        assertSame(v, vectorDataGroup.get(3));
        assertTrue(maskGroup.contains(u.getName()));
        assertTrue(maskGroup.contains(v.getName()));

        vectorDataGroup.remove(u);
        assertEquals(3, vectorDataGroup.getNodeCount());
        assertEquals(3, maskGroup.getNodeCount());
        assertFalse(vectorDataGroup.contains(u));
        assertSame(v, vectorDataGroup.get(2));
        assertFalse(maskGroup.contains(u.getName()));
        assertTrue(maskGroup.contains(v.getName()));

        vectorDataGroup.removeAll();
        assertEquals(0, vectorDataGroup.getNodeCount());
        assertEquals(0, maskGroup.getNodeCount());
    }

    @Test
    public void cannotAddNullToVectorDataGroup() {
        try {
            vectorDataGroup.add(null);
            fail();
        } catch (NullPointerException expected) {
        }

        assertEquals(2, vectorDataGroup.getNodeCount());
        assertEquals(2, maskGroup.getNodeCount());
    }

    private SimpleFeatureType getFeatureType() {
        final SimpleFeatureTypeBuilder featureTypeBuilder = new SimpleFeatureTypeBuilder();
        featureTypeBuilder.setCRS(DefaultGeographicCRS.WGS84);
        featureTypeBuilder.setName("TestVectorType");

        return featureTypeBuilder.buildFeatureType();
    }
}
