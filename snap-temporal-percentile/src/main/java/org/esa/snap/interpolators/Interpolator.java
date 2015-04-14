package org.esa.snap.interpolators;

public interface Interpolator {

    InterpolatingFunction interpolate(double[] x, double[] y);
    int getMinNumPoints();

}
