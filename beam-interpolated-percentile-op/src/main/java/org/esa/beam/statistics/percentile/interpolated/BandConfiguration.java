package org.esa.beam.statistics.percentile.interpolated;

import org.esa.beam.framework.gpf.annotations.Parameter;

public class BandConfiguration {

    @Parameter(description = "The name of the band in the source products. If empty, parameter 'expression' must " +
                             "be provided.")
    String sourceBandName;

    @Parameter(description = "The percentiles.",
               defaultValue = "90")
    int[] percentiles = new int[]{90};

    @Parameter(description = "The interpolation method.",
               defaultValue = "linear",
               valueSet = {"linear", "spline"}
//               valueSet = {"linear", "spline", "quadratic"}
    )
    String interpolationMethod = "linear";

    @Parameter(description =
                           "The fallback start value for time series interpolation if there is no interpolation start " +
                           "value in cases of cloudy areas in the oldest input product.",
               defaultValue = "0.0")
    Double startValueFallback = 0.0;

    @Parameter(description =
                           "The fallback end value for time series interpolation if there is no interpolation end " +
                           "value in cases of cloudy areas in the newest input product.",
               defaultValue = "0.0")
    Double endValueFallback = 0.0;

    @Parameter(description = "The band maths expression serving as criterion for whether to consider pixels for " +
                             "computation.")
    String validPixelExpression;

    // todo use fallback or use first and last available value
    public BandConfiguration() {
        // used by DOM converter
    }
}
