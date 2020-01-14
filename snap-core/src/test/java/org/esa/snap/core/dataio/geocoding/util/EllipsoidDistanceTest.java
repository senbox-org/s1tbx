package org.esa.snap.core.dataio.geocoding.util;

import org.geotools.referencing.datum.DefaultEllipsoid;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class EllipsoidDistanceTest {

    @Test
    public void testDistance_frontPole_WGS84() {
        final EllipsoidDistance distance = new EllipsoidDistance(0.0, 0.0, DefaultEllipsoid.WGS84);

        assertEquals(0.0, distance.distance(0.0, 0.0), 1e-8);

        // distances 1 deg along the greenwich meridian north and south must be the same
        assertEquals(110573.1381278223, distance.distance(0.0, 1.0), 1e-8);
        assertEquals(110573.1381278223, distance.distance(0.0, -1.0), 1e-8);

        // distances 1 deg along the equator east and west must be the same
        assertEquals(111319.49079327357, distance.distance(1.0, 0.0), 1e-8);
        assertEquals(111319.49079327357, distance.distance(-1.0, 0.0), 1e-8);
    }

    @Test
    public void testDistance_Berlin_Tokio_WGS84() {
        // this is the original example from Wikipedia for the simplified distance on an ellipsoid:
        // https://de.wikipedia.org/wiki/Orthodrome
        // 2019-09-1
        final EllipsoidDistance distance = new EllipsoidDistance(13.4, 52.51666666667, DefaultEllipsoid.WGS84);

        assertEquals(8941202.504586853, distance.distance(139.76666666667, 35.7), 1e-8);
    }

    @Test
    public void testDistance_Berlin_Tokio_GRS80() {
        final EllipsoidDistance distance = new EllipsoidDistance(13.4, 52.51666666667, DefaultEllipsoid.GRS80);

        assertEquals(8941202.504646894, distance.distance(139.76666666667, 35.7), 1e-8);
    }
}
