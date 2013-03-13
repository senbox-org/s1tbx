package org.esa.beam.interpolators;

import org.esa.beam.statistics.percentile.interpolated.InterpolatedPercentileOp;

public class InterpolatorFactory {

    public static Interpolator createInterpolator(String interpolationMethod) {
        if (InterpolatedPercentileOp.P_CALCULATION_METHOD_LINEAR_INTERPOLATION.equalsIgnoreCase(interpolationMethod)) {
            return new LinearInterpolator();
        } else if (InterpolatedPercentileOp.P_CALCULATION_METHOD_SPLINE_INTERPOLATION.equalsIgnoreCase(interpolationMethod)) {
            return new SplineInterpolator();
        } else if (InterpolatedPercentileOp.P_CALCULATION_METHOD_QUADRATIC_INTERPOLATION.equalsIgnoreCase(interpolationMethod)) {
            return new QuadraticInterpolator();
        } else {
            return null;
        }
    }

}
