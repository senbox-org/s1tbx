package org.esa.snap.core.dataio.geocoding;

import org.esa.snap.core.dataio.geocoding.forward.PixelForward;
import org.esa.snap.core.dataio.geocoding.forward.PixelInterpolatingForward;
import org.esa.snap.core.dataio.geocoding.forward.TiePointBilinearForward;
import org.esa.snap.core.dataio.geocoding.forward.TiePointSplineForward;
import org.esa.snap.core.dataio.geocoding.inverse.PixelGeoIndexInverse;
import org.esa.snap.core.dataio.geocoding.inverse.PixelQuadTreeInverse;
import org.esa.snap.core.dataio.geocoding.inverse.TiePointInverse;
import org.junit.Test;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class ComponentFactoryTest {

    @Test
    public void testGetForward() {
        ForwardCoding forwardCoding = ComponentFactory.getForward("FWD_PIXEL");
        assertTrue(forwardCoding instanceof PixelForward);

        forwardCoding = ComponentFactory.getForward("FWD_PIXEL_INTERPOLATING");
        assertTrue(forwardCoding instanceof PixelInterpolatingForward);

        forwardCoding = ComponentFactory.getForward("FWD_TIE_POINT_BILINEAR");
        assertTrue(forwardCoding instanceof TiePointBilinearForward);

        forwardCoding = ComponentFactory.getForward("FWD_TIE_POINT_SPLINE");
        assertTrue(forwardCoding instanceof TiePointSplineForward);
    }

    @Test
    public void testGetForward_unknown_key() {
        try {
            ComponentFactory.getForward("what_the_hell??");
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException expected) {
        }
    }

    @Test
    public void testGetInverse() {
        InverseCoding inverseCoding = ComponentFactory.getInverse("INV_PIXEL_QUAD_TREE");
        assertTrue(inverseCoding instanceof PixelQuadTreeInverse);

        inverseCoding = ComponentFactory.getInverse("INV_PIXEL_QUAD_TREE_INTERPOLATING");
        assertTrue(inverseCoding instanceof PixelQuadTreeInverse);

        inverseCoding = ComponentFactory.getInverse("INV_PIXEL_GEO_INDEX");
        assertTrue(inverseCoding instanceof PixelGeoIndexInverse);

        inverseCoding = ComponentFactory.getInverse("INV_PIXEL_GEO_INDEX_INTERPOLATING");
        assertTrue(inverseCoding instanceof PixelGeoIndexInverse);

        inverseCoding = ComponentFactory.getInverse("INV_TIE_POINT");
        assertTrue(inverseCoding instanceof TiePointInverse);
    }

    @Test
    public void testGetInverse_unknown_key() {
        try {
            ComponentFactory.getInverse("yo man, whassup?");
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException expected) {
        }
    }
}
