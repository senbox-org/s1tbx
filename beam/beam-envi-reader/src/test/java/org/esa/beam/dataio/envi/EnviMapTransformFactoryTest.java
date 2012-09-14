package org.esa.beam.dataio.envi;

import junit.framework.TestCase;
import org.esa.beam.framework.dataop.maptransf.MapTransform;

public class EnviMapTransformFactoryTest extends TestCase {

    public void testThrowsIllegalArgumentExceptionOnUnregisteredProjection() {
        final double[] parameter = new double[0];

        try {
            EnviMapTransformFactory.create(-11, parameter);
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException expected) {
        }
    }

    public void testCreateAlbersEqualAreaConic() {
        final double[] parameter = new double[]{
                1.0,    // a    semi major
                2.0,    // b    semi minor
                3.0,    // lat0 latitude of origin
                4.0,    // lon0 central meridian
                5.0,    // x0   false easting
                6.0,    // y0   false northing
                7.0,    // sp1  latitude of intersection 1
                8.0,    // sp2  latitude of intersection 2
        };

        final MapTransform transform = EnviMapTransformFactory.create(9, parameter);
        assertEquals("Albers Equal Area Conic", transform.getDescriptor().getName());
        final double[] parameterValues = transform.getParameterValues();
        assertEquals(9, parameterValues.length);
        assertEquals(1.0, parameterValues[0]);  // a    semi major
        assertEquals(2.0, parameterValues[1]);  // b    semi minor
        assertEquals(3.0, parameterValues[2]);  // lat0 latitude of origin
        assertEquals(4.0, parameterValues[3]);  // lon0 central meridian
        assertEquals(7.0, parameterValues[4]);  // sp1  latitude of intersection 1
        assertEquals(8.0, parameterValues[5]);  // sp2  latitude of intersection 2
        assertEquals(0.0, parameterValues[6]);  // scale factor - ignored
        assertEquals(5.0, parameterValues[7]);  // x0   false easting
        assertEquals(6.0, parameterValues[8]);  // y0   false northing
    }
}
