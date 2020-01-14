package org.esa.snap.core.dataio.geocoding.util;

import org.esa.snap.core.util.math.FXYSum;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.mock;

public class ApproximationTest {

    @Test
    public void testConstructionAndGetter() {
        final FXYSum fx = mock(FXYSum.class);
        final FXYSum fy = mock(FXYSum.class);

        final Approximation approximation = new Approximation(fx, fy, 22.3, 44.5, 77.8);
        assertSame(fx, approximation.getFX());
        assertSame(fy, approximation.getFY());
        assertEquals(22.3, approximation.getCenterLat(), 1e-8);
        assertEquals(44.5, approximation.getCenterLon(), 1e-8);
        assertEquals(77.8, approximation.getMinSquareDistance(), 1e-8);
    }

    @Test
    public void testGetSquareDistance() {
        final Approximation approximation = new Approximation(null, null, 20.0, 30.0, 77.8);

        assertEquals(0.0, approximation.getSquareDistance(20.0, 30.0), 1e-8);
        assertEquals(4.0, approximation.getSquareDistance(22.0, 30.0), 1e-8);
        assertEquals(9.0, approximation.getSquareDistance(20.0, 33.0), 1e-8);
        assertEquals(2.0, approximation.getSquareDistance(19.0, 31.0), 1e-8);
    }
}
