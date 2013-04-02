package org.esa.beam.interpolators;

public interface Interpolator {

    InterpolatingFunction interpolate(double[] x, double[] y);
    int getMinNumPoints();

}
