package org.esa.snap.core.dataio.geocoding.forward;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class AntiMeridianLonInterpolatorTest {

    @Test
    public void testInterpolate_anti_meridian() {
        final TiePointSplineForward.AntiMeridianLonInterpolator interpolator = new TiePointSplineForward.AntiMeridianLonInterpolator();

        final double[][] longitudes = {
                {179.8645, -179.7243, -179.3128},
                {179.5499, 179.9598, -179.63},
                {179.2396, 179.648, -179.9431}
        };
        interpolator.setData(longitudes);

        double lon = interpolator.interpolate(0.5, 0.0);
        assertEquals(-179.92995625, lon, 1e-8);

        lon = interpolator.interpolate(1.5, 0.0);
        assertEquals(-179.51860625, lon, 1e-8);

        lon = interpolator.interpolate(0.5, 1.0);
        assertEquals(179.75479375, lon, 1e-8);

        lon = interpolator.interpolate(1.5, 1.0);
        assertEquals(-179.83515625, lon, 1e-8);
    }

    @Test
    public void testInterpolate_greenwich() {
        final TiePointSplineForward.AntiMeridianLonInterpolator interpolator = new TiePointSplineForward.AntiMeridianLonInterpolator();

        final double[][] longitudes = {
                {-1.5, -0.5, 0.5},
                {-1.4, -0.4, 0.6},
                {-1.3, -0.3, 0.7}
        };
        interpolator.setData(longitudes);

        double lon = interpolator.interpolate(0.5, 0.0);
        assertEquals(-1.0, lon, 1e-8);

        lon = interpolator.interpolate(1.5, 0.0);
        assertEquals(0.0, lon, 1e-8);

        lon = interpolator.interpolate(0.5, 1.0);
        assertEquals(-0.9, lon, 1e-8);

        lon = interpolator.interpolate(1.5, 1.0);
        assertEquals(0.1, lon, 1e-8);
    }
}
