package org.esa.snap.core.dataio.geocoding.util;

import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;

public class SplineInterpolatorTest {

    @Test
    public void testGetSecondDerivatives_3() {
        final double[] x = new double[]{0., 1., 2.};
        final double[] y = new double[]{7.4096, 8.8793, 10.8336};

        final double[] derivatives = SplineInterpolator.getSecondDerivatives(x, y);
        assertEquals(3, derivatives.length);
        assertEquals(0.0, derivatives[0], 1e-8);
        assertEquals(1.4537999999999984, derivatives[1], 1e-8);
        assertEquals(0.0, derivatives[2], 1e-8);
    }

    @Test
    public void testGetSecondDerivatives_3_AMSUB() {
        final double[] x = new double[]{0., 1., 2.};
        final double[] y = new double[]{-73.5887, -73.5118, -73.4339};

        final double[] derivatives = SplineInterpolator.getSecondDerivatives(x, y);
        assertEquals(3, derivatives.length);
        assertEquals(0.0, derivatives[0], 1e-8);
        assertEquals(0.002999999999971692, derivatives[1], 1e-8);
        assertEquals(0.0, derivatives[2], 1e-8);
    }

    @Test
    public void testGetSecondDerivatives_3_AMSRE() {
        final double[] x = new double[]{0., 1., 2.};
        final double[] y = new double[]{0.18938532, 0.21874169, 0.24794407};

        final double[] derivatives = SplineInterpolator.getSecondDerivatives(x, y);
        assertEquals(3, derivatives.length);
        assertEquals(0.0, derivatives[0], 1e-8);
        assertEquals(-4.6196999999997823E-4, derivatives[1], 1e-8);
        assertEquals(0.0, derivatives[2], 1e-8);
    }

    @Test
    public void testGetSecondDerivatives_4() {
        final double[] x = new double[]{10., 11., 12., 13.};
        final double[] y = new double[]{18.58608, 18.558794, 18.531345, 18.503735};

        final double[] derivatives = SplineInterpolator.getSecondDerivatives(x, y);
        assertEquals(4, derivatives.length);
        assertEquals(0.0, derivatives[0], 1e-8);
        assertEquals(-4.008499999862636E-4, derivatives[1], 1e-8);
        assertEquals(-3.526000000192653E-4, derivatives[2], 1e-8);
        assertEquals(0.0, derivatives[3], 1e-8);
    }

    @Test
    public void testGetSecondDerivative() {
        final double[] y = new double[]{7.4096, 8.8793, 10.8336f};

        final double derivative = SplineInterpolator.getSecondDerivative(y);
        assertEquals(1.4538001327514616, derivative, 1e-8);
    }

    @Test
    public void testGetSecondDerivative_AMSUB() {
        final double[] y = new double[]{-73.5887, -73.5118, -73.4339};

        final double derivative = SplineInterpolator.getSecondDerivative(y);
        assertEquals(0.002999999999971692, derivative, 1e-8);
    }

    @Test
    public void testGetSecondDerivative_AMSRE() {
        final double[] y = new double[]{0.18938532, 0.21874169, 0.24794407};

        final double derivative = SplineInterpolator.getSecondDerivative(y);
        assertEquals(-4.6196999999997823E-4, derivative, 1e-8);
    }

    @Test
    public void testInterpolate() {
        final double[] y = new double[]{7.4096, 8.8793, 10.8336};
        final double deriv = 1.453798770904541;

        double y_int = SplineInterpolator.interpolate(y, deriv, 0.0);
        assertEquals(7.4096, y_int, 1e-8);

        y_int = SplineInterpolator.interpolate(y, deriv, 0.5);
        assertEquals(8.053587576818467, y_int, 1e-8);

        y_int = SplineInterpolator.interpolate(y, deriv, 1.0);
        assertEquals(8.8793, y_int, 1e-8);

        y_int = SplineInterpolator.interpolate(y, deriv, 1.5);
        assertEquals(9.765587576818467, y_int, 1e-8);

        y_int = SplineInterpolator.interpolate(y, deriv, 2.0);
        assertEquals(10.8336, y_int, 1e-8);
    }

    @Test
    public void testInterpolate_AMSUB() {
        final double[] y = new double[]{-11.4373, -11.6285, -11.8411};
        final double deriv = -0.06419849395751953;

        double y_int = SplineInterpolator.interpolate(y, deriv, 0.0);
        assertEquals(-11.4373, y_int, 1e-8);

        y_int = SplineInterpolator.interpolate(y, deriv, 0.5);
        assertEquals(-11.528887594127657, y_int, 1e-8);

        y_int = SplineInterpolator.interpolate(y, deriv, 1.0);
        assertEquals(-11.6285, y_int, 1e-8);

        y_int = SplineInterpolator.interpolate(y, deriv, 1.5);
        assertEquals(-11.7307875941276554, y_int, 1e-8);

        y_int = SplineInterpolator.interpolate(y, deriv, 2.0);
        assertEquals(-11.8411, y_int, 1e-8);
    }

    @Test
    public void testInterpolate2d_AMSUB() {
        final double[] latitudes = new double[]{84.4789, 84.6487, 84.8217,
                84.5111, 84.6818, 84.856,
                84.5389, 84.7105, 84.8856};

        final double[] derivatives = new double[3];
        derivatives[0] = SplineInterpolator.getSecondDerivative(Arrays.copyOfRange(latitudes, 0, 3));
        derivatives[1] = SplineInterpolator.getSecondDerivative(Arrays.copyOfRange(latitudes, 3, 6));
        derivatives[2] = SplineInterpolator.getSecondDerivative(Arrays.copyOfRange(latitudes, 6, 9));

        double lat_int = SplineInterpolator.interpolate2d(latitudes, derivatives, 0.5, 0.5);
        assertEquals(84.58031132812499, lat_int, 1e-8);

        lat_int = SplineInterpolator.interpolate2d(latitudes, derivatives, 1.5, 0.5);
        assertEquals(84.75226445312501, lat_int, 1e-8);

        lat_int = SplineInterpolator.interpolate2d(latitudes, derivatives, 0.5, 1.5);
        assertEquals(84.610733203125, lat_int, 1e-8);

        lat_int = SplineInterpolator.interpolate2d(latitudes, derivatives, 1.5, 1.5);
        assertEquals(84.78366132812499, lat_int, 1e-8);

        lat_int = SplineInterpolator.interpolate2d(latitudes, derivatives, 0.0, 0.0);
        assertEquals(84.4789, lat_int, 1e-8);

        lat_int = SplineInterpolator.interpolate2d(latitudes, derivatives, 2.0, 1.0);
        assertEquals(84.856, lat_int, 1e-8);

        lat_int = SplineInterpolator.interpolate2d(latitudes, derivatives, 1.0, 2.0);
        assertEquals(84.7105, lat_int, 1e-8);
    }

    @Test
    public void testInterpolate2d_noDerivatives_AMSUB() {
        final double[][] latitudes = new double[][]{{84.4789, 84.6487, 84.8217},
                {84.5111, 84.6818, 84.856},
                {84.5389, 84.7105, 84.8856}};

        double lat_int = SplineInterpolator.interpolate2d(latitudes, 0.5, 0.5);
        assertEquals(84.58031132812499, lat_int, 1e-8);

        lat_int = SplineInterpolator.interpolate2d(latitudes, 1.5, 0.5);
        assertEquals(84.75226445312501, lat_int, 1e-8);

        lat_int = SplineInterpolator.interpolate2d(latitudes, 0.5, 1.5);
        assertEquals(84.610733203125, lat_int, 1e-8);

        lat_int = SplineInterpolator.interpolate2d(latitudes, 1.5, 1.5);
        assertEquals(84.78366132812499, lat_int, 1e-8);

        lat_int = SplineInterpolator.interpolate2d(latitudes, 0.0, 0.0);
        assertEquals(84.4789, lat_int, 1e-8);

        lat_int = SplineInterpolator.interpolate2d(latitudes, 2.0, 1.0);
        assertEquals(84.856, lat_int, 1e-8);

        lat_int = SplineInterpolator.interpolate2d(latitudes, 1.0, 2.0);
        assertEquals(84.7105, lat_int, 1e-8);
    }

    @Test
    public void testInterpolate_3() {
        final double[] x = new double[]{0., 1., 2.};
        final double[] y = new double[]{7.4096, 8.8793, 10.8336};
        final double[] deriv = new double[]{0.0, 1.453798770904541, 0.0};

        double y_int = SplineInterpolator.interpolate(x, y, deriv, 0.0);
        assertEquals(7.4096, y_int, 1e-8);

        y_int = SplineInterpolator.interpolate(x, y, deriv, 0.5);
        assertEquals(8.053587576818467, y_int, 1e-8);

        y_int = SplineInterpolator.interpolate(x, y, deriv, 1.0);
        assertEquals(8.8793, y_int, 1e-8);

        y_int = SplineInterpolator.interpolate(x, y, deriv, 1.5);
        assertEquals(9.765587576818467, y_int, 1e-8);

        y_int = SplineInterpolator.interpolate(x, y, deriv, 2.0);
        assertEquals(10.8336, y_int, 1e-8);
    }

    @Test
    public void testInterpolate_3_AMSUB() {
        final double[] x = new double[]{0., 1., 2.};
        final double[] y = new double[]{-11.4373, -11.6285, -11.8411};
        final double[] deriv = SplineInterpolator.getSecondDerivatives(x, y);

        double y_int = SplineInterpolator.interpolate(x, y, deriv, 0.0);
        assertEquals(-11.4373, y_int, 1e-8);

        y_int = SplineInterpolator.interpolate(x, y, deriv, 0.5);
        assertEquals(-11.528887500000002, y_int, 1e-8);

        y_int = SplineInterpolator.interpolate(x, y, deriv, 1.0);
        assertEquals(-11.6285, y_int, 1e-8);

        y_int = SplineInterpolator.interpolate(x, y, deriv, 1.5);
        assertEquals(-11.7307875, y_int, 1e-8);

        y_int = SplineInterpolator.interpolate(x, y, deriv, 2.0);
        assertEquals(-11.8411, y_int, 1e-8);
    }

    @Test
    public void testInterpolate_4() {
        final double[] x = new double[]{10., 11., 12., 13.};
        final double[] y = new double[]{18.58608, 18.558794, 18.531345, 18.503735};
        final double[] deriv = new double[]{0.0, -3.972053527832031E-4, -3.566741943359375E-4, 0.0};

        double y_int = SplineInterpolator.interpolate(x, y, deriv, 10.0);
        assertEquals(18.58608, y_int, 1e-8);

        y_int = SplineInterpolator.interpolate(x, y, deriv, 10.5);
        assertEquals(18.57246182533455, y_int, 1e-8);

        y_int = SplineInterpolator.interpolate(x, y, deriv, 11.0);
        assertEquals(18.558794, y_int, 1e-8);

        y_int = SplineInterpolator.interpolate(x, y, deriv, 11.5);
        assertEquals(18.5451166174716958, y_int, 1e-8);

        y_int = SplineInterpolator.interpolate(x, y, deriv, 12.0);
        assertEquals(18.531345, y_int, 1e-8);
    }
}
