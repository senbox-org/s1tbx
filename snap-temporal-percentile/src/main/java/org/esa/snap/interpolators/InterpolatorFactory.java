package org.esa.snap.interpolators;

import org.esa.snap.statistics.percentile.interpolated.TemporalPercentileOp;

public class InterpolatorFactory {

    public static Interpolator createInterpolator(String interpolationMethod) {
        if (TemporalPercentileOp.GAP_FILLING_METHOD_LINEAR_INTERPOLATION.equalsIgnoreCase(interpolationMethod)) {
            return new LinearInterpolator();
        } else if (TemporalPercentileOp.GAP_FILLING_METHOD_SPLINE_INTERPOLATION.equalsIgnoreCase(interpolationMethod)) {
            return new SplineInterpolator();
        } else if (TemporalPercentileOp.GAP_FILLING_METHOD_QUADRATIC_INTERPOLATION.equals(interpolationMethod)){
            return new QuadraticInterpolator();
        } else {
            return new Interpolator() {
                @Override
                public InterpolatingFunction interpolate(double[] x, double[] y) {
                    return null;
                }

                @Override
                public int getMinNumPoints() {
                    return 1;
                }
            };
        }
    }
}
