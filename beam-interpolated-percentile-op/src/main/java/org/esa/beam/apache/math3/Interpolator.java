package org.esa.beam.apache.math3;

public interface Interpolator {

    PolynomialSplineFunction interpolate(double[] x, double[] y);

}
